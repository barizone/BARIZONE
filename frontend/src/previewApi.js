import axios from "axios";
import { neighborhoods as allNeighborhoods } from "@/pages/NeighborhoodNews";

// I dati offline restano separati: online le API effettive non usano vetrine fittizie di ripiego.
const neighborhoods = allNeighborhoods.filter(q => !["Tutta Bari", "Quartiere da verificare"].includes(q));
const image = id => `https://images.unsplash.com/${id}?w=1200&q=80`;
const restaurants = [];
const events = [
  { id: "demo-e1", title: "Spazio per eventi culturali", category: "Mostre", venue: "Bari · luogo da confermare", quartiere: "Murat", date: "2027-01-01T12:00:00Z", price: "Da verificare", description: "Esempio di come apparirà un evento con fonte e dettagli verificati.", cover_image: image("photo-1577083552431-6e5fd01aa342"), is_demo: true },
  { id: "demo-e2", title: "Spazio per eventi sportivi", category: "Sport", venue: "Bari · luogo da confermare", quartiere: "Poggiofranco", date: "2027-01-02T12:00:00Z", price: "Da verificare", description: "Scheda dimostrativa; nessun evento reale è annunciato.", cover_image: image("photo-1461896836934-ffe607ba8211"), is_demo: true },
];
const merchants = [
  { id: "demo-m1", business_name: "Azienda di esempio", category: "Alimentari", quartiere: "Carrassi", description: "Scheda dimostrativa. Sarà sostituita dopo la verifica di una reale attività aderente a Barizone.", status: "published", is_demo: true },
];
const news = [{ id: "demo-n1", title: "Le notizie del tuo quartiere", subtitle: "Anteprima della sezione notizie", category: "Attualità", quartiere: "Bari", published_at: "2026-09-29T08:00:00Z", content: "Qui appariranno notizie con fonte e data verificate.", cover_image: image("photo-1516483638261-f4dbaf036963"), is_demo: true, is_featured: true }];

const companies=[...merchants,...restaurants.map(r=>({...r,business_name:r.name,category:r.type,directory_category:"Ristoranti",status:"published"}))];
axios.defaults.adapter = async config => {
  if(window.location.protocol==='file:'&&config.method==='get'&&config.url.split('?')[0].endsWith('/merchants/demo-m1'))return {data:companies[0],status:200,headers:{},config};
  const offline=window.location.protocol==='file:';
  if(offline && /\/api\/(auth|admin|push)(\/|$|\?)/.test(config.url)){const error=new Error("Apri l’app online per usare questa funzione.");error.response={data:{detail:error.message},status:401};throw error;}
  if(offline && /\/api\/news(\/|$|\?)/.test(config.url))return {data:[],status:200,headers:{},config};
  if (!offline && /\/api\/(news|auth|merchants|admin|push|groups|subscribers|media)(\/|$|\?)/.test(config.url)) {
    const url=new URL(config.url,offline?"https://offline.barizone.test":window.location.origin);
    Object.entries(config.params||{}).forEach(([key,value])=>{if(value!==undefined&&value!==null)url.searchParams.set(key,value)});
    const response=await fetch(url,{method:config.method.toUpperCase(),credentials:'same-origin',headers:{'Content-Type':'application/json'},body:config.data||undefined});
    const data=await response.json();
    if(!response.ok){const error=new Error(data.detail||'Servizio non disponibile');error.response={data,status:response.status};throw error;}
    const result=data;
    return {data:result,status:response.status,statusText:response.statusText,headers:{},config};
  }
  const path = new URL(config.url, offline?"https://offline.barizone.test":window.location.origin).pathname.replace(/^.*\/api/, "");
  if (path === "/auth/me") return Promise.reject(new Error("Nessun accesso nell'anteprima"));
  if (config.method !== "get") return Promise.reject(new Error("Funzione disponibile solo nella versione con server"));
  let data;
  if (path === "/meta") data = { neighborhoods, restaurant_types: [...new Set(restaurants.map(r => r.type))] };
  else if (path === "/news/featured") data = news;
  else if (path === "/news") data = news.filter(n => !config.params?.category || n.category === config.params.category);
  else if (path.startsWith("/news/")) data = path.endsWith("/comments") ? [] : news.find(n => n.id === path.split("/")[2]);
  else if (path === "/events") data = events.filter(e => (!config.params?.category || e.category === config.params.category) && (!config.params?.quartiere || e.quartiere === config.params.quartiere));
  else if (path === "/restaurants") data = restaurants.filter(r => (!config.params?.type || r.type === config.params.type) && (!config.params?.quartiere || r.quartiere === config.params.quartiere));
  else if (path === "/merchants") data = companies.filter(m => !config.params?.quartiere || m.quartiere === config.params.quartiere);
  else if (path.startsWith("/merchants/")) data = companies.find(m => m.id === path.split("/")[2]);
  else data = [];
  return { data, status: 200, statusText: "OK", headers: {}, config };
};

