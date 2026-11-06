import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import axios from "axios";
import { API } from "@/context/AuthContext";
import { Calendar, MapPin } from "lucide-react";
import { neighborhoods } from "@/pages/NeighborhoodNews";

/**
 * Eventi: notizie reali della categoria "Eventi", ciascuna con data e fonte.
 * Non esistono eventi inseriti a mano o di esempio: se non ce ne sono, la pagina lo dice.
 */
export default function EventsPage() {
  const [events, setEvents] = useState([]);
  const [q, setQ] = useState("");
  const [state, setState] = useState("loading");

  useEffect(() => {
    let active = true;
    setState("loading");
    axios.get(`${API}/news`, { params: { scope: "current", category: "Eventi", neighborhood: q || undefined, limit: 100 } })
      .then(r => { if (active) { setEvents(r.data.filter(n => n.source_url)); setState("ready"); } })
      .catch(() => { if (active) setState("error"); });
    return () => { active = false; };
  }, [q]);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div className="border-b border-[#E5DFD3] pb-6">
        <div className="font-mono-accent text-[10px] uppercase tracking-widest text-[#D9531E] flex items-center gap-2"><Calendar className="w-3 h-3" /> Calendario</div>
        <h1 className="font-serif-display text-4xl sm:text-5xl font-black text-[#0B2545] tracking-tight mt-1">Eventi in città</h1>
      </div>

      <div className="my-6">
        <label className="block font-ui text-sm text-[#0B2545]">Quartiere
          <select value={q} onChange={e => setQ(e.target.value)} className="block mt-1 min-h-11 font-ui text-base px-3 rounded-md bg-white border border-[#E5DFD3] text-[#0B2545]">
            <option value="">Tutta Bari</option>
            {neighborhoods.filter(n => n !== "Tutta Bari").map(n => <option key={n} value={n}>{n}</option>)}
          </select>
        </label>
      </div>

      {state === "loading" && <p role="status" className="text-slate-600 py-5">Caricamento…</p>}
      {state === "error" && <p role="alert" className="text-red-700 py-5">Eventi temporaneamente non disponibili. Riprova tra poco.</p>}

      <div className="adaptive-card-grid">
        {events.map(e => (
          <article key={e.id} className="bg-white rounded-md border border-[#E5DFD3] overflow-hidden">
            {e.cover_image && <img src={e.cover_image} alt="" loading="lazy" className="w-full aspect-[16/9] object-cover" />}
            <div className="p-5">
              <div className="flex items-center justify-between">
                <div className="font-mono-accent text-[10px] uppercase tracking-widest text-[#D9531E]">{e.category}</div>
                <time dateTime={e.published_at} className="font-body text-xs text-slate-600">{new Date(e.published_at).toLocaleDateString("it-IT", { day: "2-digit", month: "short", year: "numeric" })}</time>
              </div>
              <h2 className="font-serif-display text-xl font-bold text-[#0B2545] mt-2 leading-tight"><Link to={`/notizie/${e.id}`}>{e.title}</Link></h2>
              {e.subtitle && <p className="mt-2 font-body text-sm text-slate-600 line-clamp-3">{e.subtitle}</p>}
              <div className="mt-4 flex items-center justify-between text-xs font-body text-slate-600">
                <span className="flex items-center gap-1"><MapPin className="w-3 h-3" aria-hidden="true" /> {e.quartiere || "Bari"}</span>
                <a href={e.source_url} target="_blank" rel="noopener noreferrer" className="text-[#D9531E] underline inline-flex items-center min-h-11">Fonte ↗</a>
              </div>
            </div>
          </article>
        ))}
      </div>
      {state === "ready" && events.length === 0 && <p className="text-center text-slate-600 py-8 font-body">Nessun evento pubblicato al momento{q ? ` per ${q}` : ""}.</p>}
    </div>
  );
}
