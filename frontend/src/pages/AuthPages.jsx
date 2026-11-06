import React,{useEffect,useState} from 'react';
import {Link,useNavigate,useSearchParams} from 'react-router-dom';
import axios from 'axios';
import {API,useAuth} from '@/context/AuthContext';
export function LoginPage(){
 const {user,setUser}=useAuth(),navigate=useNavigate(),[searchParams]=useSearchParams();
 const [username,setUsername]=useState('leonardo.favia');
 const [password,setPassword]=useState(''),[error,setError]=useState(''),[busy,setBusy]=useState(false);
 const requested=searchParams.get('next')||'/admin',next=requested.startsWith('/')&&!requested.startsWith('//')?requested:'/admin';
 useEffect(()=>{if(user)navigate(next,{replace:true})},[user,next,navigate]);
 const submit=async event=>{
  event.preventDefault();setBusy(true);setError('');
    try{const response=await axios.post(`${API}/auth/login`,{username,password});setUser(response.data);navigate(next,{replace:true})}
  catch(e){setError(e.response?.data?.detail||'Accesso non riuscito. Riprova.')}
  finally{setBusy(false)}
 };
 if(user)return null;
 return <AuthShell title="Accesso redazione" subtitle="Area riservata all’amministratore Barizone.">
  <form onSubmit={submit} className="space-y-4">
   <label className="block font-ui text-sm text-[#0B2545]">Nome utente<input name="username" autoComplete="username" maxLength={80} value={username} onChange={event=>setUsername(event.target.value)} className="block w-full mt-1 px-3 py-3 border border-[#E5DFD3] rounded"/></label>
   <label className="block font-ui text-sm text-[#0B2545]">Password amministratore<input name="password" type="password" autoComplete="current-password" required maxLength={1024} value={password} onChange={event=>setPassword(event.target.value)} className="block w-full mt-1 px-3 py-3 border border-[#E5DFD3] rounded font-body text-base"/></label>
   {error&&<p role="alert" className="text-red-700 text-sm">{error}</p>}
   <button disabled={busy} className="w-full bg-[#0B2545] text-white rounded py-3 font-ui font-semibold disabled:opacity-50">{busy?'Accesso…':'Accedi'}</button>
  </form><p className="text-sm mt-4">Area riservata alla redazione. Sei un utente o un’attività? <Link to="/accedi" className="underline">Accedi qui</Link>.</p>
 </AuthShell>;
}
function AuthShell({title,subtitle,children}){return <div className="px-4 py-6 sm:py-8"><div className="max-w-md mx-auto"><Link to="/" className="flex justify-center mb-6"><img src="/logo-224.webp" alt="Barizone" className="h-20 w-20 object-contain"/></Link><div className="bg-white border border-[#E5DFD3] rounded-md p-6 sm:p-8"><h1 className="font-serif-display text-3xl font-black text-[#0B2545]">{title}</h1><p className="mt-2 mb-6 text-slate-600 font-body">{subtitle}</p>{children}</div></div></div>}
