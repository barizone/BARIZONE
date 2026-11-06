import React from 'react';
import {Link} from 'react-router-dom';
import {MapPin,ArrowUpRight} from 'lucide-react';
export function NewsCard({item}){return <Link to={'/notizie/'+item.id} data-testid={'news-card-'+item.id} className="group flex flex-col sm:flex-row items-start gap-3 sm:gap-5 bg-white border border-[#E5DFD3] rounded-lg p-4 sm:p-5 hover:border-[#D9531E] transition-colors">
 {item.cover_image?<figure className="w-full sm:w-56 lg:w-64 shrink-0"><img src={item.cover_image} alt={item.image_alt||item.title} loading="lazy" className="w-full aspect-[16/10] object-cover rounded-md"/><figcaption className="text-[10px] leading-snug text-slate-500 mt-1">{item.cover_caption||'Immagine della fonte'}</figcaption></figure>:<span className="hidden sm:flex items-center justify-center w-12 h-12 shrink-0 rounded-full bg-[#F8F5EE] text-[#D9531E]" aria-hidden="true"><MapPin size={24}/></span>}

 <div className="min-w-0 flex-1"><div className="flex flex-wrap gap-x-3 gap-y-1 text-xs font-semibold uppercase tracking-wide"><span className="text-[#D9531E]">{item.quartiere||'Tutta Bari'}</span><span className="text-slate-500">{item.category}</span></div>
 <h2 className="font-serif-display text-xl sm:text-2xl font-bold leading-tight mt-2 group-hover:text-[#D9531E]">{item.title}</h2>
 {item.subtitle&&<p className="text-sm leading-relaxed text-slate-700 mt-2">{item.subtitle}</p>}
 <p className="text-xs text-slate-500 mt-3">{item.published_at?new Date(item.published_at).toLocaleDateString('it-IT'):'Data fonte non indicata'} · {item.author||'Redazione Barizone'}{item.video_url?' · Servizio video nella fonte':''}</p>
 </div><ArrowUpRight className="shrink-0 text-[#D9531E]" size={18}/>
 </Link>}
