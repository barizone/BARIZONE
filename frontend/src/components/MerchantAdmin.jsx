import React, { useEffect, useState } from "react";
import axios from "axios";
import {neighborhoods} from "@/pages/NeighborhoodNews";
import { API } from "@/context/AuthContext";

const blank = {
  entity_type: "Commerciante", maps_url: "", photos: [], business_name: "", owner_name: "", email: "", phone: "", category: "",
  quartiere: "", address: "", description: "", promotion: "", plan: "Gratuito",
  cover_image: "", legal_name: "", vat_number: "", tax_code: "", legal_address: "", postal_code: "", city: "Bari", province: "BA", pec: "", slogan: "", verified: false, is_demo: false, contract_active: false, notifications_enabled: false,
};

export default function MerchantAdmin() {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState(blank);
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [uploading,setUploading]=useState(false);
  const [notice,setNotice]=useState('');
  const preparePhoto = async file => {
    if (!['image/jpeg','image/png','image/webp'].includes(file.type)) throw new Error('Seleziona una foto JPG, PNG o WebP.');
    if (file.size > 20 * 1024 * 1024) throw new Error('Foto troppo grande: massimo 20 MB prima dell’adattamento.');
    if (file.size <= 1048576) return file;
    const bitmap = await createImageBitmap(file);
    try {
      const canvas = document.createElement('canvas');
      const ratio = Math.min(1, 1600 / Math.max(bitmap.width, bitmap.height));
      canvas.width = Math.max(1, Math.round(bitmap.width * ratio));
      canvas.height = Math.max(1, Math.round(bitmap.height * ratio));
      const ctx = canvas.getContext('2d');
      ctx.fillStyle = '#ffffff'; ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
      for (const quality of [0.9, 0.75, 0.6, 0.4]) {
        const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', quality));
        if (blob && blob.size <= 1048576) return new File([blob], 'foto-azienda.jpg', {type:'image/jpeg'});
      }
      throw new Error('Impossibile adattare la foto: scegli un’immagine più piccola.');
    } finally { bitmap.close(); }
  };
  const upload = async (e, cover = false) => {
    const input = e.currentTarget, files = [...input.files];
    if (!files.length) return;
    if (!cover && files.length + form.photos.length > 6) { setError('Massimo 6 foto in galleria. Puoi comunque cambiare la copertina.'); input.value=''; return; }
    setUploading(true); setError(''); setNotice('');
    try {
      for (const file of files) {
        const body = new FormData(); body.append('photo', await preparePhoto(file));
        const r = await fetch('/api/admin/media', {method:'POST',body});
        const d = await r.json();
        if (!r.ok) throw new Error(d.detail || 'Caricamento non riuscito.');
        setForm(f => cover ? {...f,cover_image:d.url} : {...f,cover_image:f.cover_image||d.url,photos:[...f.photos,d.url]});
      }
      setNotice('Foto caricata. Premi “Salva e pubblica” per aggiornare la vetrina.');
    } catch (err) { setError(err.message); }
    finally { setUploading(false); input.value=''; }
  };
  const reload = () => axios.get(`${API}/admin/merchants`).then(r => setItems(r.data)).catch(e => setError(e.response?.data?.detail || e.message));
  useEffect(() => { reload(); }, []);
  const change = (key, value) => setForm(current => ({ ...current, [key]: value }));
  const save = async e => {
    e.preventDefault();const publish=e.nativeEvent.submitter?.value==='publish';setBusy(true);setError('');setNotice('');
    try {
      const payload={...form,notifications_enabled:!!(form.notifications_enabled&&form.contract_active&&!form.is_demo)};
      const response=editing?await axios.put(`${API}/admin/merchants/${editing}`,payload):await axios.post(`${API}/admin/merchants`,payload);
      setEditing(response.data.id);
      if(publish)await axios.post(`${API}/admin/merchants/${response.data.id}/status`,null,{params:{status:'published'}});
      setEditing(null);setForm(blank);setNotice(publish||response.data.status==='published'?'Vetrina pubblicata: visibile subito ricaricando la pagina, oppure entro 30 secondi senza ricaricare.':'Salvata in bozza: non è ancora visibile sul sito. Premi “Pubblica in galleria” per renderla pubblica.');await reload();
    } catch(e){setError(e.response?.data?.detail||e.message);await reload()}
    finally{setBusy(false)}
  };
  const status = async (item, next) => {
    setError("");setNotice("");
    try { await axios.post(`${API}/admin/merchants/${item.id}/status`, null, { params: { status: next } }); setNotice(next==='published'?'Vetrina pubblicata: visibile subito ricaricando, oppure entro 30 secondi.':'Stato aggiornato.');await reload(); }
    catch (e) { setError(e.response?.data?.detail || e.message); }
  };
  const edit = item => {
    setEditing(item.id);
    setForm(Object.fromEntries(Object.keys(blank).map(k => [k, item[k] ?? blank[k]])));
    window.scrollTo({ top: 0, behavior: "smooth" });
  };
  const field = (key, label, type = "text", required = key === "business_name") => (
    <label key={key} className="font-body text-sm text-[#0B2545]">{label}
      <input className="block w-full mt-1 p-2 border border-[#E5DFD3] rounded" type={type} value={form[key]} required={required} onChange={e => change(key, e.target.value)} />
    </label>
  );
  return <div className="mt-8">
    <h2 className="font-serif-display text-2xl font-bold text-[#0B2545]">{editing ? "Modifica scheda" : "Nuova vetrina"}</h2>
    <p className="font-body text-sm text-slate-600 mt-2">Solo l’amministratore compila le schede. È obbligatorio soltanto il nome dell’attività; tutti gli altri dati sono facoltativi e possono essere aggiunti in seguito. Il salvataggio crea una bozza; la pubblicazione è un’azione separata. Dopo la pubblicazione, “Vedi nel rione” apre il punto del sito in cui compare la scheda.</p>
    <form onSubmit={save} className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-white border border-[#E5DFD3] p-5 rounded-md mt-4">
      <label className="md:col-span-2 flex gap-2 text-sm"><input type="checkbox" checked={form.is_demo} onChange={e=>change('is_demo',e.target.checked)}/> Azienda fittizia per verificare le procedure (sarà riconoscibile nella vetrina pubblica)</label><label className="font-body text-sm">Tipo di vetrina<select className="block border p-2 rounded w-full mt-1" value={form.entity_type} onChange={e=>change('entity_type',e.target.value)}>{['Commerciante','Azienda','Professionista'].map(v=><option key={v}>{v}</option>)}</select></label>
      {field("business_name", "Nome attività (obbligatorio)")}
      {field("owner_name", "Titolare")}
      {field("email", "Email", "email")}
      {field("phone", "Telefono")}
      {field("category", "Categoria")}
      <label className="text-sm">Rione<select className="block w-full mt-1 p-2 border rounded" value={form.quartiere} onChange={e=>change('quartiere',e.target.value)}><option value="">Seleziona il rione</option>{form.quartiere&&!neighborhoods.includes(form.quartiere)&&<option>{form.quartiere}</option>}{neighborhoods.map(q=><option key={q}>{q}</option>)}</select></label>
      {field("address", "Indirizzo")}
      {field("cover_image", "Foto di copertina: URL HTTPS o percorso caricato", "text", false)}
      <div className="md:col-span-2">
        <label className="block text-sm font-medium">Cambia copertina dal computer o telefono
          <input type="file" accept="image/jpeg,image/png,image/webp" disabled={uploading||busy} onChange={e=>upload(e,true)} className="block w-full mt-2 border rounded p-3"/>
        </label>
        <p className="text-xs text-slate-600 mt-1">Formato consigliato: 1600 × 1200 pixel (4:3). JPG, PNG o WebP fino a 20 MB, adattati automaticamente senza tagliare l’immagine. La copertina può essere sostituita anche con la galleria piena.</p>
        {uploading&&<p role="status">Caricamento foto…</p>}
        {form.cover_image&&<img src={form.cover_image} alt="Anteprima copertina aziendale" className="mt-3 aspect-[4/3] w-full object-contain rounded bg-[#0B2545]"/>}
      </div>
      {field("legal_name", "Ragione sociale", "text", false)}
      {field("vat_number", "Partita IVA (11 cifre)", "text", false)}
      {field("tax_code", "Codice fiscale aziendale", "text", false)}
      {field("legal_address", "Indirizzo della sede legale", "text", false)}
      {field("postal_code", "CAP della sede legale", "text", false)}
      {field("city", "Comune della sede legale", "text", false)}
      {field("province", "Provincia della sede legale", "text", false)}
      {field("pec", "PEC", "email", false)}
      {field("maps_url", "Google Maps (facoltativo: altrimenti uso l’indirizzo)", "url", false)}
      <label className="md:col-span-2 text-sm">Carica foto dal computer o telefono (consigliate 1600 × 1200 pixel, orizzontali 4:3; massimo 6, JPG/PNG/WebP fino a 20 MB; adattamento automatico)<input type="file" accept="image/jpeg,image/png,image/webp" multiple disabled={uploading} onChange={e=>upload(e)} className="block mt-2"/></label>
      {form.photos.length>0&&<div className="md:col-span-2 flex flex-wrap gap-3">{form.photos.map(src=><div key={src}><img src={src} alt="Foto caricata" className="w-28 aspect-[4/3] object-contain bg-[#0B2545] rounded"/><button type="button" onClick={()=>change('cover_image',src)} className="text-xs underline">Usa in copertina</button><button type="button" onClick={()=>setForm(f=>({...f,photos:f.photos.filter(p=>p!==src),cover_image:f.cover_image===src?'':f.cover_image}))} className="block text-xs underline">Rimuovi dalla vetrina</button></div>)}</div>}
      {field("slogan", "Slogan o commento dell’azienda", "text", false)}
      <label className="md:col-span-2 font-body text-sm">Descrizione<textarea className="block w-full mt-1 p-2 border border-[#E5DFD3] rounded" rows={3} value={form.description} onChange={e => change("description", e.target.value)} /></label>

      {field("promotion", "Promozione (facoltativa)", "text", false)}
      <label className="font-body text-sm flex items-center gap-2"><input type="checkbox" checked={form.verified} onChange={e => change("verified", e.target.checked)} /> Dati aziendali verificati dalla redazione</label>
      <label className="font-body text-sm flex items-center gap-2"><input type="checkbox" checked={form.contract_active} onChange={e => change("contract_active", e.target.checked)} /> Accordo attivo registrato</label>
      <p className="md:col-span-2 font-body text-xs text-slate-600">Dopo la pubblicazione, apri “QR e clienti” per il codice personale dell’attività, le adesioni e le comunicazioni al suo gruppo.</p>
      <div className="md:col-span-2 flex gap-3">
        <button value="publish" disabled={busy||uploading} className="bg-[#0B2545] text-white px-5 py-2 rounded">{busy?"Salvataggio…":"Salva e pubblica"}</button><button value="draft" disabled={busy||uploading} className="border px-5 py-2 rounded">Salva bozza</button>
        {editing && <button type="button" onClick={() => { setEditing(null); setForm(blank); }} className="border px-5 py-2 rounded">Annulla</button>}
      </div>
    </form>
    {notice&&<p role="status" className="text-green-800 mt-3">{notice}</p>}
    {error && <p role="alert" className="text-red-700 mt-3">{String(error)}</p>}
    <h2 className="font-serif-display text-2xl font-bold text-[#0B2545] mt-10">Schede gestite ({items.length})</h2>
    <div className="space-y-3 mt-4">{items.map(item => <article key={item.id} className="bg-white border border-[#E5DFD3] p-4 rounded">
      <h3 className="font-serif-display text-lg font-bold">{item.business_name}</h3>
      <p className="font-body text-sm">{item.quartiere} · {item.category} · {({draft:'BOZZA · non visibile',published:'PUBBLICATA',suspended:'SOSPESA',archived:'ARCHIVIATA'})[item.status]||item.status} {item.is_demo ? "· ESEMPIO" : ""}</p>
      {item.status==='draft'&&item.publication_issues?.length>0&&<p className="mt-2 text-sm text-amber-900">Per pubblicare completa: {item.publication_issues.join(', ')}.</p>}
      <div className="flex flex-wrap gap-2 mt-3">{item.status==='published'&&<a href={'/quartieri?q='+encodeURIComponent(item.quartiere)+'#attivita-del-rione'} className="border px-3 py-1 rounded">Vedi nel rione</a>}{item.status==='published'&&<a href={'/gestione-clienti?azienda='+item.id} className="border px-3 py-1 rounded">QR e clienti</a>}
        <button onClick={() => edit(item)} className="border px-3 py-1 rounded">Modifica</button>
        {item.status !== "published" && <button onClick={() => status(item, "published")} className="bg-[#15803D] text-white px-3 py-1 rounded">Pubblica in galleria</button>}
        {item.status === "published" && <button onClick={() => status(item, "suspended")} className="border px-3 py-1 rounded">Sospendi</button>}
        {item.status !== "archived" && <button onClick={() => status(item, "archived")} className="border px-3 py-1 rounded">Archivia</button>}
      </div>
    </article>)}</div>
  </div>;
}


