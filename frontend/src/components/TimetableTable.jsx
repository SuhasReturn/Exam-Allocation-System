export default function TimetableTable({ rows }) {
  return (
    <div className="table-container">
      <table className="data-table">
        <thead>
          <tr>
            <th>Date</th>
            <th>Session</th>
            <th>Code</th>
            <th>Course</th>
            <th>Semester</th>
            <th>Faculty</th>
            <th>Enrolled</th>
          </tr>
        </thead>
        <tbody>
          {rows.map(row => (
            <tr key={row.examId}>
              <td>{row.examDate}</td>
              <td><span className="badge badge-primary">{row.session}</span></td>
              <td style={{ fontWeight: 500 }}>{row.courseCode}</td>
              <td>{row.courseTitle}</td>
              <td>{row.semester}</td>
              <td>{row.facultyName}</td>
              <td>{row.enrolledCount}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
