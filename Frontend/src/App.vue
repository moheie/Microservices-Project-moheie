<template>
  <div id="app">
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
      <div class="container">
        <router-link class="navbar-brand" to="/">Home Dish</router-link>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
          <ul class="navbar-nav me-auto">
            <li v-if="isCustomer" class="nav-item">
              <router-link class="nav-link" to="/customer-dashboard">My Dashboard</router-link>
            </li>
            <li v-if="isCustomer" class="nav-item">
              <router-link class="nav-link" to="/cart">My Cart</router-link>
            </li>
            <li v-if="isCustomer" class="nav-item">
              <router-link class="nav-link" to="/orders">My Orders</router-link>
            </li>
            <li v-if="isCustomer" class="nav-item">
              <router-link class="nav-link" to="/order-confirmation">Order Confirmation</router-link>
            </li>
            <li v-if="isCustomer" class="nav-item">
              <router-link class="nav-link" to="/browse-dishes">Browse Dishes</router-link>
            </li>
            <li v-if="isSeller" class="nav-item">
              <router-link class="nav-link" to="/seller-dashboard">My Dashboard</router-link>
            </li>
            <li v-if="isAdmin" class="nav-item">
              <router-link class="nav-link" to="/admin-dashboard">Admin Dashboard</router-link>
            </li>
            <li v-if="isAdmin" class="nav-item">
              <router-link class="nav-link" to="/create-seller">Create Seller</router-link>
            </li>
            <li v-if="isAdmin" class="nav-item">
              <router-link class="nav-link" to="/list-sellers">List Sellers</router-link>
            </li>
            <li v-if="isAdmin" class="nav-item">
              <router-link class="nav-link" to="/list-customers">List Customers</router-link>
            </li>
          </ul>
          <ul class="navbar-nav">
            <li class="nav-item" v-if="!isAuthenticated">
              <router-link class="nav-link" to="/login">Login</router-link>
            </li>
            <li class="nav-item" v-if="!isAuthenticated">
              <router-link class="nav-link" to="/register">Register</router-link>
            </li>
            <li class="nav-item" v-if="isAuthenticated">
              <a class="nav-link" href="#" @click="logout">Logout</a>
            </li>
          </ul>
        </div>
      </div>
    </nav>

    <div class="container mt-4">
      <router-view/>
    </div>
  </div>
</template>

<script>
import { computed } from 'vue'
import { useStore } from 'vuex'

export default {
  name: 'App',
  setup() {
    const store = useStore()
    const isAuthenticated = computed(() => store.getters.isAuthenticated)
    const currentUser = computed(() => store.getters.currentUser)

    const isCustomer = computed(() => currentUser.value?.role === 'CUSTOMER')
    const isSeller = computed(() => currentUser.value?.role === 'RESTAURANT_REPRESENTATIVE')
    const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')
    const logout = () => {
      store.dispatch('logout')
    }

    return {
      isAuthenticated,
      isCustomer,
      isSeller,
      isAdmin,
      logout
    }
  }
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  min-height: 100vh;
  background: url('../a1795bfa180447e15c0e71027646207a.jpg') no-repeat center center fixed;
  background-size: cover;
}
</style>