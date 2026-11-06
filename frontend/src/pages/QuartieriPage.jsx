import React, { useEffect, useState } from "react";
import axios from "axios";
import { Link, useSearchParams } from "react-router-dom";
import { API } from "@/context/AuthContext";
import {neighborhoods} from "./NeighborhoodNews";
import { MapPin, Tag } from "lucide-react";
import RadioSoundCityLogo from "@/components/RadioSoundCityLogo";

const QUARTIERI_COLORS = {
  "Murat": "#0B2545",
  "San Nicola / Bari Vecchia": "#D9531E",
  "Madonnella": "#D9A036",
  "Libertà": "#2563EB",
  "Poggiofranco": "#15803D",
  "Japigia": "#0284C7",
  "Carrassi": "#8B5CF6",
  "San Pasquale": "#059669",
  "Picone": "#EA580C",
  "San Paolo": "#4F46E5",
  "Torre a Mare": "#0891B2",
};

export default function QuartieriPage() {
  const [merchants, setMerchants] = useState([]);
  const meta = { neighborhoods };
  const [searchParams, setSearchParams] = useSearchParams();
  const q = searchParams.get("q") || "";
  const category=searchParams.get("categoria")||"";
  const selectFilters=(quartiere=q,categoria=category)=>setSearchParams({...quartiere?{q:quartiere}:{},...categoria?{categoria}:{}});
  const [search, setSearch] = useState("");

  useEffect(() => {
    let active=true;const refresh=()=>axios.get(`${API}/merchants`, { params: q ? { quartiere: q } : {} }).then(r=>{if(active)setMerchants(r.data)}).catch(()=>{});refresh();const timer=setInterval(refresh,30000);return()=>{active=false;clearInterval(timer)};
  }, [q]);



  const filtered=merchants.filter(m=>(!category||(m.directory_category||m.category)===category)&&(!search||(m.business_name+m.category+m.description).toLowerCase().includes(search.toLowerCase())));

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-5 sm:py-7">
      <div className="border-b border-[#E5DFD3] pb-3">
        <div className="font-mono-accent text-[10px] uppercase tracking-widest text-[#D9531E]">Aziende · Tutti i quartieri di Bari</div>
        <h1 className="font-serif-display text-4xl sm:text-5xl font-black text-[#0B2545] tracking-tight mt-1">Aziende di Bari, per quartiere</h1>
        <p className="font-body text-slate-600 mt-2 max-w-2xl">Le vetrine sono pubblicate dalla redazione.</p>
      </div>

      <div className="flex flex-wrap gap-2 my-3">
        <button
          onClick={() => selectFilters("",category)}
          data-testid="quartiere-btn-all"
          className={`flex-1 basis-36 p-3 rounded-md font-serif-display font-bold text-sm border transition-all ${!q ? "bg-[#0B2545] text-white border-[#0B2545]" : "bg-white text-[#0B2545] border-[#E5DFD3]"}`}
        >Tutti</button>
        {meta.neighborhoods.map((n) => (
          <button
            key={n}
            onClick={() => selectFilters(n,category)}
            data-testid={`quartiere-btn-${n.replace(/[\s/]+/g, "-").toLowerCase()}`}
            className={`flex-1 basis-36 p-3 rounded-md font-serif-display font-bold text-sm border transition-all text-left ${q === n ? "text-white border-transparent" : "bg-white border-[#E5DFD3] hover:border-[#0B2545]"}`}
            style={q === n ? { background: QUARTIERI_COLORS[n] } : { color: QUARTIERI_COLORS[n] }}
          >{n}</button>
        ))}
      </div>

      <div className="flex flex-wrap gap-3 mb-3" aria-label="Categorie aziende">{["","Ristoranti",...new Set(merchants.filter(m=>!m.directory_category).map(m=>m.category))].map(c=><button key={c} onClick={()=>selectFilters(q,c)} className={`px-4 py-2 rounded border font-ui text-sm ${category===c?"bg-[#0B2545] text-white":"bg-white text-[#0B2545] border-[#E5DFD3]"}`}>{c||"Tutte le aziende"}</button>)}</div>
      <input
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        placeholder="Cerca esercente, categoria..."
        data-testid="merchant-search-input"
        className="w-full max-w-md mb-3 px-4 py-2 rounded-md bg-white border border-[#E5DFD3] font-body text-sm focus:border-[#0B2545] outline-none"
      />

      {(!q || q === "Libertà") && (!category || category === "Radio e media") && (!search || "radio sound city network radio e media libertà".includes(search.toLowerCase())) && (
        <section aria-label="Radio Sound City Network, rione Libertà" className="mb-3 rounded-lg border border-[#E5DFD3] bg-white p-3 sm:p-4">
          <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center">
            <RadioSoundCityLogo className="radio-sound-city-image shrink-0" />
            <div className="flex-1 min-w-0">
              <p className="text-xs uppercase tracking-widest text-[#2563EB]">Radio e media · Libertà</p>
              <h2 className="font-serif-display font-bold text-xl text-[#0B2545] mt-1">Radio Sound City Network</h2>
              <p className="text-sm text-slate-700 mt-1">Alternative Station Since 1989. Radio digitale dedicata alla musica e agli artisti emergenti.</p>
              <a href="https://www.radiosoundcity.net/radio/" target="_blank" rel="noopener noreferrer" className="inline-block mt-3 rounded bg-[#0B2545] text-white px-4 py-2 text-sm">Ascolta la radio ↗</a>
            </div>
          </div>
        </section>
      )}
      <div className="adaptive-card-grid">
        {filtered.map((m) => (
          <div key={m.id} data-testid={`merchant-card-${m.id}`} className="p-6 bg-white border border-[#E5DFD3] rounded-md hover:shadow-md transition-shadow relative overflow-hidden">
            <div className="absolute top-0 right-0 w-1 h-full" style={{ background: QUARTIERI_COLORS[m.quartiere] || "#0B2545" }}></div>
            {m.cover_image&&<img src={m.cover_image} alt={`Foto di ${m.business_name}`} className="w-full h-44 object-cover rounded mb-4"/>}
            <div className="flex items-start justify-between gap-3">
              <div>
                <div className="font-mono-accent text-[10px] uppercase tracking-widest" style={{ color: QUARTIERI_COLORS[m.quartiere] }}>{m.category}</div>
                <h3 className="font-serif-display font-bold text-xl text-[#0B2545] mt-1">{m.business_name}</h3>
                <div className="text-xs font-body text-slate-600 mt-1 flex items-center gap-1"><MapPin className="w-3 h-3"/> {m.quartiere}{!m.is_demo && ` · ${m.address}`}</div>
              </div>
            </div>
            {m.owner_name&&<p className="mt-3 text-sm text-slate-600">Proprietario: {m.owner_name}</p>}{m.vat_number&&<p className="mt-1 text-sm text-slate-600">Partita IVA: {m.vat_number}</p>}{m.slogan&&<p className="mt-3 font-semibold text-[#D9531E]">{m.slogan}</p>}<p className="mt-3 font-body text-sm text-slate-700 leading-relaxed">{m.description}</p>
            {m.promotion && (
              <div className="mt-4 p-3 bg-[#F8F5EE] border-l-2 border-[#D9531E] rounded-r flex items-start gap-2">
                <Tag className="w-4 h-4 text-[#D9531E] mt-0.5 shrink-0" />
                <div>
                  {!m.is_demo && <div className="font-mono-accent text-[9px] uppercase tracking-widest text-[#D9531E]">Promo</div>}
                  <div className="font-body text-sm text-[#0B2545]">{m.promotion}</div>
                </div>
              </div>
            )}
            <Link to={`/aziende/${m.id}`} className="mt-4 block w-full text-center border border-[#0B2545] text-[#0B2545] font-ui text-sm py-2 rounded">Apri scheda azienda</Link>
          </div>
        ))}
      </div>
      {filtered.length === 0 && !((!q || q === "Libertà") && (!category || category === "Radio e media") && (!search || "radio sound city network radio e media libertà".includes(search.toLowerCase()))) && <p className="text-center text-slate-500 py-5 font-body">Nessun esercente trovato.</p>}
    </div>
  );
}
