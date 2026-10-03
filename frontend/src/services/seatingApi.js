import axios from 'axios';

const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export function generateSeating() {
  return api.post('/admin/seating/generate');
}

export function getSeatingBySlot(slotId) {
  return api.get(`/admin/seating/${slotId}`);
}

export function getMyExams() {
  return api.get('/student/my-exams');
}
