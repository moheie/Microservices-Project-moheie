<template>  
<div class="container">
    <div class="dashboard-header mb-4 ">
      <div class="p-3 text-center">
        <h1 class="mb-3">Customer Dashboard</h1>
      </div>
      <div class="notification-bell">
        <notification-bell user-type="customer" />
      </div>
      <div v-if="user" class="user-info text-center">
        <h2 class="welcome">Welcome, {{ user.username }}!</h2>
        <p class="text-muted">{{ user.email }}</p>
      </div>
      <div v-else class="p-3 text-center">
        <div class="spinner-border" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
        <p>Loading user information...</p>
      </div>
    </div>

    <div class="row">
      <div class="col-md-4 mb-4">
        <div class="card h-100">
          <div class="card-body text-center">
            <i class="bi bi-cart-plus fs-1 mb-3 text-primary"></i>
            <h3>Browse Menu</h3>
            <p>Explore our delicious dishes from various restaurants.</p>
            <router-link to="/browse-dishes" class="btn btn-primary w-100">Browse Menu</router-link>
          </div>
        </div>
      </div>

      <div class="col-md-4 mb-4">
        <div class="card h-100">
          <div class="card-body text-center">
            <i class="bi bi-bag fs-1 mb-3 text-success"></i>
            <h3>Place Order</h3>
            <p>Ready to order? Add dishes to your cart and checkout.</p>
            <router-link to="/cart" class="btn btn-success w-100">Order Now</router-link>
          </div>
        </div>
      </div>

      <div class="col-md-4 mb-4">
        <div class="card h-100">
          <div class="card-body text-center">
            <i class="bi bi-list-check fs-1 mb-3 text-info"></i>
            <h3>My Orders</h3>
            <p>View your current and past orders.</p>
            <router-link to="/orders" class="btn btn-info w-100">View Orders</router-link>
          </div>
        </div>
      </div>
    </div>

    <div class="row mt-4">
      <div class="col-md-6 mb-4">
        <div class="card h-100">
          <div class="card-header bg-warning text-white">
            <h4 class="mb-0">Quick Order</h4>
          </div>
          <div class="card-body">
            <div v-if="recentOrders.length">
              <p>Order again from your recent restaurants:</p>
              <div class="list-group">
                <button v-for="order in recentOrders" :key="order.id" 
                  class="list-group-item list-group-item-action d-flex justify-content-between align-items-center"
                  @click="goToRestaurant(order.companyName)">
                  #{{ order.id }}
                  <span class="badge bg-primary rounded-pill">{{ order.date }}</span>
                </button>
              </div>
            </div>
            <div v-else class="text-center">
              <p>No recent orders found.</p>
              <router-link to="/browse-dishes" class="btn btn-outline-primary">Discover Restaurants</router-link>
            </div>
          </div>
        </div>
      </div>

      <div class="col-md-6 mb-4">
        <div class="card h-100">
          <div class="card-header bg-info text-white">
            <h4 class="mb-0">Cart Status</h4>
          </div>
          <div class="card-body">
            <div v-if="loading" class="text-center">
              <div class="spinner-border" role="status">
                <span class="visually-hidden">Loading...</span>
              </div>
              <p>Loading your cart...</p>
            </div>
            <div v-else-if="cart && cart.dishes && cart.dishes.length > 0" class="text-center">
              <p>You have {{ cart.dishes.length }} items in your cart.</p>
              <router-link to="/cart" class="btn btn-primary">View Cart</router-link>
            </div>
            <div v-else class="text-center">
              <p>Your cart is empty.</p>
              <router-link to="/browse-dishes" class="btn btn-outline-success">Start Shopping</router-link>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import { useStore } from 'vuex';
import { useRouter } from 'vue-router';
import axios from 'axios';
import NotificationBell from '@/components/NotificationBell.vue';

export default {
  name: 'CustomerDashboard',
  components: {
    NotificationBell
  },
  setup() {
    const store = useStore();
    const router = useRouter();
    const cart = ref({ items: [] });
    const loading = ref(true);
    const recentOrders = ref([]);
    const error = ref(null);


    const user = computed(() => store.getters.currentUser);

    // Helper function to add auth token to requests
    const getAuthHeaders = () => {
      const token = sessionStorage.getItem('token');
      return token ? { Authorization: `Bearer ${token}` } : {};
    };

    const fetchCart = async () => {
      loading.value = true;
      try {
        const response = await axios.get('http://localhost:8084/order-service/api/cart/dishes', {
          headers: getAuthHeaders()
        });
        cart.value.dishes = response.data || [];
      } catch (err) {
        error.value = 'Failed to load cart. Please try again.';
        console.error('Error fetching cart:', err);
      } finally {
        loading.value = false;
      }
    };

    const fetchRecentOrders = async () => {
      const headers = getAuthHeaders();
      try {
        const response = await axios.get('http://localhost:8084/order-service/api/orders/getOrders', {headers});
        recentOrders.value = response.data || [];
      } catch (err) {
        console.error('Error fetching recent orders:', err);
      }
    };

    const calculateTotal = () => {
      if (!cart.value || !cart.value.items) return '0.00';
      const total = cart.value.items.reduce((sum, item) => {
        return sum + (item.dishPrice * item.quantity);
      }, 0);
      return total.toFixed(2);
    };

    const goToRestaurant = (restaurantName) => {
      // We'll navigate to browse-dishes with a query parameter
      router.push({
        path: '/browse-dishes',
        query: { restaurant: restaurantName }
      });
    };

    onMounted(() => {
      fetchCart();
      fetchRecentOrders();
    });

    return {
      user,
      cart,
      loading,
      recentOrders,
      calculateTotal,
      goToRestaurant
    };
  }
};
</script>

<style scoped>
.dashboard-header {
  padding: 30px 0;
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 10px;
  margin-bottom: 30px;
}

.welcome {
  font-size: 2rem;
  color: #343a40;
}

.card {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s;
  background-color: rgba(255, 255, 255, 0.9);
  height: 100%;
}

.card:hover {
  transform: translateY(-5px);
}

.card-header {
  padding: 15px;
}

.bi {
  display: block;
  margin-bottom: 10px;
}

.notification-bell {
  position: absolute;
  top: 50px;
  right: 20px;
}

/* For responsive spacing */
@media (max-width: 768px) {
  .welcome {
    font-size: 1.5rem;
  }
}
</style>