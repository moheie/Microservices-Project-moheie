<template>
  <div class="admin-dashboard text-center mt-5">
    <h1>Admin Dashboard</h1>
    <p v-if="user">Welcome, {{ user.username }}! This is your dashboard.</p>
    <p v-if="user">Email: {{ user.email }}</p>
    <p v-else>Loading user information...</p>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';

export default {
  name: 'AdminDashboard',
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
</style>