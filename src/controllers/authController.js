import jwt from 'jsonwebtoken';
import { supabase } from '../config/supabase.js';

export const login = async (req, res) => {
  try {
    const { email, password } = req.body;
    
    // In a real app, you would hash passwords or use Supabase Auth directly.
    // Here we simulate a basic check against the users table.
    const { data: user, error } = await supabase
      .from('users')
      .select('*')
      .eq('email', email)
      .single();

    if (error || !user) {
      return res.status(401).json({ success: false, message: 'Invalid credentials', errors: [error?.message || 'User not found'] });
    }

    // Verify password (Mock - assuming plaintext for simple demo, use bcrypt in production)
    if (user.password !== password) {
      return res.status(401).json({ success: false, message: 'Invalid credentials', errors: [] });
    }

    const token = jwt.sign(
      { id: user.id, email: user.email, role: user.role },
      process.env.JWT_SECRET || 'DEPORCS_JUARA_OLIM2026',
      { expiresIn: process.env.JWT_EXPIRES_IN || '1d' }
    );

    res.json({ success: true, message: 'Login successful', data: { token, user: { id: user.id, name: user.name, role: user.role } } });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Internal Server Error', errors: [err.message] });
  }
};
