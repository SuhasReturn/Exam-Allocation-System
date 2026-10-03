import { useState, useEffect } from 'react';
import TimetableTable from '../components/TimetableTable';
import ClashWarningBanner from '../components/ClashWarningBanner';
import EmptyState from '../components/EmptyState';
import { generateTimetable, getTimetable, validateTimetable } from '../services/timetableApi';
import '../styles/TimetablePage.css';

export default function TimetablePage() {
  const [rows, setRows] = useState([]);
  const [clashes, setClashes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [startDate, setStartDate] = useState('2025-12-01');
  const [slotsPerDay, setSlotsPerDay] = useState(2);
  const [message, setMessage] = useState('');

  useEffect(() => {
    loadTimetable();
  }, []);

  async function loadTimetable() {
    setLoading(true);
    try {
      const response = await getTimetable();
      setRows(response.data);

      if (response.data.length > 0) {
        const clashResponse = await validateTimetable();
        setClashes(clashResponse.data.violations || []);
      }
    } catch (err) {
      // timetable not generated yet
    } finally {
      setLoading(false);
    }
  }

  async function handleGenerate() {
    setGenerating(true);
    setMessage('');
    setClashes([]);

    try {
      const response = await generateTimetable(startDate, slotsPerDay);
      setMessage(`Generated timetable for ${response.data.examCount} courses`);

      if (response.data.clashReport?.violations?.length > 0) {
        setClashes(response.data.clashReport.violations);
      }

      await loadTimetable();
    } catch (err) {
      const msg = err.response?.data?.message || 'Generation failed';
      setMessage(msg);
    } finally {
      setGenerating(false);
    }
  }

  if (loading) {
    return <div className="loading-container"><div className="spinner" /></div>;
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Exam Timetable</h1>
        <p className="page-subtitle">Generate and view the exam schedule</p>
      </div>

      <div className="timetable-controls">
        <div className="form-group">
          <label className="form-label">Start Date</label>
          <input
            className="form-input"
            type="date"
            value={startDate}
            onChange={e => setStartDate(e.target.value)}
          />
        </div>

        <div className="form-group">
          <label className="form-label">Slots / Day</label>
          <select
            className="form-input"
            value={slotsPerDay}
            onChange={e => setSlotsPerDay(Number(e.target.value))}
          >
            <option value={1}>1 (FN only)</option>
            <option value={2}>2 (FN + AN)</option>
          </select>
        </div>

        <button
          className="btn btn-primary"
          onClick={handleGenerate}
          disabled={generating}
        >
          {generating ? 'Generating...' : 'Generate Timetable'}
        </button>
      </div>

      {message && (
        <div style={{ marginBottom: 16, fontSize: 14, color: 'var(--color-text-muted)' }}>
          {message}
        </div>
      )}

      <ClashWarningBanner violations={clashes} />

      {rows.length === 0 ? (
        <EmptyState
          title="No timetable generated"
          message="Import courses and enrollments, then generate the timetable above."
        />
      ) : (
        <div className="card">
          <TimetableTable rows={rows} />
        </div>
      )}
    </div>
  );
}
