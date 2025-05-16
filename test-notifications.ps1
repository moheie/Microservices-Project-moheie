# Test script for sending notifications to RabbitMQ exchanges
# Run this with PowerShell to simulate various notifications for testing

Write-Host "RabbitMQ Notification Test Script" -ForegroundColor Yellow
Write-Host "--------------------------------" -ForegroundColor Yellow
Write-Host ""
Write-Host "This script will send test notifications to RabbitMQ to verify the notification system."
Write-Host "Make sure the following services are running:"
Write-Host "  1. RabbitMQ (accessible at http://localhost:15672)" -ForegroundColor Cyan
Write-Host "  2. notification-service" -ForegroundColor Cyan
Write-Host "  3. Frontend application" -ForegroundColor Cyan
Write-Host ""

# Check if RabbitMQ is accessible
try {
    $response = Invoke-WebRequest -Uri "http://localhost:15672/" -Method GET -ErrorAction SilentlyContinue
    Write-Host "RabbitMQ is accessible!" -ForegroundColor Green
}
catch {
    Write-Host "⚠️ Warning: Could not connect to RabbitMQ at http://localhost:15672" -ForegroundColor Red
    Write-Host "  Make sure RabbitMQ is running before continuing." -ForegroundColor Red
    Write-Host ""
    $continue = Read-Host "Do you want to continue anyway? (y/n)"
    if ($continue -ne "y") {
        exit
    }
}
Write-Host ""

# Function to send a notification to RabbitMQ using PowerShell's Invoke-RestMethod
function Send-RabbitMQNotification {
    param (
        [string]$exchange,
        [string]$routingKey,
        [string]$message,
        [string]$vhost = "%2F"  # Default vhost (/)
    )
    
    Write-Host "📨 Sending to exchange: $exchange with routing key: $routingKey" -ForegroundColor Green
    Write-Host "   Message: $message"
    
    # Build the request body
    $body = @{
        properties = @{}
        routing_key = $routingKey
        payload = $message
        payload_encoding = "string"
    } | ConvertTo-Json
    
    # Set up authentication
    $base64AuthInfo = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("guest:guest"))
    $headers = @{
        Authorization = "Basic $base64AuthInfo"
    }
    
    # Send the request
    try {
        $result = Invoke-RestMethod -Method Post `
            -Uri "http://localhost:15672/api/exchanges/$vhost/$exchange/publish" `
            -Body $body `
            -ContentType "application/json" `
            -Headers $headers
        
        if ($result.routed -eq $true) {
            Write-Host "✅ Message successfully routed!" -ForegroundColor Green
        } else {
            Write-Host "⚠️ Warning: Message was not routed to any queue" -ForegroundColor Yellow
        }
    }
    catch {
        Write-Host "❌ Error sending message: $_" -ForegroundColor Red
        
        # Show more detailed error information
        if ($_.Exception.Response) {
            $statusCode = $_.Exception.Response.StatusCode
            Write-Host "   Status Code: $statusCode" -ForegroundColor Red
            
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $responseBody = $reader.ReadToEnd()
                Write-Host "   Response: $responseBody" -ForegroundColor Red
                $reader.Close()
            } catch {
                # Ignore errors reading the response body
            }
        }
    }
    Write-Host ""
}

# Test Order notifications
function Test-OrderNotifications {
    Write-Host "🛒 Testing Order Notifications..." -ForegroundColor Cyan
    Write-Host "These should appear in the admin dashboard under Error Logs when severity is Error" -ForegroundColor DarkGray
    
    Send-RabbitMQNotification -exchange "admin-log" -routingKey "Order_*" -message "Order_Info: Order #12345 has been processed successfully"
    Start-Sleep -Seconds 1
    
    Send-RabbitMQNotification -exchange "admin-log" -routingKey "Order_*" -message "Order_Error: Failed to process order #12347 due to payment verification error"
}

# Test Stock notifications
function Test-StockNotifications {
    Write-Host "📦 Testing Stock Notifications..." -ForegroundColor Cyan
    Write-Host "These should appear in the admin dashboard under Error Logs when severity is Error" -ForegroundColor DarkGray
    
    Send-RabbitMQNotification -exchange "admin-log" -routingKey "Stock_*" -message "Stock_Info: Product 'Italian Pizza' inventory updated to 50 units"
    Start-Sleep -Seconds 1
    
    # Using double quotes for strings with special characters
    Send-RabbitMQNotification -exchange "admin-log" -routingKey "Stock_*" -message "Stock_Warning: Product 'American Pizza' is running low (5 units remaining)"
    Start-Sleep -Seconds 1
    
    Send-RabbitMQNotification -exchange "admin-log" -routingKey "Stock_*" -message "Stock_Error: Unable to update inventory for product 'Hawaiian Pizza' - database error"
}

# Test Payment notifications
function Test-PaymentNotifications {
    Write-Host "💳 Testing Payment Notifications..." -ForegroundColor Cyan
    Write-Host "These should appear in the admin dashboard under Payment Failures" -ForegroundColor DarkGray
    
    # Format: "orderId:userId:reason" (complete format)
    Write-Host "Testing complete payment format (orderId:userId:reason)..." -ForegroundColor DarkGray
    Send-RabbitMQNotification -exchange "payments-exchange" -routingKey "PaymentFailed" -message "54320:101:Payment verification failed due to insufficient funds"
    Start-Sleep -Seconds 1
    
    # Format: "orderId:reason" (partial format, should still work with our fix)
    Write-Host "Testing partial payment format (orderId:reason)..." -ForegroundColor DarkGray
    Send-RabbitMQNotification -exchange "payments-exchange" -routingKey "PaymentFailed" -message "54321:Credit card declined - please use another payment method"
    Start-Sleep -Seconds 1

    
}

# Check WebSocket connections and service availability
function Test-WebSocketConnections {
    Write-Host "🔌 Testing WebSocket Connections..." -ForegroundColor Cyan
    
    # First, check notification service
    $notificationServiceAvailable = $false
    try {
        Write-Host "Testing notification-service availability..."
        # Try different possible endpoints
        $endpoints = @(
            "http://localhost:8085/actuator/health",
            "http://localhost:8085/health",
            "http://localhost:8085/api/notifications/health",
            "http://localhost:8085/"
        )
        
        foreach ($endpoint in $endpoints) {
            try {
                $response = Invoke-WebRequest -Uri $endpoint -Method GET -TimeoutSec 5 -ErrorAction SilentlyContinue
                if ($response.StatusCode -eq 200) {
                    Write-Host "✅ Notification service is running at $endpoint!" -ForegroundColor Green
                    $notificationServiceAvailable = $true
                    break
                }
            } catch {
                # Continue to next endpoint
            }
        }
        
        if (-not $notificationServiceAvailable) {
            Write-Host "⚠️ Could not confirm notification-service is running on port 8085" -ForegroundColor Yellow
        }
    }
    catch {
        Write-Host "❌ Error checking notification service: $_" -ForegroundColor Red
    }
    
    # Check frontend application
    try {
        Write-Host "Testing frontend application availability..."
        $response = Invoke-WebRequest -Uri "http://localhost:8081" -Method GET -TimeoutSec 5 -ErrorAction SilentlyContinue
        Write-Host "✅ Frontend application is running on port 8081!" -ForegroundColor Green
    }
    catch {
        Write-Host "⚠️ Could not confirm frontend is running on port 8081" -ForegroundColor Yellow
        Write-Host "   Note: Frontend may be running on a different port" -ForegroundColor Gray
    }
    
    # Check RabbitMQ Management API
    try {
        # Set up authentication
        $base64AuthInfo = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("guest:guest"))
        $headers = @{
            Authorization = "Basic $base64AuthInfo"
        }
        
        # Check exchanges
        $exchanges = Invoke-RestMethod -Uri "http://localhost:15672/api/exchanges/%2F" -Headers $headers -ErrorAction SilentlyContinue
        
        Write-Host "📊 RabbitMQ Exchanges:"
        foreach ($exchange in $exchanges) {
            if ($exchange.name -eq "admin-log" -or $exchange.name -eq "payments-exchange") {
                Write-Host "   - $($exchange.name)" -ForegroundColor Green
            }
        }
        
        # Check queues
        $queues = Invoke-RestMethod -Uri "http://localhost:15672/api/queues/%2F" -Headers $headers -ErrorAction SilentlyContinue
        
        Write-Host "📬 RabbitMQ Queues:"
        foreach ($queue in $queues) {
            Write-Host "   - $($queue.name) (messages: $($queue.messages), consumers: $($queue.consumers))" -ForegroundColor Green
        }
    }
    catch {
        Write-Host "❌ Could not retrieve RabbitMQ configuration: $_" -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "📋 WebSocket Connection Checklist:" -ForegroundColor Yellow
    Write-Host "  1. Ensure notification-service is running on port 8085" -ForegroundColor White
    Write-Host "  2. Ensure RabbitMQ is running and accessible" -ForegroundColor White
    Write-Host "  3. Check browser console for WebSocket connection issues" -ForegroundColor White
    Write-Host "  4. Verify that you're logged in as an admin in the frontend" -ForegroundColor White
    Write-Host ""
    
    Write-Host "📝 Troubleshooting Tips:" -ForegroundColor Yellow
    Write-Host "  • If you see 'Message successfully routed' but no notifications appear in the UI:" -ForegroundColor White
    Write-Host "    - Check if notification-service is reading from the correct queues" -ForegroundColor White
    Write-Host "    - Verify WebSocket connections in browser console (look for ws:// connections)" -ForegroundColor White
    Write-Host "    - Check notification-service logs for any errors processing messages" -ForegroundColor White
    Write-Host ""
}



# Main menu
function Show-Menu {
    Write-Host "🔔 RabbitMQ Notification Test Menu" -ForegroundColor Green
    Write-Host "1: Test Order Notifications"
    Write-Host "2: Test Stock Notifications"
    Write-Host "3: Test Payment Notifications"
    Write-Host "4: Test All Notification Types"
    Write-Host "5: Test WebSocket Connections"
    Write-Host "8: Exit"
    Write-Host ""
}

# Menu loop
$exit = $false
while (-not $exit) {
    Show-Menu
    $choice = Read-Host "Enter your choice (1-8)"
    
    switch ($choice) {
        "1" { Test-OrderNotifications }
        "2" { Test-StockNotifications }
        "3" { Test-PaymentNotifications }
        "4" {
            Test-OrderNotifications
            Test-StockNotifications
            Test-PaymentNotifications
        }
        "5" { Test-WebSocketConnections }
        "8" { $exit = $true }
        default { Write-Host "Invalid choice, please try again." -ForegroundColor Red }
    }
    
    if (-not $exit) {
        Write-Host ""
        Write-Host "Press any key to continue..." -ForegroundColor Yellow
        $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
        Clear-Host
    }
}

Write-Host "Notification testing complete!" -ForegroundColor Green
