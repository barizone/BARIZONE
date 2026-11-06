import React,{useEffect,useState} from 'react';
import axios from 'axios';
import {Link,useNavigate} from 'react-router-dom';
import {API} from '@/context/AuthContext';
import NewsCarousel from '@/components/NewsCarousel';
import {neighborhoods} from '@/pages/NeighborhoodNews';
import BusinessSpotlight from '@/components/BusinessSpotlight';
import WeeklyWeather from '@/components/WeeklyWeather';
export default function Home(){
 const navigate=useNavigate();
 const [news,setNews]=useState([]),[merchants,setMerchants]=useState([]),[category,setCategory]=useState('Tutte'),[loading,setLoading]=useState(true),[error,setError]=useState(false),[editorialStatus,setEditorialStatus]=useState(null);
 useEffect(()=>{let active=true;const refresh=async()=>{const [n,m,status]=await Promise.allSettled([axios.get(`${API}/news`,{params:{scope:'current',limit:100}}),axios.get(`${API}/merchants`),axios.get(`${API}/news/status`)]);if(!active)return;if(n.status==='fulfilled'){setNews(n.value.data.filter((x,i,a)=>x.source_url&&a.findIndex(y=>y.id===x.id||y.source_url===x.source_url)===i));setError(false)}else setError(true);if(m.status==='fulfilled')setMerchants(m.value.data);if(status.status==='fulfilled')setEditorialStatus(status.value.data);setLoading(false)};refresh();const timer=setInterval(refresh,30000);return()=>{active=false;clearInterval(timer)}},[]);
 const categories=['Tutte',...new Set(news.map(n=>n.category))],shown=news.filter(n=>category==='Tutte'||n.category===category);
 const edition=news.reduce((max,n)=>{const d=n.edition_date||n.fact_date||n.published_at?.slice(0,10)||'';return d>max?d:max},'');
 return <div className="text-[#0B2545]">
 <div className="home-discovery max-w-7xl mx-auto px-4 sm:px-6 pt-3 pb-2">
 <section aria-labelledby="rione-heading">
  <div className="rounded-xl bg-[#0B2545] text-white p-4 sm:p-5">
   <p className="text-xs uppercase tracking-widest text-[#EFB94F]">Oggi a Bari · rione per rione</p>
   <h1 id="rione-heading" className="font-serif-display text-3xl sm:text-4xl font-bold mt-1">Scegli il tuo rione</h1>
   <label htmlFor="home-rione" className="block text-sm mt-3 mb-2">Notizie, luoghi e attività del tuo quartiere</label>
   <select id="home-rione" defaultValue="" onChange={e=>{if(e.target.value)navigate('/quartieri?q='+encodeURIComponent(e.target.value))}} className="w-full min-h-12 rounded-lg bg-white text-[#0B2545] px-3 py-3 text-base border border-white/30 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#EFB94F]">
    <option value="" disabled>Seleziona un quartiere o rione</option>
    {neighborhoods.filter(q=>q!=='Tutta Bari').map(q=><option key={q} value={q}>{q}</option>)}
   </select>
  </div>
 </section>
 <div className="home-discovery-side min-w-0">
 <section aria-label="Informazioni e attività">
  <Link to="/aziende" className="flex items-center justify-between gap-3 rounded-lg border border-[#E5DFD3] bg-white px-4 py-3 mb-4 hover:border-[#D9531E]"><span className="font-semibold">Le attività dei rioni</span><span className="text-sm">Scopri le vetrine →</span></Link>
  <BusinessSpotlight/>
 </section>
 </div>
 </div>
 <section className="max-w-7xl mx-auto px-4 sm:px-6 pt-3 pb-2"><div className="flex flex-wrap items-end justify-between gap-3"><div><p className="text-xs uppercase tracking-widest text-[#D9531E]">La prima app dei Baresi</p><h2 className="font-serif-display text-3xl sm:text-4xl font-black mt-1">Barizone News</h2></div><Link to="/archivio" className="underline text-sm inline-flex items-center min-h-11">Archivio notizie →</Link></div><p className="text-sm mt-3">{edition?`Edizione del ${new Date(edition+'T12:00:00').toLocaleDateString('it-IT')} · `:''}Notizie verificate, divise per rione.</p>{editorialStatus?.checked_at&&<p className="text-xs mt-2 text-slate-600">Ultimo controllo delle fonti: <time dateTime={editorialStatus.checked_at}>{new Date(editorialStatus.checked_at).toLocaleString('it-IT',{timeZone:'Europe/Rome',dateStyle:'short',timeStyle:'short'})}</time>{editorialStatus.outcome==='no_new_items'?' · Nessuna nuova notizia verificata.':''}</p>}</section>
 <section className="max-w-7xl mx-auto px-4 sm:px-6 py-3"><div aria-label="Categorie notizie" className="flex flex-wrap gap-2 mb-4">{categories.map(c=><button key={c} aria-pressed={category===c} onClick={()=>setCategory(c)} className={`px-3 py-2 rounded text-sm border ${category===c?'bg-[#0B2545] text-white border-[#0B2545]':'bg-white border-[#E5DFD3]'}`}>{c}</button>)}</div>
 {loading&&<p role="status">Caricamento rassegna…</p>}{error&&<p role="alert">Aggiornamento temporaneamente non disponibile.</p>}
 <NewsCarousel key={category} items={shown}/>{!loading&&!error&&!shown.length&&<p className="py-3">Nessuna notizia verificata per questa selezione.</p>}</section>

 <WeeklyWeather/>
 {merchants.length>0&&<section className="max-w-7xl mx-auto px-4 sm:px-6 py-3"><h2 className="font-serif-display text-2xl font-bold mb-3">Attività nei rioni</h2><div className="flex flex-col gap-3">{merchants.map(m=><Link key={m.id} to={'/aziende/'+m.id} className="flex flex-col sm:flex-row items-start gap-4 border border-[#E5DFD3] bg-white rounded p-4">{m.cover_image&&<img src={m.cover_image} alt={m.business_name} loading="lazy" className="w-full sm:w-48 aspect-[4/3] object-contain bg-[#0B2545] rounded shrink-0"/>}<div className="min-w-0 flex-1"><h3 className="text-lg font-bold">{m.business_name}</h3>{m.slogan&&<p className="text-[#D9531E] font-semibold mt-1">{m.slogan}</p>}<p className="text-sm mt-1">{m.quartiere} · {m.description}</p></div></Link>)}</div></section>}
 </div>;
}

