import { useState, useEffect } from 'react';
import EmptyState from '../components/EmptyState';
import { generateDuties, getAllDuties, replaceDuty } from '../services/dutyApi';
import '../styles/DutyChartPage.css';

export default function DutyChartPage() {
  const [duties, setDuties] = useState([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [replacingId, setReplacingId] = useState(null);
  const [message, setMessage] = useState('');

  useEffect(() => {
    loadDuties();
  }, []);

  async function loadDuties() {
    setLoading(true);
    try {
      const response = await getAllDuties();
      setDuties(response.data);
    } catch (err) {
      // no duties yet
    } finally {
      setLoading(false);
    }
  }

  async function handleGenerate() {
    setGenerating(true);
    setMessage('');
    try {
      const response = await generateDuties();
      setMessage(response.data.message);
      await loadDuties();
    } catch (err) {
      setMessage(err.response?.data?.message || 'Duty assignment failed');
    } finally {
      setGenerating(false);
    }
  }

  async function handleReplace(dutyId) {
    setReplacingId(dutyId);
    try {
      const response = await replaceDuty(dutyId);
      setMessage(`Replaced: ${response.data.newFacultyName} assigned to ${response.data.hallName}`);
      await loadDuties();
    } catch (err) {
      setMessage(err.response?.data?.message || 'Replacement failed');
    } finally {
      setReplacingId(null);
    }
  }

  if (loading) {
    return <div className="loading-container"><div className="spinner" /></div>;
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Invigilator Duty Chart</h1>
        <p className="page-subtitle">Assign and manage invigilator duties</p>
      </div>

      <div className="duty-controls">
        <button
          className="btn btn-primary"
          onClick={handleGenerate}
          disabled={generating}
        >
          {generating ? 'Assigning...' : 'Generate Duties'}
        </button>
      </div>

      {message && (
        <div style={{ marginBottom: 16, fontSize: 14, color: 'var(--color-text-muted)' }}>
          {message}
        </div>
      )}

      {duties.length === 0 ? (
        <EmptyState
          title="No duties assigned"
          message="Generate seating first, then generate duties."
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
                  <th>Faculty</th>
                  <th>Department</th>
                  <th>Role</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {duties.map(duty => (
                  <tr key={duty.dutyId}>
                    <td>{duty.examDate}</td>
                    <td><span className="badge badge-primary">{duty.session}</span></td>
                    <td>{duty.hallName}</td>
                    <td style={{ fontWeight: 500 }}>{duty.facultyName}</td>
                    <td>{duty.facultyDepartment}</td>
                    <td>
                      <span className={`badge ${duty.dutyRole === 'CHIEF' ? 'badge-success' : 'badge-primary'}`}>
                        {duty.dutyRole}
                      </span>
                    </td>
                    <td>
                      <button
                        className="duty-replace-btn"
                        onClick={() => handleReplace(duty.dutyId)}
                        disabled={replacingId === duty.dutyId}
                      >
                        {replacingId === duty.dutyId ? 'Replacing...' : 'Replace'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
