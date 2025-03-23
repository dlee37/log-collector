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

## Installation & Setup
- placeholder for instructions

### **Clone the Repository**
```sh
git clone https://github.com/dlee37/log-collector-service.git