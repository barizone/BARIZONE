import React, { useEffect, useState, useCallback } from 'react';
import axios from 'axios';
export const API = `${process.env.REACT_APP_BACKEND_URL || ''}/api`;
axios.defaults.withCredentials = true;
const AuthContext = React.createContext(null);
export const AuthProvider = ({ children }) => {
 const [user,setUser]=useState(null),[loading,setLoading]=useState(true);
 const checkAuth=useCallback(async()=>{try{setUser((await axios.get(`${API}/auth/me`)).data)}catch{setUser(null)}finally{setLoading(false)}},[]);
 useEffect(()=>{checkAuth()},[checkAuth]);
 const logout=async()=>{try{await axios.post(`${API}/auth/logout`)}finally{setUser(null)}};
 return <AuthContext.Provider value={{user,setUser,loading,logout,checkAuth}}>{children}</AuthContext.Provider>;
};
export const useAuth=()=>React.useContext(AuthContext);
