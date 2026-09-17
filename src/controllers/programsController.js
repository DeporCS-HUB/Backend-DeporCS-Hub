import { supabase } from '../config/supabase.js';

export const getPrograms = async (req, res) => {
  try {
    const { data, error } = await supabase.from('programs').select('*');
    if (error) throw error;
    res.json({ success: true, message: 'Programs fetched successfully', data });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to fetch programs', errors: [err.message] });
  }
};

export const createProgram = async (req, res) => {
  try {
    const { data, error } = await supabase.from('programs').insert([req.body]).select();
    if (error) throw error;
    res.status(201).json({ success: true, message: 'Program created successfully', data: data[0] });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to create program', errors: [err.message] });
  }
};
