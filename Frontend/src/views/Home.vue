<template>
  <div class="home">
    <h1 class="mb-4">Home Dishes delivery</h1>
    <div class="row">
      <div v-for="dish in dishes" :key="dish.id" class="col-md-4 mb-4">
        <div class="card h-100">
          <div class="card-body">
            <h5 class="card-title">{{ dish.name }}</h5>
            <p class="card-text">{{ dish.description }}</p>
            <p class="card-text">
              <strong>Price:</strong> ${{ dish.price }}
            </p>
            <p class="card-text">
              <strong>Stock:</strong> {{ dish.stockCount }}
            </p>
            <button 
              v-if="isAuthenticated"
              @click="addToCart(dish.id)"
              class="btn btn-primary"
              :disabled="dish.stockCount === 0"
            >
              Add to Cart
            </button>
            <router-link v-else to="/login" class="btn btn-primary">
              Login to Order
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { computed, onMounted } from 'vue'
import { useStore } from 'vuex'

export default {
  name: 'Home',
  setup() {
    const store = useStore()

    const dishes = computed(() => store.getters.allDishes)
    const isAuthenticated = computed(() => store.getters.isAuthenticated)

    onMounted(async () => {
      try {
        await store.dispatch('fetchDishes')
      } catch (error) {
        console.error('Error fetching dishes:', error)
      }
    })

    const addToCart = async (productId) => {
      try {
        await store.dispatch('addToCart', { productId, quantity: 1 })
      } catch (error) {
        console.error('Error adding to cart:', error)
      }
    }

    return {
      dishes,
      isAuthenticated,
      addToCart
    }
  }
}
</script>

<style scoped>
.card {
  transition: transform 0.2s;
}

.card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}
</style> 