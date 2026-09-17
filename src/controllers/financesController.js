import { supabase } from '../config/supabase.js';

export const getFinances = async (req, res) => {
  try {
    const { data, error } = await supabase.from('finances').select('*');
    if (error) throw error;
    res.json({ success: true, message: 'Finances fetched successfully', data });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to fetch finances', errors: [err.message] });
  }
};

export const createFinanceRecord = async (req, res) => {
  try {
    const { data, error } = await supabase.from('finances').insert([req.body]).select();
    if (error) throw error;
    res.status(201).json({ success: true, message: 'Finance record created successfully', data: data[0] });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to create finance record', errors: [err.message] });
  }
};
