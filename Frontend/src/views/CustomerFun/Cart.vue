<template>
  <div class="container">
    <div class="card">
      <div class="card-header bg-primary text-white">
        <h2>My Shopping Cart</h2>
      </div>
      <div class="card-body">
        <div v-if="loading" class="text-center">
          <div class="spinner-border" role="status">
            <span class="visually-hidden">Loading...</span>
          </div>
          <p>Loading your cart...</p>
        </div>
        <div v-else-if="cart && cart.items && cart.items.length">
          <table class="table table-hover">
            <thead>
              <tr>
                <th>Item</th>
                <th>Restaurant</th>
                <th>Price</th>
                <th>Quantity</th>
                <th>Total</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in cart.items" :key="index">
                <td>{{ item.dishName }}</td>
                <td>{{ item.companyName }}</td>
                <td>${{ formatPrice(item.dishPrice) }}</td>
                <td>
                  <div class="quantity-control">
                    <button class="btn btn-sm btn-secondary" @click="decreaseQuantity(item)">-</button>
                    <span class="mx-2">{{ item.quantity }}</span>
                    <button class="btn btn-sm btn-secondary" @click="increaseQuantity(item)">+</button>
                  </div>
                </td>
                <td>${{ formatPrice(item.dishPrice * item.quantity) }}</td>
                <td>
                  <button class="btn btn-danger btn-sm" @click="removeItem(item)">Remove</button>
                </td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td colspan="4" class="text-end fw-bold">Total:</td>
                <td colspan="2" class="fw-bold">${{ formatPrice(calculateTotal()) }}</td>
              </tr>
            </tfoot>
          </table>
          <div class="d-flex justify-content-end mt-3">
            <button class="btn btn-outline-secondary me-2" @click="clearCart">Clear Cart</button>
            <button class="btn btn-primary" @click="proceedToCheckout">Proceed to Checkout</button>
          </div>
        </div>
        <div v-else class="text-center">
          <p>Your cart is empty.</p>
          <router-link to="/browse-dishes" class="btn btn-primary">Browse Dishes</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import store from '@/store';

export default {
  name: 'Cart',
  setup() {
    const router = useRouter();
    const cart = ref({ items: [] });
    const loading = ref(true);
    const error = ref(null);

    // Helper function to add auth token to requests
    const getAuthHeaders = () => {
      const token = sessionStorage.getItem('token');
      return token ? { Authorization: `Bearer ${token}` } : {};
    };

    const fetchCart = async () => {
      loading.value = true;
      try {
        const response = await axios.get('http://localhost:8084/order-service/api/cart/get', {
          headers: getAuthHeaders()
        });
        cart.value = response.data;
      } catch (err) {
        error.value = 'Failed to load cart. Please try again.';
        console.error('Error fetching cart:', err);
        if (err.response && err.response.status === 401) {
          store.commit('LOGOUT');
          window.location.href = '/login';
        }
      } finally {
        loading.value = false;
      }
    };

    const calculateTotal = () => {
      return cart.value.items.reduce((sum, item) => {
        return sum + (item.dishPrice * item.quantity);
      }, 0);
    };

    const formatPrice = (price) => {
      return price.toFixed(2);
    };    const increaseQuantity = async (item) => {
      try {
        await axios.post(`http://localhost:8084/order-service/api/cart/add`, null, {
          params: {
            productId: item.productId,
            quantity: 1,
            dishName: item.dishName,
            dishPrice: item.dishPrice,
            companyName: item.companyName
          },
          headers: getAuthHeaders()
        });
        fetchCart();
      } catch (err) {
        error.value = 'Failed to update item quantity. Please try again.';
        console.error('Error updating quantity:', err);
        if (err.response && err.response.status === 401) {
          store.commit('LOGOUT');
          window.location.href = '/login';
        }
      }
    };

    const decreaseQuantity = async (item) => {
      if (item.quantity > 1) {
        try {
          await axios.delete(`http://localhost:8084/order-service/api/cart/remove`, {
            params: {
              productId: item.productId,
              quantity: 1
            },
            headers: getAuthHeaders()
          });
          fetchCart();
        } catch (err) {
          error.value = 'Failed to update item quantity. Please try again.';
          console.error('Error updating quantity:', err);
          if (err.response && err.response.status === 401) {
            store.commit('LOGOUT');
            window.location.href = '/login';
          }
        }
      } else {
        removeItem(item);
      }
    };

    const removeItem = async (item) => {
      try {
        await axios.delete(`http://localhost:8084/order-service/api/cart/remove`, {
          params: {
            productId: item.productId,
            quantity: item.quantity
          },
          headers: getAuthHeaders()
        });
        fetchCart();
      } catch (err) {
        error.value = 'Failed to remove item. Please try again.';
        console.error('Error removing item:', err);
        if (err.response && err.response.status === 401) {
          store.commit('LOGOUT');
          window.location.href = '/login';
        }
      }
    };

    const clearCart = async () => {
      try {
        await axios.delete('http://localhost:8084/order-service/api/cart/clear', {
          headers: getAuthHeaders()
        });
        fetchCart();
      } catch (err) {
        error.value = 'Failed to clear cart. Please try again.';
        console.error('Error clearing cart:', err);
        if (err.response && err.response.status === 401) {
          store.commit('LOGOUT');
          window.location.href = '/login';
        }
      }
    };

    const proceedToCheckout = () => {
      router.push('/order-confirmation');
    };

    onMounted(() => {
      fetchCart();
    });

    return {
      cart,
      loading,
      error,
      calculateTotal,
      formatPrice,
      increaseQuantity,
      decreaseQuantity,
      removeItem,
      clearCart,
      proceedToCheckout
    };
  }
}
</script>

<style scoped>
.quantity-control {
  display: flex;
  align-items: center;
}

.card {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
  background-color: rgba(255, 255, 255, 0.9);
}

.card-header {
  padding: 15px;
}

.btn-secondary {
  background-color: #6c757d;
  border-color: #6c757d;
}

.btn-secondary:hover {
  background-color: #5a6268;
  border-color: #545b62;
}

.btn-danger {
  background-color: #dc3545;
  border-color: #dc3545;
}

.btn-danger:hover {
  background-color: #c82333;
  border-color: #bd2130;
}
</style>