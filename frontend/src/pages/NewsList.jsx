import React,{useEffect,useState} from 'react';
import {Link} from 'react-router-dom';
import axios from 'axios';
import {API} from '@/context/AuthContext';
import {NewsCard} from '@/components/NewsCard';
export default function NewsList({archive=false}){
 const [items,setItems]=useState([]),[error,setError]=useState(false),[category,setCategory]=useState('Tutte');
 useEffect(()=>{let active=true;const refresh=()=>axios.get(`${API}/news`,{params:{scope:archive?'archive':'current',limit:100}}).then(r=>{if(active){setItems(r.data);setError(false)}}).catch(()=>{if(active)setError(true)});refresh();const t=setInterval(refresh,60000);return()=>{active=false;clearInterval(t)}},[archive]);
 const categories=['Tutte',...new Set(items.map(n=>n.category))];
 return <div className="max-w-7xl mx-auto px-4 sm:px-6 py-6 text-[#0B2545]"><h1 className="font-serif-display text-3xl font-bold">{archive?'Archivio delle notizie':'Ultime notizie'}</h1><p className="text-sm mt-2">{archive?'Edizioni precedenti, con data e fonte originali.':'La selezione aggiornata della redazione.'}</p><Link to={archive?'/notizie':'/archivio'} className="inline-block underline text-sm mt-3">{archive?'Torna alle ultime notizie':'Consulta l’archivio'}</Link><div className="flex flex-wrap gap-2 my-4">{categories.map(c=><button key={c} onClick={()=>setCategory(c)} aria-pressed={c===category} className={`px-3 py-2 rounded border text-sm ${c===category?'bg-[#0B2545] text-white':'bg-white'}`}>{c}</button>)}</div>{error&&<p role="alert">Notizie temporaneamente non disponibili.</p>}<div className="space-y-3">{items.filter(n=>category==='Tutte'||n.category===category).map(n=><NewsCard key={n.id} item={n}/>)}</div>{!items.length&&!error&&<p className="py-3">Nessuna notizia disponibile.</p>}</div>
}
