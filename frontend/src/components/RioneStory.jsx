import React from 'react';
import stories from '@/data/rioneStories.json';

function Photo({photo,context=false,primary=false}) {
 return <figure className="min-w-0 bg-[#F8F6F1]">
  <img src={photo.src} alt={photo.caption} loading={primary?'eager':'lazy'} decoding="async" width="1200" height="800" className="w-full aspect-[3/2] object-cover" />
  <figcaption className="px-4 py-3 text-xs leading-relaxed text-[#35465A]">
   {context&&<strong className="block text-[#A43F14] mb-1">Foto di contesto · non è una veduta del rione</strong>}
   {photo.caption}{' '}<a href={photo.photopage} target="_blank" rel="noreferrer" className="underline">{photo.author}</a>{' · '}<a href={photo.licenseurl} target="_blank" rel="noreferrer" className="underline">{photo.license}</a>. Immagine ridimensionata; anteprima ritagliata.
  </figcaption>
 </figure>;
}

export default function RioneStory({neighborhood}) {
 const story=stories.find(s=>s.name===neighborhood);
 if(!story)return null;
 return <article aria-label={'Storia di '+story.name} className="my-6 overflow-hidden rounded-xl border border-[#E5DFD3] bg-white">
  <div className="grid md:grid-cols-2 items-center">
   <Photo photo={story.photo} context={story.context} primary />
   <header className="p-5 sm:p-8 min-w-0">
    <p className="text-xs uppercase font-semibold tracking-widest text-[#D9531E]">{story.name} · storia del rione</p>
    <h2 className="font-serif-display text-3xl sm:text-4xl font-bold mt-3 leading-tight">{story.title}</h2>
    <p className="mt-4 leading-relaxed">{story.intro}</p>
   </header>
  </div>
  <div className="grid md:grid-cols-2 gap-6 p-5 sm:p-8">
   <section><h3 className="font-serif-display text-2xl font-bold">Perché si chiama così?</h3><p className="mt-3 leading-relaxed">{story.origin}</p></section>
   <section><h3 className="font-serif-display text-2xl font-bold">Un po’ di storia</h3><p className="mt-3 leading-relaxed">{story.history}</p></section>
  </div>
  {story.note&&<p className="mx-5 sm:mx-8 mb-5 border-l-4 border-[#D9531E] bg-[#F8F3EA] p-4 text-sm leading-relaxed">{story.note}</p>}
  {!!story.gallery.length&&<details className="mx-5 sm:mx-8 mb-5"><summary className="cursor-pointer font-semibold underline">Altre fotografie dall’archivio</summary><div className="grid sm:grid-cols-2 gap-4 mt-4">{story.gallery.map(photo=><Photo key={photo.id} photo={photo}/>)}</div></details>}
  <details className="border-t border-[#E5DFD3] p-5 sm:px-8"><summary className="cursor-pointer text-sm font-semibold underline">Fonti della storia e diritti fotografici</summary>
   <ul className="mt-3 list-disc pl-5 text-sm space-y-2">{story.sources.map(([title,url])=><li key={url}><a href={url} target="_blank" rel="noreferrer" className="underline break-words">{title}</a></li>)}</ul>
   <p className="mt-3 text-xs leading-relaxed">Autore, fonte e licenza sono riportati sotto ogni fotografia. Gli adattamenti delle fotografie CC BY-SA sono disponibili con la medesima licenza indicata. Questa scheda storica resta consultabile anche in assenza di nuove notizie.</p>
   {[story.photo,...story.gallery].filter(p=>p.rightsnote).map(p=><p key={p.id} className="text-xs mt-2">{p.rightsnote}</p>)}
  </details>
 </article>;
}
