import axios from 'axios';

const api = axios.create({
  baseURL: '/api'
});

// attach JWT token to every outgoing request
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function loginUser(username, password) {
  return api.post('/auth/login', { username, password });
}
