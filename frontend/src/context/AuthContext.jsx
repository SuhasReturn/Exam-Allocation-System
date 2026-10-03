import { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [role, setRole] = useState(localStorage.getItem('role'));
  const [username, setUsername] = useState(localStorage.getItem('username'));

  const isLoggedIn = !!token;

  function login(tokenValue, roleValue, usernameValue) {
    localStorage.setItem('token', tokenValue);
    localStorage.setItem('role', roleValue);
    localStorage.setItem('username', usernameValue);
    setToken(tokenValue);
    setRole(roleValue);
    setUsername(usernameValue);
  }

  function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('username');
    setToken(null);
    setRole(null);
    setUsername(null);
  }

  return (
    <AuthContext.Provider value={{ token, role, username, isLoggedIn, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return context;
}
