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
      </div>      
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';
import NotificationBell from '@/components/NotificationBell.vue';

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
}
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

