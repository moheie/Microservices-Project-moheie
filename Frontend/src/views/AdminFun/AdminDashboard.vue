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

    // Send test notifications after a short delay
    setTimeout(() => this.sendTestNotifications(), 3000);

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
    });    // Initialize WebSocket connections for admin notifications
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
    },
    async initializeWebSocketConnections() {
      try {
        // Get auth token from store or local storage
        const token = localStorage.getItem('token');
        const headers = {
          Authorization: `Bearer ${token}`
        };

        // Initialize WebSocket connection
        await WebSocketService.connect('admin', headers);

        // Subscribe to payment failures
        WebSocketService.subscribe('PAYMENT_FAILURES', (notification) => {
          if (notification.type === 'PAYMENT_FAILURE') {
            this.paymentFailures.unshift(notification);
            // Optionally show a toast notification
            this.$toast.error(`Payment failure: ${notification.message}`);
          }
        });

        // Subscribe to system error logs
        WebSocketService.subscribe('SYSTEM_ERRORS', (notification) => {
          if (notification.type === 'ERROR_LOG') {
            this.errorLogs.unshift(notification);
            // Show toast based on severity
            const severity = notification.severity.toLowerCase();
            this.$toast[severity](`${notification.serviceName}: ${notification.message}`);
          }
        });
      } catch (error) {
        console.error('Failed to initialize WebSocket connections:', error);
        this.$toast.error('Failed to connect to notification service');
      }
    },
    
    cleanupWebSocketConnections() {
      WebSocketService.unsubscribe('PAYMENT_FAILURES');
      WebSocketService.unsubscribe('SYSTEM_ERRORS');
    },
    async sendTestNotifications() {
      try {
        const token = localStorage.getItem('token');
        const headers = {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        };

        // Test payment failure notification
        await fetch('http://localhost:8082/order-service/test/payment-failure?orderId=999&message=TestPaymentFailure', {
          method: 'POST',
          headers
        });

        // Wait a bit before sending the next notification
        await new Promise(resolve => setTimeout(resolve, 2000));

        // Test system error notification
        await fetch('http://localhost:8083/product-service/test/system-error?serviceName=ProductService&severity=Error&message=TestSystemError', {
          method: 'POST',
          headers
        });

      } catch (error) {
        console.error('Failed to send test notifications:', error);
      }
    }
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

