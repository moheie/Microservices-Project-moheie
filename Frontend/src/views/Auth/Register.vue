<template>
  <div class="register">
    <div class="alert alert-info text-center mb-3">
      Only customers can register. Admins and seller reps should contact support.
    </div>
    <div class="row justify-content-center">
      <div class="col-md-6">
        <div class="card">
          <div class="card-body">
            <h2 class="card-title text-center mb-4">Register</h2>
            <form @submit.prevent="handleRegister">
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
                <label for="email" class="form-label">Email</label>
                <input
                  type="email"
                  class="form-control"
                  id="email"
                  v-model="email"
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
                  {{ loading ? 'Registering...' : 'Register' }}
                </button>
              </div>
              <div v-if="error" class="alert alert-danger mt-3">
                {{ error }}
              </div>
            </form>
            <div class="text-center mt-3">
              <p>
                Already have an account?
                <router-link to="/login">Login here</router-link>
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
  name: 'Register',
  setup() {
    const store = useStore()
    const router = useRouter()
    const username = ref('')
    const email = ref('')
    const password = ref('')
    const loading = ref(false)
    const error = ref('')

    const handleRegister = async () => {
      loading.value = true;
      error.value = '';

      try {
        await store.dispatch('register', {
          username: username.value,
          email: email.value,
          password: password.value
        });

        // Role-based redirect
        const user = store.getters.currentUser;
        if (user && user.role) {
          if (user.role === 'ADMIN') {
            router.push('/admin-dashboard');
          } else if (user.role === 'RESTAURANT_REPRESENTATIVE') {
            router.push('/seller-dashboard');
          } else if (user.role === 'CUSTOMER') {
            router.push('/customer-dashboard');
          } else {
            router.push('/');
          }
        } else {
          router.push('/');
        }
      } catch (err) {
        error.value = err.message || 'Registration failed. Please try again.';
      } finally {
        loading.value = false;
      }
    };

    return {
      username,
      email,
      password,
      loading,
      error,
      handleRegister
    }
  }
}
</script>

<style scoped>
.register {
  padding: 2rem 0;
}

.card {
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}
</style> 