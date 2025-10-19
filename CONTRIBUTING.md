# Contributing to AI Stock Trading Platform

First off, thank you for considering contributing to the AI Stock Trading Platform! It's people like you that make this platform such a great tool.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Testing Guidelines](#testing-guidelines)

## 📜 Code of Conduct

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to the project maintainers.

### Our Standards

- Using welcoming and inclusive language
- Being respectful of differing viewpoints and experiences
- Gracefully accepting constructive criticism
- Focusing on what is best for the community
- Showing empathy towards other community members

## 🚀 Getting Started

### Prerequisites

Before you begin, ensure you have installed:
- Node.js 18+
- Java JDK 17+
- Python 3.10+
- PostgreSQL 13+ or MySQL 8+
- Git

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
```bash
git clone https://github.com/YOUR-USERNAME/AI-Stock-Trading-Platform.git
cd AI-Stock-Trading-Platform
```

3. Add the upstream repository:
```bash
git remote add upstream https://github.com/Gharsallah-Islem/AI-Stock-Trading-Platform.git
```

### Set Up Development Environment

Follow the installation instructions in the [README.md](README.md) to set up your local development environment.

## 🔄 Development Workflow

### Branch Strategy

We use Git Flow for our branching strategy:

- `main` - Production-ready code only
- `develop` - Integration branch for features
- `feature/*` - New features (branch from `develop`)
- `bugfix/*` - Bug fixes (branch from `develop`)
- `hotfix/*` - Critical fixes (branch from `main`)
- `release/*` - Release preparation (branch from `develop`)

### Creating a Feature Branch

```bash
# Update your local develop branch
git checkout develop
git pull upstream develop

# Create your feature branch
git checkout -b feature/your-feature-name

# Make your changes and commit
git add .
git commit -m "feat: add your feature"

# Push to your fork
git push origin feature/your-feature-name
```

## 📝 Coding Standards

### Frontend (Angular/TypeScript)

- Follow [Angular Style Guide](https://angular.io/guide/styleguide)
- Use TypeScript strict mode
- Use standalone components
- Follow reactive programming patterns with RxJS
- Use Material Design components
- Write meaningful variable and function names
- Maximum line length: 120 characters

Example:
```typescript
// Good
export class UserProfileComponent implements OnInit {
  private readonly userService = inject(UserService);
  userProfile$: Observable<UserProfile>;

  ngOnInit(): void {
    this.userProfile$ = this.userService.getUserProfile();
  }
}

// Bad
export class UserProfile {
  constructor(private us: UserService) {}
  data: any;
}
```

### Backend (Java/Spring Boot)

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use meaningful names for classes, methods, and variables
- Keep methods small and focused (max 30 lines)
- Use constructor injection for dependencies
- Write comprehensive JavaDoc for public APIs
- Handle exceptions properly

Example:
```java
// Good
@Service
@RequiredArgsConstructor
public class PredictionService {
    private final PythonModelService modelService;
    private final StockDataService stockDataService;

    /**
     * Generates stock price predictions for the specified symbol.
     * 
     * @param symbol Stock symbol (e.g., "AAPL")
     * @return PredictionResponse containing predictions
     * @throws PredictionException if prediction fails
     */
    public PredictionResponse predictStock(String symbol) {
        // Implementation
    }
}
```

### AI/ML (Python)

- Follow [PEP 8 Style Guide](https://pep8.org/)
- Use type hints for function parameters and return values
- Document functions with docstrings
- Use meaningful variable names
- Keep functions focused and small
- Use virtual environments

Example:
```python
# Good
def predict_stock_price(
    symbol: str,
    model: tf.keras.Model,
    scaler: MinMaxScaler,
    days_ahead: int = 30
) -> np.ndarray:
    """
    Predicts stock prices for the specified number of days.
    
    Args:
        symbol: Stock ticker symbol
        model: Trained LSTM model
        scaler: Fitted MinMaxScaler for data normalization
        days_ahead: Number of days to predict
        
    Returns:
        Array of predicted prices
    """
    # Implementation
```

## 💬 Commit Guidelines

We follow [Conventional Commits](https://www.conventionalcommits.org/) specification.

### Commit Message Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, semicolons, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks
- `perf`: Performance improvements

### Examples

```bash
feat(frontend): add real-time price updates to dashboard
fix(backend): resolve null pointer in prediction service
docs(readme): update installation instructions
refactor(ai): optimize LSTM model training pipeline
test(frontend): add unit tests for portfolio component
```

## 🔍 Pull Request Process

### Before Submitting

1. **Update your branch** with the latest changes from `develop`:
```bash
git checkout develop
git pull upstream develop
git checkout your-feature-branch
git rebase develop
```

2. **Run all tests** and ensure they pass:
```bash
# Frontend
cd Frontend/stock-prediction-frontend
ng test
ng lint

# Backend
cd Backend/stock-prediction-backend
./mvnw test

# AI
cd AI
pytest tests/
```

3. **Update documentation** if needed

4. **Check code quality**:
```bash
# Frontend
ng lint --fix

# Backend
./mvnw checkstyle:check

# Python
black AI/
flake8 AI/
```

### Pull Request Template

When creating a PR, use this template:

```markdown
## Description
Brief description of the changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed

## Screenshots (if applicable)
Add screenshots for UI changes

## Checklist
- [ ] Code follows the project's style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex code
- [ ] Documentation updated
- [ ] No new warnings generated
- [ ] Tests added/updated
- [ ] All tests passing
```

### Review Process

1. At least one maintainer must review and approve
2. All CI/CD checks must pass
3. No merge conflicts with the target branch
4. All reviewer comments addressed
5. Documentation updated if needed

## 🧪 Testing Guidelines

### Frontend Tests

```typescript
// Component test example
describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load market data on init', () => {
    // Test implementation
  });
});
```

### Backend Tests

```java
@SpringBootTest
class PredictionServiceTest {
    
    @Autowired
    private PredictionService predictionService;
    
    @Test
    void shouldReturnPredictionForValidSymbol() {
        // Given
        String symbol = "AAPL";
        
        // When
        PredictionResponse response = predictionService.predictStock(symbol);
        
        // Then
        assertNotNull(response);
        assertEquals(symbol, response.getSymbol());
    }
}
```

### Python Tests

```python
import pytest
from stock_predictor_pro import StockPredictor

def test_prediction_generation():
    """Test that predictions are generated correctly."""
    predictor = StockPredictor()
    predictions = predictor.predict("AAPL", days=30)
    
    assert predictions is not None
    assert len(predictions) == 30
    assert all(p > 0 for p in predictions)
```

## 📊 Performance Guidelines

- Frontend: Keep bundle size under 2MB
- Backend: API response time < 500ms
- AI: Model prediction time < 2 seconds
- Database: Query optimization for < 100ms response

## 🐛 Bug Reports

When reporting bugs, include:

1. **Description**: Clear description of the bug
2. **Steps to Reproduce**: Detailed steps
3. **Expected Behavior**: What should happen
4. **Actual Behavior**: What actually happens
5. **Screenshots**: If applicable
6. **Environment**: OS, browser, versions
7. **Logs**: Relevant error messages

## 💡 Feature Requests

When suggesting features, include:

1. **Problem**: What problem does it solve?
2. **Solution**: Proposed solution
3. **Alternatives**: Alternative solutions considered
4. **Additional Context**: Any other relevant information

## 📞 Contact

- Create an issue for bugs or features
- Email: your.email@example.com
- Discord: [Join our server](#)

## 🎉 Recognition

Contributors will be recognized in:
- README.md Contributors section
- Release notes
- Project website (if applicable)

---

Thank you for contributing to AI Stock Trading Platform! 🚀
