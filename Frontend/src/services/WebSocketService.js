// WebSocketService.js
// This service manages WebSocket connections for real-time notifications

class WebSocketService {
  constructor() {
    this.socket = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectTimeout = 3000; // 3 seconds
    this.subscribers = {};
    this.connected = false;
  }

  /**
   * Connect to the WebSocket server
   * @param {string} userType - The type of user (admin, customer, seller)
   * @param {string} token - Authentication token
   * @returns {Promise} - Resolves when connection is established
   */
  connect(userType, token) {
    return new Promise((resolve, reject) => {
      try {
        // Close any existing connection
        if (this.socket) {
          this.socket.close();
        }

        // Create new WebSocket connection
        this.socket = new WebSocket('ws://localhost:8085/notifications');

        // Set up event handlers
        this.socket.onopen = () => {
          console.log('WebSocket connection established');
          this.connected = true;
          this.reconnectAttempts = 0;
          
          // Subscribe to user-specific channel
          this.socket.send(JSON.stringify({
            type: 'subscribe',
            userType,
            token
          }));
          
          resolve();
        };

        this.socket.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            
            // Notify all subscribers
            if (this.subscribers[data.type]) {
              this.subscribers[data.type].forEach(callback => callback(data));
            }
            
            // Also notify general subscribers
            if (this.subscribers['*']) {
              this.subscribers['*'].forEach(callback => callback(data));
            }
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
          this._attemptReconnect(userType, token);
        };
      } catch (error) {
        console.error('Failed to connect to WebSocket server:', error);
        reject(error);
      }
    });
  }

  /**
   * Attempt to reconnect to the WebSocket server
   * @private
   */
  _attemptReconnect(userType, token) {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
      
      setTimeout(() => {
        this.connect(userType, token).catch(() => {
          // Silent catch as we're already handling reconnect logic
        });
      }, this.reconnectTimeout);
    } else {
      console.error('Maximum reconnection attempts reached');
    }
  }

  /**
   * Subscribe to notification events
   * @param {string} type - The type of notification to subscribe to, or '*' for all
   * @param {function} callback - The callback function to execute when notification is received
   */
  subscribe(type, callback) {
    if (!this.subscribers[type]) {
      this.subscribers[type] = [];
    }
    this.subscribers[type].push(callback);
  }

  /**
   * Unsubscribe from notification events
   * @param {string} type - The type of notification
   * @param {function} callback - The callback function to remove
   */
  unsubscribe(type, callback) {
    if (this.subscribers[type]) {
      this.subscribers[type] = this.subscribers[type].filter(cb => cb !== callback);
    }
  }

  /**
   * Send a message to the WebSocket server
   * @param {object} message - The message to send
   */
  send(message) {
    if (this.socket && this.connected) {
      this.socket.send(JSON.stringify(message));
    } else {
      console.error('Cannot send message: WebSocket not connected');
    }
  }

  /**
   * Close the WebSocket connection
   */
  disconnect() {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
      this.connected = false;
    }
  }
}

// Export as a singleton
export default new WebSocketService();
