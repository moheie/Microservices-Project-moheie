<template>
  <div class="notification-bell position-relative">
    <button class="btn btn-link position-relative" @click="toggleNotifications">
      <i class="bi bi-bell-fill fs-4"></i>
      <span v-if="unreadCount > 0" 
            class="badge bg-danger rounded-pill notification-badge">
        {{ unreadCount }}
      </span>
    </button>
    
    <transition name="fade">
      <div v-if="showNotifications" class="notification-dropdown shadow">
        <div class="notification-header">
          <h6 class="m-0">Notifications</h6>
          <div class="d-flex gap-2">
            <button v-if="notifications.length > 0" 
                    class="btn btn-sm btn-link text-primary"
                    @click="markAllAsRead">
              Mark all as read
            </button>
            <button class="btn btn-sm btn-link text-secondary"
                    @click="refreshNotifications">
              <i class="bi bi-arrow-clockwise"></i>
            </button>
          </div>
        </div>
        
        <div class="notification-body">
          <div v-if="loading" class="text-center py-3">
            <div class="spinner-border text-primary spinner-border-sm" role="status">
              <span class="visually-hidden">Loading...</span>
            </div>
          </div>
          
          <template v-else-if="notifications.length > 0">
            <div v-for="notification in sortedNotifications"
                 :key="notification.id"
                 class="notification-item"
                 :class="{ 
                   'unread': !notification.read,
                   [getNotificationClass(notification.type)]: true
                 }"
                 @click="markAsRead(notification)">
              <div class="notification-icon">
                <i :class="getNotificationIcon(notification.type)"></i>
              </div>
              <div class="notification-content">
                <div class="notification-title">{{ notification.title }}</div>
                <div class="notification-message">{{ notification.message }}</div>
                <div class="notification-time">
                  {{ formatTime(notification.timestamp || notification.time) }}
                </div>
              </div>
            </div>
          </template>
          
          <div v-else class="no-notifications">
            <i class="bi bi-inbox text-muted fs-4 mb-2"></i>
            <p class="text-muted mb-0">No notifications available</p>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import WebSocketService from '@/services/WebSocketService';
import { getAuthHeaders } from '@/utils/auth';

export default {
  name: "NotificationBell",
  
  props: {
    userType: {
      type: String,
      required: true,
      validator: value => ['admin', 'seller', 'customer'].includes(value)
    }
  },

  data() {
    return {
      showNotifications: false,
      notifications: [],
      unreadCount: 0,
      loading: false,
      wsConnected: false,
      lastRefresh: null
    };
  },

  computed: {
    sortedNotifications() {
      return [...this.notifications].sort((a, b) => {
        // Sort by read status first (unread first)
        if (a.read !== b.read) return a.read ? 1 : -1;
        
        // Then sort by timestamp
        const timeA = new Date(a.timestamp || a.time);
        const timeB = new Date(b.timestamp || b.time);
        return timeB - timeA;
      });
    }
  },

  mounted() {
    this.setupNotificationSystem();
    this.loadNotifications();
    
    // Close dropdown when clicking outside
    document.addEventListener('click', this.handleOutsideClick);
    
    // Refresh notifications periodically (every 5 minutes)
    this.refreshInterval = setInterval(this.refreshNotifications, 300000);
  },

  beforeUnmount() {
    document.removeEventListener('click', this.handleOutsideClick);
    clearInterval(this.refreshInterval);
    this.disconnectWebSocket();
  },
  
  methods: {    
    setupNotificationSystem() {
      if (!WebSocketService.isConnected()) {
        const headers = getAuthHeaders();
        if (!headers.Authorization) {
          console.warn('No auth token available for notifications');
          return;
        }

        console.log(`Connecting to notification service as ${this.userType}...`);
        WebSocketService.connect(this.userType, headers)
          .then(() => {
            console.log('Successfully connected to notification service');
            this.wsConnected = true;
            this.setupSubscriptions();
          })
          .catch(error => {
            console.error('Failed to connect to notification service:', error);
            this.wsConnected = false;
          });
      } else {
        this.wsConnected = true;
        this.setupSubscriptions();
      }
    },

    setupSubscriptions() {
      // Subscribe to general notifications
      WebSocketService.subscribe('*', this.handleNotification);
      
      // Subscribe to user type specific notifications
      WebSocketService.subscribe(this.userType, this.handleNotification);
      
      // Special subscriptions for different user types
      if (this.userType === 'admin') {
        WebSocketService.subscribe('error', this.handleNotification);
        WebSocketService.subscribe('system', this.handleNotification);
      } else if (this.userType === 'seller') {
        WebSocketService.subscribe('stock', this.handleNotification);
        WebSocketService.subscribe('order', this.handleNotification);
      } else if (this.userType === 'customer') {
        WebSocketService.subscribe('order', this.handleNotification);
        WebSocketService.subscribe('payment', this.handleNotification);
      }
    },

    disconnectWebSocket() {
      if (this.wsConnected) {
        WebSocketService.unsubscribe('*', this.handleNotification);
        WebSocketService.unsubscribe(this.userType, this.handleNotification);
        
        if (this.userType === 'admin') {
          WebSocketService.unsubscribe('error', this.handleNotification);
          WebSocketService.unsubscribe('system', this.handleNotification);
        } else if (this.userType === 'seller') {
          WebSocketService.unsubscribe('stock', this.handleNotification);
          WebSocketService.unsubscribe('order', this.handleNotification);
        } else if (this.userType === 'customer') {
          WebSocketService.unsubscribe('order', this.handleNotification);
          WebSocketService.unsubscribe('payment', this.handleNotification);
        }
      }
    },

    handleNotification(notification) {
      console.log('Received notification:', notification);
      this.addNotification(notification);
    },

    addNotification(notification) {
      // Add timestamp if not present
      const notificationWithTime = {
        ...notification,
        timestamp: notification.timestamp || new Date().toISOString(),
        read: false
      };
      
      // Add to start of array and update unread count
      this.notifications.unshift(notificationWithTime);
      this.unreadCount++;
      
      // Show a native browser notification if the dropdown is not visible
      if (!this.showNotifications) {
        this.showBrowserNotification(notification);
      }
    },

    async markAsRead(notification) {
      if (!notification.read) {
        try {
          const headers = getAuthHeaders();
          const response = await fetch(
            `http://localhost:8085/api/notifications/${notification.id}/read`,
            {
              method: 'PATCH',
              headers
            }
          );

          if (response.ok) {
            notification.read = true;
            this.unreadCount = Math.max(0, this.unreadCount - 1);
          }
        } catch (error) {
          console.error('Error marking notification as read:', error);
        }
      }
    },

    async markAllAsRead() {
      try {
        const headers = getAuthHeaders();
        const token = headers.Authorization?.split(' ')[1];
        if (!token) return;

        const userId = WebSocketService.extractUserIdFromToken(token);
        if (!userId) return;

        const response = await fetch(
          `http://localhost:8085/api/notifications/read-all/by-user/${userId}`,
          {
            method: 'PATCH',
            headers
          }
        );

        if (response.ok) {
          this.notifications.forEach(n => n.read = true);
          this.unreadCount = 0;
        }
      } catch (error) {
        console.error('Error marking all notifications as read:', error);
      }
    },

    async loadNotifications() {
      try {
        this.loading = true;
        const headers = getAuthHeaders();
        const token = headers.Authorization?.split(' ')[1];
        if (!token) return;

        const userId = WebSocketService.extractUserIdFromToken(token);
        if (!userId) return;

        const response = await fetch(
          `http://localhost:8085/api/notifications/by-user/${userId}`,
          { headers }
        );

        if (response.ok) {
          const data = await response.json();
          this.notifications = data.map(n => ({
            ...n,
            timestamp: n.timestamp || new Date().toISOString()
          }));
          this.unreadCount = data.filter(n => !n.read).length;
          this.lastRefresh = new Date();
        }
      } catch (error) {
        console.error('Error loading notifications:', error);
      } finally {
        this.loading = false;
      }
    },

    refreshNotifications() {
      // Only refresh if it's been more than 1 minute since last refresh
      const now = new Date();
      if (!this.lastRefresh || (now - this.lastRefresh) > 60000) {
        this.loadNotifications();
      }
    },

    toggleNotifications() {
      this.showNotifications = !this.showNotifications;
      if (this.showNotifications) {
        this.refreshNotifications();
      }
    },

    handleOutsideClick(event) {
      if (!event.target.closest('.notification-bell')) {
        this.showNotifications = false;
      }
    },

    formatTime(timestamp) {
      const date = new Date(timestamp);
      const now = new Date();
      const diff = now - date;
      
      // Less than 1 minute
      if (diff < 60000) {
        return 'Just now';
      }
      // Less than 1 hour
      if (diff < 3600000) {
        const minutes = Math.floor(diff / 60000);
        return `${minutes} minute${minutes > 1 ? 's' : ''} ago`;
      }
      // Less than 24 hours
      if (diff < 86400000) {
        const hours = Math.floor(diff / 3600000);
        return `${hours} hour${hours > 1 ? 's' : ''} ago`;
      }
      // More than 24 hours
      return date.toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    },

    getNotificationIcon(type) {
      const icons = {
        payment: 'bi bi-credit-card',
        order: 'bi bi-bag',
        stock: 'bi bi-box',
        error: 'bi bi-exclamation-circle',
        warning: 'bi bi-exclamation-triangle',
        info: 'bi bi-info-circle',
        system: 'bi bi-hdd-network',
        admin: 'bi bi-shield-check'
      };
      return icons[type] || 'bi bi-bell';
    },

    getNotificationClass(type) {
      const classes = {
        error: 'notification-error',
        warning: 'notification-warning',
        info: 'notification-info',
        system: 'notification-system',
        admin: 'notification-admin',
        stock: 'notification-stock',
        order: 'notification-order',
        payment: 'notification-payment'
      };
      return classes[type] || '';
    },

    showBrowserNotification(notification) {
      if (!('Notification' in window)) return;
      
      if (Notification.permission === 'granted') {
        new Notification(notification.title, {
          body: notification.message,
          icon: '/favicon.ico'
        });
      } else if (Notification.permission !== 'denied') {
        Notification.requestPermission().then(permission => {
          if (permission === 'granted') {
            new Notification(notification.title, {
              body: notification.message,
              icon: '/favicon.ico'
            });
          }
        });
      }
    }
  }
};
</script>

<style scoped>
.notification-bell {
  display: inline-block;
}

.notification-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  font-size: 0.7rem;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  padding: 0 4px;
}

.notification-dropdown {
  position: absolute;
  top: calc(100% + 10px);
  right: -10px;
  width: 360px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 15px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  overflow: hidden;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
  background-color: #f8f9fa;
}

.notification-body {
  max-height: 400px;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  transition: all 0.2s ease;
}

.notification-item:hover {
  background-color: #f8f9fa;
}

.notification-item.unread {
  background-color: #f0f7ff;
}

.notification-icon {
  margin-right: 12px;
  display: flex;
  align-items: flex-start;
  padding-top: 2px;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-weight: 600;
  font-size: 0.9rem;
  margin-bottom: 4px;
  color: #2c3e50;
}

.notification-message {
  font-size: 0.85rem;
  color: #666;
  margin-bottom: 4px;
  word-wrap: break-word;
}

.notification-time {
  font-size: 0.75rem;
  color: #999;
}

.no-notifications {
  padding: 32px 16px;
  text-align: center;
  color: #666;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

/* Notification type specific styles */
.notification-error .notification-icon i {
  color: #dc3545;
}

.notification-warning .notification-icon i {
  color: #ffc107;
}

.notification-info .notification-icon i {
  color: #0dcaf0;
}

.notification-system .notification-icon i {
  color: #6610f2;
}

.notification-admin .notification-icon i {
  color: #198754;
}

.notification-stock .notification-icon i {
  color: #fd7e14;
}

.notification-order .notification-icon i {
  color: #0d6efd;
}

.notification-payment .notification-icon i {
  color: #20c997;
}

/* Transition animations */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>