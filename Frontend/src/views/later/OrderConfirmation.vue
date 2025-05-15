<template>
  <!-- comment  -->
  <div class="container">
    <div class="card">
      <div class="card-header bg-primary text-white">
        <h2>Order Confirmation</h2>
      </div>
      <div class="card-body">
        <div v-if="loading" class="text-center">
          <div class="spinner-border" role="status">
            <span class="visually-hidden">Loading...</span>
          </div>
          <p>Loading your cart details...</p>
        </div>
        <div v-else-if="error" class="alert alert-danger">
          {{ error }}
        </div>
        <div v-else>
          <!-- Order confirmation content goes here -->
        </div>
      </div>
    </div>
  </div>
</template>

<!-- <template>
  <div class="container">
    <div class="card">
      <div class="card-header bg-primary text-white">
        <h2>Order Confirmation</h2>
      </div>
      <div class="card-body">
        <div v-if="loading" class="text-center">
          <div class="spinner-border" role="status">
            <span class="visually-hidden">Loading...</span>
          </div>
          <p>Loading your cart details...</p>
        </div>
        <div v-else-if="error" class="alert alert-danger">
          {{ error }}
        </div>
        <div v-else>
          <div v-if="!cart || !cart.items || cart.items.length === 0" class="text-center">
            <p>Your cart is empty. Please add items to your cart before proceeding to checkout.</p>
            <router-link to="/new-order" class="btn btn-primary">Browse Dishes</router-link>
          </div>
          <div v-else>
            <h4 class="mb-4">Please review your order</h4>
            
            <div v-if="!orderConfirmed">
              <div class="table-responsive">
                <table class="table">
                  <thead>
                    <tr>
                      <th>Item</th>
                      <th>Restaurant</th>
                      <th>Price</th>
                      <th>Quantity</th>
                      <th>Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in cart.items" :key="item.productId">
                      <td>{{ item.dishName }}</td>
                      <td>{{ item.companyName }}</td>
                      <td>${{ formatPrice(item.dishPrice) }}</td>
                      <td>{{ item.quantity }}</td>
                      <td>${{ formatPrice(item.dishPrice * item.quantity) }}</td>
                    </tr>
                  </tbody>
                  <tfoot>
                    <tr>
                      <td colspan="4" class="text-end fw-bold">Order Total:</td>
                      <td class="fw-bold">${{ formatPrice(calculateTotal()) }}</td>
                    </tr>
                  </tfoot>
                </table>
              </div>

              <div class="shipping-info my-4">
                <h5>Shipping Information</h5>
                <form @submit.prevent="confirmOrder">
                  <div class="mb-3">
                    <label for="address" class="form-label">Address</label>
                    <input 
                      type="text" 
                      class="form-control" 
                      id="address" 
                      v-model="shippingInfo.address" 
                      required
                    >
                  </div>
                  <div class="row">
                    <div class="col-md-6 mb-3">
                      <label for="city" class="form-label">City</label>
                      <input 
                        type="text" 
                        class="form-control" 
                        id="city" 
                        v-model="shippingInfo.city" 
                        required
                      >
                    </div>
                    <div class="col-md-3 mb-3">
                      <label for="state" class="form-label">State</label>
                      <input 
                        type="text" 
                        class="form-control" 
                        id="state" 
                        v-model="shippingInfo.state" 
                        required
                      >
                    </div>
                    <div class="col-md-3 mb-3">
                      <label for="zipCode" class="form-label">Zip Code</label>
                      <input 
                        type="text" 
                        class="form-control" 
                        id="zipCode" 
                        v-model="shippingInfo.zipCode" 
                        required
                      >
                    </div>
                  </div>

                  <div class="mb-3">
                    <label for="phoneNumber" class="form-label">Phone Number</label>
                    <input 
                      type="tel" 
                      class="form-control" 
                      id="phoneNumber" 
                      v-model="shippingInfo.phoneNumber" 
                      required
                    >
                  </div>

                  <h5 class="mt-4">Payment Method</h5>
                  <div class="mb-3">
                    <div class="form-check">
                      <input 
                        class="form-check-input" 
                        type="radio" 
                        name="paymentMethod" 
                        id="creditCard" 
                        value="creditCard" 
                        v-model="paymentMethod" 
                        checked
                      >
                      <label class="form-check-label" for="creditCard">
                        Credit Card
                      </label>
                    </div>
                    <div class="form-check">
                      <input 
                        class="form-check-input" 
                        type="radio" 
                        name="paymentMethod" 
                        id="paypal" 
                        value="paypal" 
                        v-model="paymentMethod"
                      >
                      <label class="form-check-label" for="paypal">
                        PayPal
                      </label>
                    </div>
                  </div>

                  <div v-if="paymentMethod === 'creditCard'" class="credit-card-info">
                    <div class="mb-3">
                      <label for="cardNumber" class="form-label">Card Number</label>
                      <input 
                        type="text" 
                        class="form-control" 
                        id="cardNumber" 
                        v-model="paymentInfo.cardNumber" 
                        required
                      >
                    </div>
                    <div class="row">
                      <div class="col-md-6 mb-3">
                        <label for="expiryDate" class="form-label">Expiry Date (MM/YY)</label>
                        <input 
                          type="text" 
                          class="form-control" 
                          id="expiryDate" 
                        v-model="paymentInfo.expiryDate" 
                          required
                        >
                      </div>
                      <div class="col-md-6 mb-3">
                        <label for="cvv" class="form-label">CVV</label>
                        <input 
                          type="text" 
                          class="form-control" 
                          id="cvv" 
                          v-model="paymentInfo.cvv" 
                          required
                        >
                      </div>
                    </div>
                    <div class="mb-3">
                      <label for="nameOnCard" class="form-label">Name on Card</label>
                      <input 
                        type="text" 
                        class="form-control" 
                        id="nameOnCard" 
                        v-model="paymentInfo.nameOnCard" 
                        required
                      >
                    </div>
                  </div>

                  <div class="d-flex justify-content-end mt-4">
                    <router-link to="/cart" class="btn btn-outline-secondary me-2">
                      Back to Cart
                    </router-link>
                    <button type="submit" class="btn btn-success" :disabled="submitting">
                      {{ submitting ? 'Processing...' : 'Place Order' }}
                    </button>
                  </div>
                </form>
              </div>
            </div>

            <div v-else class="order-success text-center">
              <div class="check-container mb-4">
                <i class="bi bi-check-circle-fill text-success" style="font-size: 5rem;"></i>
              </div>
              <h3 class="mb-3">Order Placed Successfully!</h3>
              <p class="mb-3">Thank you for your order. Your order has been received and is being processed.</p>
              <p class="mb-3">Order Number: <strong>{{ confirmationDetails.orderId }}</strong></p>
              <p class="mb-3">A confirmation has been sent to your email address.</p>
              <div class="mt-4">
                <router-link to="/orders" class="btn btn-primary">
                  View My Orders
                </router-link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import store from '@/store';

export default {
  name: 'OrderConfirmation',
  setup() {
    const cart = ref(null);
    const loading = ref(true);
    const error = ref(null);
    const submitting = ref(false);
    const orderConfirmed = ref(false);
    const confirmationDetails = ref({});

    const shippingInfo = ref({
      address: '',
      city: '',
      state: '',
      zipCode: '',
      phoneNumber: ''
    });

    const paymentMethod = ref('creditCard');
    
    const paymentInfo = ref({
      cardNumber: '',
      expiryDate: '',
      cvv: '',
      nameOnCard: ''
    });

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
      if (!cart.value || !cart.value.items || !cart.value.items.length) return 0;
      
      return cart.value.items.reduce((sum, item) => {
        return sum + (item.dishPrice * item.quantity);
      }, 0);
    };

    const formatPrice = (price) => {
      return parseFloat(price).toFixed(2);
    };    const confirmOrder = async () => {
      submitting.value = true;
      try {
        // First, send shipping and payment info if your API supports it
        // This is a simplified version - you would typically send the shipping and payment info
        
        // Confirm the order
        const response = await axios.post('http://localhost:8084/order-service/api/orders/confirm', {}, {
          headers: getAuthHeaders()
        });
        
        // Store the confirmation details
        confirmationDetails.value = {
          orderId: response.data.id,
          orderDate: response.data.orderDate,
          status: response.data.status,
          totalPrice: response.data.totalPrice
        };
        
        // Mark the order as confirmed
        orderConfirmed.value = true;
      } catch (err) {
        error.value = 'Failed to place your order. Please try again.';
        console.error('Error placing order:', err);
        if (err.response && err.response.status === 401) {
          store.commit('LOGOUT');
          window.location.href = '/login';
        }
      } finally {
        submitting.value = false;
      }
    };

    onMounted(() => {
      fetchCart();
    });

    return {
      cart,
      loading,
      error,
      shippingInfo,
      paymentMethod,
      paymentInfo,
      submitting,
      orderConfirmed,
      confirmationDetails,
      calculateTotal,
      formatPrice,
      confirmOrder
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

.shipping-info,
.order-success {
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.check-container {
  width: 120px;
  height: 120px;
  margin: 0 auto;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f8f9fa;
  border: 3px solid #28a745;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(40, 167, 69, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(40, 167, 69, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(40, 167, 69, 0);
  }
}
</style> -->