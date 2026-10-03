import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import '../styles/NavigationBar.css';

export default function NavigationBar() {
  const { role, username, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <nav className="navbar">
      <div className="navbar-brand">
        <h2>ExamAlloc</h2>
        <span>{role} Panel</span>
      </div>

      <ul className="navbar-links">
        {role === 'ADMIN' && (
          <>
            <li>
              <NavLink to="/admin/dashboard" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                <span>Dashboard</span>
              </NavLink>
            </li>
            <li>
              <NavLink to="/admin/import" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                <span>Import Data</span>
              </NavLink>
            </li>
            <li>
              <NavLink to="/admin/timetable" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                <span>Timetable</span>
              </NavLink>
            </li>
            <li>
              <NavLink to="/admin/seating" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                <span>Seating Plan</span>
              </NavLink>
            </li>
            <li>
              <NavLink to="/admin/duties" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                <span>Duty Chart</span>
              </NavLink>
            </li>
          </>
        )}

        {role === 'STUDENT' && (
          <li>
            <NavLink to="/student/exams" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
              <span>My Exams</span>
            </NavLink>
          </li>
        )}

        {role === 'FACULTY' && (
          <li>
            <NavLink to="/faculty/duties" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
              <span>My Duties</span>
            </NavLink>
          </li>
        )}
      </ul>

      <div className="navbar-footer">
        <div className="navbar-user">{username}</div>
        <button className="logout-btn" onClick={handleLogout}>
          Logout
        </button>
      </div>
    </nav>
  );
}
