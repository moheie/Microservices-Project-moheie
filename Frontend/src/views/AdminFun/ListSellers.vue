<template>
  <div>
    <h1>List Sellers</h1>
    <table class="table table-striped">
      <thead>
        <tr>
          <th>Username</th>
          <th>Email</th>
          <th>Company Name</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="seller in sellers" :key="seller.id">
          <td>{{ seller.username }}</td>
          <td>{{ seller.email }}</td>
          <td>{{ seller.companyName }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { getAuthHeaders } from '@/utils/auth';

const sellers = ref([]);

const fetchSellers = async () => {
  try {
    const headers = getAuthHeaders();
    const response = await axios.get('http://localhost:8080/auth-service/api/auth/restaurants', { headers });
    sellers.value = response.data;
  } catch (error) {
    console.error('Error fetching sellers:', error.response?.data || error.message);
  }
};

onMounted(() => {
  fetchSellers();
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