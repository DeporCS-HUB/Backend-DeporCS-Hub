import express from 'express';
import { getInventory, createInventoryItem } from '../controllers/inventoryController.js';
import { authenticate, requireRole } from '../middlewares/authMiddleware.js';

const router = express.Router();

router.use(authenticate);

router.get('/', getInventory);
router.post('/', requireRole(['staff', 'admin']), createInventoryItem);

export default router;
