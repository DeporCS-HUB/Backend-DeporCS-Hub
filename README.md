# 🏆 Backend DeporCS Hub

Backend service for **DeporCS Hub (Departemen Olahraga Hub)** — a centralized information and management system for operational workflows, program tracking, financial monitoring (RAB), inventory management, and team task collaboration.

This backend acts as the **core API and business logic layer**, providing secure access control, structured data flow, and integration with PostgreSQL (Supabase) and Google OAuth.

---

## ✨ Core Responsibilities

The backend is designed to handle:

- **Executive Dashboard Data**
  - Aggregated metrics for programs, budgets, and active tasks.
- **Program Kerja (Proker) & Event Management**
  - CRUD operations, status tracking, and activity timeline.
- **Financial Module (RAB)**
  - Budget proposal flow, income/outcome records, and fund monitoring per proker.
- **Task Management (Kanban API)**
  - Board/list/task endpoints for collaborative daily operations.
- **Inventory Management**
  - Asset registration, borrowing/returning flow, and stock status tracking.
- **Role-Based Security**
  - Strict separation between **Member** and **Staff** access at API and database level.

---

## 🛠️ Tech Stack

- **Runtime:** Node.js
- **Framework:** Express.js
- **Database:** PostgreSQL (Supabase)
- **Authentication:** Google OAuth
- **Deployment:** Render

---

## 📁 Suggested Project Structure

```bash
backend-deporcs-hub/
├── src/
│   ├── config/         # Environment, database, app configuration
│   ├── controllers/    # Request handlers
│   ├── services/       # Business logic
│   ├── repositories/   # Data access layer (queries)
│   ├── middleware/     # Auth, validation, error handlers
│   ├── routes/         # API route definitions
│   ├── utils/          # Helper functions
│   └── app.js          # Express app setup
├── .env
├── package.json
└── server.js
```

---

## 🚀 Getting Started (Development)

### 1) Prerequisites

Make sure you have:

- **Node.js** (LTS recommended)
- **npm** or **yarn**
- **Supabase project** (PostgreSQL + Auth setup)
- **Google OAuth credentials**

### 2) Clone Repository

```bash
git clone https://github.com/DeporCS-HUB/Backend-DeporCS-Hub.git
cd Backend-DeporCS-Hub
```

### 3) Install Dependencies

```bash
npm install
```

### 4) Configure Environment Variables

Create a `.env` file in the root directory:

```env
PORT=5000
NODE_ENV=development

# Supabase / PostgreSQL
SUPABASE_URL=your_supabase_url
SUPABASE_ANON_KEY=your_supabase_anon_key
SUPABASE_SERVICE_ROLE_KEY=your_supabase_service_role_key
DATABASE_URL=your_postgres_connection_url

# Google OAuth
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_CALLBACK_URL=http://localhost:5000/api/auth/google/callback

# JWT / Session (if used)
JWT_SECRET=your_jwt_secret
JWT_EXPIRES_IN=1d
```

> Adjust variable names according to your actual implementation.

### 5) Run Development Server

```bash
npm run dev
```

If you don’t use nodemon:

```bash
npm start
```

---

## 🔐 Authentication & Authorization

- Uses **Google OAuth** for login flow.
- User identity and role are validated before protected route access.
- Recommended roles:
  - `member`
  - `staff`
  - `admin` (optional)
- Enforce role constraints both in:
  - **API middleware** (route-level protection)
  - **Database policies/constraints** (Supabase RLS or SQL constraints)

---

## 📌 API Conventions (Recommended)

- Base path: `/api`
- Response format (example):
  ```json
  {
    "success": true,
    "message": "Data fetched successfully",
    "data": {}
  }
  ```
- Error format (example):
  ```json
  {
    "success": false,
    "message": "Unauthorized access",
    "errors": []
  }
  ```

---

## 🧪 Scripts (Example)

Add or adjust these in `package.json`:

```json
{
  "scripts": {
    "dev": "nodemon server.js",
    "start": "node server.js",
    "lint": "eslint .",
    "test": "jest"
  }
}
```

---

## 🌍 Deployment (Render)

1. Push backend code to GitHub.
2. Create a new **Web Service** in Render.
3. Connect repository: `DeporCS-HUB/Backend-DeporCS-Hub`.
4. Set environment variables in Render dashboard.
5. Deploy and verify health endpoint (recommended: `/api/health`).

---

## 🤝 Contribution

1. Fork this repository
2. Create a feature branch:
   ```bash
   git checkout -b feat/your-feature
   ```
3. Commit your changes:
   ```bash
   git commit -m "feat: add your feature"
   ```
4. Push branch:
   ```bash
   git push origin feat/your-feature
   ```
5. Open a Pull Request

---

## 📄 License

Specify your license here (e.g., MIT).

---

## 👥 Maintainers

Managed by **DeporCS-HUB Team**.
