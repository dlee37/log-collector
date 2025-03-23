# Log Collector Service

**A lightweight REST API to fetch and filter log entries from Unix-based servers.**  
This service allows on-demand monitoring of log files without needing direct SSH access.

## Features
- Fetch logs from `/var/log/` via HTTP REST API.
- Supports filtering by **filename, keyword, and number of entries**.
- Returns logs in **reverse chronological order** (newest first).
- Optimized for **large files (>1GB)**.
- Minimal dependencies in business logic (only built-in Java libraries).

---

### **Prerequisites**
- Java 21+
- Maven (for dependency management)
- Docker (optional but highly recommended)
- Intellij (optional but highly recommended)

## Installation & Setup

### 1. Clone the repository
```bash
git clone https://github.com/dlee37/log-collector.git
```

### 2. Build the application
```bash
mvn clean install
```

### 3. Add a sample log directory and add log files
```bash
mkdir -p ./sample-logs
```

### 4. Build the Docker image
```bash
docker build -t log-collector:latest .
```

### 5. Run and mount the logs into the container
```bash
docker run -p 8080:8080 -v "$(pwd)/sample-logs:/var/log" log-collector:latest
```

## Running with Debugger Enabled
To enable remote debugging (e.g., for IntelliJ or VS Code), update your `Dockerfile` to expose the debugger and include the appropriate JVM flags.

### 1. Modify the Dockerfile

In the final stage of your Dockerfile, update the `ENTRYPOINT` and expose the debugger port (`5005`):

```dockerfile
# Add this to your runtime Dockerfile stage

EXPOSE 5005

ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "app.jar"]
```

- suspend=n means the app starts immediately and does not wait for the debugger
- Set suspend=y if you want the JVM to wait for your IDE to connect before starting execution

### 2. Comment out the production entrypoint line in the Dockerfile
```dockerfile
# ENTRYPOINT ["java", "-jar", "log-collector-service.jar"]
```

### 3. Build the Image
```bash
docker build -t log-collector:debug .
```

### 4. Run the container with debug port exposed
```bash
docker run -p 8080:8080 -p 5005:5005 -v "$(pwd)/sample-logs:/var/log" log-collector:debug
```

## Connect your debugger on Intellij
1. Go to Run > Edit Configurations > Add New > Remote JVM Debugger
2. Set host to localhost and port 5005
3. Click debug

## Development Notes
- Uses RandomAccessFile + reverse chunk reading
- Assumes log files are always new with '\n' character
- Cache auto evicts every 2 minutes or when LRU cap is hit
- Supports pagination
- Debug friendly Dockerfile

## Design Overview
- **Controller Layer**: Accepts incoming HTTP requests and delegates to services.
- **Service Layer**: Reads files from the end of the file with 4KB chunks in reverse using Java's built in RandomAccessFile class
- **Models**: DTO for request and response for better modularity
- **Caching**: In-memory TTL and LRU cache using Java's built in LinkedHashMap to cache more detailed requests (search term, larger offsets)
- **Paging**: Pages requests to not overwhelm responses
- **Timeout Handling**: Custom Timeout utility to handle long-lasting requests
- **Docker Runtime**: Mounts local files directly to /var/log

## Testing
- To run all the unit tests, run the following command:
```bash
mvn test
```
`mvn clean install` will also run all tests and build the jar.