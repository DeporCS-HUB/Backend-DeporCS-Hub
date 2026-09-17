import { supabase } from '../config/supabase.js';

export const getInventory = async (req, res) => {
  try {
    const { data, error } = await supabase.from('inventory').select('*');
    if (error) throw error;
    res.json({ success: true, message: 'Inventory fetched successfully', data });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to fetch inventory', errors: [err.message] });
  }
};

export const createInventoryItem = async (req, res) => {
  try {
    const { data, error } = await supabase.from('inventory').insert([req.body]).select();
    if (error) throw error;
    res.status(201).json({ success: true, message: 'Inventory item added successfully', data: data[0] });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to create inventory item', errors: [err.message] });
  }
};
