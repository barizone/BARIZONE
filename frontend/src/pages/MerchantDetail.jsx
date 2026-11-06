import React, { useEffect, useState } from "react";
import axios from "axios";
import { Link, useParams } from "react-router-dom";
import { API } from "@/context/AuthContext";

export default function MerchantDetail() {
  const { id } = useParams();
  const [merchant, setMerchant] = useState(null);
  const [error, setError] = useState("");
  useEffect(() => {
    axios.get(`${API}/merchants/${id}`).then(r => setMerchant(r.data)).catch(() => setError("Attività non disponibile."));
  }, [id]);
  if (error) return <div className="max-w-4xl mx-auto p-8">{error}</div>;
  if (!merchant) return <div className="max-w-4xl mx-auto p-8">Caricamento scheda...</div>;
  return <div className="max-w-4xl mx-auto px-4 sm:px-6 py-10">
    <Link to="/aziende" className="text-sm text-[#D9531E]">Torna alla galleria</Link>
    <article className="bg-white border border-[#E5DFD3] p-6 sm:p-9 mt-5 rounded-md">
      {merchant.cover_image&&<img src={merchant.cover_image} alt={`Foto di ${merchant.business_name}`} className="w-full aspect-[4/3] object-contain bg-[#0B2545] rounded mb-6"/>}
      <div className="font-mono-accent text-xs uppercase text-[#D9531E]">{merchant.category} · {merchant.quartiere}</div>
      <h1 className="font-serif-display text-4xl font-black text-[#0B2545] mt-2">{merchant.business_name}</h1>
      {merchant.slogan&&<p className="font-serif-display text-2xl text-[#D9531E] mt-5">{merchant.slogan}</p>}
      {(merchant.maps_url||merchant.address)&&<a href={merchant.maps_url||'https://www.google.com/maps/search/?api=1&query='+encodeURIComponent([merchant.business_name,merchant.address,merchant.city||'Bari'].filter(Boolean).join(', '))} target="_blank" rel="noopener noreferrer" className="inline-block bg-[#0B2545] text-white px-5 py-3 rounded mt-5">Dove trovarmi · Google Maps ↗</a>}
      {merchant.photos?.length>1&&<div className="grid grid-cols-2 sm:grid-cols-3 gap-3 mt-5">{merchant.photos.filter(src=>src!==merchant.cover_image).map(src=><img key={src} src={src} alt={'Foto di '+merchant.business_name} loading="lazy" className="w-full aspect-[4/3] object-contain bg-[#0B2545] rounded"/>)}</div>}
      <Link to={'/adesioni?azienda='+merchant.id} className="inline-block bg-[#D9531E] text-white px-5 py-3 rounded mt-5 ml-2">Iscriviti alle comunicazioni di questa attività</Link>
      <h2 className="font-serif-display text-xl font-bold mt-7">Presentazione dell’azienda</h2>
      <p className="font-body text-slate-700 mt-3 whitespace-pre-line">{merchant.description}</p>
      {['owner_name','legal_name','vat_number','address','email','phone'].some(k=>merchant[k])&&<h2 className="font-serif-display text-xl font-bold mt-7">Dati aziendali</h2>}
      <dl className="grid sm:grid-cols-2 gap-4 mt-4">{[['owner_name','Nome e cognome del proprietario'],['legal_name','Ragione sociale'],['vat_number','Partita IVA'],['tax_code','Codice fiscale aziendale'],['legal_address','Sede legale'],['postal_code','CAP'],['city','Comune'],['province','Provincia'],['address','Indirizzo dell’attività'],['phone','Telefono'],['email','Email'],['pec','PEC']].filter(([key])=>merchant[key]).map(([key,label])=><div key={key}><dt className="text-sm text-slate-500">{label}</dt><dd className="mt-1 break-words">{merchant[key]}</dd></div>)}</dl>
      {merchant.promotion && <div className="mt-6 p-4 border-l-2 border-[#D9531E] bg-[#F8F5EE]">
        <strong>Promozione</strong>
        <p>{merchant.promotion}</p>
      </div>}
    </article>
  </div>;
}

