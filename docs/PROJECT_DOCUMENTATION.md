# 📚 Project Documentation

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Technology Stack](#technology-stack)
4. [Project Structure](#project-structure)
5. [Setup & Installation](#setup--installation)
6. [Development Guide](#development-guide)
7. [API Documentation](#api-documentation)
8. [Deployment](#deployment)
9. [Testing](#testing)
10. [Troubleshooting](#troubleshooting)

## 🎯 Project Overview

### What is AI Stock Trading Platform?

AI Stock Trading Platform is a comprehensive full-stack application that combines artificial intelligence, real-time data processing, and modern web technologies to provide users with:

- **Intelligent Stock Price Predictions** using LSTM neural networks
- **Real-Time Market Data** and live trading capabilities
- **Advanced Portfolio Management** with detailed analytics
- **AI-Powered News Analysis** with sentiment tracking
- **Professional Trading Interface** with multiple order types
- **Subscription-Based Access** (FREE, PREMIUM, PRO tiers)

### Key Features

#### For Users
- View real-time stock predictions
- Execute virtual trades
- Track portfolio performance
- Get AI-powered market insights
- Set up custom alerts
- Access news with sentiment analysis

#### For Administrators
- Monitor system health
- Manage user subscriptions
- View usage analytics
- Control feature access
- Review prediction accuracy

## 🏗️ Architecture

### High-Level Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                         Client Layer                              │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              Angular 18 Frontend (SPA)                      │ │
│  │  - Material Design UI                                       │ │
│  │  - Reactive State Management                                │ │
│  │  - Real-time Updates (WebSocket)                            │ │
│  │  - Chart Visualizations                                     │ │
│  └────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTPS/WSS
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                      Application Layer                            │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │           Spring Boot 3 Backend (REST API)                  │ │
│  │  ┌───────────┐  ┌───────────┐  ┌───────────┐             │ │
│  │  │ Security  │  │  Trading  │  │ Portfolio │             │ │
│  │  │ (JWT)     │  │  Service  │  │  Service  │             │ │
│  │  └───────────┘  └───────────┘  └───────────┘             │ │
│  │  ┌───────────┐  ┌───────────┐  ┌───────────┐             │ │
│  │  │   User    │  │   Stock   │  │Prediction │             │ │
│  │  │  Service  │  │  Service  │  │  Service  │             │ │
│  │  └───────────┘  └───────────┘  └───────────┘             │ │
│  └────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                         AI/ML Layer                               │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              Python ML Service                              │ │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐                 │ │
│  │  │  LSTM    │  │  LSTM    │  │  LSTM    │                 │ │
│  │  │  Model   │  │  Model   │  │  Model   │                 │ │
│  │  │ (AAPL)   │  │ (MSFT)   │  │ (^GSPC)  │                 │ │
│  │  └──────────┘  └──────────┘  └──────────┘                 │ │
│  │  ┌────────────────────────────────────────┐                │ │
│  │  │     yfinance - Market Data API         │                │ │
│  │  └────────────────────────────────────────┘                │ │
│  └────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                       Data Layer                                  │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              PostgreSQL Database                            │ │
│  │  - Users & Authentication                                   │ │
│  │  - Portfolios & Trades                                      │ │
│  │  - Predictions & Analytics                                  │ │
│  │  - Subscriptions & Usage                                    │ │
│  └────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

### Data Flow

#### 1. Prediction Request Flow
```
User Request → Angular UI → HTTP Request → Spring Boot API
     ↓
JWT Validation → Rate Limiting → Prediction Service
     ↓
Python Subprocess → LSTM Model → yfinance Data
     ↓
Prediction Result → JSON Response → Angular UI → Chart Display
```

#### 2. Trading Flow
```
User Trade → Angular Form → HTTP POST → Spring Boot API
     ↓
JWT Validation → Trade Service → Portfolio Update
     ↓
Database Transaction → WebSocket Notification → Real-time UI Update
```

## 💻 Technology Stack

### Frontend Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Angular | 18.x | SPA Framework |
| TypeScript | 5.x | Type-safe JavaScript |
| Material Design | Latest | UI Components |
| Chart.js | Latest | Data Visualization |
| RxJS | Latest | Reactive Programming |
| SCSS | - | Styling |

### Backend Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Programming Language |
| Spring Boot | 3.x | Backend Framework |
| Spring Security | - | Authentication/Authorization |
| Spring Data JPA | - | Database Access |
| JWT | - | Token Authentication |
| Maven | 3.9+ | Build Tool |

### AI/ML Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Python | 3.10+ | Programming Language |
| TensorFlow | 2.x | Deep Learning Framework |
| Keras | Latest | Neural Network API |
| NumPy | Latest | Numerical Computing |
| Pandas | Latest | Data Manipulation |
| yfinance | Latest | Market Data |
| Scikit-learn | Latest | ML Utilities |

### Database & Infrastructure

| Technology | Version | Purpose |
|------------|---------|---------|
| PostgreSQL | 15+ | Primary Database |
| Docker | Latest | Containerization |
| Docker Compose | Latest | Multi-container Apps |
| Git | Latest | Version Control |

## 📁 Project Structure

```
AI-Stock-Trading-Platform/
│
├── Frontend/
│   └── stock-prediction-frontend/
│       ├── src/
│       │   ├── app/
│       │   │   ├── core/              # Core services & guards
│       │   │   ├── shared/            # Shared components
│       │   │   ├── features/          # Feature modules
│       │   │   │   ├── dashboard/
│       │   │   │   ├── trading-interface/
│       │   │   │   ├── portfolio-tracker/
│       │   │   │   ├── news-insights/
│       │   │   │   └── settings/
│       │   │   └── services/          # Business services
│       │   ├── assets/                # Static assets
│       │   └── styles/                # Global styles
│       ├── angular.json
│       ├── package.json
│       └── tsconfig.json
│
├── Backend/
│   └── stock-prediction-backend/
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/com/stockapp/
│       │   │   │   ├── config/        # Configuration classes
│       │   │   │   ├── controller/    # REST controllers
│       │   │   │   ├── model/         # Entity classes
│       │   │   │   ├── repository/    # Data repositories
│       │   │   │   ├── service/       # Business logic
│       │   │   │   ├── security/      # Security config
│       │   │   │   ├── dto/           # Data transfer objects
│       │   │   │   └── exception/     # Custom exceptions
│       │   │   └── resources/
│       │   │       └── application.yml
│       │   └── test/                  # Unit & integration tests
│       ├── pom.xml
│       └── mvnw
│
├── AI/
│   ├── stock_predictor_pro.py       # Main ML script
│   ├── predict_simple.py             # Simple predictions
│   ├── predict_live.py               # Live predictions
│   ├── requirements.txt              # Python dependencies
│   ├── lstm_model_AAPL.h5           # Trained models
│   ├── lstm_model_MSFT.h5
│   └── lstm_model_^GSPC.h5
│
├── .github/
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug_report.md
│   │   └── feature_request.md
│   └── PULL_REQUEST_TEMPLATE.md
│
├── docs/
│   └── BRANCH_STRATEGY.md
│
├── docker-compose.yml
├── .gitignore
├── README.md
├── CONTRIBUTING.md
├── CHANGELOG.md
├── SECURITY.md
└── LICENSE
```

## 🚀 Setup & Installation

### Prerequisites Checklist

- [ ] Node.js 18+ installed
- [ ] Java JDK 17+ installed
- [ ] Python 3.10+ installed
- [ ] PostgreSQL 15+ or MySQL 8+ installed
- [ ] Git installed
- [ ] Maven 3.9+ (or use included mvnw)

### Step-by-Step Installation

#### 1. Clone Repository
```bash
git clone https://github.com/Gharsallah-Islem/AI-Stock-Trading-Platform.git
cd AI-Stock-Trading-Platform
```

#### 2. Database Setup
```sql
-- PostgreSQL
CREATE DATABASE stock_trading_db;
CREATE USER stock_user WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE stock_trading_db TO stock_user;
```

#### 3. Backend Setup
```bash
cd Backend/stock-prediction-backend

# Update application.yml with your database credentials
# src/main/resources/application.yml

# Build project
./mvnw clean install

# Run backend
./mvnw spring-boot:run
```

Backend will run on `http://localhost:8080`

#### 4. AI/ML Setup
```bash
cd AI

# Create virtual environment
python -m venv venv

# Activate virtual environment
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Train models (if not already trained)
python stock_predictor_pro.py
```

#### 5. Frontend Setup
```bash
cd Frontend/stock-prediction-frontend

# Install dependencies
npm install

# Start development server
ng serve
```

Frontend will run on `http://localhost:4200`

### Docker Setup (Alternative)

```bash
# Build and run all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

## 🔧 Development Guide

### Running in Development Mode

#### Terminal 1 - Backend
```bash
cd Backend/stock-prediction-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Terminal 2 - Frontend
```bash
cd Frontend/stock-prediction-frontend
ng serve --open
```

#### Terminal 3 - Python Environment
```bash
cd AI
source venv/bin/activate  # Keep active for backend ML calls
```

### Making Changes

1. **Create Feature Branch**
```bash
git checkout develop
git pull origin develop
git checkout -b feature/your-feature-name
```

2. **Make Changes**
- Write code
- Add tests
- Update documentation

3. **Test Changes**
```bash
# Frontend
ng test
ng lint

# Backend
./mvnw test

# Python
pytest tests/
```

4. **Commit and Push**
```bash
git add .
git commit -m "feat: your feature description"
git push origin feature/your-feature-name
```

5. **Create Pull Request**
- Go to GitHub
- Create PR from your branch to `develop`
- Fill in PR template
- Request reviews

### Code Style Guidelines

#### TypeScript/Angular
```typescript
// Use standalone components
@Component({
  selector: 'app-example',
  standalone: true,
  imports: [CommonModule, /* ... */],
  template: `...`
})
export class ExampleComponent implements OnInit {
  // Use dependency injection
  private readonly service = inject(ServiceName);
  
  // Use observables for async data
  data$: Observable<Data>;
  
  ngOnInit(): void {
    this.data$ = this.service.getData();
  }
}
```

#### Java/Spring Boot
```java
@Service
@RequiredArgsConstructor
public class ExampleService {
    private final Repository repository;
    
    @Transactional
    public Result performOperation(Input input) {
        // Business logic here
        return result;
    }
}
```

#### Python
```python
def predict_stock(
    symbol: str,
    days: int = 30
) -> np.ndarray:
    """
    Predict stock prices.
    
    Args:
        symbol: Stock ticker symbol
        days: Number of days to predict
        
    Returns:
        Array of predicted prices
    """
    # Implementation
```

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api
```

### Authentication

#### Register
```http
POST /auth/register
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

#### Login
```http
POST /auth/login
Content-Type: application/json

{
  "username": "johndoe",
  "password": "SecurePass123!"
}

Response: { "token": "jwt-token-here" }
```

### Predictions

#### Get Single Stock Prediction
```http
GET /predictions/predict?symbol=AAPL
Authorization: Bearer {token}
```

#### Batch Predictions
```http
GET /predictions/batch?symbols=AAPL,MSFT,^GSPC
Authorization: Bearer {token}
```

### Portfolio

#### Get Portfolio
```http
GET /portfolio
Authorization: Bearer {token}
```

#### Execute Trade
```http
POST /trades/execute
Authorization: Bearer {token}
Content-Type: application/json

{
  "symbol": "AAPL",
  "type": "buy",
  "quantity": 10,
  "orderType": "market"
}
```

## 🚢 Deployment

### Production Checklist

- [ ] Environment variables configured
- [ ] Database backups enabled
- [ ] HTTPS/SSL certificates installed
- [ ] API rate limiting enabled
- [ ] Logging configured
- [ ] Monitoring set up
- [ ] Security headers configured
- [ ] CORS properly configured

### Environment Variables

#### Backend (.env)
```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=stock_trading_db
DB_USER=stock_user
DB_PASSWORD=your_password
JWT_SECRET=your-secret-key-min-256-bits
PYTHON_PATH=/path/to/python
ALPHAVANTAGE_API_KEY=your_api_key
```

#### Frontend (environment.ts)
```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.yourdomain.com',
  wsUrl: 'wss://api.yourdomain.com/ws'
};
```

## 🧪 Testing

### Frontend Tests
```bash
# Unit tests
ng test

# E2E tests
ng e2e

# Coverage report
ng test --code-coverage
```

### Backend Tests
```bash
# All tests
./mvnw test

# Specific test class
./mvnw test -Dtest=ServiceNameTest

# Integration tests
./mvnw verify
```

### Python Tests
```bash
# All tests
pytest

# With coverage
pytest --cov=.

# Specific test file
pytest tests/test_predictor.py
```

## 🔍 Troubleshooting

### Common Issues

#### Frontend won't start
```bash
# Clear node modules
rm -rf node_modules package-lock.json
npm install
```

#### Backend fails to connect to database
- Check database is running
- Verify credentials in application.yml
- Check firewall settings

#### Python models not found
```bash
# Retrain models
cd AI
python stock_predictor_pro.py
```

#### Port already in use
```bash
# Kill process on port (Windows)
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Kill process on port (Linux/Mac)
lsof -i :8080
kill -9 <PID>
```

### Getting Help

1. Check [GitHub Issues](https://github.com/Gharsallah-Islem/AI-Stock-Trading-Platform/issues)
2. Review [Contributing Guidelines](CONTRIBUTING.md)
3. Contact maintainers

---

**Last Updated:** October 19, 2025
**Version:** 1.0.0
