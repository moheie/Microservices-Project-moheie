<script>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { getAuthHeaders } from '@/utils/auth'; // Assuming you have a utility function for auth headers

export default {
  name: 'BrowseDishes',  setup() {
    const route = useRouter().currentRoute.value;
    const dishes = ref([]);
    const loading = ref(true);
    const error = ref(null);
    const searchQuery = ref('');
    const restaurantFilter = ref(route.query.restaurant || '');

    const fetchDishes = async () => {
      loading.value = true;
      try {
        const headers = getAuthHeaders();
        const response = await axios.get('http://localhost:8083/product-service/api/dish/getDishForCustomer', { headers });
        dishes.value = response.data;
      } catch (err) {
        error.value = 'Failed to load dishes. Please try again.';
        console.error('Error fetching dishes:', err);
      } finally {
        loading.value = false;
      }
    };

    const addToCartFromBrowse = async (dish) => {
      try {
        const headers = getAuthHeaders();
        console.log('Adding to cart:', dish);
        console.log('Headers:', headers);
        
        await axios.post(
          `http://localhost:8084/order-service/api/cart/add`,
          null, 
          {
            headers: headers,
            params: {
              dishId: dish.id, 
              quantity: 1,
              dishName: dish.name,
              dishPrice: dish.price, 
              companyName: dish.companyName
            }
          }
        );
        alert('Item added to cart successfully!');
      } catch (err) {
        console.error('Error adding to cart:', err);
        alert('Failed to add item to cart. Please try again.');
      }
    };

    const restaurants = computed(() => {
      const uniqueRestaurants = new Set();
      dishes.value.forEach(dish => {
        if (dish.companyName) {
          uniqueRestaurants.add(dish.companyName);
        }
      });
      return [...uniqueRestaurants];
    });

    const filteredDishes = computed(() => {
      let filtered = [...dishes.value];

      // Apply search filter
      if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase();
        filtered = filtered.filter(dish => 
          dish.name.toLowerCase().includes(query) || 
          dish.description?.toLowerCase().includes(query)
        );
      }

      // Apply restaurant filter
      if (restaurantFilter.value) {
        filtered = filtered.filter(dish => dish.companyName === restaurantFilter.value);
      }

      return filtered;
    });

    const formatPrice = (price) => {
      return parseFloat(price).toFixed(2);
    };

    const formatStock = (stock) => {
      return stock
    };

    onMounted(() => {
      fetchDishes();
    });

    return {
      dishes,
      loading,
      error,
      searchQuery,
      restaurantFilter,
      restaurants,
      filteredDishes,
      formatPrice,
      formatStock,
      addToCartFromBrowse
    };
  }
}
</script>

<template>
  <div class="container">
    <div class="card">
      <div class="card-header bg-primary text-white">
        <h2>Browse Dishes</h2>
      </div>
      <div class="card-body">
        <div v-if="loading" class="text-center">
          <div class="spinner-border" role="status">
            <span class="visually-hidden">Loading...</span>
          </div>
          <p>Loading dishes...</p>
        </div>
        <div v-else-if="error" class="alert alert-danger">
          {{ error }}
        </div>
        <div v-else>
          <!-- Search and Filter Section -->
          <div class="row mb-4">
            <div class="col-md-6">
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
            <div class="col-md-6">
              <div class="input-group">
                <label class="input-group-text">Restaurant</label>
                <select class="form-select" v-model="restaurantFilter">
                  <option value="">All Restaurants</option>
                  <option v-for="restaurant in restaurants" :key="restaurant" :value="restaurant">
                    {{ restaurant }}
                  </option>
                </select>
              </div>
            </div>
          </div>

          <!-- Dishes Grid -->
          <div class="row">
            <div v-for="dish in filteredDishes" :key="dish.id" class="col-md-4 mb-4">
              <div class="card h-100">
                <div class="card-body">
                  <h5 class="card-title">{{ dish.name }}</h5>
                  <p class="card-text dish-description">{{ dish.description }}</p>
                  <p><strong>Restaurant:</strong> {{ dish.companyName }}</p>
                  <p><strong>Stock:</strong> {{ formatStock(dish.stockCount) }}</p>
                  <p class="price">${{ formatPrice(dish.price) }}</p>
                </div>
                <div class="card-footer">
                  <button class ="btn btn-primary" @click="addToCartFromBrowse(dish)">
                    Add to Cart
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- No Results -->
          <div v-if="filteredDishes.length === 0" class="text-center mt-4">
            <p>No dishes match your search criteria.</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s, box-shadow 0.3s;
  background-color: rgba(255, 255, 255, 0.9);
}

.card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
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