import { supabase } from '../config/supabase.js';

export const getTasks = async (req, res) => {
  try {
    const { data, error } = await supabase.from('tasks').select('*');
    if (error) throw error;
    res.json({ success: true, message: 'Tasks fetched successfully', data });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to fetch tasks', errors: [err.message] });
  }
};

export const createTask = async (req, res) => {
  try {
    const { data, error } = await supabase.from('tasks').insert([
      { ...req.body, status: req.body.status || 'todo' }
    ]).select();
    if (error) throw error;
    res.status(201).json({ success: true, message: 'Task created successfully', data: data[0] });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to create task', errors: [err.message] });
  }
};
