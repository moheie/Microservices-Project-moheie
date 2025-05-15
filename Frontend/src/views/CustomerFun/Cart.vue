<template>
  <div class="container">
    <div class="card">
      <div class="card-header bg-primary text-white">
        <h2>My Shopping Cart</h2>
      </div>      <div class="card-body">        <div v-if="loading" class="text-center">
          <div class="spinner-border" role="status">
            <span class="visually-hidden">Loading...</span>
          </div>
          <p>Loading your cart...</p>
        </div>
        <div v-else-if="hasDishes">
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
            <tbody>                <tr v-for="(item, index) in getGroupedDishes(cart.dishes || [])" :key="index">                
                <td>{{ item.name || 'Unknown Item' }}</td>
                <td>{{ item.companyName || 'Unknown Restaurant' }}</td>
                <td>${{ formatPrice(item.price || 0) }}</td>
                <td>
                  <div class="quantity-control">
                    <button class="btn btn-sm btn-secondary" @click="decreaseQuantity(item)">-</button>
                    <span class="mx-2">{{ item.quantity || 1 }}</span>
                    <button class="btn btn-sm btn-secondary" @click="increaseQuantity(item)">+</button>
                  </div>
                </td>
                <td>${{ formatPrice((item.price || 0) * (item.quantity || 1)) }}</td>
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
        </div>        <div v-else class="text-center">
          <p>Your cart is empty or the cart data structure is different than expected.</p>
          <router-link to="/browse-dishes" class="btn btn-primary mb-3">Browse Dishes</router-link>
          <div class="mt-3">
            <details>
              <summary class="btn btn-outline-secondary btn-sm">Debug Info (Click to expand)</summary>
              <div class="mt-3 text-start">                <div class="alert alert-info">
                  <p><strong>Cart Object Structure:</strong></p>
                  <pre class="mb-0">{{ JSON.stringify(cart, null, 2) }}</pre>
                </div>
                
                <div v-if="checkCartDataIssues.length > 0" class="alert alert-warning mt-3">
                  <p><strong>Detected Issues:</strong></p>
                  <ul>
                    <li v-for="(issue, index) in checkCartDataIssues" :key="index">
                      {{ issue }}
                    </li>
                  </ul>
                  <p class="mt-2"><strong>Possible Solutions:</strong></p>
                  <ol>
                    <li>Check authentication - make sure you're logged in</li>
                    <li>Check backend service is running correctly</li>
                    <li>Try adding a product to your cart</li>
                    <li>Check browser console for API response details</li>
                  </ol>
                </div>
                
                <p><strong>Does cart exist?</strong> {{ cart ? 'Yes' : 'No' }}</p>
                <p><strong>Does cart.dishes exist?</strong> {{ cart && cart.dishes ? 'Yes' : 'No' }}</p>
                <p><strong>Is cart.dishes an array?</strong> {{ cart && cart.dishes && Array.isArray(cart.dishes) ? 'Yes' : 'No' }}</p>
                <p><strong>cart.dishes length:</strong> {{ cart && cart.dishes && Array.isArray(cart.dishes) ? cart.dishes.length : 'N/A' }}</p>
                  <button class="btn btn-warning mt-3" @click="fetchCart">
                  Refresh Cart Data
                </button>
                
                <div class="mt-3">
                  <button class="btn btn-info mt-2 me-2" @click="testWithSampleData">
                    Test with Sample Data
                  </button>
                  <button class="btn btn-success mt-2" @click="addTestDish">
                    Add Test Dish to Cart
                  </button>
                </div>
              </div>
            </details>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import store from '@/store';
import { getAuthHeaders } from '@/utils/auth'; // Adjust the import path as necessary

export default {
  name: 'Cart',
  setup() {    const router = useRouter();
    const cart = ref({ dishes: [] });
    const loading = ref(true);
    const error = ref(null);    
    const safeCartDishes = computed(() => {
      return cart.value && cart.value.dishes ? cart.value.dishes : [];
    });

    const hasDishes = computed(() => {
      return cart.value && 
        cart.value.dishes && 
        Array.isArray(cart.value.dishes) && 
        cart.value.dishes.length > 0;
    });    
    // Add this function to check cart issues
    const checkCartDataIssues = computed(() => {
      const issues = [];
      
      if (!cart.value) {
        issues.push('Cart object is undefined or null');
        return issues;
      }
      
      if (!cart.value.dishes) {
        issues.push('Cart does not have a dishes property');
      } else if (!Array.isArray(cart.value.dishes)) {
        issues.push('Cart dishes is not an array');
      } else if (cart.value.dishes.length === 0) {
        // Not necessarily an issue, just empty cart
      }
      
      // Check if the cart has expected properties based on Postman response
      if (!cart.value.id && cart.value.id !== 0) issues.push('Missing id property');
      if (!cart.value.userId && cart.value.userId !== 0) issues.push('Missing userId property');
      if (!cart.value.productIds) issues.push('Missing productIds property');
      
      return issues;
    });
    const fetchCart = async () => {
      loading.value = true;
      try {
        const headers = getAuthHeaders();
        
        // Add debugging for auth headers
        console.log('Auth headers:', headers);
        
        // First get cart information for basic cart details
        const cartResponse = await axios.get('http://localhost:8084/order-service/api/cart/get', {headers});
        console.log('Axios Cart response (full cart):', cartResponse.data);
        
        // Then directly fetch only the dishes using our new endpoint
        const dishesResponse = await axios.get('http://localhost:8084/order-service/api/cart/dishes', {headers});
        console.log('Axios Dishes response:', dishesResponse.data);
        
        // For testing - use this sample data if needed
        const sampleDishes = [
          {
            "id": null,
            "dishId": 9,
            "name": "Burger",
            "companyName": "McDonalds",
            "price": 49.99
          },
          {
            "id": null,
            "dishId": 9,
            "name": "Burger",
            "companyName": "McDonalds", 
            "price": 49.99
          }
        ];
        
        // Check for URL parameter to use sample data (for testing)
        const urlParams = new URLSearchParams(window.location.search);
        const useSampleData = urlParams.get('useSampleData') === 'true';
        
        // Use the dishes directly from our new endpoint
        let dishesArray;
        if (useSampleData) {
          console.log('Using sample dish data because of URL parameter');
          dishesArray = sampleDishes;
        } else {
          // The dishes endpoint directly returns an array of dishes
          dishesArray = Array.isArray(dishesResponse.data) ? dishesResponse.data : [];
          console.log('Dishes array from endpoint:', dishesArray);
        }
        
        // Log detailed information about the dishes
        console.log('Dishes count:', dishesArray.length);
        if (dishesArray.length > 0) {
          console.log('First dish sample:', dishesArray[0]);
        }
        
        // Update our cart value, using the dishes array and basic cart info
        cart.value = {
          dishes: dishesArray,
          // Get these properties from the cart response
          id: cartResponse.data?.id || 0,
          userId: cartResponse.data?.userId || 0,
          productIds: cartResponse.data?.productIds || []
        };
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
    };const calculateTotal = () => {
      if (!cart.value.dishes || cart.value.dishes.length === 0) return 0;
      
      // Use grouped dishes to calculate total with quantities
      const groupedItems = getGroupedDishes(cart.value.dishes);
      return groupedItems.reduce((sum, dish) => {
        return sum + (dish.price * dish.quantity);
      }, 0);
    };

    const getGroupedDishes = (dishes) => {
      if (!dishes || dishes.length === 0) return [];
      
      // Group dishes by dishId
      const groupedDishes = dishes.reduce((acc, dish) => {
        const existingDish = acc.find(d => d.dishId === dish.dishId);
        if (existingDish) {
          existingDish.quantity += 1;
        } else {
          acc.push({
            ...dish,
            quantity: 1
          });
        }
        return acc;
      }, []);
      
      return groupedDishes;
    };

    const formatPrice = (price) => {
      return parseFloat(price).toFixed(2);
    };    const decreaseQuantity = async (item) => {
      if (item.quantity > 1) {
        try {
          await axios.delete(`http://localhost:8084/order-service/api/cart/remove`, {
            params: {
              productId: item.dishId, // Use dishId instead of id
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
    };    const increaseQuantity = async (item) => {
      try {
        console.log('Increasing quantity for item:', item);
        
        // Validate all required fields are present
        if (!item.dishId) {
          console.error('Missing dishId in item', item);
          error.value = 'Cannot increase quantity: Item is missing required fields';
          return;
        }
        
        await axios.post(`http://localhost:8084/order-service/api/cart/add`, null, {
          params: {
            productId: item.dishId, // Use dishId instead of id
            quantity: 1,
            dishName: item.name || 'Unknown Item', // Use name instead of dishName, with fallback
            dishPrice: item.price || 0, // Use price instead of dishPrice, with fallback
            companyName: item.companyName || 'Unknown Restaurant' // With fallback
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
    };const removeItem = async (item) => {
      try {
        console.log('Removing item:', item);
        
        // Validate required fields
        if (!item.dishId) {
          console.error('Missing dishId in item', item);
          error.value = 'Cannot remove item: Missing required fields';
          return;
        }
        
        await axios.delete(`http://localhost:8084/order-service/api/cart/remove`, {
          params: {
            productId: item.dishId, // Use dishId instead of id
            quantity: item.quantity || 1 // Default to 1 if quantity is missing
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
    };    const testWithSampleData = () => {
      // Create more diverse sample data with different quantities
      const sampleData = {
        "id": 1,
        "userId": 2,
        "dishes": [
          // Multiple of the same dish to test grouping
          {
            "id": null,
            "dishId": 9,
            "name": "Burger",
            "companyName": "McDonalds",
            "price": 49.99
          },
          {
            "id": null,
            "dishId": 9,
            "name": "Burger",
            "companyName": "McDonalds", 
            "price": 49.99
          },
          {
            "id": null,
            "dishId": 9, 
            "name": "Burger",
            "companyName": "McDonalds",
            "price": 49.99
          },
          // Different dish from same restaurant
          {
            "id": null,
            "dishId": 11,
            "name": "Fries",
            "companyName": "McDonalds", 
            "price": 19.99
          },
          // Dish from another restaurant
          {
            "id": null,
            "dishId": 20,
            "name": "Fried Chicken",
            "companyName": "KFC", 
            "price": 79.99
          }
        ],
        "productIds": [9, 9, 9, 11, 20]
      };
      
      cart.value = sampleData;
      
      // Log the result and visually verify that grouping works
      console.log('Set sample cart data:', cart.value);
      console.log('Grouped dishes for display:', getGroupedDishes(cart.value.dishes));
      console.log('Total price:', calculateTotal());
    };

    const addTestDish = async () => {
      try {
        const headers = getAuthHeaders();
        // Create a sample dish
        const testDish = {
          productId: Math.floor(Math.random() * 1000) + 1, // Random ID for testing
          quantity: 1,
          dishName: "Test Burger",
          dishPrice: 49.99,
          companyName: "Test Restaurant"
        };
        
        console.log('Adding test dish to cart:', testDish);
        
        // Call the add endpoint
        await axios.post(`http://localhost:8084/order-service/api/cart/add`, null, {
          params: {
            productId: testDish.productId,
            quantity: testDish.quantity,
            dishName: testDish.dishName,
            dishPrice: testDish.dishPrice,
            companyName: testDish.companyName
          },
          headers: headers
        });
        
        // Refresh the cart to show the new dish
        fetchCart();
      } catch (err) {
        error.value = 'Failed to add test dish. Please try again.';
        console.error('Error adding test dish:', err);
        if (err.response && err.response.status === 401) {
          store.commit('LOGOUT');
          window.location.href = '/login';
        }
      }
    };    onMounted(() => {
      fetchCart();
    });    return {
      cart,
      loading,
      error,
      hasDishes,
      calculateTotal,
      increaseQuantity,
      decreaseQuantity,
      removeItem,
      clearCart,
      proceedToCheckout,
      getGroupedDishes,
      formatPrice,
      safeCartDishes,
      checkCartDataIssues,
      fetchCart,
      testWithSampleData,
      addTestDish
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