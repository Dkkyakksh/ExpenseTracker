# Expense Tracker — Spring Boot + Gemini Vision API

A REST API backend for tracking personal expenses. Supports AI-powered receipt scanning via Google Gemini Vision, manual expense entry, monthly budget tracking, savings rollover, and analytics.

---

## Tech Stack

- Java 17, Spring Boot 3.2
- Spring Data JPA + Hibernate → PostgreSQL
- Spring WebFlux WebClient (for Gemini API calls)
- Bean Validation (`jakarta.validation`)
- Lombok
- Maven

---

## Architecture

```
Client
  │
  ├── POST /api/expenses/upload     → GeminiService → ExpenseService → PostgreSQL
  ├── POST /api/expenses            → ExpenseService → PostgreSQL
  ├── GET|PATCH|DELETE /api/expenses/**
  ├── GET /api/analytics/savings-progress
  ├── POST /api/analytics/budget
  ├── POST /api/analytics/sweep
  └── GET /api/health
```

Layered: Controller → Service → Repository. DTOs cross the API boundary — entities never exposed directly.

---

## Database Schema

### `expenses`
| Column             | Type           |
|--------------------|----------------|
| id                 | BIGSERIAL PK   |
| merchant_name      | VARCHAR        |
| category           | VARCHAR        |
| total_amount       | NUMERIC(10,2)  |
| tax_amount         | NUMERIC(10,2)  |
| currency           | VARCHAR(3)     |
| expense_date       | DATE           |
| payment_method     | VARCHAR        |
| notes              | VARCHAR(500)   |
| raw_extracted_text | TEXT           |
| image_file_name    | VARCHAR        |
| created_at         | TIMESTAMP      |

### `expense_items`
| Column      | Type          |
|-------------|---------------|
| id          | BIGSERIAL PK  |
| expense_id  | BIGINT FK     |
| name        | VARCHAR       |
| quantity    | INTEGER       |
| unit_price  | NUMERIC(10,2) |
| total_price | NUMERIC(10,2) |

### `monthly_budgets`
| Column         | Type          |
|----------------|---------------|
| id             | BIGSERIAL PK  |
| month          | DATE (unique) |
| planned_salary | NUMERIC(12,2) |
| is_rolled_over | BOOLEAN       |

### `user_stats`
| Column                    | Type          |
|---------------------------|---------------|
| id                        | BIGINT PK (=1)|
| total_accumulated_savings | NUMERIC(14,2) |
| total_deficit             | NUMERIC(14,2) |

---

## Setup

### Prerequisites
- Java 17+, Maven 3.8+, PostgreSQL 14+
- [Google Gemini API key](https://aistudio.google.com/app/apikey)

### 1. Create database
```sql
CREATE DATABASE expense_tracker_db;
```

### 2. Configure local properties
Copy and fill in `src/main/resources/application-local.properties`:
```properties
spring.datasource.username=postgres
spring.datasource.password=your_password
gemini.api.key=your_gemini_api_key
```

### 3. Run
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

API starts on `http://localhost:8080`

---

## API Reference

### Expenses

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/expenses/upload` | Upload receipt image → Gemini parses → saved |
| POST | `/api/expenses` | Manual expense entry (no AI) |
| GET | `/api/expenses` | All expenses, newest first |
| GET | `/api/expenses/{id}` | Single expense with line items |
| PATCH | `/api/expenses/{id}` | Update fields (corrections) |
| DELETE | `/api/expenses/{id}` | Delete expense + line items |
| GET | `/api/expenses/category/{category}` | Filter by category (case-insensitive) |
| GET | `/api/expenses/categories` | All distinct categories |
| GET | `/api/expenses/date-range?start=&end=` | Filter by date range |
| GET | `/api/expenses/summary` | Total spend + breakdown by category/merchant |
| GET | `/api/expenses/summary/date-range?start=&end=` | Summary for date range |

### Analytics & Budget

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/analytics/budget` | Set/update monthly planned salary |
| GET | `/api/analytics/savings-progress` | Current balance + 12-month history + lifetime savings |
| POST | `/api/analytics/sweep` | Trigger month-end rollover |

### Health

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | App + DB connectivity check |

---

## Key cURL Examples

```bash
# Upload receipt
curl -X POST http://localhost:8080/api/expenses/upload \
  -F "file=@receipt.jpg"

# Manual entry
curl -X POST http://localhost:8080/api/expenses \
  -H "Content-Type: application/json" \
  -d '{"category":"food","totalAmount":349.00,"merchantName":"Swiggy","expenseDate":"2026-03-31"}'

# Set monthly budget
curl -X POST http://localhost:8080/api/analytics/budget \
  -H "Content-Type: application/json" \
  -d '{"month":"2026-03","plannedSalary":80000}'

# Get savings progress
curl http://localhost:8080/api/analytics/savings-progress

# Trigger month-end sweep
curl -X POST http://localhost:8080/api/analytics/sweep

# Health check
curl http://localhost:8080/api/health
```

---

## Response Envelope

All endpoints return a consistent `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "optional message",
  "data": { },
  "errorCode": null,
  "timestamp": "2026-03-31T06:33:13Z"
}
```

Errors follow the same shape with `success: false` and an `errorCode` like `EXP_001`, `FILE_002`, etc.

---

## Monthly Budget & Savings Logic

- Set a `plannedSalary` per month via `POST /api/analytics/budget`
- `currentMonthBalance` = `plannedSalary - SUM(expenses this month)` — calculated live, never stored
- `POST /api/analytics/sweep` processes all previous unrolled months:
    - Positive remaining balance → added to `totalAccumulatedSavings`
    - Negative (overspent) → magnitude added to `totalDeficit`
- `netSavings` = `totalAccumulatedSavings - totalDeficit`
- `GET /api/analytics/savings-progress` returns the full picture: current balance, lifetime stats, and a 12-month breakdown

---

## Project Structure

```
src/main/java/com/expensetracker/
├── config/
│   ├── AppConfig.java                  # WebClient + ObjectMapper beans
│   └── GlobalExceptionHandler.java     # Centralized error handling
├── controller/
│   ├── ExpenseController.java
│   ├── AnalyticsController.java
│   └── HealthController.java
├── dto/                                # Request/Response DTOs
├── entities/                           # JPA entities
├── exception/
│   ├── BaseException.java
│   ├── AppExceptions.java              # All typed exceptions
│   └── ErrorCode.java                  # Error codes with HTTP status
├── repository/                         # Spring Data JPA repositories
└── service/
    ├── ExpenseService.java
    ├── BudgetService.java
    └── GeminiService.java
```

---

## Notes

- All string fields (`category`, `merchantName`, `paymentMethod`) are normalized to lowercase on save — consistent data regardless of input or Gemini output
- `currency` is uppercased (e.g. `INR`, `USD`)
- Gemini prompt enforces pure JSON output — a `cleanJsonText` step strips any accidental markdown fences
- WebClient configured with 10s connect timeout and 60s read/write timeout for Gemini's variable response times
- Secrets are never hardcoded — `application-local.properties` is gitignored
