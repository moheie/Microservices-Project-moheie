<script>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import store from '@/store';
import { getAuthHeaders } from '@/utils/auth'; // Adjust the import path as necessary

export default {
  name: 'Orders',
  setup() {
    const orders = ref([]);
    const loading = ref(true);
    const error = ref(null);
    const expandedOrders = ref([]);
    const statusFilter = ref('all');

    const fetchOrders = async () => {
      loading.value = true;
      try {
        const headers = getAuthHeaders();
        const response = await axios.get('http://localhost:8084/order-service/api/orders/getOrders', {headers});
        orders.value = response.data;
      } catch (err) {
        error.value = 'Failed to load orders. Please try again.';
        console.error('Error fetching orders:', err);
        if (err.response && err.response.status === 401) {
          store.commit('LOGOUT');
          window.location.href = '/login';
        }
      } finally {
        loading.value = false;
      }
    };

    const filteredOrders = computed(() => {
      if (statusFilter.value === 'all') {
        return orders.value;
      }
      return orders.value.filter(order => order.status === statusFilter.value);
    });

    const formatDate = (dateValue) => {
      if (!dateValue) return 'N/A';
      
      // Handle date as array [year, month, day, hour, minute, second, nanosecond]
      if (Array.isArray(dateValue)) {
        const [year, month, day, hour, minute] = dateValue;
        const date = new Date(year, month - 1, day, hour, minute); // month is 0-indexed in JS Date
        return new Intl.DateTimeFormat('en-US', {
          year: 'numeric',
          month: 'short',
          day: 'numeric',
          hour: '2-digit',
          minute: '2-digit'
        }).format(date);
      }
      
      // Handle string date format
      const date = new Date(dateValue);
      return new Intl.DateTimeFormat('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      }).format(date);
    };

    const formatPrice = (price) => {
      return parseFloat(price).toFixed(2);
    };

    const toggleOrderDetails = (orderId) => {
      if (expandedOrders.value.includes(orderId)) {
        expandedOrders.value = expandedOrders.value.filter(id => id !== orderId);
      } else {
        expandedOrders.value.push(orderId);
      }
    };

    const getStatusBadgeClass = (status) => {
      switch (status) {
        case 'PENDING': return 'bg-warning text-dark';
        case 'BEING_DELIVERED': return 'bg-info text-dark';
        case 'DELIVERED': return 'bg-success';
        case 'CANCELED': return 'bg-danger';
        default: return 'bg-secondary';
      }
    };

    const getOrderHeaderClass = (status) => {
      switch (status) {
        case 'PENDING': return 'bg-warning bg-opacity-25';
        case 'BEING_DELIVERED': return 'bg-info bg-opacity-25';
        case 'DELIVERED': return 'bg-success bg-opacity-25';
        case 'CANCELED': return 'bg-danger bg-opacity-25';
        default: return 'bg-secondary bg-opacity-25';
      }
    };

    const getProgressPercentage = (status) => {
      switch (status) {
        case 'PENDING': return '25%';
        case 'BEING_DELIVERED': return '50%';
        case 'DELIVERED': return '100%';
        case 'CANCELED': return '100%';
        default: return '0%';
      }
    };

    const getProgressBarClass = (status) => {
      switch (status) {
        case 'PENDING': return 'bg-warning';
        case 'CONFIRMED': return 'bg-info';
        case 'SHIPPED': return 'bg-primary';
        case 'DELIVERED': return 'bg-success';
        case 'CANCELED': return 'bg-danger';
        default: return 'bg-secondary';
      }
    };

    const getOrderTotal = (order) => {
      if (!order.dishes || order.dishes.length === 0) return 0;
      return order.dishes.reduce((total, dish) => total + dish.price, 0);
    };

    const getGroupedDishes = (dishes) => {
      if (!dishes || dishes.length === 0) return [];
      
      // Group dishes by dishId
      const groupedDishes = dishes.reduce((acc, dish) => {
        const existingDish = acc.find(d => d.dishId === dish.dishId);
        if (existingDish) {
          existingDish.quantity += 1;
          existingDish.totalPrice += dish.price;
        } else {
          acc.push({
            ...dish,
            quantity: 1,
            totalPrice: dish.price
          });
        }
        return acc;
      }, []);
      
      return groupedDishes;
    };    const cancelOrder = async (orderId) => {
      try {
        const headers = getAuthHeaders();
        await axios.post(`http://localhost:8084/order-service/api/orders/cancel/${orderId}`, {}, { headers });
        // Update the local state
        const orderIndex = orders.value.findIndex(order => order.id === orderId);
        if (orderIndex !== -1) {
          orders.value[orderIndex].status = 'CANCELED';
        }
      } catch (err) {
        error.value = 'Failed to cancel order. Please try again.';
        console.error('Error canceling order:', err);
        if (err.response && err.response.status === 401) {
          store.commit('LOGOUT');
          window.location.href = '/login';
        }
      }
    };

    onMounted(() => {
      fetchOrders();
    });    return {
      orders,
      loading,
      error,
      expandedOrders,
      statusFilter,
      filteredOrders,
      formatDate,
      formatPrice,
      toggleOrderDetails,
      getStatusBadgeClass,
      getOrderHeaderClass,
      getProgressPercentage,
      getProgressBarClass,
      getOrderTotal,
      getGroupedDishes,
      cancelOrder,
    };
  }
}
</script>

<template>
  <div class="container">
    <div class="card">
      <div class="card-header bg-primary text-white">
        <h2>My Orders</h2>
      </div>
      <div class="card-body">
        <div v-if="loading" class="text-center">
          <div class="spinner-border" role="status">
            <span class="visually-hidden">Loading...</span>
          </div>
          <p>Loading your orders...</p>
        </div>        <div v-else-if="!orders.length" class="text-center">
          <p>You haven't placed any orders yet.</p>
          <router-link to="/browse-dishes" class="btn btn-primary">Browse Dishes</router-link>
        </div>
        <div v-else>
          <div class="mb-3">
            <label for="orderFilter" class="form-label">Filter Orders</label>
            <select class="form-select" v-model="statusFilter">
              <option value="all">All Orders</option>
              <option value="PENDING">Pending</option>
              <option value="BEING_DELIVERED">Being Delivered</option>
              <option value="DELIVERED">Delivered</option>
              <option value="CANCELED">Canceled</option>
            </select>
          </div>

          <div v-for="order in filteredOrders" :key="order.id" class="order-card mb-4">
            <div class="card">
              <div class="card-header" :class="getOrderHeaderClass(order.status)">
                <div class="d-flex justify-content-between align-items-center">
                  <h5 class="mb-0">Order #{{ order.id }}</h5>
                  <span class="badge" :class="getStatusBadgeClass(order.status)">{{ order.status }}</span>
                </div>
              </div>              <div class="card-body">
                <div class="row">
                  <div class="col-md-6">
                    <p><strong>Order Date:</strong> {{ formatDate(order.createdAt) }}</p>
                    <p><strong>Total:</strong> ${{ formatPrice(getOrderTotal(order)) }}</p>
                  </div>
                  <div class="col-md-6">
                    <p><strong>User ID:</strong> {{ order.userId }}</p>
                  </div>
                </div>

                <div class="mt-3">
                  <button class="btn btn-sm btn-outline-primary" @click="toggleOrderDetails(order.id)">
                    {{ expandedOrders.includes(order.id) ? 'Hide Details' : 'View Details' }}
                  </button>
                </div>                <div v-if="expandedOrders.includes(order.id)" class="order-details mt-3">
                  <h6>Order Items</h6>
                  <table class="table table-sm">
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
                      <tr v-for="(item, index) in getGroupedDishes(order.dishes)" :key="index">
                        <td>{{ item.name }}</td>
                        <td>{{ item.companyName }}</td>
                        <td>${{ formatPrice(item.price) }}</td>
                        <td>{{ item.quantity }}</td>
                        <td>${{ formatPrice(item.totalPrice) }}</td>
                      </tr>
                    </tbody>
                  </table>                  <div v-if="order.status === 'PENDING'" class="mt-3 d-flex justify-content-end">
                    <button class="btn btn-danger" @click="cancelOrder(order.id)">Cancel Order</button>
                  </div>
                </div>
              </div>
              <div class="card-footer bg-white">
                <div class="progress">
                  <div 
                    class="progress-bar" 
                    role="progressbar" 
                    :style="{ width: getProgressPercentage(order.status) }" 
                    :class="getProgressBarClass(order.status)">
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
  background-color: rgba(255, 255, 255, 0.9);
}

.order-details {
  background-color: #f8f9fa;
  padding: 15px;
  border-radius: 5px;
}

.progress {
  height: 8px;
  background-color: #e9ecef;
}

/* Status text colors */
.text-pending {
  color: #ffc107;
}

.text-confirmed {
  color: #0dcaf0;
}

.text-shipped {
  color: #0d6efd;
}

.text-delivered {
  color: #198754;
}

/* .text-canceled {
  color: #dc3545;
} */
</style>