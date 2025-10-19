# 🚀 AI Stock Trading Platform

<div align="center">

![Platform Banner](https://img.shields.io/badge/AI-Powered-blue?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Production%20Ready-success?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)

**A Professional Full-Stack Trading Platform with Real-Time AI Predictions**

[Features](#-features) • [Tech Stack](#-tech-stack) • [Installation](#-installation) • [Architecture](#-architecture) • [API Documentation](#-api-documentation) • [Contributing](#-contributing)

</div>

---

## 📊 Overview

AI Stock Trading Platform is a comprehensive, enterprise-grade financial application that combines cutting-edge **Deep Learning** with modern web technologies to deliver real-time stock market predictions, advanced portfolio management, and intelligent trading capabilities.

### 🎯 Key Highlights

- 🤖 **LSTM Neural Networks** for accurate stock price predictions
- 📈 **Real-Time Market Data** integration with live updates
- 💼 **Advanced Portfolio Management** with detailed analytics
- 📰 **AI-Powered News Analysis** with sentiment tracking
- 🎨 **Modern Material Design** interface with responsive layouts
- 🔐 **Secure Authentication** with JWT and role-based access
- 💎 **Three-Tier Subscription Model** (FREE/PREMIUM/PRO)

---

## ✨ Features

### 🎯 Core Functionality

| Feature | Description |
|---------|-------------|
| **AI Predictions** | Multi-stock LSTM models (AAPL, MSFT, S&P 500) trained on historical data |
| **Live Trading** | Real-time order execution with market, limit, and stop-loss orders |
| **Portfolio Tracker** | Comprehensive portfolio monitoring with performance analytics |
| **News Insights** | AI-powered sentiment analysis of financial news |
| **Advanced Charts** | Interactive visualizations with technical indicators |
| **Risk Management** | Automated risk assessment and position sizing |

### 💎 Premium Features

- **Pro Dashboard** with advanced analytics
- **Custom Alerts** and notifications
- **API Access** for external integrations
- **Webhook Support** for real-time events
- **Priority Support** and dedicated resources

---

## 🛠️ Tech Stack

### Frontend
```
🎨 Angular 18 (Standalone Components)
📝 TypeScript 5.x
🎭 Material Design 3
📊 Chart.js for data visualization
🔄 RxJS for reactive programming
```

### Backend
```
☕ Java Spring Boot 3.x
🔐 Spring Security with JWT
💾 JPA/Hibernate for ORM
🌐 RESTful API architecture
📮 WebSocket support for real-time updates
```

### AI/ML
```
🐍 Python 3.10+
🧠 TensorFlow/Keras
📈 LSTM Neural Networks
🔧 Keras Tuner for hyperparameter optimization
📊 NumPy, Pandas, Scikit-learn
💹 yfinance for market data
```

### Database & Infrastructure
```
🗄️ PostgreSQL / MySQL
🐳 Docker & Docker Compose
📦 Maven for Java dependencies
📦 npm for frontend packages
🔄 Git for version control
```

---

## 🚀 Installation

### Prerequisites

- **Node.js** 18+ and npm
- **Java JDK** 17+
- **Python** 3.10+
- **PostgreSQL** 13+ or MySQL 8+
- **Git**

### Quick Start

#### 1️⃣ Clone the Repository
```bash
git clone https://github.com/Gharsallah-Islem/AI-Stock-Trading-Platform.git
cd AI-Stock-Trading-Platform
```

#### 2️⃣ Set Up Backend (Spring Boot)
```bash
cd Backend/stock-prediction-backend

# Configure database in application.properties
# src/main/resources/application.properties

# Build and run
./mvnw clean install
./mvnw spring-boot:run
```

**Backend runs on:** `http://localhost:8080`

#### 3️⃣ Set Up AI/ML Services (Python)
```bash
cd AI

# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Train models (if not already trained)
python stock_predictor_pro.py

# Models will be saved in AI/ directory
```

#### 4️⃣ Set Up Frontend (Angular)
```bash
cd Frontend/stock-prediction-frontend

# Install dependencies
npm install

# Start development server
ng serve
```

**Frontend runs on:** `http://localhost:4200`

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Angular Frontend (Port 4200)              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │Dashboard │  │ Trading  │  │Portfolio │  │  News    │   │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘   │
└───────┼─────────────┼─────────────┼─────────────┼──────────┘
        │             │             │             │
        └─────────────┴─────────────┴─────────────┘
                          │
                    REST API (HTTPS)
                          │
┌─────────────────────────┼──────────────────────────────────┐
│             Spring Boot Backend (Port 8080)                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │  Auth        │  │  Trading     │  │  Portfolio   │    │
│  │  Service     │  │  Service     │  │  Service     │    │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘    │
│         │                  │                  │            │
│  ┌──────┴──────────────────┴──────────────────┴───────┐   │
│  │         Python Model Service (Subprocess)          │   │
│  └─────────────────────────┬──────────────────────────┘   │
└────────────────────────────┼─────────────────────────────┘
                             │
┌────────────────────────────┼─────────────────────────────┐
│              Python AI/ML Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ LSTM Model   │  │ Data Pipeline│  │  Prediction  │  │
│  │   (AAPL)     │  │  Processor   │  │   Service    │  │
│  ├──────────────┤  └──────────────┘  └──────────────┘  │
│  │ LSTM Model   │                                        │
│  │   (MSFT)     │         ┌──────────────┐             │
│  ├──────────────┤         │  yfinance    │             │
│  │ LSTM Model   │         │  Market Data │             │
│  │   (^GSPC)    │         └──────────────┘             │
│  └──────────────┘                                        │
└──────────────────────────────────────────────────────────┘
                             │
┌────────────────────────────┼─────────────────────────────┐
│                    Database Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │    Users     │  │  Portfolios  │  │    Trades    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└──────────────────────────────────────────────────────────┘
```

---

## 📡 API Documentation

### Authentication Endpoints

#### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "johndoe",
  "password": "SecurePass123!"
}

Response: { "token": "jwt-token-here" }
```

### Prediction Endpoints

#### Get Stock Prediction
```http
GET /api/predictions/predict?symbol=AAPL
Authorization: Bearer {jwt-token}

Response:
{
  "symbol": "AAPL",
  "currentPrice": 175.50,
  "predictions": {
    "1d": 176.80,
    "7d": 182.30,
    "30d": 189.50
  },
  "confidence": 0.87,
  "trend": "bullish",
  "timestamp": "2025-10-19T10:30:00Z"
}
```

#### Get Multiple Stock Predictions
```http
GET /api/predictions/batch?symbols=AAPL,MSFT,^GSPC
Authorization: Bearer {jwt-token}
```

### Portfolio Endpoints

#### Get User Portfolio
```http
GET /api/portfolio
Authorization: Bearer {jwt-token}
```

#### Execute Trade
```http
POST /api/trades/execute
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
  "symbol": "AAPL",
  "type": "buy",
  "quantity": 10,
  "orderType": "market"
}
```

---

## 🧪 Testing

### Backend Tests
```bash
cd Backend/stock-prediction-backend
./mvnw test
```

### Frontend Tests
```bash
cd Frontend/stock-prediction-frontend
ng test
```

### AI Model Tests
```bash
cd AI
python -m pytest tests/
```

---

## 📦 Deployment

### Docker Deployment

```bash
# Build and run all services
docker-compose up -d

# Services will be available at:
# Frontend: http://localhost:80
# Backend: http://localhost:8080
# Database: localhost:5432
```

### Production Build

#### Frontend
```bash
cd Frontend/stock-prediction-frontend
ng build --configuration production
# Output: dist/stock-prediction-frontend
```

#### Backend
```bash
cd Backend/stock-prediction-backend
./mvnw clean package -DskipTests
# Output: target/stock-prediction-backend-0.0.1-SNAPSHOT.jar
```

---

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Workflow

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Branch Strategy

- `main` - Production-ready code
- `develop` - Integration branch for features
- `feature/*` - New features
- `bugfix/*` - Bug fixes
- `release/*` - Release preparation

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**Islem Gharsallah**

- GitHub: [@Gharsallah-Islem](https://github.com/Gharsallah-Islem)
- LinkedIn: [Connect with me](https://www.linkedin.com/in/islem-gharsallah-649a63305/)

---

## 🙏 Acknowledgments

- **TensorFlow Team** for the amazing ML framework
- **Spring Boot** for the robust backend framework
- **Angular Team** for the powerful frontend framework
- **Material Design** for the beautiful UI components
- **Chart.js** for the interactive charts
- **yfinance** for the market data API

---

## 📊 Project Stats

![GitHub stars](https://img.shields.io/github/stars/Gharsallah-Islem/AI-Stock-Trading-Platform?style=social)
![GitHub forks](https://img.shields.io/github/forks/Gharsallah-Islem/AI-Stock-Trading-Platform?style=social)
![GitHub watchers](https://img.shields.io/github/watchers/Gharsallah-Islem/AI-Stock-Trading-Platform?style=social)

---

<div align="center">

**Made with ❤️ and lots of ☕**

⭐ Star this repository if you find it helpful!

</div>
