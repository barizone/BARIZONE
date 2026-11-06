import React, { useEffect, useState } from "react";
import axios from "axios";
import { API } from "@/context/AuthContext";
import { MapPin, Phone, Star } from "lucide-react";
import DemoNotice from "@/components/DemoNotice";

export default function RestaurantsPage() {
  const [items, setItems] = useState([]);
  const [meta, setMeta] = useState({ neighborhoods: [], restaurant_types: [] });
  const [type, setType] = useState("");
  const [q, setQ] = useState("");
  const [newOnly, setNewOnly] = useState(false);

  useEffect(() => {
    const params = {};
    if (type) params.type = type;
    if (q) params.quartiere = q;
    if (newOnly) params.new_only = true;
    axios.get(`${API}/restaurants`, { params }).then(r => setItems(r.data));
  }, [type, q, newOnly]);

  useEffect(() => { axios.get(`${API}/meta`).then(r => setMeta(r.data)); }, []);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div className="border-b border-[#E5DFD3] pb-6">
        <div className="font-mono-accent text-[10px] uppercase tracking-widest text-[#D9531E]">La guida</div>
        <h1 className="font-serif-display text-4xl sm:text-5xl font-black text-[#0B2545] tracking-tight mt-1">Ristoranti & Pizzerie</h1>
        <p className="font-body text-slate-600 mt-2 max-w-2xl">Dalle trattorie storiche alle nuove aperture — tutto il gusto di Bari, un morso alla volta.</p>
      </div>
      <DemoNotice>Le schede «Esempio» non confermano attività, indirizzi, recapiti, aperture o valutazioni.</DemoNotice>

      <div className="flex flex-wrap gap-3 my-6 items-center">
        <select value={type} onChange={(e) => setType(e.target.value)} data-testid="restaurant-type-select" className="font-ui text-sm px-3 py-1.5 rounded-md bg-white border border-[#E5DFD3] text-[#0B2545]">
          <option value="">Tutti i tipi</option>
          {meta.restaurant_types.map(t => <option key={t}>{t}</option>)}
        </select>
        <select value={q} onChange={(e) => setQ(e.target.value)} data-testid="restaurant-quartiere-select" className="font-ui text-sm px-3 py-1.5 rounded-md bg-white border border-[#E5DFD3] text-[#0B2545]">
          <option value="">Tutti i quartieri</option>
          {meta.neighborhoods.map(n => <option key={n}>{n}</option>)}
        </select>
        <label className="flex items-center gap-2 font-ui text-sm text-[#0B2545] cursor-pointer">
          <input type="checkbox" checked={newOnly} onChange={(e) => setNewOnly(e.target.checked)} data-testid="restaurant-new-only" className="accent-[#D9531E]" />
          Solo nuove aperture
        </label>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {items.map((r) => (
          <div key={r.id} data-testid={`restaurant-${r.id}`} className="bg-white rounded-md border border-[#E5DFD3] overflow-hidden hover:border-[#D9531E] transition-colors">
            {r.cover_image && (
              <div className="aspect-[4/3] overflow-hidden relative">
                <img src={r.cover_image} alt="" className="w-full h-full object-cover" />
                {r.is_demo && <span className="absolute top-3 left-3 bg-amber-100 text-amber-950 text-[10px] font-semibold uppercase tracking-wide px-2 py-1 rounded-sm shadow">Foto illustrativa · esempio</span>}
                {r.is_new_opening && !r.is_demo && <span className="absolute top-3 left-3 bg-[#D9531E] text-white font-mono-accent text-[9px] uppercase tracking-widest px-2 py-0.5 rounded-sm">Nuova</span>}
              </div>
            )}
            <div className="p-5">
              <div className="flex items-center justify-between">
                <div className="font-mono-accent text-[10px] uppercase tracking-widest text-[#0B2545]/60">{r.type} · {r.price_range}</div>
                {!r.is_demo && r.rating != null && <div className="flex items-center gap-1 text-xs text-[#D9A036]"><Star className="w-3 h-3 fill-current"/> {r.rating}</div>}
              </div>
              <h3 className="font-serif-display text-xl font-bold text-[#0B2545] mt-1">{r.name}</h3>
              {r.is_demo && <p className="text-xs font-semibold text-amber-800 mt-2">ATTIVITÀ DI ESEMPIO · NON AUTENTICATA</p>}
              <p className="mt-2 font-body text-sm text-slate-600 line-clamp-2">{r.description}</p>
              <div className="mt-3 space-y-1 text-xs font-body text-slate-600">
                <div className="flex items-center gap-1"><MapPin className="w-3 h-3"/> {r.is_demo ? r.quartiere : `${r.address} · ${r.quartiere}`}</div>
                {!r.is_demo && r.phone && <div className="flex items-center gap-1"><Phone className="w-3 h-3"/> {r.phone}</div>}
              </div>
            </div>
          </div>
        ))}
      </div>
      {items.length === 0 && <p className="text-center text-slate-500 py-16 font-body">Nessun ristorante trovato.</p>}
    </div>
  );
}
