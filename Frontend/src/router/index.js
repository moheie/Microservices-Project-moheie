import { createRouter, createWebHistory } from 'vue-router'
import store from '../store'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Auth/Login.vue'),
    meta: { requiresGuest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Auth/Register.vue'),
    meta: { requiresGuest: true }
  },  
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('../views/CustomerFun/Cart.vue'),
    meta: { requiresAuth: true, roles: ['CUSTOMER'] }
  },
  {
    path: '/admin-dashboard',
    name: 'AdminDashboard',
    component: () => import('../views/AdminFun/AdminDashboard.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/seller-dashboard',
    name: 'SellerDashboard',
    component: () => import('../views/SellerFun/SellerDashboard.vue'),
    meta: { requiresAuth: true, roles: ['RESTAURANT_REPRESENTATIVE'] }
  },
  {
    path: '/customer-dashboard',
    name: 'CustomerDashboard',
    component: () => import('../views/CustomerFun/CustomerDashboard.vue'),
    meta: { requiresAuth: true, roles: ['CUSTOMER'] }
  },
  {
    path: '/create-seller',
    name: 'CreateSeller',
    component: () => import('../views/AdminFun/CreateSeller.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/list-customers',
    name: 'ListCustomers',
    component: () => import('../views/AdminFun/ListCustomers.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/list-sellers',
    name: 'ListSellers',
    component: () => import('../views/AdminFun/ListSellers.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('../views/CustomerFun/Orders.vue'),
    meta: { requiresAuth: true, roles: ['CUSTOMER'] }
  },
  // {
  //   path: '/order-confirmation',
  //   name: 'OrderConfirmation',
  //   component: () => import('../views/CustomerFun/OrderConfirmation.vue'),
  //   meta: { requiresAuth: true, roles: ['CUSTOMER'] }
  // },
  {
    path: '/browse-dishes',
    name: 'BrowseDishes',
    component: () => import('../views/CustomerFun/BrowseDishes.vue'),
    meta: { requiresAuth: true, roles: ['CUSTOMER'] }
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

router.beforeEach((to, from, next) => {
  const isAuthenticated = store.getters.isAuthenticated
  const currentUser = store.getters.currentUser

  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!isAuthenticated) {
      next('/login')
    } else {
      // Check for role-based access restrictions
      if (to.meta.roles && to.meta.roles.length) {
        if (currentUser && to.meta.roles.includes(currentUser.role)) {
          next()
        } else {
          // Redirect to appropriate dashboard based on user role
          if (currentUser.role === 'ADMIN') {
            next('/admin-dashboard')
          } else if (currentUser.role === 'RESTAURANT_REPRESENTATIVE') {
            next('/seller-dashboard')
          } else if (currentUser.role === 'CUSTOMER') {
            next('/customer-dashboard')
          } else {
            // Fallback if role is unknown
            next('/')
          }
        }
      } else {
        // No specific role requirements
        next()
      }
    }
  } else if (to.matched.some(record => record.meta.requiresGuest)) {
    if (isAuthenticated) {
      next('/')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router