import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';

dotenv.config();

const app = express();
const PORT = 3000;

app.use(cors());
app.use(express.json());

// --- MOCK DATABASE ---
const store = {
  users: new Map(),
  programs: new Map(),
  finances: new Map(),
  tasks: new Map(),
  inventory: new Map()
};

// Initial mock data
store.users.set('1', { id: '1', name: 'Admin', role: 'staff' });
store.programs.set('p1', { id: 'p1', title: 'Sports Tournament', status: 'active' });
store.finances.set('f1', { id: 'f1', programId: 'p1', type: 'income', amount: 5000 });
store.tasks.set('t1', { id: 't1', title: 'Prepare equipment', status: 'todo' });
store.inventory.set('i1', { id: 'i1', name: 'Soccer Ball', quantity: 10, status: 'available' });

// --- MIDDLEWARE ---
// Mock auth middleware
const authenticate = (req, res, next) => {
  // In a real app, verify JWT here. For now, assume admin user.
  req.user = store.users.get('1');
  if (!req.user) return res.status(401).json({ success: false, message: 'Unauthorized access', errors: [] });
  next();
};

const requireRole = (role) => (req, res, next) => {
  if (req.user.role !== role) {
    return res.status(403).json({ success: false, message: 'Forbidden: Insufficient permissions', errors: [] });
  }
  next();
};

// --- ROUTES ---

// Health Check
app.get('/api/health', (req, res) => {
  res.json({ success: true, message: 'Server is healthy', data: null });
});

// Auth Routes (Mocked)
app.post('/api/auth/google/callback', (req, res) => {
  res.json({ success: true, message: 'Mock Google OAuth successful', data: { token: 'mock-jwt-token' } });
});

// Executive Dashboard
app.get('/api/dashboard', authenticate, (req, res) => {
  res.json({
    success: true,
    message: 'Dashboard data fetched successfully',
    data: {
      activePrograms: store.programs.size,
      totalFinances: Array.from(store.finances.values()).reduce((acc, curr) => acc + curr.amount, 0),
      pendingTasks: Array.from(store.tasks.values()).filter(t => t.status === 'todo').length
    }
  });
});

// Programs (Proker)
app.get('/api/programs', authenticate, (req, res) => {
  res.json({ success: true, message: 'Programs fetched', data: Array.from(store.programs.values()) });
});
app.post('/api/programs', authenticate, requireRole('staff'), (req, res) => {
  const newId = `p${Date.now()}`;
  const program = { id: newId, ...req.body };
  store.programs.set(newId, program);
  res.status(201).json({ success: true, message: 'Program created', data: program });
});

// Finances (RAB)
app.get('/api/finances', authenticate, (req, res) => {
  res.json({ success: true, message: 'Finances fetched', data: Array.from(store.finances.values()) });
});
app.post('/api/finances', authenticate, requireRole('staff'), (req, res) => {
  const newId = `f${Date.now()}`;
  const finance = { id: newId, ...req.body };
  store.finances.set(newId, finance);
  res.status(201).json({ success: true, message: 'Finance record created', data: finance });
});

// Task Management (Kanban)
app.get('/api/tasks', authenticate, (req, res) => {
  res.json({ success: true, message: 'Tasks fetched', data: Array.from(store.tasks.values()) });
});
app.post('/api/tasks', authenticate, (req, res) => {
  const newId = `t${Date.now()}`;
  const task = { id: newId, status: 'todo', ...req.body };
  store.tasks.set(newId, task);
  res.status(201).json({ success: true, message: 'Task created', data: task });
});

// Inventory Management
app.get('/api/inventory', authenticate, (req, res) => {
  res.json({ success: true, message: 'Inventory fetched', data: Array.from(store.inventory.values()) });
});
app.post('/api/inventory', authenticate, requireRole('staff'), (req, res) => {
  const newId = `i${Date.now()}`;
  const item = { id: newId, ...req.body };
  store.inventory.set(newId, item);
  res.status(201).json({ success: true, message: 'Inventory item added', data: item });
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({ success: false, message: 'Internal Server Error', errors: [err.message] });
});

// 404 Route
app.use((req, res) => {
  res.status(404).json({ success: false, message: 'Route not found', errors: [] });
});

// --- SERVER START ---
app.listen(PORT, '0.0.0.0', () => {
  console.log(`Server running on http://0.0.0.0:${PORT}`);
});
