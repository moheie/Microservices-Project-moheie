<template>
  <div class="admin-dashboard mt-5">
    <div class="container">
      <div class="p-3 text-center">
        <h1>Admin Dashboard</h1>
      </div>
      
      <div class="notification-bell mb-4">
        <notification-bell user-type="admin" />
      </div>

      <div class="user-info text-center mb-5">
        <p v-if="user" class="h4">Welcome, {{ user.username }}! This is your dashboard.</p>
        <p v-if="user" class="text-muted">Email: {{ user.email }}</p>
        <p v-else>Loading user information...</p>
      </div>      <div class="row">
        <div class="col-md-6 mb-4">
          <div class="card h-100">
            <div class="card-body text-center">
              <i class="bi bi-credit-card-x fs-1 mb-3 text-danger"></i>
              <h3>Payment Failures</h3>
              <p>Monitor and address payment failures from customers</p>
              <div class="d-flex justify-content-between align-items-center mb-3">
                <span class="badge bg-danger" v-if="paymentFailureCount">
                  {{ paymentFailureCount }} New
                </span>
              </div>
              <button class="btn btn-danger w-100" @click="showPaymentFailures">
                View Payment Issues
              </button>
            </div>
          </div>
        </div>

        <div class="col-md-6 mb-4">
          <div class="card h-100">
            <div class="card-body text-center">
              <i class="bi bi-exclamation-triangle fs-1 mb-3 text-warning"></i>
              <h3>Error Logs</h3>
              <p>View system error logs from all services</p>
              <div class="d-flex justify-content-between align-items-center mb-3">
                <span class="badge bg-warning text-dark" v-if="errorLogsCount">
                  {{ errorLogsCount }} New
                </span>
              </div>
              <button class="btn btn-warning w-100" @click="showErrorLogs">
                View Error Logs
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Log Details Modal -->
      <div class="modal fade" id="logModal" tabindex="-1" ref="logModal">
        <div class="modal-dialog modal-lg">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">
                {{ activeTab === 'payments' ? 'Payment Failures' : 'Error Logs' }}
              </h5>
              <button type="button" class="btn-close" @click="closeModal"></button>
            </div>
            <div class="modal-body">
              <ul class="nav nav-tabs mb-3">
                <li class="nav-item">
                  <a class="nav-link" :class="{ active: activeTab === 'payments' }" 
                     href="#" @click.prevent="activeTab = 'payments'">
                    Payment Failures
                  </a>
                </li>
                <li class="nav-item">
                  <a class="nav-link" :class="{ active: activeTab === 'errors' }"
                     href="#" @click.prevent="activeTab = 'errors'">
                    Error Logs
                  </a>
                </li>
              </ul>

              <div v-if="activeTab === 'payments'">
                <div v-for="notification in paymentFailures" :key="notification.id"
                     class="alert" :class="notification.read ? 'alert-secondary' : 'alert-danger'">
                  <div class="d-flex justify-content-between align-items-start">
                    <div>
                      <h6 class="mb-1">{{ notification.title }}</h6>
                      <p class="mb-1">{{ notification.message }}</p>
                      <small class="text-muted">{{ formatTime(notification.timestamp) }}</small>
                    </div>
                    <button v-if="!notification.read" 
                            class="btn btn-sm btn-outline-secondary"
                            @click="markAsRead(notification)">
                      Mark as Read
                    </button>
                  </div>
                </div>
              </div>

              <div v-if="activeTab === 'errors'">
                <div v-for="notification in errorLogs" :key="notification.id"
                     class="alert" :class="notification.read ? 'alert-secondary' : 'alert-warning'">
                  <div class="d-flex justify-content-between align-items-start">
                    <div>
                      <div class="d-flex align-items-center mb-1">
                        <h6 class="mb-0">{{ notification.serviceName }}</h6>
                        <span class="badge bg-danger ms-2">{{ notification.severity }}</span>
                      </div>
                      <p class="mb-1">{{ notification.message }}</p>
                      <small class="text-muted">{{ formatTime(notification.timestamp) }}</small>
                    </div>
                    <button v-if="!notification.read" 
                            class="btn btn-sm btn-outline-secondary"
                            @click="markAsRead(notification)">
                      Mark as Read
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';
import NotificationBell from '@/components/NotificationBell.vue';
import { Modal } from 'bootstrap';
import WebSocketService from '@/services/WebSocketService';

export default {
  name: 'AdminDashboard',
  components: {
    NotificationBell
  },
  data() {
    return {
      activeTab: 'payments',
      modal: null,
      paymentFailures: [],
      errorLogs: [],
      // Track notifications that have been read
      readNotifications: new Set()
    };
  },  async mounted() {
    // Initialize Bootstrap modal
    this.modal = new Modal(this.$refs.logModal);

    // Subscribe to notifications from the store
    this.$store.subscribe((mutation) => {
      if (mutation.type === 'addNotification') {
        const notification = mutation.payload;
        if (notification.type === 'PAYMENT_FAILURE') {
          this.paymentFailures.unshift(notification);
        } else if (notification.type === 'ERROR_LOG') {
          this.errorLogs.unshift(notification);
        }
      }
    });// Initialize WebSocket connections for admin notifications
    this.initializeWebSocketConnections();
  },
  beforeUnmount() {
    // Cleanup WebSocket subscriptions when component is unmounted
    this.cleanupWebSocketConnections();
  },
  computed: {
    ...mapGetters(['currentUser']),
    user() {
      return this.currentUser;
    },
    paymentFailureCount() {
      return this.paymentFailures.filter(n => !this.readNotifications.has(n.id)).length;
    },
    errorLogsCount() {
      return this.errorLogs.filter(n => !this.readNotifications.has(n.id)).length;
    }
  },
  methods: {
    showPaymentFailures() {
      this.activeTab = 'payments';
      this.modal.show();
    },
    showErrorLogs() {
      this.activeTab = 'errors';
      this.modal.show();
    },
    closeModal() {
      this.modal.hide();
    },
    markAsRead(notification) {
      this.readNotifications.add(notification.id);
      this.$store.dispatch('markNotificationAsRead', notification.id);
    },
    formatTime(timestamp) {
      if (!timestamp) return '';
      const date = new Date(timestamp);
      const now = new Date();
      const diff = now - date;
      
      // Less than a minute
      if (diff < 60000) {
        return 'Just now';
      }
      // Less than an hour
      if (diff < 3600000) {
        const minutes = Math.floor(diff / 60000);
        return `${minutes} minute${minutes > 1 ? 's' : ''} ago`;
      }
      // Less than a day
      if (diff < 86400000) {
        const hours = Math.floor(diff / 3600000);
        return `${hours} hour${hours > 1 ? 's' : ''} ago`;
      }
      // More than a day
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
    },    async initializeWebSocketConnections() {
      try {
        // Get auth token from store or local storage
        const token = localStorage.getItem('token');
        const headers = {
          Authorization: `Bearer ${token}`
        };

        // Initialize WebSocket connection
        await WebSocketService.connect('admin', headers);        // Subscribe to all notification types with a unified handler
        WebSocketService.subscribe('PAYMENT_FAILURES', this.handleNotification);
        WebSocketService.subscribe('error', this.handleNotification);
        WebSocketService.subscribe('system', this.handleNotification);
        WebSocketService.subscribe('order', this.handleNotification);
        WebSocketService.subscribe('stock', this.handleNotification);
        WebSocketService.subscribe('admin', this.handleNotification);
        
        // Debug: Add a wildcard subscription to log all notifications
        WebSocketService.subscribe('*', (notification) => {
          console.log('Wildcard notification received:', notification);
        });

      } catch (error) {
        console.error('Failed to initialize WebSocket connections:', error);
        this.$toast.error('Failed to connect to notification service');
      }
    },
      cleanupWebSocketConnections() {
      WebSocketService.unsubscribe('PAYMENT_FAILURES', this.handleNotification);
      WebSocketService.unsubscribe('error', this.handleNotification);
      WebSocketService.unsubscribe('system', this.handleNotification);
      WebSocketService.unsubscribe('order', this.handleNotification);
      WebSocketService.unsubscribe('stock', this.handleNotification);
      WebSocketService.unsubscribe('admin', this.handleNotification);
      WebSocketService.unsubscribe('*');
    },
      handleNotification(notification) {
      console.log('AdminDashboard handling notification:', notification);
      
      // Skip duplicate notifications
      const existingPaymentFailure = this.paymentFailures.find(n => n.id === notification.id);
      const existingErrorLog = this.errorLogs.find(n => n.id === notification.id);
      
      if (existingPaymentFailure || existingErrorLog) {
        console.log('Skipping duplicate notification', notification.id);
        return;
      }
      
      // Ensure the notification has an ID and timestamp
      const processedNotification = {
        ...notification,
        id: notification.id || `notification_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
        timestamp: notification.timestamp || new Date().toISOString()
      };
      
      // Parse RabbitMQ admin log messages (e.g., "Order_Error: Some message")
      if (notification.message && notification.message.includes(':')) {
        try {
          const parts = notification.message.split(':', 2);
          if (parts.length >= 2) {
            const serviceInfo = parts[0];
            const logMessage = parts[1].trim();
            
            // Extract service info if available (e.g., "Order_Error")
            if (serviceInfo.includes('_')) {
              const serviceInfoParts = serviceInfo.split('_');
              if (serviceInfoParts.length >= 2) {
                processedNotification.serviceName = processedNotification.serviceName || serviceInfoParts[0];
                processedNotification.severity = processedNotification.severity || serviceInfoParts[1];
                processedNotification.message = logMessage;
              }
            }
          }
        } catch (error) {
          console.error('Error parsing notification message:', error);
        }
      }
      
      // Payment notifications
      if (notification.type === 'payment' || 
          notification.type === 'PAYMENT_FAILURES' ||
          (notification.title && notification.title.toLowerCase().includes('payment')) ||
          processedNotification.message?.includes('payment')) {
        
        this.paymentFailures.unshift({
          ...processedNotification, 
          title: processedNotification.title || 'Payment Notification',
          message: processedNotification.message || 'Payment status update'
        });
        this.$toast.error(`Payment notification: ${processedNotification.message || processedNotification.title}`);
        return;
      }
      
      // Error notifications from Order service
      if ((processedNotification.serviceName === 'Order' && processedNotification.severity === 'Error') ||
          notification.type === 'order') {
        
        this.errorLogs.unshift({
          ...processedNotification,
          serviceName: 'Order',
          severity: processedNotification.severity || 'Error'
        });
        this.$toast.error(`Order service error: ${processedNotification.message}`);
        return;
      }
      
      // Error notifications from Stock service
      if ((processedNotification.serviceName === 'Stock' && processedNotification.severity === 'Error') ||
          notification.type === 'stock') {
        
        this.errorLogs.unshift({
          ...processedNotification,
          serviceName: 'Stock',
          severity: processedNotification.severity || 'Error'
        });
        this.$toast.warning(`Stock service alert: ${processedNotification.message}`);
        return;
      }
      
      // General error notifications
      if (notification.type === 'error' || 
          notification.type === 'system' ||
          notification.severity === 'Error') {
        
        this.errorLogs.unshift({
          ...processedNotification,
          serviceName: processedNotification.serviceName || 'System',
          severity: processedNotification.severity || 'Error'
        });
        this.$toast.error(`${processedNotification.serviceName || 'System'} error: ${processedNotification.message || processedNotification.title}`);
        return;
      }
      
      // Handle admin-specific notifications
      if (notification.type === 'admin') {
        // Determine if it's an error or payment notification based on content
        if (processedNotification.message?.toLowerCase().includes('payment') || 
            processedNotification.title?.toLowerCase().includes('payment')) {
          this.paymentFailures.unshift(processedNotification);
        } else {
          this.errorLogs.unshift(processedNotification);
        }
        return;
      }
      
      // For other notifications that don't match specific criteria, log them
      console.log('Unclassified notification:', processedNotification);
    },
  }
};
</script>

<style scoped>
.admin-dashboard {
  color: #333;
}

.card {
  transition: transform 0.2s;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  background-color: rgba(255, 255, 255, 0.9);
}

.card:hover {
  transform: translateY(-5px);
}

.notification-bell {
  position: absolute;
  top: 50px;
  right: 20px;
}

</style>

