import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import StatCard from '../components/StatCard';
import { getStudents, getCourses, getFaculty, getHalls } from '../services/importApi';
import { getTimetable } from '../services/timetableApi';
import '../styles/AdminDashboardPage.css';

export default function AdminDashboardPage() {
  const [stats, setStats] = useState({
    students: 0,
    courses: 0,
    faculty: 0,
    halls: 0,
    exams: 0
  });
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    loadStats();
  }, []);

  async function loadStats() {
    try {
      const [studentsRes, coursesRes, facultyRes, hallsRes, timetableRes] =
        await Promise.all([
          getStudents().catch(() => ({ data: [] })),
          getCourses().catch(() => ({ data: [] })),
          getFaculty().catch(() => ({ data: [] })),
          getHalls().catch(() => ({ data: [] })),
          getTimetable().catch(() => ({ data: [] })),
        ]);

      setStats({
        students: studentsRes.data.length,
        courses: coursesRes.data.length,
        faculty: facultyRes.data.length,
        halls: hallsRes.data.length,
        exams: timetableRes.data.length
      });
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner" />
      </div>
    );
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Dashboard</h1>
        <p className="page-subtitle">Overview of your exam allocation system</p>
      </div>

      <div className="dashboard-stats">
        <StatCard value={stats.students} label="Students" />
        <StatCard value={stats.courses} label="Courses" />
        <StatCard value={stats.faculty} label="Faculty" />
        <StatCard value={stats.halls} label="Halls" />
        <StatCard value={stats.exams} label="Exams Scheduled" />
      </div>

      <div className="dashboard-actions">
        <button className="btn btn-primary" onClick={() => navigate('/admin/import')}>
          Import Data
        </button>
        <button className="btn btn-secondary" onClick={() => navigate('/admin/timetable')}>
          View Timetable
        </button>
        <button className="btn btn-secondary" onClick={() => navigate('/admin/seating')}>
          View Seating
        </button>
        <button className="btn btn-secondary" onClick={() => navigate('/admin/duties')}>
          View Duties
        </button>
      </div>
    </div>
  );
}
