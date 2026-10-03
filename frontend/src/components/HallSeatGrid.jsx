import '../styles/HallSeatGrid.css';

const COURSE_COLORS = [
  'var(--course-color-0)',
  'var(--course-color-1)',
  'var(--course-color-2)',
  'var(--course-color-3)',
  'var(--course-color-4)',
  'var(--course-color-5)',
  'var(--course-color-6)',
  'var(--course-color-7)',
];

export default function HallSeatGrid({ hallData, isDegraded }) {
  const { hallName, totalRows, totalColumns, seatedCount, seats } = hallData;

  // assign a color index to each unique course code
  const courseCodes = [...new Set(seats.map(s => s.courseCode))];
  const courseColorMap = {};
  courseCodes.forEach((code, i) => {
    courseColorMap[code] = COURSE_COLORS[i % COURSE_COLORS.length];
  });

  // build a seat lookup: seatNo -> seat data
  const seatMap = {};
  for (const seat of seats) {
    seatMap[seat.seatNo] = seat;
  }

  const totalCells = totalRows * totalColumns;

  return (
    <div className="seat-grid-container card">
      <div className="seat-grid-header">
        <div>
          <span className="seat-grid-hall-name">{hallName}</span>
          {isDegraded && (
            <span className="badge badge-warning" style={{ marginLeft: 8 }}>
              Degraded
            </span>
          )}
        </div>
        <span className="seat-grid-info">
          {seatedCount} / {totalCells} seats filled
        </span>
      </div>

      <div className="seat-grid" style={{ gridTemplateColumns: `repeat(${totalColumns}, 1fr)` }}>
        {Array.from({ length: totalCells }, (_, i) => {
          const seatNo = i + 1;
          const seat = seatMap[seatNo];

          if (!seat) {
            return (
              <div key={seatNo} className="seat-cell empty">
                {seatNo}
              </div>
            );
          }

          return (
            <div
              key={seatNo}
              className="seat-cell"
              style={{ backgroundColor: courseColorMap[seat.courseCode] }}
            >
              {seatNo}
              <div className="seat-tooltip">
                {seat.studentName} ({seat.studentRegNo})
                <br />
                {seat.courseCode} — {seat.courseTitle}
              </div>
            </div>
          );
        })}
      </div>

      <div className="seat-legend">
        {courseCodes.map(code => (
          <div key={code} className="legend-item">
            <div className="legend-swatch" style={{ backgroundColor: courseColorMap[code] }} />
            {code}
          </div>
        ))}
      </div>
    </div>
  );
}
