<template>
  <div class="container">
    <div class="card">
      <div class="card-header bg-primary text-white">
        <h2>New Order</h2>
      </div>
      <div class="card-body">
        <div v-if="loading" class="text-center">
          <div class="spinner-border" role="status">
            <span class="visually-hidden">Loading...</span>
          </div>
          <p>Loading dishes...</p>
        </div>
        <div v-else-if="dishes.length === 0" class="text-center">
          <p>No dishes are available at the moment.</p>
        </div>
        <div v-else>
          <div class="row mb-4">
            <div class="col-md-4">
              <div class="input-group">
                <input
                  type="text"
                  class="form-control"
                  placeholder="Search dishes..."
                  v-model="searchQuery"
                />
                <button class="btn btn-outline-secondary" type="button">
                  <i class="bi bi-search"></i> Search
                </button>
              </div>
            </div>
          </div>

          <div class="row">
            <div v-for="dish in filteredDishes" :key="dish.id" class="col-md-4 mb-4">
              <div class="card h-100">
                <div class="card-body">
                  <h5 class="card-title">{{ dish.name }}</h5>
                  <p class="card-text dish-description">{{ dish.description }}</p>
                  <p><strong>Restaurant:</strong> {{ dish.companyName }}</p>
                  <p class="price">${{ formatPrice(dish.price) }}</p>
                  <div class="d-flex align-items-center mt-3">
                    <div class="input-group me-2" style="max-width: 120px;">
                      <button 
                        class="btn btn-outline-secondary" 
                        type="button" 
                        @click="decreaseQuantity(dish)"
                        :disabled="getQuantity(dish.id) <= 0"
                      >
                        -
                      </button>
                      <input 
                        type="text" 
                        class="form-control text-center" 
                        readonly 
                        :value="getQuantity(dish.id)"
                      >
                      <button 
                        class="btn btn-outline-secondary" 
                        type="button" 
                        @click="increaseQuantity(dish)"
                      >
                        +
                      </button>
                    </div>
                    <button 
                      class="btn btn-primary" 
                      @click="addToCart(dish)"
                      :disabled="getQuantity(dish.id) <= 0"
                    >
                      Add to Cart
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="cartItems.length > 0" class="mt-4">
            <div class="card">
              <div class="card-header bg-success text-white">
                <h3>Your Current Order</h3>
              </div>
              <div class="card-body">
                <table class="table">
                  <thead>
                    <tr>
                      <th>Item</th>
                      <th>Restaurant</th>
                      <th>Price</th>
                      <th>Quantity</th>
                      <th>Total</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in cartItems" :key="item.id">
                      <td>{{ item.name }}</td>
                      <td>{{ item.companyName }}</td>
                      <td>${{ formatPrice(item.price) }}</td>
                      <td>{{ item.quantity }}</td>
                      <td>${{ formatPrice(item.price * item.quantity) }}</td>
                      <td>
                        <button class="btn btn-danger btn-sm" @click="removeFromCart(item)">
                          Remove
                        </button>
                      </td>
                    </tr>
                  </tbody>
                  <tfoot>
                    <tr>
                      <td colspan="4" class="text-end">Total:</td>
                      <td colspan="2">${{ formatPrice(calculateTotal()) }}</td>
                    </tr>
                  </tfoot>
                </table>
                <div class="d-flex justify-content-end">
                  <button class="btn btn-outline-secondary me-2" @click="clearOrder">
                    Clear Order
                  </button>
                  <router-link to="/cart" class="btn btn-primary">
                    View Cart
                  </router-link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import store from '@/store';

export default {
  name: 'NewOrder',
  setup() {
    const dishes = ref([]);
    const loading = ref(true);
    const error = ref(null);
    const searchQuery = ref('');
    const selectedDishes = ref({});
    const cartItems = ref([]);

    // Helper function to add auth token to requests
    const getAuthHeaders = () => {
      const token = sessionStorage.getItem('token');
      return token ? { Authorization: `Bearer ${token}` } : {};
    };

    const fetchDishes = async () => {
      loading.value = true;
      try {
        const response = await axios.get('http://localhost:8083/product-service/api/dish/getDishes', {
          headers: getAuthHeaders()
        });
        dishes.value = response.data;
      } catch (err) {
        error.value = 'Failed to load dishes. Please try again.';
        console.error('Error fetching dishes:', err);
        if (err.response && err.response.status === 401) {
          store.commit('LOGOUT');
          window.location.href = '/login';
        }
      } finally {
        loading.value = false;
      }
    };

    const filteredDishes = computed(() => {
      if (!searchQuery.value) {
        return dishes.value;
      }
      const query = searchQuery.value.toLowerCase();
      return dishes.value.filter(dish => 
        dish.name.toLowerCase().includes(query) || 
        dish.description?.toLowerCase().includes(query) ||
        dish.companyName?.toLowerCase().includes(query)
      );
    });

    const formatPrice = (price) => {
      return parseFloat(price).toFixed(2);
    };

    const getQuantity = (dishId) => {
      return selectedDishes.value[dishId] || 0;
    };

    const increaseQuantity = (dish) => {
      if (!selectedDishes.value[dish.id]) {
        selectedDishes.value[dish.id] = 0;
      }
      selectedDishes.value[dish.id]++;
    };

    const decreaseQuantity = (dish) => {
      if (selectedDishes.value[dish.id] && selectedDishes.value[dish.id] > 0) {
        selectedDishes.value[dish.id]--;
      }
    };    const addToCart = async (dish) => {
      const quantity = selectedDishes.value[dish.id] || 0;
      if (quantity > 0) {
        try {
          await axios.post('http://localhost:8084/order-service/api/cart/add', null, {
            params: {
              productId: dish.id,
              quantity: quantity,
              dishName: dish.name,
              dishPrice: dish.price,
              companyName: dish.companyName
            },
            headers: getAuthHeaders()
          });
          
          // Add to local display cart
          const existingItem = cartItems.value.find(item => item.id === dish.id);
          if (existingItem) {
            existingItem.quantity += quantity;
          } else {
            cartItems.value.push({
              id: dish.id,
              name: dish.name,
              companyName: dish.companyName,
              price: dish.price,
              quantity: quantity
            });
          }
          
          // Reset the quantity
          selectedDishes.value[dish.id] = 0;
        } catch (err) {
          error.value = 'Failed to add item to cart. Please try again.';
          console.error('Error adding to cart:', err);
          if (err.response && err.response.status === 401) {
            store.commit('LOGOUT');
            window.location.href = '/login';
          }
        }
      }
    };

    const removeFromCart = async (item) => {
      try {
        await axios.delete('http://localhost:8084/order-service/api/cart/remove', {
          params: {
            productId: item.id,
            quantity: item.quantity
          },
          headers: getAuthHeaders()
        });
        
        // Remove from local display cart
        cartItems.value = cartItems.value.filter(cartItem => cartItem.id !== item.id);
      } catch (err) {
        error.value = 'Failed to remove item from cart. Please try again.';
        console.error('Error removing from cart:', err);
        if (err.response && err.response.status === 401) {
          store.commit('LOGOUT');
          window.location.href = '/login';
        }
      }
    };

    const clearOrder = async () => {
      try {
        await axios.delete('http://localhost:8084/order-service/api/cart/clear', {
          headers: getAuthHeaders()
        });
        cartItems.value = [];
      } catch (err) {
        error.value = 'Failed to clear cart. Please try again.';
        console.error('Error clearing cart:', err);
        if (err.response && err.response.status === 401) {
          store.commit('LOGOUT');
          window.location.href = '/login';
        }
      }
    };

    const calculateTotal = () => {
      return cartItems.value.reduce((total, item) => {
        return total + (item.price * item.quantity);
      }, 0);
    };

    onMounted(() => {
      fetchDishes();
    });

    return {
      dishes,
      loading,
      error,
      searchQuery,
      filteredDishes,
      formatPrice,
      getQuantity,
      increaseQuantity,
      decreaseQuantity,
      addToCart,
      cartItems,
      removeFromCart,
      clearOrder,
      calculateTotal
    };
  }
}
</script>

<style scoped>
.card {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
  background-color: rgba(255, 255, 255, 0.9);
}

.card-header {
  padding: 15px;
}

.dish-description {
  min-height: 60px;
  max-height: 60px;
  overflow: hidden;
}

.price {
  font-weight: bold;
  color: #28a745;
  font-size: 1.1rem;
}

/* For responsive spacing */
@media (max-width: 576px) {
  .input-group {
    margin-bottom: 15px;
  }
}
</style>