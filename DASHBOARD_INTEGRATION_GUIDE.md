# Central Dashboard - Integration Guide

**Quick Integration Steps** to connect metrics tracking to existing Central components.

---

## 🔗 Integration Points

### 1. SessionManager Integration

Find `SessionManager.java` and integrate these calls:

```java
// Add this field (auto-wired by Spring)
@Autowired
private MetricsService metricsService;

// In onAdapterConnect() method, add:
public void onAdapterConnect() {
    // ... existing code ...
    metricsService.onAdapterConnect();  // ADD THIS LINE
    logger.info("Adapter connected");
}

// In onAdapterDisconnect() method, add:
public void onAdapterDisconnect() {
    // ... existing code ...
    metricsService.onAdapterDisconnect();  // ADD THIS LINE
    logger.info("Adapter disconnected");
}

// In startSession() method, add:
public void startSession() {
    // ... existing code ...
    metricsService.onSessionStart();  // ADD THIS LINE
    sessionState = SessionState.ACTIVE;
}

// In endSession() method, add:
public void endSession() {
    // ... existing code ...
    metricsService.onSessionEnd();  // ADD THIS LINE
    sessionState = SessionState.ENDED;
}
```

### 2. SenderThreadPool Integration

Find `SenderThreadPool.java` and add metrics call:

```java
// When sending a transaction, add:
private void sendTransaction(String txnJson) {
    // ... existing send code ...
    metricsService.recordTransactionSent();  // ADD THIS LINE
}
```

### 3. ReceiverThreadPool Integration

Find `ReceiverThreadPool.java` and add metrics calls:

```java
// Add metrics field
@Autowired
private MetricsService metricsService;

// In the response processing loop, add:
private void processResponse(String responseJson) {
    // ... existing code to parse response ...
    
    metricsService.recordTransactionReceived();  // Record received
    
    // Extract transaction result and response time
    String result = response.getStatus();  // APPROVED, REJECTED, or ERROR
    long responseTimeMs = response.getResponseTimeMs();
    
    metricsService.recordTransactionResult(result, responseTimeMs);  // Record result
    
    // ... rest of processing ...
}
```

---

## 📋 Step-by-Step Integration

### Step 1: Find and Open SessionManager
```bash
vi central/src/main/java/com/gracefulshutdown/central/application/SessionManager.java
```

### Step 2: Add @Autowired Field
Add after class declaration:
```java
@Autowired(required = false)
private MetricsService metricsService;
```

### Step 3: Add Calls to Connection Methods
In each method where connection/session state changes:
```java
if (metricsService != null) {
    metricsService.onAdapterConnect();  // or other method
}
```

### Step 4: Repeat for SenderThreadPool and ReceiverThreadPool
Similar pattern - add @Autowired and call metrics methods at key points.

### Step 5: Rebuild and Test
```bash
cd central
mvn clean package -DskipTests
java -jar target/central-transaction-server-1.0.0.jar
```

### Step 6: Access Dashboard
```
Browser: http://localhost:5000/central/dashboard
```

---

## 🔍 Finding the Right Methods

### SessionManager Methods to Modify
1. **onAdapterConnect()** → add `metricsService.onAdapterConnect()`
2. **onAdapterDisconnect()** → add `metricsService.onAdapterDisconnect()`
3. **startSession()** → add `metricsService.onSessionStart()`
4. **endSession()** → add `metricsService.onSessionEnd()`

### SenderThreadPool Methods to Modify
1. **sendTransaction()** → add `metricsService.recordTransactionSent()` when sending

### ReceiverThreadPool Methods to Modify
1. **processResponse()** → add `metricsService.recordTransactionReceived()`
2. **processResponse()** → add `metricsService.recordTransactionResult(result, timeMs)`

---

## ✅ Verification

### After Integration
1. **Start Central**:
   ```bash
   java -jar target/central-transaction-server-1.0.0.jar
   ```

2. **Open Dashboard**:
   ```
   http://localhost:5000/central/dashboard
   ```

3. **Send Test Data**:
   ```
   > start-session
   > send-transactions 10
   > end-session
   ```

4. **Verify Dashboard Updates**:
   - Check if "Minutes Connected" increases
   - Check if "Minutes Session Active" increases
   - Check if "Transactions Sent" increases
   - Check if "Transactions Received" increases
   - Check if approvals/rejections are recorded

5. **Verify Database**:
   ```bash
   sqlite3 central-metrics.db "SELECT COUNT(*) FROM metrics;"
   ```

---

## 🚀 Quick Integration Template

### Template for SessionManager
```java
@Component
public class SessionManager {
    
    @Autowired(required = false)
    private MetricsService metricsService;
    
    public void onAdapterConnect() {
        // ... existing code ...
        if (metricsService != null) {
            metricsService.onAdapterConnect();
        }
    }
    
    public void onAdapterDisconnect() {
        // ... existing code ...
        if (metricsService != null) {
            metricsService.onAdapterDisconnect();
        }
    }
    
    public void startSession() {
        // ... existing code ...
        if (metricsService != null) {
            metricsService.onSessionStart();
        }
    }
    
    public void endSession() {
        // ... existing code ...
        if (metricsService != null) {
            metricsService.onSessionEnd();
        }
    }
}
```

### Template for SenderThreadPool
```java
@Component
public class SenderThreadPool {
    
    @Autowired(required = false)
    private MetricsService metricsService;
    
    private void sendTransaction(String txnJson) {
        // ... existing send code ...
        try {
            socket.getOutputStream().write(txnJson.getBytes());
            if (metricsService != null) {
                metricsService.recordTransactionSent();
            }
        } catch (IOException e) {
            // ... error handling ...
        }
    }
}
```

### Template for ReceiverThreadPool
```java
@Component
public class ReceiverThreadPool {
    
    @Autowired(required = false)
    private MetricsService metricsService;
    
    private void processResponse(String responseJson, long responseTimeMs) {
        // Parse response
        Response response = parseResponse(responseJson);
        
        // Record received
        if (metricsService != null) {
            metricsService.recordTransactionReceived();
            metricsService.recordTransactionResult(
                response.getStatus(),  // "APPROVED", "REJECTED", "ERROR"
                responseTimeMs
            );
        }
        
        // ... rest of processing ...
    }
}
```

---

## 🆘 Troubleshooting Integration

### Dashboard Shows No Updates
**Problem**: Metrics not appearing on dashboard
**Solution**: 
1. Check that metrics methods are being called
2. Add logging: `logger.info("Recording transaction sent");`
3. Verify database file exists: `ls -la central-metrics.db`
4. Verify MetricsService is autowired: check Spring logs

### NullPointerException on metricsService
**Problem**: MetricsService is null
**Solution**:
1. Use null-check: `if (metricsService != null) { ... }`
2. Ensure @Autowired annotation is present
3. Ensure @Component annotation on MetricsService (already in code)

### Database Lock Errors
**Problem**: "database is locked" error
**Solution**:
1. Close all other connections to the database
2. Increase connection timeout (not usually needed)
3. Check that capture interval isn't too aggressive

### Slow Performance
**Problem**: Dashboard is slow to load
**Solution**:
1. Increase refresh interval: change 30000 to 60000 in dashboard.html
2. Limit history records returned
3. Add database indexes (already done)

---

## 📊 Expected Behavior

### When Dashboard is Working Correctly
1. **On Page Load**:
   - Status section loads with current values (zeros initially)
   - History tables show "No data" message

2. **After Adapter Connects**:
   - "Adapter Connection" badge changes to "CONNECTED"
   - "Minutes Connected" starts incrementing

3. **After start-session**:
   - "Session Status" badge changes to "ACTIVE"
   - "Minutes Session Active" starts incrementing

4. **While Sending Transactions**:
   - "Transactions Sent" count increases
   - "Transactions Received" count increases
   - "Approved", "Rejected", "Errored" counts update
   - Approval percentage updates

5. **After Enough Time**:
   - History tables populate with data points
   - Each tab shows data for its time period

---

## 🎯 Summary

To enable the dashboard:

1. **Add metrics calls** to SessionManager (4 methods)
2. **Add metrics call** to SenderThreadPool (1 method)
3. **Add metrics calls** to ReceiverThreadPool (2 methods)
4. **Rebuild** the JAR
5. **Start** Central server
6. **Access** http://localhost:5000/central/dashboard

Total integration time: **15-30 minutes** depending on code familiarity.

---

**Status**: Ready for integration  
**Complexity**: Low (copy-paste code snippets)  
**Testing**: Quick (5 minute end-to-end test)  

