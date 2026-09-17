import express from 'express';
import { getPrograms, createProgram } from '../controllers/programsController.js';
import { authenticate, requireRole } from '../middlewares/authMiddleware.js';

const router = express.Router();

router.use(authenticate);

router.get('/', getPrograms);
router.post('/', requireRole(['staff', 'admin']), createProgram);

export default router;
