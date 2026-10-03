import { useState, useEffect } from 'react';
import EmptyState from '../components/EmptyState';
import { getMyDuties, markUnavailableDates } from '../services/dutyApi';
import '../styles/FacultyDutyPage.css';

export default function FacultyDutyPage() {
  const [duties, setDuties] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [unavailableDate, setUnavailableDate] = useState('');
  const [unavailableMessage, setUnavailableMessage] = useState('');

  useEffect(() => {
    loadMyDuties();
  }, []);

  async function loadMyDuties() {
    try {
      const response = await getMyDuties();
      setDuties(response.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load your duties');
    } finally {
      setLoading(false);
    }
  }

  async function handleMarkUnavailable() {
    if (!unavailableDate) return;
    setUnavailableMessage('');

    try {
      const response = await markUnavailableDates([unavailableDate]);
      setUnavailableMessage(response.data.message);
      setUnavailableDate('');
    } catch (err) {
      setUnavailableMessage(err.response?.data?.message || 'Failed to mark date');
    }
  }

  if (loading) {
    return <div className="loading-container"><div className="spinner" /></div>;
  }

  if (error) {
    return (
      <div>
        <div className="page-header">
          <h1 className="page-title">My Duties</h1>
        </div>
        <div className="card" style={{ color: 'var(--color-danger)' }}>{error}</div>
      </div>
    );
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">My Duties</h1>
        <p className="page-subtitle">Your invigilation assignments</p>
      </div>

      {duties.length === 0 ? (
        <EmptyState
          title="No duties assigned"
          message="Your invigilation duties will appear here once the admin assigns them."
        />
      ) : (
        <div className="card">
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Session</th>
                  <th>Hall</th>
                  <th>Role</th>
                </tr>
              </thead>
              <tbody>
                {duties.map(duty => (
                  <tr key={duty.dutyId}>
                    <td>{duty.examDate}</td>
                    <td><span className="badge badge-primary">{duty.session}</span></td>
                    <td style={{ fontWeight: 500 }}>{duty.hallName}</td>
                    <td>
                      <span className={`badge ${duty.dutyRole === 'CHIEF' ? 'badge-success' : 'badge-primary'}`}>
                        {duty.dutyRole}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <div className="faculty-unavailable-section">
        <h2>Mark Unavailable Dates</h2>
        <div className="unavailable-form">
          <div className="form-group">
            <label className="form-label">Date</label>
            <input
              type="date"
              className="form-input"
              value={unavailableDate}
              onChange={e => setUnavailableDate(e.target.value)}
            />
          </div>
          <button
            className="btn btn-secondary"
            onClick={handleMarkUnavailable}
            disabled={!unavailableDate}
          >
            Mark Unavailable
          </button>
        </div>
        {unavailableMessage && (
          <div className="unavailable-result">{unavailableMessage}</div>
        )}
      </div>
    </div>
  );
}
