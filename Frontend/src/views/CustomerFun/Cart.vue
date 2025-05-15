<template>
  <div class="container">
    <div class="card">        
          <div class="card-header bg-primary text-white">
        <h2>Shopping Cart</h2>
      </div>
      <div class="card-body">
        <div v-if="loading" class="text-center">
          <div class="spinner-border" role="status">
            <span class="visually-hidden">Loading...</span>
          </div>
          <p>Loading your cart...</p>
        </div>
        <div v-else-if="error" class="alert alert-danger">
          {{ error }}
        </div>
        <div v-else-if="!cart || !cart.dishes || cart.dishes.length === 0" class="text-center">
          <p>Your cart is empty</p>
          <router-link to="/browse-dishes" class="btn btn-primary">Browse Dishes</router-link>
        </div>
        <div v-else>
          <div class="table-responsive">
            <table class="table">
              <thead>
                <tr>
                  <th>Dish</th>
                  <th>Restaurant</th>
                  <th>Price</th>
                  <th>Quantity</th>
                  <th>Total</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(dish, index) in groupedDishes" :key="index">
                  <td>{{ dish.name }}</td>
                  <td>{{ dish.companyName }}</td>
                  <td>${{ formatPrice(dish.price) }}</td>
                  <td>
                    <div class="quantity-control">
                      <button class="btn btn-sm btn-secondary" @click="updateQuantity(dish, -1)">-</button>
                      <input type="number" v-model.number="dish.quantity" class="form-control mx-2" min="1" style="width: 60px;" readonly />
                      <button class="btn btn-sm btn-secondary" @click="updateQuantity(dish, 1)">+</button>
                    </div>
                  </td>
                  <td>${{ formatPrice(dish.price * dish.quantity) }}</td>
                  <td>
                    <button class="btn btn-sm btn-danger" @click="removeItem(dish)">Remove</button>
                  </td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <td colspan="4" class="text-end"><strong>Total:</strong></td>
                  <td colspan="2"><strong>${{ formatPrice(calculateTotal()) }}</strong></td>
                </tr>
              </tfoot>
            </table>
          </div>            <div class="d-flex justify-content-between mt-4">
            <router-link to="/browse-dishes" class="btn btn-secondary">Continue Shopping</router-link>
            <button class="btn btn-primary" @click="confirmOrder">Confirm Order</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import { getAuthHeaders } from '@/utils/auth';
import { useRouter } from 'vue-router';

export default {
  name: 'Cart',
  setup() {
    const router = useRouter();
    const cart = ref({ dishes: [] });
    const loading = ref(true);
    const error = ref(null);

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
    
    const confirmOrder = async () => {
      try {
        const response = await axios.post('http://localhost:8084/order-service/api/orders/confirm', {}, {
          headers: getAuthHeaders()
        });
        if (response.status === 201) {
          cart.value.dishes = []; // Clear cart after confirmation
          // Redirect to the orders page
          router.push('/orders');
        }
      } catch (err) {
        error.value = 'Failed to confirm order. Please try again.';
        console.error('Error confirming order:', err);
      }
    };

    const groupedDishes = computed(() => {
      return cart.value.dishes || [];
    });

    const formatPrice = (price) => {
      return parseFloat(price).toFixed(2);
    };

    const calculateTotal = () => {
      return groupedDishes.value.reduce((total, dish) => {
        return total + (dish.price * dish.quantity);
      }, 0);
    };    
    
    const updateQuantity = async (dish, change) => {
      const headers = getAuthHeaders();
      const newQuantity = dish.quantity + change;
      if (newQuantity < 1) return; // Prevent negative quantity

      try {
        await axios.put('http://localhost:8084/order-service/api/cart/update', {}, {
          headers: headers,
          params: {
            productId: dish.dishId,
            quantity: newQuantity
          }
        });
        await fetchCart(); // Refresh cart after update
      } catch (err) {
        console.error('Error updating quantity:', err);
        error.value = 'Failed to update quantity. Please try again.';
      }
    };

    const removeItem = async (dish) => {
      try {
        await axios.delete('http://localhost:8084/order-service/api/cart/remove', {
          headers: getAuthHeaders(),
          params: {
            productId: dish.dishId,
            quantity: dish.quantity
          }
        });
        await fetchCart(); // Refresh cart after removal
      } catch (err) {
        console.error('Error removing item:', err);
        error.value = 'Failed to remove item. Please try again.';
      }
    };

    onMounted(() => {
      fetchCart();
    });

    return {
      cart,
      loading,
      error,
      groupedDishes,
      formatPrice,
      calculateTotal,
      updateQuantity,
      removeItem,
      confirmOrder
    };
  }
};
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

.table {
  background-color: rgba(255, 255, 255, 0.95);
}
</style>