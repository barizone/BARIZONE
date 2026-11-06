import React, { useEffect, useState } from "react";
import axios from "axios";
import { API } from "@/context/AuthContext";
import { Calendar, MapPin, Ticket } from "lucide-react";
import DemoNotice from "@/components/DemoNotice";

const CATS = ["Tutti", "Concerti", "Mostre", "Sagre", "Teatro", "Sport"];

export default function EventsPage() {
  const [events, setEvents] = useState([]);
  const [meta, setMeta] = useState({ neighborhoods: [] });
  const [cat, setCat] = useState("Tutti");
  const [q, setQ] = useState("");

  useEffect(() => {
    const params = {};
    if (cat !== "Tutti") params.category = cat;
    if (q) params.quartiere = q;
    axios.get(`${API}/events`, { params }).then(r => setEvents(r.data));
  }, [cat, q]);

  useEffect(() => { axios.get(`${API}/meta`).then(r => setMeta(r.data)); }, []);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div className="border-b border-[#E5DFD3] pb-6">
        <div className="font-mono-accent text-[10px] uppercase tracking-widest text-[#D9531E] flex items-center gap-2"><Calendar className="w-3 h-3"/> Calendario</div>
        <h1 className="font-serif-display text-4xl sm:text-5xl font-black text-[#0B2545] tracking-tight mt-1">Eventi in città</h1>
      </div>
      <DemoNotice>Gli eventi di esempio non sono confermati: date, prezzi e luoghi non devono essere usati per prenotare.</DemoNotice>

      <div className="flex flex-wrap gap-3 my-6">
        <div className="flex gap-2 overflow-x-auto no-scrollbar">
          {CATS.map(c => (
            <button key={c} onClick={() => setCat(c)} data-testid={`event-cat-${c.toLowerCase()}`} className={`font-mono-accent text-[10px] uppercase tracking-widest px-3 py-1.5 rounded-sm ${cat === c ? "bg-[#D9531E] text-white" : "bg-white text-[#0B2545] border border-[#E5DFD3]"}`}>{c}</button>
          ))}
        </div>
        <select value={q} onChange={(e) => setQ(e.target.value)} data-testid="event-quartiere-select" className="ml-auto font-ui text-sm px-3 py-1.5 rounded-md bg-white border border-[#E5DFD3] text-[#0B2545]">
          <option value="">Tutti i quartieri</option>
          {meta.neighborhoods.map(n => <option key={n} value={n}>{n}</option>)}
        </select>
      </div>

      <div className="adaptive-card-grid">
        {events.map((e) => (
          <div key={e.id} data-testid={`event-card-${e.id}`} className="bg-white rounded-md border border-[#E5DFD3] overflow-hidden">
            {e.cover_image && <div className="aspect-[16/9] relative"><img src={e.cover_image} alt="" className="w-full h-full object-cover" />{e.is_demo && <span className="absolute top-3 left-3 bg-amber-100 text-amber-950 text-[10px] font-semibold uppercase tracking-wide px-2 py-1 rounded-sm shadow">Foto illustrativa · esempio</span>}</div>}
            <div className="p-5">
              <div className="flex items-center justify-between">
                <div className="font-mono-accent text-[10px] uppercase tracking-widest text-[#D9531E]">{e.category}</div>
                <div className="font-serif-display text-2xl font-black text-[#0B2545]">
                  {new Date(e.date).toLocaleDateString("it-IT", { day: "2-digit" })}
                  <span className="text-xs uppercase font-mono-accent tracking-widest ml-1 text-[#0B2545]/60">{new Date(e.date).toLocaleDateString("it-IT", { month: "short" })}</span>
                </div>
              </div>
              <h3 className="font-serif-display text-xl font-bold text-[#0B2545] mt-2 leading-tight">{e.title}</h3>
              {e.is_demo && <p className="mt-2 text-xs font-semibold text-amber-800">EVENTO DI ESEMPIO · NON CONFERMATO</p>}
              <p className="mt-2 font-body text-sm text-slate-600 line-clamp-2">{e.description}</p>
              <div className="mt-4 flex items-center justify-between text-xs font-body text-slate-600">
                <span className="flex items-center gap-1"><MapPin className="w-3 h-3"/> {e.venue}</span>
                <span className="flex items-center gap-1 font-semibold text-[#0B2545]"><Ticket className="w-3 h-3"/> {e.price}</span>
              </div>
              {!e.is_demo && e.source_url && <a href={e.source_url} target="_blank" rel="noopener noreferrer" className="text-xs text-[#D9531E] underline mt-3 inline-block">Fonte dell’evento ↗</a>}
            </div>
          </div>
        ))}
      </div>
      {events.length === 0 && <p className="text-center text-slate-500 py-5 font-body">Nessun evento trovato.</p>}
    </div>
  );
}
