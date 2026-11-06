import React, {useEffect, useState} from 'react';
import {NewsCard} from './NewsCard';

export default function NewsCarousel({items}) {
 const [index,setIndex]=useState(0),[paused,setPaused]=useState(false),[hovered,setHovered]=useState(false),[focused,setFocused]=useState(false);
 const [reducedMotion,setReducedMotion]=useState(()=>window.matchMedia('(prefers-reduced-motion: reduce)').matches);
 useEffect(()=>{const media=window.matchMedia('(prefers-reduced-motion: reduce)');const change=()=>setReducedMotion(media.matches);media.addEventListener('change',change);return()=>media.removeEventListener('change',change)},[]);
 const stopped=paused||hovered||focused||reducedMotion;
 const current=items.length?index%items.length:0;
 useEffect(()=>{if(stopped||items.length<2)return;const timer=setInterval(()=>{if(!document.hidden)setIndex(i=>(i+1)%items.length)},6000);return()=>clearInterval(timer)},[stopped,items.length]);
 if(!items.length)return null;
 const move=delta=>{setPaused(true);setIndex((current+delta+items.length)%items.length)};
 return <div className="news-carousel" role="region" aria-roledescription="carosello" aria-label="Barizone News" onMouseEnter={()=>setHovered(true)} onMouseLeave={()=>setHovered(false)} onFocusCapture={e=>setFocused(Boolean(e.target.closest('a')))} onBlurCapture={e=>{if(!e.currentTarget.contains(e.relatedTarget))setFocused(false)}}>
  <div className="flex flex-wrap items-center justify-between gap-3 mb-3">
   <p className="text-sm text-slate-600">{items.length>1?`Notizia ${current+1} di ${items.length}`:'La notizia del momento'}</p>
   {items.length>1&&<div className="flex gap-2" aria-label="Comandi notizie">
    <button type="button" className="news-carousel-control" aria-label="Notizia precedente" onClick={()=>move(-1)}>←</button>
    <button type="button" className="news-carousel-control" aria-pressed={paused} onClick={()=>setPaused(p=>!p)}>{paused?'Riprendi':'Pausa'}</button>
    <button type="button" className="news-carousel-control" aria-label="Notizia successiva" onClick={()=>move(1)}>→</button>
   </div>}
  </div>
  <div aria-live="off"><NewsCard item={items[current]}/></div>
  <p className="text-xs text-slate-600 mt-3">Tocca la notizia per leggerla.{items.length>1&&!reducedMotion?' Le notizie cambiano ogni 6 secondi; puoi metterle in pausa.':''}</p>
 </div>;
}

