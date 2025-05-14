<template>
  <div class="create-seller">
    <h1>Create Seller Representative</h1>
    <form @submit.prevent="createSeller">
      <div class="form-group">
        <label for="username">Username</label>
        <input
            type="text"
            id="username"
            v-model="form.username"
            class="form-control"
            required
        />
      </div>
      <div class="form-group">
        <label for="email">Email</label>
        <input
            type="email"
            id="email"
            v-model="form.email"
            class="form-control"
            required
        />
      </div>
      <div class="form-group">
        <label for="companyName">Company Name</label>
        <input
            type="text"
            id="companyName"
            v-model="form.companyName"
            class="form-control"
            required
        />
      </div>
      <button type="submit" class="btn btn-primary mt-3">Create Seller</button>
    </form>
    <div v-if="responseMessage" class="alert mt-3" :class="responseClass">
      {{ responseMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import axios from 'axios';
import { getAuthHeaders } from '@/utils/auth';

// Define reactive variables
const form = ref({
  username: '',
  email: '',
  companyName: '',
});

const responseMessage = ref('');
const responseClass = ref('');

const downloadCredentials = (username, password) => {
  const element = document.createElement('a');
  const fileContent = `Username: ${username}\nPassword: ${password}`;
  const file = new Blob([fileContent], { type: 'text/plain' });
  element.href = URL.createObjectURL(file);
  element.download = 'credentials.txt';
  document.body.appendChild(element);
  element.click();
  document.body.removeChild(element);
};

const createSeller = async () => {
  responseMessage.value = ''; // Clear previous messages
  responseClass.value = ''; // Reset response class

  try {
    const headers = {
      ...getAuthHeaders(),
      'Content-Type': 'application/json', // Explicitly set Content-Type to application/json
    };

    const response = await axios.post(
      'http://localhost:8080/auth-service/api/auth/register/restaurant',
      null, // No request body
      {
        headers,
        params: {
          username: form.value.username,
          email: form.value.email,
          companyName: form.value.companyName,
        },
      }
    );

    const generatedPassword = response.data.password;
    downloadCredentials(form.value.username, generatedPassword);

    responseMessage.value = response.data.message || 'Seller created successfully.';
    responseClass.value = 'alert-success';
  } catch (error) {
    console.error('Error details:', error.response?.data || error.message);
    responseMessage.value =
      error.response?.data?.message || 'An error occurred while creating the seller.';
    responseClass.value = 'alert-danger';
  }
};
</script>

<style scoped>
.create-seller {
  max-width: 600px;
  margin: 0 auto;
}
</style>