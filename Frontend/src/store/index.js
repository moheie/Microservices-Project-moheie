import { createStore } from 'vuex'
import axios from 'axios'

const API_URL = 'http://localhost:8080/auth-service/api/auth'
const ORDER_URL = 'http://localhost:8084/order-service/api/orders'
const CART_URL = 'http://localhost:8084/order-service/api/cart'
const PRODUCT_URL = 'http://localhost:8083/product-service/api/dish'

export default createStore({
  state: {
    token: localStorage.getItem('token') || null,
    user: JSON.parse(localStorage.getItem('user')) || null,
    cart: [],
    dishes: []
  },
  getters: {
    isAuthenticated: state => !!state.token,
    currentUser: state => state.user,
    cartItems: state => state.cart,
    allDishes: state => state.dishes
  },
  mutations: {
    setToken(state, token) {
      state.token = token
      localStorage.setItem('token', token)
    },
    setUser(state, user) {
      state.user = user
      localStorage.setItem('user', JSON.stringify(user))
    },
    clearAuth(state) {
      state.token = null
      state.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },
    setCart(state, cart) {
      state.cart = cart
    },
    setDishes(state, dishes) {
      state.dishes = dishes
    }
  },
  actions: {
    async login({ commit }, credentials) {
      const url = new URL(`${API_URL}/login`);
      Object.entries(credentials).forEach(([key, value]) => url.searchParams.append(key, value));
      const response = await fetch(url, { method: 'POST' });
      const data = await response.json();
      if (!data.token) {
        throw new Error('Token not found in response');
      }
      commit('setToken', data.token);
      const user = {
        id: data.id,
        username: data.username,
        email: data.email,
        companyName: data.companyName,
        role: data.role
      };
      commit('setUser', user);
      return data;
    },
    async register({ commit }, userData) {
      const url = new URL(`${API_URL}/register/customer`);
      Object.entries(userData).forEach(([key, value]) => url.searchParams.append(key, value));
      const response = await fetch(url, { method: 'POST' });

      // Log the raw response for debugging
      console.log('Raw response:', response);

      let data;
      try {
        data = await response.json(); // Attempt to parse JSON
      } catch (err) {
        console.error('Error parsing JSON:', err);
        throw new Error('Invalid response from server. Please try again.');
      }

      if (response.ok) {
        commit('setToken', data.token);
        const user = {
          id: data.id,
          username: data.username,
          email: data.email,
          companyName: data.companyName,
          role: data.role
        };
        commit('setUser', user);
        return data;
      } else {
        throw new Error(data.message || 'Registration failed');
      }
    },
    logout({ commit }) {
      commit('clearAuth')
    },
    async fetchCart({ commit, state }) {
      const response = await axios.get(`${CART_URL}/get`, {
        headers: { Authorization: `Bearer ${state.token}` }
      })
      commit('setCart', response.data)
      return response
    },
    async addToCart({ dispatch, state }, { productId, quantity }) {
      await axios.post(`${ORDER_URL}/cart/add`, null, {
        params: { productId, quantity },
        headers: { Authorization: `Bearer ${state.token}` }
      })
      dispatch('fetchCart')
    },
    async fetchDishes({ commit }) {
      const response = await axios.get(`${PRODUCT_URL}/getDishes`)
      commit('setDishes', response.data)
      return response
    }
  }
})