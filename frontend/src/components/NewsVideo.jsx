import React, { useState } from "react";
export function videoSource(value) {
  try {
    const url = new URL(value);
    if (!['http:', 'https:'].includes(url.protocol)) return null;
    let id;
    if (url.hostname === 'youtu.be') id = url.pathname.split('/')[1];
    if (['youtube.com','www.youtube.com','m.youtube.com'].includes(url.hostname)) id = url.searchParams.get('v') || url.pathname.match(/^\/(?:shorts|embed)\/([^/]+)/)?.[1];
    if (id && /^[a-zA-Z0-9_-]{11}$/.test(id)) return { kind: 'embed', src: `https://www.youtube-nocookie.com/embed/${id}` };
    if (['vimeo.com','www.vimeo.com','player.vimeo.com'].includes(url.hostname)) { const match=url.pathname.match(/(?:video\/)?(\d+)\/?$/); if(match) return {kind:'embed',src:`https://player.vimeo.com/video/${match[1]}?dnt=1`}; }
    if (/\.(mp4|webm)$/i.test(url.pathname)) return {kind:'file',src:url.href};
    return {kind:'link',src:url.href};
  } catch { return null; }
}
export default function NewsVideo({url,title,poster}) {
  const [playing,setPlaying]=useState(false);
  const source=videoSource(url);
  if(!source)return null;
  if(source.kind==='link')return <a href={source.src} target="_blank" rel="noopener noreferrer" className="block bg-[#0B2545] text-white rounded-md px-5 py-4">▶ Guarda il video della notizia ↗</a>;
  return <div className="aspect-video bg-[#0B2545] rounded-md overflow-hidden relative">
    {!playing ? <button type="button" onClick={()=>setPlaying(true)} aria-label={`Riproduci video: ${title}`} className="absolute inset-0 w-full h-full flex flex-col items-center justify-center text-white gap-3">
      {poster&&<img src={poster} alt="" className="absolute inset-0 w-full h-full object-cover opacity-50"/>}
      <span className="relative text-3xl bg-[#D9531E] rounded-full w-16 h-16 flex items-center justify-center">▶</span><span className="relative font-ui text-sm font-semibold">Guarda il video</span>
    </button> : source.kind==='file' ? <video controls playsInline autoPlay poster={poster||undefined} className="w-full h-full" src={source.src}><a href={source.src}>Apri video</a></video> : <iframe src={source.src} title={title} className="w-full h-full" allow="fullscreen; picture-in-picture" referrerPolicy="strict-origin-when-cross-origin" allowFullScreen/>}
  </div>;
}
