<template>
  <div>
    <h1>List Customers</h1>
    <table class="table table-striped">
      <thead>
        <tr>
          <th>Username</th>
          <th>Email</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="customer in customers" :key="customer.id">
          <td>{{ customer.username }}</td>
          <td>{{ customer.email }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { getAuthHeaders } from '@/utils/auth';

const customers = ref([]);

const fetchCustomers = async () => {
  try {
    const headers = getAuthHeaders();
    const response = await axios.get('http://localhost:8080/auth-service/api/auth/customers', { headers });
    customers.value = response.data;
  } catch (error) {
    console.error('Error fetching customers:', error.response?.data || error.message);
  }
};

onMounted(() => {
  fetchCustomers();
});
</script>

<style scoped>
.table {
  width: 100%;
  margin-top: 20px;
  border-collapse: collapse;
}

.table th, .table td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}

.table th {
  background-color: #f2f2f2;
}
</style>