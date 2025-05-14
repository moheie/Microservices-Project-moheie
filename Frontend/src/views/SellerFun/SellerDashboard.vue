<template>  <div class="seller-dashboard container mt-5">
    <div class="d-flex justify-content-between align-items-center">
      <h1 class="mb-4">Seller Dashboard</h1>
      <notification-bell user-type="seller" />
    </div>
    <p v-if="user" class="text-center mb-5">Welcome, {{ user.username }}! This is your dashboard.</p>
    <p v-else class="text-center">Loading user information...</p>

    <div class="row">
      <!-- Current Dishes -->
      <div class="col-md-6 mb-4">
        <div class="card h-100">
          <div class="card-body text-center">
            <i class="bi bi-list-ul fs-1 mb-3 text-primary"></i>
            <h3>Current Menu</h3>
            <p>View and manage your current dishes</p>
            <button @click="showCurrentDishes = true" class="btn btn-primary w-100">View Menu</button>
          </div>
        </div>
      </div>

      <!-- Add New Dish -->
      <div class="col-md-6 mb-4">
        <div class="card h-100">
          <div class="card-body text-center">
            <i class="bi bi-plus-circle fs-1 mb-3 text-success"></i>
            <h3>Add New Dish</h3>
            <p>Add a new dish to your menu</p>
            <button @click="showAddDish = true" class="btn btn-success w-100">Add Dish</button>
          </div>
        </div>
      </div>

      <!-- Order History -->
      <div class="col-md-12 mb-4">
        <div class="card h-100">
          <div class="card-body text-center">
            <i class="bi bi-clock-history fs-1 mb-3 text-info"></i>
            <h3>Order History</h3>
            <p>View your past orders and sales history</p>
            <button @click="showOrderHistory = true" class="btn btn-info w-100">View History</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Current Dishes Modal -->
    <div v-if="showCurrentDishes" class="modal fade show" style="display: block" tabindex="-1">
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">Current Menu</h5>
            <button type="button" class="btn-close" @click="showCurrentDishes = false"></button>
          </div>
          <div class="modal-body">
            <div v-if="loadingDishes" class="text-center">
              <div class="spinner-border" role="status">
                <span class="visually-hidden">Loading...</span>
              </div>
            </div>
            <div v-else-if="dishes.length === 0" class="text-center">
              <p>No dishes available. Add some dishes to your menu!</p>
            </div>
            <div v-else>
              <div class="table-responsive">
                <table class="table">
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Description</th>
                      <th>Price</th>
                      <th>Stock</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="dish in dishes" :key="dish.id">
                      <td>{{ dish.name }}</td>
                      <td>{{ dish.description }}</td>
                      <td>${{ formatPrice(dish.price) }}</td>
                      <td>{{ dish.stockCount }}</td>
                      <td>
                        <button @click="editDish(dish)" class="btn btn-sm btn-primary me-2">Edit</button>
                        <button @click="deleteDish(dish.id)" class="btn btn-sm btn-danger">Delete</button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Add Dish Modal -->
    <div v-if="showAddDish" class="modal fade show" style="display: block" tabindex="-1">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">{{ editingDish ? 'Edit Dish' : 'Add New Dish' }}</h5>
            <button type="button" class="btn-close" @click="closeAddDishModal"></button>
          </div>
          <div class="modal-body">
            <form @submit.prevent="saveDish">
              <div class="mb-3">
                <label class="form-label">Name</label>
                <input v-model="newDish.name" type="text" class="form-control" required>
              </div>
              <div class="mb-3">
                <label class="form-label">Description</label>
                <textarea v-model="newDish.description" class="form-control" rows="3"></textarea>
              </div>
              <div class="mb-3">
                <label class="form-label">Price</label>
                <input v-model="newDish.price" type="number" step="0.01" class="form-control" required>
              </div>
              <div class="mb-3">
                <label class="form-label">Stock Count</label>
                <input v-model="newDish.stockCount" type="number" class="form-control" required>
              </div>
              <div class="text-end">
                <button type="button" class="btn btn-secondary me-2" @click="closeAddDishModal">Cancel</button>
                <button type="submit" class="btn btn-primary">{{ editingDish ? 'Save Changes' : 'Add Dish' }}</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>

    <!-- Order History Modal -->
    <div v-if="showOrderHistory" class="modal fade show" style="display: block" tabindex="-1">
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">Order History</h5>
            <button type="button" class="btn-close" @click="showOrderHistory = false"></button>
          </div>
          <div class="modal-body">
            <div v-if="loadingOrders" class="text-center">
              <div class="spinner-border" role="status">
                <span class="visually-hidden">Loading...</span>
              </div>
            </div>
            <div v-else-if="orders.length === 0" class="text-center">
              <p>No orders found.</p>
            </div>
            <div v-else>
              <div class="table-responsive">
                <table class="table">
                  <thead>
                    <tr>
                      <th>Order ID</th>
                      <th>Customer</th>
                      <th>Date</th>
                      <th>Items</th>
                      <th>Total</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="order in orders" :key="order.id">
                      <td>{{ order.id }}</td>
                      <td>{{ order.userId }}</td>
                      <td>{{ formatDate(order.createdAt) }}</td>
                      <td>
                        <ul class="list-unstyled mb-0">
                          <li v-for="dish in order.dishes" :key="dish.id">
                            {{ dish.name }} x{{ dish.quantity }}
                          </li>
                        </ul>
                      </td>
                      <td>${{ calculateOrderTotal(order) }}</td>
                      <td>{{ order.status }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Backdrop -->
    <div v-if="showCurrentDishes || showAddDish || showOrderHistory" 
         class="modal-backdrop fade show"></div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import { useStore } from 'vuex';
import axios from 'axios';
import { getAuthHeaders } from '@/utils/auth';
import NotificationBell from '@/components/NotificationBell.vue';

export default {
  name: 'SellerDashboard',
  components: {
    NotificationBell
  },
  setup() {
    const store = useStore();
    const user = computed(() => store.getters.currentUser);
    const dishes = ref([]);
    const orders = ref([]);
    const showCurrentDishes = ref(false);
    const showAddDish = ref(false);
    const showOrderHistory = ref(false);
    const loadingDishes = ref(false);
    const loadingOrders = ref(false);
    const editingDish = ref(null);

    const newDish = ref({
      name: '',
      description: '',
      price: 0,
      stockCount: 0
    });

    const fetchDishes = async () => {
      loadingDishes.value = true;
      try {
        const response = await axios.get('http://localhost:8083/product-service/api/dish/getDishes', {
          headers: getAuthHeaders()
        });
        dishes.value = response.data;
      } catch (err) {
        console.error('Error fetching dishes:', err);
      } finally {
        loadingDishes.value = false;
      }
    };

    // const fetchOrders = async () => {
    //   loadingOrders.value = true;
    //   try {
    //     const response = await axios.get('http://localhost:8083/product-service/api/dish/getSoldDishes', {
    //       headers: getAuthHeaders()
    //     });
    //     orders.value = response.data;
    //   } catch (err) {
    //     console.error('Error fetching orders:', err);
    //   } finally {
    //     loadingOrders.value = false;
    //   }    // };    
    
    const saveDish = async () => {
      try {
        const headers = {
          ...getAuthHeaders(),
          'Content-Type': 'application/json'
        };
        
        if (editingDish.value) {
          await axios({
            method: 'put',
            url: `http://localhost:8083/product-service/api/dish/update`,
            params: {
              dishId: editingDish.value.id,
              name: newDish.value.name,
              description: newDish.value.description,
              price: newDish.value.price,
              stockCount: newDish.value.stockCount
            },
            headers
          });
        } else {
          await axios({
            method: 'post',
            url: 'http://localhost:8083/product-service/api/dish/create',
            params: {
              name: newDish.value.name,
              description: newDish.value.description,
              price: newDish.value.price,
              stockCount: newDish.value.stockCount
            },
            headers
          });
        }

        await fetchDishes();
        closeAddDishModal();
      } catch (err) {
        console.error('Error saving dish:', err);
      }
    };

    const deleteDish = async (dishId) => {
      if (!confirm('Are you sure you want to delete this dish?')) return;

      try {
        await axios.delete('http://localhost:8083/product-service/api/dish/delete', {
          params: { dishId },
          headers: getAuthHeaders()
        });
        await fetchDishes();
      } catch (err) {
        console.error('Error deleting dish:', err);
      }
    };

    const editDish = (dish) => {
      editingDish.value = dish;
      newDish.value = { ...dish };
      showAddDish.value = true;
    };

    const closeAddDishModal = () => {
      showAddDish.value = false;
      editingDish.value = null;
      newDish.value = {
        name: '',
        description: '',
        price: 0,
        stockCount: 0
      };
    };

    const formatPrice = (price) => {
      return parseFloat(price).toFixed(2);
    };

    const formatDate = (date) => {
      return new Date(date).toLocaleString();
    };

    const calculateOrderTotal = (order) => {
      return formatPrice(order.dishes.reduce((total, dish) => {
        return total + (dish.price * dish.quantity);
      }, 0));
    };

    onMounted(() => {
      fetchDishes();
    });

    return {
      user,
      dishes,
      orders,
      showCurrentDishes,
      showAddDish,
      showOrderHistory,
      loadingDishes,
      loadingOrders,
      newDish,
      editingDish,
      saveDish,
      deleteDish,
      editDish,
      closeAddDishModal,
      formatPrice,
      formatDate,
      calculateOrderTotal
    };
  }
};
</script>

<style scoped>
.seller-dashboard {
  color: #333;
}

.card {
  transition: transform 0.2s;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  background-color: rgba(255, 255, 255, 0.9);
}

.card:hover {
  transform: translateY(-5px);
}

.modal {
  background-color: rgba(0, 0, 0, 0.5);
}

.table {
  margin-bottom: 0;
}

.table th {
  font-weight: 600;
}

.bi {
  opacity: 0.8;
}
</style>