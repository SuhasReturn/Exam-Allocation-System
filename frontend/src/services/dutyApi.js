import axios from 'axios';

const api = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '/api' });

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export function generateDuties() {
  return api.post('/admin/duties/generate');
}

export function getAllDuties() {
  return api.get('/admin/duties');
}

export function replaceDuty(dutyId) {
  return api.put(`/admin/duties/${dutyId}/replace`);
}

export function getMyDuties() {
  return api.get('/faculty/my-duties');
}

export function markUnavailableDates(dates) {
  return api.post('/faculty/unavailable-dates', { dates });
}
