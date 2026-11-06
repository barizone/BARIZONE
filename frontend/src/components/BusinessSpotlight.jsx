import React,{useEffect,useState} from 'react';
import {Link} from 'react-router-dom';
import {useAuth} from '@/context/AuthContext';
export default function BusinessSpotlight({sidebar=false}){
 const containerClass=sidebar?'home-spotlight home-spotlight-sidebar':'home-spotlight w-full';
 const [data,setData]=useState(null);const {user}=useAuth();
 useEffect(()=>{let active=true;async function refresh(){try{const r=await fetch('/api/merchants/spotlight',{cache:'no-store'});if(!r.ok)throw Error();const d=await r.json();if(active)setData(d)}catch{if(active)setData(null)}}refresh();const timer=setInterval(refresh,30000);const focus=()=>refresh();window.addEventListener('focus',focus);return()=>{active=false;clearInterval(timer);window.removeEventListener('focus',focus)}},[]);
 if(!data)return null;
 if(!data.merchant)return user?.role==='admin'?<section className={containerClass}><div className="border rounded-lg bg-[#F8F5EE] p-4"><strong>Vetrina del giorno</strong><p className="text-sm mt-1">Nessuna attività pubblicata. Pubblica una scheda per visualizzarla nel banner e nel suo rione.</p><Link to="/admin" className="inline-block underline mt-2 text-sm">Apri le schede aziende →</Link></div></section>:null;
 const m=data.merchant;
 return <section aria-label="Vetrina del giorno" className={containerClass}><div className={`bg-[#0B2545] text-white rounded-xl overflow-hidden flex flex-col ${sidebar?'':'sm:flex-row'} shadow-sm`}>{m.cover_image&&<div className={`w-full ${sidebar?'':'sm:w-1/2'} shrink-0 self-start`}><img src={m.cover_image} alt={m.business_name} className="block w-full aspect-[4/3] object-contain bg-[#0B2545]"/></div>}<div className="p-5 sm:p-7 flex-1"><div className="flex flex-wrap justify-between gap-2 text-xs"><span className="uppercase tracking-widest text-[#EFB94F]">Vetrina del giorno · Barizone</span><span>{new Date(data.date+'T12:00:00').toLocaleDateString('it-IT')}</span></div><p className="text-xs mt-3 text-white/80">{m.entity_type||m.category} · {m.quartiere}</p><h2 className="font-serif-display font-bold text-2xl sm:text-3xl mt-1">{m.business_name}</h2>{m.slogan&&<p className="mt-2 text-lg">{m.slogan}</p>}<Link to={'/aziende/'+m.id} className="inline-block bg-[#D9531E] text-white px-5 py-2.5 rounded mt-4">Scopri l’attività →</Link></div></div></section>;
}
