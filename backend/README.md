# coinTrack Backend

**Modern Spring Boot backend for the CoinTrack personal finance platform**, featuring secure JWT authentication, MongoDB integration, multi-broker support (Zerodha, AngelOne, Upstox), real-time market data via WebSocket, and standardized API responses.

---

## 🚀 Features

### 🔐 **Security & Authentication**

- JWT Authentication: Secure login and protected endpoints
- BCrypt Password Hashing: Industry-standard password security
- CORS/CSRF Protection: Safe frontend-backend communication
- Role-based Access Control: User permissions and authentication guards

### 🏦 **Multi-Broker Integration**

- **Zerodha (Kite API)**: Complete integration with holdings, positions, orders, SIPs
- **AngelOne**: Full broker integration with portfolio management
- **Upstox**: Comprehensive trading platform integration
- **Extensible Architecture**: Easy to add new brokers via BrokerService interface

### 📊 **Portfolio Management**

- **Real-time Portfolio Tracking**: Live holdings and positions across all brokers
- **Aggregated Portfolio View**: Unified dashboard with multi-broker data
- **Performance Analytics**: P&L calculations and portfolio insights
- **Asynchronous Processing**: Concurrent broker data fetching for optimal performance

### 📈 **Market Data & Trading**

- **Live Market Data**: Real-time price feeds via WebSocket (NSE/BSE)
- **Historical Data**: Candle data and market history
- **Order Management**: Place, modify, and track orders
- **Watchlist Management**: Custom watchlists and alerts

### 🏗️ **Technical Excellence**

- **MongoDB Integration**: Flexible document-based data storage
- **Spring Boot 3.x**: Latest Spring ecosystem with modern Java features
- **RESTful APIs**: Standardized endpoints with consistent response formats
- **WebSocket Support**: Real-time data streaming
- **Docker Ready**: Containerized deployment support

---

## 📁 Project Structure

```
coinTrack/backend/
├── .gitattributes                           # Git attributes configuration
├── .gitignore                               # Git ignore rules
├── .mvn/wrapper/                            # Maven wrapper configuration
├── mvnw & mvnw.cmd                          # Maven wrapper scripts
├── pom.xml                                  # Maven project configuration
├── README.md                                # Project documentation
└── src/main/java/com/urva/myfinance/coinTrack/
    ├── Config/                              # Configuration classes (3 files)
    │   ├── CorsConfig.java                  # CORS configuration
    │   ├── JwtFilter.java                   # JWT authentication filter
    │   └── SecurityConfig.java              # Spring Security configuration
    ├── Controller/                          # REST controllers (6 files)
    │   ├── AuthController.java              # Authentication endpoints
    │   ├── BrokerController.java            # Multi-broker management
    │   ├── MarketDataController.java        # Market data & WebSocket
    │   ├── PortfolioController.java         # Portfolio aggregation
    │   ├── UserController.java              # User management
    │   └── WatchlistController.java         # Watchlist management
    ├── DTO/                                 # Data Transfer Objects (5 files)
    │   ├── AuthRequest.java                 # Enhanced login request
    │   ├── AuthResponse.java                # Enhanced login response
    │   ├── BrokerConnectionRequest.java     # Broker connection DTO
    │   ├── PortfolioResponse.java           # Standardized portfolio response
    │   └── StandardizedDataResponse.java    # Generic API response wrapper
    ├── Model/                               # Data models (7 files)
    │   ├── AngelOneAccount.java             # AngelOne broker account
    │   ├── BrokerAccount.java               # Abstract broker account base
    │   ├── LiveMarketData.java              # Real-time market data
    │   ├── UpstoxAccount.java               # Upstox broker account
    │   ├── User.java                        # User model
    │   ├── Watchlist.java                   # Watchlist model
    │   └── ZerodhaAccount.java              # Zerodha broker account
    ├── Repository/                          # Data repositories (6 files)
    │   ├── AngelOneAccountRepository.java
    │   ├── LiveMarketDataRepository.java
    │   ├── UpstoxAccountRepository.java
    │   ├── UserRepository.java
    │   ├── WatchlistRepository.java
    │   └── ZerodhaAccountRepository.java
    └── Service/                             # Business logic services (10 files)
        ├── AngelOneService.java             # AngelOne broker integration
        ├── AuthService.java                 # Authentication service
        ├── BrokerService.java               # Broker service interface
        ├── Connector/                       # WebSocket connectors (2 files)
        │   ├── BSEWebSocketConnector.java
        │   └── NSEWebSocketConnector.java
        ├── DataStandardizationService.java  # Data standardization
        ├── UpstoxService.java               # Upstox broker integration
        ├── UserService.java                 # User management service
        ├── WatchlistService.java            # Watchlist service
        ├── WebSocketService.java            # WebSocket management
        └── ZerodhaService.java              # Zerodha broker integration
```

---

## ⚙️ Configuration & Secrets

**Never commit real secrets!** All sensitive information is stored in `application-secret.properties` (gitignored).

### Required Configuration Files:

**`application.properties`:**

```properties
# Server Configuration
server.port=8080
spring.application.name=coinTrack

# MongoDB Configuration
spring.data.mongodb.database=cointrack
spring.data.mongodb.auto-index-creation=true

# JWT Configuration
jwt.expiration=86400000
```

**`application-secret.properties`:**

```properties
# Database
spring.data.mongodb.uri=mongodb+srv://username:password@cluster.mongodb.net/cointrack

# JWT Secret
jwt.secret=your-very-secure-jwt-secret-key-minimum-256-bits

# Zerodha API
zerodha.api.key=your_kite_api_key
zerodha.api.secret=your_kite_api_secret
zerodha.redirect.url=http://localhost:8080/api/zerodha/callback

# AngelOne API
angelone.api.key=your_angelone_api_key
angelone.client.id=your_angelone_client_id
angelone.password=your_angelone_password

# Upstox API
upstox.api.key=your_upstox_api_key
upstox.api.secret=your_upstox_api_secret
upstox.redirect.url=http://localhost:8080/api/upstox/callback
```

---

## 🔒 Security

- **JWT Bearer Token Authentication**: All protected endpoints require `Authorization: Bearer <token>`
- **BCrypt Password Hashing**: Industry-standard password security
- **CORS Configuration**: Controlled cross-origin resource sharing
- **CSRF Protection**: Cross-site request forgery prevention
- **Input Validation**: Comprehensive request validation using Bean Validation
- **Rate Limiting**: API rate limiting for security

---

## 📡 API Endpoints

### 🔐 Authentication Endpoints

| Method   | Endpoint                | Description          | Request Body    | Response          |
| -------- | ----------------------- | -------------------- | --------------- | ----------------- |
| `POST` | `/login`              | User authentication  | `AuthRequest` | `AuthResponse`  |
| `POST` | `/api/register`       | User registration    | `User`        | `User`          |
| `GET`  | `/api/tokens/status`  | Token refresh status | -               | Token status info |
| `POST` | `/api/tokens/refresh` | Force token refresh  | -               | Refresh result    |
| `GET`  | `/api/tokens/health`  | Token service health | -               | Health status     |

### 👤 User Management

| Method     | Endpoint            | Description    | Request Body | Response        |
| ---------- | ------------------- | -------------- | ------------ | --------------- |
| `GET`    | `/api/users`      | Get all users  | -            | `List<User>`  |
| `GET`    | `/api/users/{id}` | Get user by ID | -            | `User`        |
| `PUT`    | `/api/users/{id}` | Update user    | `User`     | `User`        |
| `DELETE` | `/api/users/{id}` | Delete user    | -            | Success message |

### 🏦 Multi-Broker Integration

| Method   | Endpoint                                     | Description              | Request Body                | Response           |
| -------- | -------------------------------------------- | ------------------------ | --------------------------- | ------------------ |
| `POST` | `/api/brokers/connect`                     | Connect broker account   | `BrokerConnectionRequest` | Connection status  |
| `GET`  | `/api/brokers/{broker}/status/{userId}`    | Broker connection status | -                           | Connection status  |
| `GET`  | `/api/brokers/{broker}/holdings/{userId}`  | Get broker holdings      | -                           | `List<Holding>`  |
| `GET`  | `/api/brokers/{broker}/positions/{userId}` | Get broker positions     | -                           | `List<Position>` |
| `GET`  | `/api/brokers/{broker}/orders/{userId}`    | Get broker orders        | -                           | `List<Order>`    |
| `GET`  | `/api/brokers/available`                   | List available brokers   | -                           | `List<String>`   |

### 📊 Portfolio Management

| Method  | Endpoint                           | Description            | Request Body | Response              |
| ------- | ---------------------------------- | ---------------------- | ------------ | --------------------- |
| `GET` | `/api/portfolio/value`           | Get portfolio value    | Query params | `PortfolioResponse` |
| `GET` | `/api/portfolio/details`         | Get detailed portfolio | Query params | `PortfolioResponse` |
| `GET` | `/api/portfolio/broker/{broker}` | Get broker portfolio   | Query params | `PortfolioResponse` |

### 📈 Market Data

| Method   | Endpoint                         | Description                 | Request Body | Response                     |
| -------- | -------------------------------- | --------------------------- | ------------ | ---------------------------- |
| `GET`  | `/api/market/live/{symbol}`    | Get live market data        | Query params | `StandardizedDataResponse` |
| `GET`  | `/api/market/live/batch`       | Get batch market data       | Query params | `StandardizedDataResponse` |
| `GET`  | `/api/market/live/stream`      | Stream live data (SSE)      | Query params | Server-Sent Events           |
| `POST` | `/api/market/live/subscribe`   | Subscribe to symbols        | Symbol list  | Subscription status          |
| `GET`  | `/api/market/history/{symbol}` | Get historical data         | Query params | `StandardizedDataResponse` |
| `GET`  | `/api/market/live/status`      | WebSocket connection status | -            | Connection status            |

### 📋 Legacy Broker-Specific Endpoints

| Method  | Endpoint                   | Description            | Notes                                           |
| ------- | -------------------------- | ---------------------- | ----------------------------------------------- |
| `GET` | `/api/zerodha/login-url` | Get Zerodha login URL  | Use `/api/brokers/connect` instead            |
| `GET` | `/api/zerodha/callback`  | Zerodha OAuth callback | Legacy endpoint                                 |
| `GET` | `/api/zerodha/me`        | Get Zerodha profile    | Use `/api/brokers/zerodha/status/{userId}`    |
| `GET` | `/api/zerodha/holdings`  | Get Zerodha holdings   | Use `/api/brokers/zerodha/holdings/{userId}`  |
| `GET` | `/api/zerodha/positions` | Get Zerodha positions  | Use `/api/brokers/zerodha/positions/{userId}` |
| `GET` | `/api/zerodha/orders`    | Get Zerodha orders     | Use `/api/brokers/zerodha/orders/{userId}`    |

### 🏥 Health & Monitoring

| Method  | Endpoint               | Description                | Response           |
| ------- | ---------------------- | -------------------------- | ------------------ |
| `GET` | `/actuator/health`   | Application health check   | Health status      |
| `GET` | `/api/broker-status` | Broker connectivity status | Broker status info |

---

## 🏗️ Architecture Highlights

### 🎯 **BrokerService Interface Pattern**

```java
public interface BrokerService {
    Map<String, Object> connect(String userId);
    boolean isConnected(String userId);
    List<Map<String, Object>> fetchHoldings(String userId);
    List<Map<String, Object>> fetchOrders(String userId);
    List<Map<String, Object>> fetchPositions(String userId);
}
```

- **Benefits**: Easy to add new brokers, consistent API, testable implementations

### 📊 **Standardized Response Format**

```json
{
  "status": "success",
  "data": { ... },
  "timestamp": "2025-09-20T19:12:08+05:30",
  "requestId": "uuid"
}
```

- **Consistent**: All endpoints return standardized responses
- **Typed**: Strong typing with generic `StandardizedDataResponse<T>`
- **Error Handling**: Comprehensive error responses

### 🔄 **Asynchronous Processing**

- **CompletableFuture**: Concurrent broker data fetching
- **ExecutorService**: Managed thread pools for performance
- **Non-blocking**: Improved response times for multi-broker operations

### 🔌 **WebSocket Integration**

- **Real-time Data**: Live market data streaming
- **Dual Exchange**: NSE and BSE connectivity
- **Auto-reconnection**: Fault-tolerant connections
- **Symbol Subscription**: Dynamic symbol watching

---

## 🚀 Development & Deployment

### Prerequisites

- **Java 21+**
- **Maven 3.6+**
- **MongoDB** (local or cloud)
- **Broker API Keys** (Zerodha, AngelOne, Upstox)

### Build & Run

```bash
# Clone the repository
git clone <repository-url>
cd coinTrack/backend

# Build the application
./mvnw clean compile

# Run in development mode
./mvnw spring-boot:run

# Build for production
./mvnw clean package -DskipTests
java -jar target/coinTrack-0.0.1-SNAPSHOT.jar
```

### Testing

```bash
# Run unit tests
./mvnw test

# Run with test coverage
./mvnw test jacoco:report
```

### Docker Deployment

```dockerfile
FROM openjdk:21-jdk-slim
COPY target/coinTrack-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

```bash
# Build Docker image
docker build -t cointrack-backend .

# Run container
docker run -p 8080:8080 cointrack-backend
```

---

## 🔧 Development Guidelines

### Code Style

- **Java 21**: Modern Java features and syntax
- **Spring Boot 3.x**: Latest Spring ecosystem
- **Lombok**: Reduced boilerplate code
- **Bean Validation**: Input validation annotations

### API Design Principles

- **RESTful**: Standard HTTP methods and status codes
- **Consistent**: Standardized response formats
- **Versioned**: API versioning support
- **Documented**: OpenAPI/Swagger documentation

### Security Best Practices

- **Input Validation**: Comprehensive request validation
- **Authentication**: JWT with proper expiration
- **Authorization**: Role-based access control
- **Secrets Management**: Environment-based configuration

---

## 🤝 Contributing

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Development Workflow

- Follow existing code patterns and architecture
- Add comprehensive tests for new features
- Update documentation for API changes
- Ensure all tests pass before submitting PR

---

## 📄 License

**MIT License** - see LICENSE file for details.

---

## 📞 Support

For questions or issues:

- Create an issue in the repository
- Check the documentation in `/docs`
- Review the API documentation at `/swagger-ui.html`

---

**Built with ❤️ using Spring Boot 3.x, MongoDB, and modern Java practices**
