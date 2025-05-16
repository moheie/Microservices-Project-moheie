// WebSocketService.js
// This service manages WebSocket connections for real-time notifications

class WebSocketService {
  constructor() {
    this.socket = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectTimeout = 5000; // 3 seconds
    this.subscribers = new Map();
    this.connected = false;
    this.userId = null;
  }
  connect(userType, headers) {
    return new Promise((resolve, reject) => {
      try {
        // Close any existing connection
        if (this.socket) {
          this.socket.close();
        }

        // Extract user ID from auth token
        const token = headers.Authorization?.split(' ')[1];
        if (token) {
          this.userId = this.extractUserIdFromToken(token);
        }

        // Create new WebSocket connection with token as query parameter
        const wsUrl = `ws://localhost:8085/notifications?token=${token}`;
        this.socket = new WebSocket(wsUrl);

        this.socket.onopen = () => {
          console.log('WebSocket connection established');
          this.connected = true;
          this.reconnectAttempts = 0;

          // Send initial subscription message
          const subscriptionMessage = {
            type: 'subscribe',
            userId: this.userId,
            userType: userType
          };

          this.socket.send(JSON.stringify(subscriptionMessage));
          resolve();
        };

        this.socket.onmessage = (event) => {
          try {
            const notification = JSON.parse(event.data);
            console.log('Received notification:', notification);

            // Notify all relevant subscribers
            this.notifySubscribers(notification);
          } catch (error) {
            console.error('Error processing WebSocket message:', error);
          }
        };

        this.socket.onerror = (error) => {
          console.error('WebSocket error:', error);
          this.connected = false;
          reject(error);
        };

        this.socket.onclose = () => {
          console.log('WebSocket connection closed');
          this.connected = false;
          this._attemptReconnect(userType, headers);
        };

      } catch (error) {
        console.error('Failed to establish WebSocket connection:', error);
        reject(error);
      }
    });
  }

  
  subscribe(type, callback) {
    if (!this.subscribers.has(type)) {
      this.subscribers.set(type, new Set());
    }
    this.subscribers.get(type).add(callback);
    console.log(`Subscribed to ${type} notifications`);
  }


  unsubscribe(type, callback) {
    if (this.subscribers.has(type)) {
      this.subscribers.get(type).delete(callback);
      if (this.subscribers.get(type).size === 0) {
        this.subscribers.delete(type);
      }
      console.log(`Unsubscribed from ${type} notifications`);
    }
  }

  
  notifySubscribers(notification) {
    // Always notify universal subscribers
    if (this.subscribers.has('*')) {
      this.subscribers.get('*').forEach(callback => callback(notification));
    }

    // Notify type-specific subscribers
    if (notification.type && this.subscribers.has(notification.type)) {
      this.subscribers.get(notification.type).forEach(callback => callback(notification));
    }

    // Notify user type specific subscribers
    if (notification.userType && this.subscribers.has(notification.userType)) {
      this.subscribers.get(notification.userType).forEach(callback => callback(notification));
    }
  }


  _attemptReconnect(userType, headers) {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
      
      setTimeout(() => {
        this.connect(userType, headers).catch(() => {
          // Silent catch as we're already handling reconnect logic
        });
      }, this.reconnectTimeout * this.reconnectAttempts); // Exponential backoff
    } else {
      console.error('Maximum reconnection attempts reached');
    }
  }


  extractUserIdFromToken(token) {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(atob(base64).split('').map(c => {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
      }).join(''));

      const payload = JSON.parse(jsonPayload);
      return payload.userId || payload.sub;
    } catch (error) {
      console.error('Error extracting user ID from token:', error);
      return null;
    }
  }


  disconnect() {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
      this.connected = false;
      this.subscribers.clear();
    }
  }

  /**
   * Check if the WebSocket is connected
   * @returns {boolean}
   */
  isConnected() {
    return this.connected && this.socket?.readyState === WebSocket.OPEN;
  }
}

// Export as singleton
export default new WebSocketService();