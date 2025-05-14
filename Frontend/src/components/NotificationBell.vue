<template>
  <div class="notification-bell position-relative">
    <button class="btn btn-link" @click="toggleNotifications">
      <i class="bi bi-bell-fill fs-4"></i>
      <span v-if="unreadCount > 0" class="badge bg-danger rounded-pill notification-badge">{{ unreadCount }}</span>
    </button>
    <div class="notification-dropdown shadow" v-if="showNotifications">
      <div class="notification-header">
        <h6 class="m-0">Notifications</h6>
        <button v-if="notifications.length > 0" class="btn btn-sm btn-link" @click="markAllAsRead">
          Mark all as read
        </button>
      </div>
      <div class="notification-body">
        <template v-if="notifications.length > 0">
          <div
            v-for="(notification, index) in notifications"
            :key="index"
            class="notification-item"
            :class="{ 'unread': !notification.read }"
            @click="markAsRead(index)"
          >
            <div class="notification-icon">
              <i :class="getNotificationIcon(notification.type)"></i>
            </div>
            <div class="notification-content">
              <div class="notification-title">{{ notification.title }}</div>
              <div class="notification-message">{{ notification.message }}</div>
              <div class="notification-time">{{ formatTime(notification.time) }}</div>
            </div>
          </div>
        </template>
        <div v-else class="no-notifications">
          No notifications available
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import WebSocketService from '@/services/WebSocketService';

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
      unreadCount: 0
    };
  },
  mounted() {
    this.setupNotificationSystem();
    // Demo notifications based on user type
    this.loadDemoNotifications();
    
    // Close dropdown when clicking outside
    document.addEventListener('click', this.handleOutsideClick);
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleOutsideClick);
    // Unsubscribe from notifications
    WebSocketService.unsubscribe('*', this.handleNotification);
  },  methods: {
    setupNotificationSystem() {
      // Connect to WebSocket server and subscribe to notifications
      const token = sessionStorage.getItem('token');
      
      if (token) {
        WebSocketService.connect(this.userType, token)
          .then(() => {
            console.log(`Connected to notification service as ${this.userType}`);
            WebSocketService.subscribe('*', this.handleNotification);
          })
          .catch(error => {
            console.error('Failed to connect to notification service:', error);
          });
      } else {
        console.error('No authentication token available');
      }
    },
    
    handleNotification(notification) {
      // Process received notification
      this.addNotification(notification);
    },
    toggleNotifications() {
      this.showNotifications = !this.showNotifications;
    },
    handleOutsideClick(event) {
      if (!event.target.closest('.notification-bell')) {
        this.showNotifications = false;
      }
    },    markAsRead(index) {
      const notification = this.notifications[index];
      if (!notification.read) {
        // Update UI immediately for better user experience
        this.notifications[index].read = true;
        this.unreadCount = Math.max(0, this.unreadCount - 1);
        
        // Call API to update server
        if (notification.id) {
          const token = sessionStorage.getItem('token');
          if (token) {
            fetch(`http://localhost:8085/api/notifications/${notification.id}/read`, {
              method: 'PATCH',
              headers: {
                'Authorization': `Bearer ${token}`
              }
            })
            .catch(error => {
              console.error('Error marking notification as read:', error);
              // Roll back UI update if failed
              this.notifications[index].read = false;
              this.unreadCount++;
            });
          }
        }
      }
    },
    markAllAsRead() {
      // Update UI immediately
      this.notifications.forEach(notification => {
        notification.read = true;
      });
      this.unreadCount = 0;
      
      // Call API to update server
      const token = sessionStorage.getItem('token');
      if (token) {
        const userId = this.getUserIdFromToken(token);
        
        if (userId) {
          // Mark all as read for this specific user
          fetch(`http://localhost:8085/api/notifications/read-all/by-user/${userId}`, {
            method: 'PATCH',
            headers: {
              'Authorization': `Bearer ${token}`
            }
          })
          .catch(error => {
            console.error('Error marking all notifications as read:', error);
          });
        } else {
          // Mark all as read for this user type
          fetch(`http://localhost:8085/api/notifications/read-all/by-type/${this.userType}`, {
            method: 'PATCH',
            headers: {
              'Authorization': `Bearer ${token}`
            }
          })
          .catch(error => {
            console.error('Error marking all notifications as read:', error);
          });
        }
      }
    },
    addNotification(notification) {
      this.notifications.unshift({
        ...notification,
        read: false,
        time: new Date()
      });
      this.unreadCount++;
    },
    formatTime(time) {
      const date = new Date(time);
      return date.toLocaleString();
    },
    getNotificationIcon(type) {
      const icons = {
        'payment': 'bi bi-credit-card',
        'order': 'bi bi-bag',
        'stock': 'bi bi-box',
        'error': 'bi bi-exclamation-circle text-danger',
        'warning': 'bi bi-exclamation-triangle text-warning',
        'info': 'bi bi-info-circle text-info'
      };
      return icons[type] || 'bi bi-bell';
    },    loadDemoNotifications() {
      // Fetch notifications from server
      const token = sessionStorage.getItem('token');
      if (!token) return;
      
      // Get the user ID from token (would normally be extracted from decoded token)
      const userId = this.getUserIdFromToken(token);
      
      // First try to load notifications for specific user if we have a user ID
      if (userId) {
        fetch(`http://localhost:8085/api/notifications/unread/by-user/${userId}`, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })
        .then(response => response.json())
        .then(data => {
          if (data && data.length > 0) {
            data.forEach(notification => {
              this.notifications.unshift({
                ...notification,
                time: new Date(notification.timestamp)
              });
            });
            this.unreadCount = data.filter(n => !n.read).length;
            return; // Exit if we got user-specific notifications
          }
          
          // If no user-specific notifications, load by user type
          return fetch(`http://localhost:8085/api/notifications/unread/by-type/${this.userType}`, {
            headers: {
              'Authorization': `Bearer ${token}`
            }
          });
        })
        .then(response => response && response.json())
        .then(data => {
          if (data && data.length > 0) {
            data.forEach(notification => {
              this.notifications.unshift({
                ...notification,
                time: new Date(notification.timestamp)
              });
            });
            this.unreadCount = data.filter(n => !n.read).length;
          }
        })
        .catch(error => {
          console.error('Error fetching notifications:', error);
          // Fall back to demo notifications if API is not available
          this.loadFallbackNotifications();
        });
      } else {
        // No user ID, try to fetch by user type
        fetch(`http://localhost:8085/api/notifications/unread/by-type/${this.userType}`, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })
        .then(response => response.json())
        .then(data => {
          if (data && data.length > 0) {
            data.forEach(notification => {
              this.notifications.unshift({
                ...notification,
                time: new Date(notification.timestamp)
              });
            });
            this.unreadCount = data.filter(n => !n.read).length;
          } else {
            // No notifications from server, load fallbacks
            this.loadFallbackNotifications();
          }
        })
        .catch(error => {
          console.error('Error fetching notifications:', error);
          // Fall back to demo notifications if API is not available
          this.loadFallbackNotifications();
        });
      }
    },
    
    loadFallbackNotifications() {
      // Add some demo notifications based on user type
      if (this.userType === 'admin') {
        this.addNotification({
          type: 'error',
          title: 'Payment Failed',
          message: 'Order #1234 payment has failed'
        });
        this.addNotification({
          type: 'error',
          title: 'System Error',
          message: 'Order_Service_Error: Database connection failed'
        });
      } else if (this.userType === 'seller') {
        this.addNotification({
          type: 'order',
          title: 'New Order',
          message: 'You have received a new order #1234'
        });
        this.addNotification({
          type: 'stock',
          title: 'Low Stock',
          message: 'Chicken Pasta is running low on stock'
        });
      } else if (this.userType === 'customer') {
        this.addNotification({
          type: 'order',
          title: 'Order Confirmed',
          message: 'Your order #1234 has been confirmed'
        });
        this.addNotification({
          type: 'payment',
          title: 'Payment Successful',
          message: 'Your payment of $24.99 has been processed'
        });
      }
    },
    
    getUserIdFromToken(token) {
      try {
        // Extract the payload from the JWT token (without verification)
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
          return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        
        const payload = JSON.parse(jsonPayload);
        return payload.userId || payload.sub || null;
      } catch (e) {
        console.error('Error decoding token:', e);
        return null;
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
}

.notification-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  width: 320px;
  background-color: white;
  border-radius: 8px;
  z-index: 1000;
  overflow: hidden;
  max-height: 400px;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 15px;
  border-bottom: 1px solid #eee;
  background-color: #f8f9fa;
}

.notification-body {
  max-height: 350px;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  padding: 12px 15px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  transition: background-color 0.2s;
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
  align-items: center;
  font-size: 1.2rem;
}

.notification-content {
  flex: 1;
}

.notification-title {
  font-weight: bold;
  font-size: 0.9rem;
  margin-bottom: 4px;
}

.notification-message {
  font-size: 0.85rem;
  color: #666;
}

.notification-time {
  font-size: 0.75rem;
  color: #999;
  margin-top: 4px;
}

.no-notifications {
  padding: 20px 15px;
  text-align: center;
  color: #888;
}
</style>
