import React,{useEffect,useState} from 'react';
import {Link,useSearchParams} from 'react-router-dom';
import axios from 'axios';
import {API} from '@/context/AuthContext';
import RioneStory from '@/components/RioneStory';
import BusinessCards from '@/components/BusinessCards';
import {NewsCard} from '@/components/NewsCard';
import {selectRioneNews,selectRioneRestaurants} from '@/lib/rioneSelection';
export const neighborhoods=['Tutta Bari','San Nicola / Bari Vecchia','Murat','Madonnella','Libertà','Carrassi','San Pasquale','Picone','Poggiofranco','Japigia','San Paolo','San Girolamo','Fesca','Marconi','Stanic','Villaggio del Lavoratore','Carbonara','Ceglie del Campo','Loseto','Palese','Santo Spirito','San Pio','Catino','Torre a Mare','Sant’Anna'];
const normalize=q=>q==='Bari Vecchia'?'San Nicola / Bari Vecchia':q;
export default function NeighborhoodNews(){
 const [params,setParams]=useSearchParams(),q=normalize(params.get('q')||'');
 const section=['eventi','news','mangiare'].includes(params.get('sezione'))?params.get('sezione'):'news';
 const select=(rione,view=section)=>setParams({...rione?{q:rione}:{},sezione:view});
 const [merchants,setMerchants]=useState([]),[merchantError,setMerchantError]=useState(false);
 const [items,setItems]=useState([]),[error,setError]=useState(false),[loading,setLoading]=useState(true);
 useEffect(()=>{setLoading(true);setItems([]);let active=true;const refresh=()=>axios.get(`${API}/news`,{params:{scope:'neighborhood',neighborhood:q||undefined,limit:100}}).then(r=>{if(active){setItems(r.data.filter(n=>!n.is_demo&&n.source_url));setError(false)}}).catch(()=>{if(active)setError(true)}).finally(()=>{if(active)setLoading(false)});refresh();const timer=setInterval(refresh,60000);return()=>{active=false;clearInterval(timer)}},[q]);
 useEffect(()=>{let active=true;setMerchants([]);const refresh=()=>axios.get(`${API}/merchants`,{params:q&&q!=='Tutta Bari'?{quartiere:q}:{}}).then(r=>{if(active){setMerchants(r.data);setMerchantError(false)}}).catch(()=>{if(active)setMerchantError(true)});refresh();const timer=setInterval(refresh,30000);return()=>{active=false;clearInterval(timer)}},[q]);
 const local=selectRioneNews(items,q,section);
 const restaurants=selectRioneRestaurants(merchants,q);
 
 return <div className="max-w-7xl mx-auto px-4 sm:px-6 py-6 text-[#0B2545]">
 <label className="flex flex-wrap items-center gap-3 mb-5 font-semibold">Seleziona il quartiere<select aria-label="Seleziona il quartiere" value={q} onChange={e=>select(e.target.value)} className="border rounded-lg p-3 bg-white max-w-full min-h-12"><option value="">Tutti i rioni</option>{neighborhoods.map(n=><option key={n}>{n}</option>)}</select></label>
 <p className="text-xs font-semibold uppercase tracking-widest text-[#D9531E]">Oggi a Bari · vivi il tuo rione</p><h1 className="font-serif-display text-3xl sm:text-4xl font-bold mt-2">{q||'Bari, rione per rione'}</h1>
 <nav aria-label="Cosa cerchi nel rione" className="grid sm:grid-cols-3 gap-3 my-5">{[['eventi','Eventi'],['news','News'],['mangiare','Dove mangiare oggi a Bari']].map(([value,label])=><button type="button" key={value} aria-pressed={section===value} onClick={()=>select(q,value)} className={'rounded-lg border px-4 py-3 text-base font-semibold '+(section===value?'bg-[#0B2545] text-white border-[#0B2545]':'bg-white border-[#E5DFD3] hover:border-[#D9531E]')}>{label}</button>)}</nav>
 <section aria-labelledby="rione-content-heading" className="mb-6">
 <h2 id="rione-content-heading" className="font-serif-display text-2xl font-bold mb-3">{section==='eventi'?'Eventi':section==='mangiare'?'Dove mangiare oggi a Bari':'News'}{q&&q!=='Tutta Bari'?' · '+q:''}</h2>
 {section==='mangiare'?<><p className="text-sm mb-4">Vetrine dei ristoranti del rione. Per mangiare oggi, contatta il locale e conferma orari e disponibilità.</p>{merchantError?<p role="alert">Vetrine temporaneamente non disponibili.</p>:restaurants.length?<BusinessCards items={restaurants}/>:<p>Nessun ristorante pubblicato per {q||'questa selezione'}.</p>}</>:<>
 <p className="text-sm mb-4">Contenuti del rione selezionato, con fonte e data originale.</p>
 {loading&&<p role="status">Caricamento {section==='eventi'?'eventi':'notizie'}…</p>}{error&&<p role="alert">Aggiornamento temporaneamente non disponibile. Riprova.</p>}
 <div className="space-y-3">{local.map(n=><NewsCard key={n.id} item={n}/>)}</div>
 {!loading&&!error&&!local.length&&<p className="py-3">{section==='eventi'?'Nessun evento verificato disponibile':'Nessuna notizia verificata disponibile'} per {q||'questa selezione'}.</p>}
 <Link className="inline-block mt-4 underline" to="/archivio">Consulta le edizioni precedenti →</Link>
 </>}
 </section>
 <RioneStory neighborhood={q}/>
 <section id="attivita-del-rione" aria-label="Attività e professionisti del rione" className="my-5"><div className="flex flex-wrap justify-between gap-2 mb-3"><h2 className="font-serif-display text-2xl font-bold">Attività e professionisti{q&&q!=='Tutta Bari'?' · '+q:''}</h2><Link to={'/aziende'+(q&&q!=='Tutta Bari'?'?q='+encodeURIComponent(q):'')} className="underline text-sm">Tutte le vetrine →</Link></div>{merchants.length?<BusinessCards items={merchants}/>:<p className="text-sm">{merchantError?'Vetrine temporaneamente non disponibili.':'Nessuna vetrina pubblicata per questo rione.'}</p>}</section>
 </div>;
}



