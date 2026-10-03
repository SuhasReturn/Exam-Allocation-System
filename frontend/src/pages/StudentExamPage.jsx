import { useState, useEffect } from 'react';
import EmptyState from '../components/EmptyState';
import { getMyExams } from '../services/seatingApi';
import '../styles/StudentExamPage.css';

export default function StudentExamPage() {
  const [exams, setExams] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadMyExams();
  }, []);

  async function loadMyExams() {
    try {
      const response = await getMyExams();
      setExams(response.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load your exams');
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return <div className="loading-container"><div className="spinner" /></div>;
  }

  if (error) {
    return (
      <div>
        <div className="page-header">
          <h1 className="page-title">My Exams</h1>
        </div>
        <div className="card" style={{ color: 'var(--color-danger)' }}>{error}</div>
      </div>
    );
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">My Exams</h1>
        <p className="page-subtitle">Your exam schedule with hall and seat assignments</p>
      </div>

      {exams.length === 0 ? (
        <EmptyState
          title="No exams found"
          message="Your exam schedule will appear here once the admin generates the timetable and seating plan."
        />
      ) : (
        <div className="student-exam-list">
          {exams.map((exam, index) => (
            <div key={index} className="card exam-card">
              <div className="exam-card-left">
                <div className="exam-card-code">{exam.courseCode}</div>
                <div className="exam-card-title">{exam.courseTitle}</div>
              </div>
              <div className="exam-card-right">
                <div className="exam-detail">
                  <div className="exam-detail-label">Date</div>
                  <div className="exam-detail-value">{exam.examDate}</div>
                </div>
                <div className="exam-detail">
                  <div className="exam-detail-label">Session</div>
                  <div className="exam-detail-value">
                    <span className="badge badge-primary">{exam.session}</span>
                  </div>
                </div>
                <div className="exam-detail">
                  <div className="exam-detail-label">Hall</div>
                  <div className="exam-detail-value">{exam.hallName}</div>
                </div>
                <div className="exam-detail">
                  <div className="exam-detail-label">Seat</div>
                  <div className="exam-detail-value" style={{ color: 'var(--color-primary)' }}>
                    #{exam.seatNo}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
