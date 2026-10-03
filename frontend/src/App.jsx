import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import NavigationBar from './components/NavigationBar';
import LoginPage from './pages/LoginPage';
import AdminDashboardPage from './pages/AdminDashboardPage';
import ImportDataPage from './pages/ImportDataPage';
import TimetablePage from './pages/TimetablePage';
import SeatingPlanPage from './pages/SeatingPlanPage';
import DutyChartPage from './pages/DutyChartPage';
import StudentExamPage from './pages/StudentExamPage';
import FacultyDutyPage from './pages/FacultyDutyPage';
import './styles/theme.css';

function AppLayout() {
  const { isLoggedIn } = useAuth();

  return (
    <>
      {isLoggedIn && <NavigationBar />}
      <main className={isLoggedIn ? 'main-content' : ''}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          {/* Admin routes */}
          <Route path="/admin/dashboard" element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <AdminDashboardPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/import" element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <ImportDataPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/timetable" element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <TimetablePage />
            </ProtectedRoute>
          } />
          <Route path="/admin/seating" element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <SeatingPlanPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/duties" element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <DutyChartPage />
            </ProtectedRoute>
          } />

          {/* Student route */}
          <Route path="/student/exams" element={
            <ProtectedRoute allowedRoles={['STUDENT']}>
              <StudentExamPage />
            </ProtectedRoute>
          } />

          {/* Faculty route */}
          <Route path="/faculty/duties" element={
            <ProtectedRoute allowedRoles={['FACULTY']}>
              <FacultyDutyPage />
            </ProtectedRoute>
          } />

          {/* Default redirect */}
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </main>
    </>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppLayout />
      </AuthProvider>
    </BrowserRouter>
  );
}
