import { useState, useEffect } from 'react';
import HallSeatGrid from '../components/HallSeatGrid';
import ClashWarningBanner from '../components/ClashWarningBanner';
import EmptyState from '../components/EmptyState';
import { generateSeating, getSeatingBySlot } from '../services/seatingApi';
import { getTimetable } from '../services/timetableApi';
import '../styles/SeatingPlanPage.css';

export default function SeatingPlanPage() {
  const [slots, setSlots] = useState([]);
  const [selectedSlotId, setSelectedSlotId] = useState('');
  const [hallsData, setHallsData] = useState([]);
  const [degradedHalls, setDegradedHalls] = useState([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    loadSlots();
  }, []);

  async function loadSlots() {
    setLoading(true);
    try {
      const response = await getTimetable();
      // extract unique slots from timetable rows
      const slotMap = {};
      for (const row of response.data) {
        const key = `${row.examDate}_${row.session}`;
        if (!slotMap[key]) {
          slotMap[key] = { examDate: row.examDate, session: row.session, examId: row.examId };
        }
      }
      setSlots(Object.values(slotMap));
    } catch (err) {
      // no timetable yet
    } finally {
      setLoading(false);
    }
  }

  async function handleGenerate() {
    setGenerating(true);
    setMessage('');
    try {
      const response = await generateSeating();
      setMessage(response.data.message);
      setDegradedHalls(response.data.degradedHalls || []);
    } catch (err) {
      setMessage(err.response?.data?.message || 'Seating generation failed');
    } finally {
      setGenerating(false);
    }
  }

  async function handleSlotChange(slotId) {
    setSelectedSlotId(slotId);
    if (!slotId) {
      setHallsData([]);
      return;
    }

    try {
      const response = await getSeatingBySlot(slotId);
      setHallsData(response.data);
    } catch (err) {
      setHallsData([]);
    }
  }

  if (loading) {
    return <div className="loading-container"><div className="spinner" /></div>;
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Seating Plan</h1>
        <p className="page-subtitle">Generate seating and view hall-wise seat assignments</p>
      </div>

      <div className="seating-controls">
        <button
          className="btn btn-primary"
          onClick={handleGenerate}
          disabled={generating}
        >
          {generating ? 'Generating...' : 'Generate Seating'}
        </button>

        <div className="form-group">
          <label className="form-label">View Slot</label>
          <select
            className="form-input"
            value={selectedSlotId}
            onChange={e => handleSlotChange(e.target.value)}
          >
            <option value="">Select a slot</option>
            {slots.map((slot, i) => (
              <option key={i} value={slot.examId}>
                {slot.examDate} {slot.session}
              </option>
            ))}
          </select>
        </div>
      </div>

      {message && (
        <div style={{ marginBottom: 16, fontSize: 14, color: 'var(--color-text-muted)' }}>
          {message}
        </div>
      )}

      {degradedHalls.length > 0 && (
        <ClashWarningBanner
          title="Degraded Halls"
          violations={degradedHalls.map(h => `${h} — imperfect course interleaving`)}
          variant="warning"
        />
      )}

      {hallsData.length === 0 ? (
        <EmptyState
          title="No seating data"
          message="Generate seating first, then select a slot above to view the hall layout."
        />
      ) : (
        <div className="seating-halls">
          {hallsData.map(hall => (
            <HallSeatGrid
              key={hall.hallId}
              hallData={hall}
              isDegraded={degradedHalls.includes(hall.hallName)}
            />
          ))}
        </div>
      )}
    </div>
  );
}
