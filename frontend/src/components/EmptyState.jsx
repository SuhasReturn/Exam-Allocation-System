import '../styles/EmptyState.css';

export default function EmptyState({ title, message }) {
  return (
    <div className="empty-state">
      <div className="empty-state-icon">📋</div>
      <div className="empty-state-title">{title || 'No data yet'}</div>
      <div className="empty-state-text">{message || 'Import or generate data to see it here.'}</div>
    </div>
  );
}
