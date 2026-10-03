import { useState } from 'react';
import { importStudents, importCourses, importEnrollments } from '../services/importApi';
import '../styles/ImportDataPage.css';

function ImportCard({ title, description, format, onImport }) {
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  async function handleUpload() {
    if (!file) return;
    setLoading(true);
    setResult(null);

    try {
      const response = await onImport(file);
      setResult({ type: 'success', message: response.data.message });
      setFile(null);
    } catch (err) {
      const message = err.response?.data?.message || 'Import failed';
      setResult({ type: 'error', message });
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="card">
      <div className="import-card-title">{title}</div>
      <div className="import-card-desc">
        {description}
        <br />
        Format: <code>{format}</code>
      </div>

      <div className="file-input-wrapper">
        <input
          type="file"
          accept=".csv"
          onChange={e => setFile(e.target.files[0])}
        />
        <button
          className="btn btn-primary"
          onClick={handleUpload}
          disabled={!file || loading}
        >
          {loading ? 'Uploading...' : 'Upload'}
        </button>
      </div>

      {result && (
        <div className={`import-result ${result.type}`}>
          {result.message}
        </div>
      )}
    </div>
  );
}

export default function ImportDataPage() {
  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Import Data</h1>
        <p className="page-subtitle">Upload CSV files to load students, courses, and enrollments</p>
      </div>

      <div className="import-page-grid">
        <ImportCard
          title="Students"
          description="Upload student records. Duplicates (by reg_no) are skipped."
          format="reg_no, name, branch, semester"
          onImport={importStudents}
        />

        <ImportCard
          title="Courses"
          description="Upload courses. Faculty must be imported first. Duplicates (by code) are skipped."
          format="code, title, semester, faculty_id"
          onImport={importCourses}
        />

        <ImportCard
          title="Enrollments"
          description="Upload student-course enrollments. Both student and course must exist."
          format="student_reg_no, course_code"
          onImport={importEnrollments}
        />
      </div>
    </div>
  );
}
