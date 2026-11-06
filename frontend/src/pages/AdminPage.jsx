import React,{useState} from 'react';
import {Navigate,Link} from 'react-router-dom';
import {useAuth} from '@/context/AuthContext';
import ContactAdmin from '@/components/ContactAdmin';
import SubscribersAdmin from '@/components/SubscribersAdmin';
import NewsAdmin from './NewsAdmin';
import PushAdmin from './PushAdmin';
import MerchantAdmin from '@/components/MerchantAdmin';
export default function AdminPage(){const {user,loading}=useAuth();const [tab,setTab]=useState(new URLSearchParams(window.location.search).get('sezione')==='iscritti'?'subscribers':new URLSearchParams(window.location.search).get('sezione')==='contatti'?'contacts':'companies');if(loading)return null;if(user?.role!=='admin')return <Navigate to="/login?next=%2Fadmin" replace/>;return <><h1 className="max-w-6xl mx-auto px-4 pt-6 font-serif-display text-3xl font-bold">Amministrazione · Leonardo Favia</h1><div className="max-w-6xl mx-auto px-4 pt-8 flex flex-wrap gap-3"><Link to="/gestione-clienti" className="px-4 py-2 rounded-md border border-[#0B2545]">QR e clienti delle aziende</Link><button onClick={()=>setTab('contacts')} className="px-4 py-2 rounded-md border border-[#0B2545]">Contatti e procedura</button><button onClick={()=>setTab('news')} className={`px-4 py-2 rounded-md ${tab==='news'?'bg-[#0B2545] text-white':'border border-[#0B2545]'}`}>Notizie</button><button onClick={()=>setTab('push')} className={`px-4 py-2 rounded-md ${tab==='push'?'bg-[#0B2545] text-white':'border border-[#0B2545]'}`}>Notifiche e campagne</button><button onClick={()=>setTab('subscribers')} className="px-4 py-2 rounded-md border border-[#0B2545]">Dashboard iscritti</button><button onClick={()=>setTab('companies')} className={`px-4 py-2 rounded-md ${tab==='companies'?'bg-[#0B2545] text-white':'border border-[#0B2545]'}`}>Schede aziende</button></div>{tab==='contacts'?<ContactAdmin/>:tab==='subscribers'?<SubscribersAdmin/>:tab==='news'?<NewsAdmin/>:tab==='push'?<PushAdmin/>:<div className="max-w-6xl mx-auto px-4 pb-10"><MerchantAdmin/></div>}</>}


