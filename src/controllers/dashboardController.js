import { supabase } from '../config/supabase.js';

export const getDashboardData = async (req, res) => {
  try {
    // In a real scenario, this could be optimized with a database view or RPC function
    const [programs, finances, tasks] = await Promise.all([
      supabase.from('programs').select('id', { count: 'exact' }).eq('status', 'active'),
      supabase.from('finances').select('amount', { count: 'exact' }).eq('type', 'income'),
      supabase.from('tasks').select('id', { count: 'exact' }).eq('status', 'todo')
    ]);

    const activeProgramsCount = programs.count || 0;
    
    // Sum finances
    const { data: financesData } = await supabase.from('finances').select('amount').eq('type', 'income');
    const totalFinances = financesData?.reduce((acc, curr) => acc + (Number(curr.amount) || 0), 0) || 0;
    
    const pendingTasksCount = tasks.count || 0;

    res.json({
      success: true,
      message: 'Dashboard data fetched successfully',
      data: {
        activePrograms: activeProgramsCount,
        totalFinances,
        pendingTasks: pendingTasksCount
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to fetch dashboard data', errors: [err.message] });
  }
};
