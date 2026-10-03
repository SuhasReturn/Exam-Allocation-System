import axios from 'axios';

const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export function generateTimetable(startDate, slotsPerDay) {
  return api.post(`/admin/timetable/generate?startDate=${startDate}&slotsPerDay=${slotsPerDay}`);
}

export function getTimetable() {
  return api.get('/admin/timetable');
}

export function validateTimetable() {
  return api.get('/admin/timetable/validate');
}
