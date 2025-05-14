<template>
  <div class="login">
    <div class="alert alert-info text-center mb-3">
      Customers, admins, and seller reps can log in here using their credentials.
    </div>
    <div class="row justify-content-center">
      <div class="col-md-6">
        <div class="card">
          <div class="card-body">
            <h2 class="card-title text-center mb-4">Login</h2>
            <form @submit.prevent="handleLogin">
              <div class="mb-3">
                <label for="username" class="form-label">Username</label>
                <input
                  type="text"
                  class="form-control"
                  id="username"
                  v-model="username"
                  required
                >
              </div>
              <div class="mb-3">
                <label for="password" class="form-label">Password</label>
                <input
                  type="password"
                  class="form-control"
                  id="password"
                  v-model="password"
                  required
                >
              </div>
              <div class="d-grid">
                <button type="submit" class="btn btn-primary" :disabled="loading">
                  {{ loading ? 'Logging in...' : 'Login' }}
                </button>
              </div>
              <div v-if="error" class="alert alert-danger mt-3">
                {{ error }}
              </div>
            </form>
            <div class="text-center mt-3">
              <p>
                Don't have an account?
                <router-link to="/register">Register here</router-link>
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'

export default {
  name: 'Login',
  setup() {
    const store = useStore()
    const router = useRouter()
    const username = ref('')
    const password = ref('')
    const loading = ref(false)
    const error = ref('')

    const handleLogin = async () => {
      loading.value = true;
      error.value = '';

      try {
        const response = await store.dispatch('login', {
          username: username.value,
          password: password.value
        });

        console.log('Login response:', response); // Debugging log

        const role = response.role; // Use role directly from the backend response

        // Store token in sessionStorage
        sessionStorage.setItem('token', response.token);
        console.log('Token stored in sessionStorage:', sessionStorage.getItem('token')); // Debugging log

        // Role-based redirect
        if (role === 'ADMIN') {
          router.push('/admin-dashboard');
        } else if (role === 'RESTAURANT_REPRESENTATIVE') {
          router.push('/seller-dashboard');
        } else if (role === 'CUSTOMER') {
          router.push('/customer-dashboard');
        } else {
          router.push('/');
        }
      } catch (err) {
        error.value = err.response?.data?.message || 'Login failed. Please try again.';
      } finally {
        loading.value = false;
      }
    };

    return {
      username,
      password,
      loading,
      error,
      handleLogin
    }
  }
}
</script>

<style scoped>
.login {
  padding: 2rem 0;
}

.card {
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}
</style>