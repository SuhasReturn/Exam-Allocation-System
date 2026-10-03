import '../styles/ClashWarningBanner.css';

export default function ClashWarningBanner({ title, violations, variant = 'danger' }) {
  if (!violations || violations.length === 0) {
    return null;
  }

  return (
    <div className={`clash-banner ${variant}`}>
      <div className="clash-banner-title">{title || 'Clashes Detected'}</div>
      <ul className="clash-banner-list">
        {violations.map((v, index) => (
          <li key={index}>{v}</li>
        ))}
      </ul>
    </div>
  );
}
