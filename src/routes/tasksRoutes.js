import express from 'express';
import { getTasks, createTask } from '../controllers/tasksController.js';
import { authenticate } from '../middlewares/authMiddleware.js';

const router = express.Router();

router.use(authenticate);

router.get('/', getTasks);
router.post('/', createTask); // All authenticated users can create tasks

export default router;
