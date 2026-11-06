import React, { useEffect, useState, useCallback } from 'react';
import axios from 'axios';

export const API = `${process.env.REACT_APP_BACKEND_URL || ''}/api`;
axios.defaults.withCredentials = true;

const AuthContext = React.createContext(null);

/**
 * Tre tipi di accesso, ciascuno con la propria sessione:
 *  - `user`    → amministratore (area redazione, /login)
 *  - `account` → utente registrato o attività (/accedi), con `account.role` = 'user' | 'merchant'
 */
export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [account, setAccount] = useState(null);
  const [loading, setLoading] = useState(true);

  const checkAuth = useCallback(async () => {
    const [admin, acc] = await Promise.allSettled([axios.get(`${API}/auth/me`), axios.get(`${API}/account/me`)]);
    setUser(admin.status === 'fulfilled' ? admin.value.data : null);
    setAccount(acc.status === 'fulfilled' ? acc.value.data : null);
    setLoading(false);
  }, []);

  useEffect(() => { checkAuth(); }, [checkAuth]);

  const logout = async () => { try { await axios.post(`${API}/auth/logout`); } finally { setUser(null); } };
  const accountLogout = async () => { try { await axios.post(`${API}/account/logout`); } finally { setAccount(null); } };

  return <AuthContext.Provider value={{ user, setUser, account, setAccount, loading, logout, accountLogout, checkAuth }}>{children}</AuthContext.Provider>;
};

export const useAuth = () => React.useContext(AuthContext);
