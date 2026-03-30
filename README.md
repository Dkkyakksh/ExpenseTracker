# 💸 Expense Tracker — Spring Boot + Gemini Vision API

A REST API that lets you **photograph a receipt**, send it to **Google Gemini Vision**, extract structured expense data (merchant, total, itemized lines, tax, date), and store it all in **PostgreSQL** for querying and analytics.

---

## 🏗️ Architecture

```
Receipt Image (JPEG/PNG)
        │
        ▼
POST /api/expenses/upload
        │
        ▼
GeminiService  ──► Gemini 1.5 Flash API (Vision)
        │              (extracts JSON from image)
        ▼
ExpenseService ──► Maps parsed JSON → Expense + ExpenseItems
        │
        ▼
PostgreSQL DB  ──► expenses + expense_items tables
        │
        ▼
REST API  ──► CRUD, filtering, summary analytics
```

---

## 🗄️ Database Schema

### `expenses`
| Column              | Type          | Description                              |
|---------------------|---------------|------------------------------------------|
| id                  | BIGSERIAL PK  |                                          |
| merchant_name       | VARCHAR       | Store/restaurant name                    |
| category            | VARCHAR       | e.g. Food & Dining, Groceries, Transport |
| total_amount        | NUMERIC(10,2) | Final amount paid                        |
| tax_amount          | NUMERIC(10,2) | Tax portion                              |
| currency            | VARCHAR(3)    | ISO code e.g. INR, USD                   |
| expense_date        | DATE          |                                          |
| payment_method      | VARCHAR       | Cash, Credit Card, UPI, etc.             |
| notes               | VARCHAR(500)  | Manual notes                             |
| raw_extracted_text  | TEXT          | Full text Gemini read from image         |
| image_file_name     | VARCHAR       | Original filename                        |
| created_at          | TIMESTAMP     |                                          |

### `expense_items`
| Column      | Type          | Description          |
|-------------|---------------|----------------------|
| id          | BIGSERIAL PK  |                      |
| expense_id  | BIGINT FK     | → expenses.id        |
| name        | VARCHAR       | Item name            |
| quantity    | INTEGER       |                      |
| unit_price  | NUMERIC(10,2) |                      |
| total_price | NUMERIC(10,2) |                      |

---

## ⚙️ Setup

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- A [Google Gemini API key](https://aistudio.google.com/app/apikey)

### 1. Create the PostgreSQL database
```sql
CREATE DATABASE expense_tracker;
```

### 2. Set environment variables
```bash
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
export GEMINI_API_KEY=your_gemini_api_key_here
```

Or create a `.env` file and source it, or set them in your IDE run config.

### 3. Build and run
```bash
cd expense-tracker
mvn clean install -DskipTests
mvn spring-boot:run
```

The API will start on **http://localhost:8080**

---

## 📡 API Reference

### 📤 Upload Receipt Image
```
POST /api/expenses/upload
Content-Type: multipart/form-data

file: <image file>
```
**Response:**
```json
{
  "message": "Receipt parsed and saved successfully",
  "expense": {
    "id": 1,
    "merchantName": "Big Basket",
    "category": "Groceries",
    "totalAmount": 845.50,
    "taxAmount": 42.75,
    "currency": "INR",
    "expenseDate": "2024-03-15",
    "paymentMethod": "UPI",
    "items": [
      { "name": "Tata Salt 1kg", "quantity": 2, "unitPrice": 22.00, "totalPrice": 44.00 },
      { "name": "Amul Butter 500g", "quantity": 1, "unitPrice": 265.00, "totalPrice": 265.00 }
    ]
  }
}
```

### 📋 List All Expenses
```
GET /api/expenses
```

### 🔍 Get Single Expense
```
GET /api/expenses/{id}
```

### ✏️ Update an Expense (manual corrections)
```
PATCH /api/expenses/{id}
Content-Type: application/json

{
  "category": "Food & Dining",
  "notes": "Team lunch"
}
```

### 🗑️ Delete an Expense
```
DELETE /api/expenses/{id}
```

### 🏷️ Filter by Category
```
GET /api/expenses/category/Groceries
```

### 📅 Filter by Date Range
```
GET /api/expenses/date-range?start=2024-01-01&end=2024-03-31
```

### 📊 Overall Summary
```
GET /api/expenses/summary
```
**Response:**
```json
{
  "totalSpend": 12450.75,
  "totalExpenses": 23,
  "byCategory": [
    { "category": "Food & Dining", "totalAmount": 5200.00 },
    { "category": "Groceries", "totalAmount": 3100.50 }
  ],
  "byMerchant": [
    { "merchantName": "Swiggy", "totalAmount": 2100.00 }
  ]
}
```

### 📊 Summary by Date Range
```
GET /api/expenses/summary/date-range?start=2024-01-01&end=2024-03-31
```

### 🏷️ List All Categories
```
GET /api/expenses/categories
```

---

## 🧪 Test with cURL

```bash
# Upload a receipt
curl -X POST http://localhost:8080/api/expenses/upload \
  -F "file=@/path/to/receipt.jpg"

# Get all expenses
curl http://localhost:8080/api/expenses

# Get summary
curl http://localhost:8080/api/expenses/summary

# Update category
curl -X PATCH http://localhost:8080/api/expenses/1 \
  -H "Content-Type: application/json" \
  -d '{"category": "Food & Dining", "notes": "Dinner with client"}'

# Delete
curl -X DELETE http://localhost:8080/api/expenses/1
```

---

## 📦 Project Structure

```
src/main/java/com/expensetracker/
├── ExpenseTrackerApplication.java
├── config/
│   ├── AppConfig.java           # WebClient + ObjectMapper beans
│   └── GlobalExceptionHandler.java
├── controller/
│   └── ExpenseController.java   # All REST endpoints
├── dto/
│   └── ExpenseDTOs.java         # Request/Response/Gemini DTOs
├── model/
│   ├── Expense.java             # Main expense entity
│   └── ExpenseItem.java         # Line item entity
├── repository/
│   ├── ExpenseRepository.java   # JPA queries + analytics
│   └── ExpenseItemRepository.java
└── service/
    ├── ExpenseService.java      # Business logic + orchestration
    └── GeminiService.java       # Gemini Vision API integration
```

---

## 🔑 Gemini API Details

- Model: `gemini-1.5-flash` (fast, cheap, great for vision tasks)
- The prompt instructs Gemini to return **pure JSON only** — no markdown fences
- Handles: itemized line items, total amount, tax, date, merchant, currency, payment method
- Supported image formats: JPEG, PNG, WebP, GIF

---

## 💡 Tips

- Gemini may not extract everything from blurry or partial images — use the `PATCH` endpoint to correct any fields manually
- The `rawExtractedText` field in the DB stores everything Gemini could read — useful for debugging
- Currency defaults to `INR` if Gemini can't determine it from the image
