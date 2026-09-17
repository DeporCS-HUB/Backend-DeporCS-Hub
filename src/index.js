import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import dotenv from 'dotenv';

import authRoutes from './routes/authRoutes.js';
import dashboardRoutes from './routes/dashboardRoutes.js';
import programsRoutes from './routes/programsRoutes.js';
import financesRoutes from './routes/financesRoutes.js';
import tasksRoutes from './routes/tasksRoutes.js';
import inventoryRoutes from './routes/inventoryRoutes.js';

dotenv.config();

const app = express();
const PORT = 3000;

// Security & Middleware
app.use(helmet());
app.use(cors());
app.use(express.json());

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/dashboard', dashboardRoutes);
app.use('/api/programs', programsRoutes);
app.use('/api/finances', financesRoutes);
app.use('/api/tasks', tasksRoutes);
app.use('/api/inventory', inventoryRoutes);

// Health Check
app.get('/api/health', (req, res) => {
  res.json({ success: true, message: 'Server is healthy, DeporCS Hub is running!', data: null });
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
