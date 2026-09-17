import express from 'express';
import { getFinances, createFinanceRecord } from '../controllers/financesController.js';
import { authenticate, requireRole } from '../middlewares/authMiddleware.js';

const router = express.Router();

router.use(authenticate);

router.get('/', getFinances);
router.post('/', requireRole(['staff', 'admin']), createFinanceRecord);

export default router;
