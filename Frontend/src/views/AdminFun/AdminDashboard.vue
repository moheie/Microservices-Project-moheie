<template>
  <div class="admin-dashboard mt-5">
    <div class="container">
      <div class="d-flex justify-content-between align-items-center mb-4">
        <h1>Admin Dashboard</h1>
        <notification-bell user-type="admin" />
      </div>
      
      <div class="user-info text-center mb-5">
        <p v-if="user" class="h4">Welcome, {{ user.username }}! This is your dashboard.</p>
        <p v-if="user" class="text-muted">Email: {{ user.email }}</p>
        <p v-else>Loading user information...</p>
      </div>

      <div class="row">
        <div class="col-md-6 mb-4">
          <div class="card h-100">
            <div class="card-body text-center">
              <i class="bi bi-credit-card-x fs-1 mb-3 text-danger"></i>
              <h3>Payment Failures</h3>
              <p>Monitor and address payment failures from customers</p>
              <button class="btn btn-danger w-100">View Payment Issues</button>
            </div>
          </div>
        </div>

        <div class="col-md-6 mb-4">
          <div class="card h-100">
            <div class="card-body text-center">
              <i class="bi bi-exclamation-triangle fs-1 mb-3 text-warning"></i>
              <h3>Error Logs</h3>
              <p>View system error logs from all services</p>
              <button class="btn btn-warning w-100">View Error Logs</button>
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

export default {
  name: 'AdminDashboard',
  components: {
    NotificationBell
  },
  computed: {
    ...mapGetters(['currentUser']),
    user() {
      return this.currentUser;
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
</style>