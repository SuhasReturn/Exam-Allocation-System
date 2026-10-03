import axios from 'axios';

const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export function importStudents(file) {
  const formData = new FormData();
  formData.append('file', file);
  return api.post('/admin/import/students', formData);
}

export function importCourses(file) {
  const formData = new FormData();
  formData.append('file', file);
  return api.post('/admin/import/courses', formData);
}

export function importEnrollments(file) {
  const formData = new FormData();
  formData.append('file', file);
  return api.post('/admin/import/enrollments', formData);
}

export function getStudents() {
  return api.get('/admin/students');
}

export function getCourses() {
  return api.get('/admin/courses');
}

export function getFaculty() {
  return api.get('/admin/faculty');
}

export function getHalls() {
  return api.get('/admin/halls');
}
