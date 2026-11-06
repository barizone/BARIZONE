import React, { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import axios from "axios";
import { API } from "@/context/AuthContext";
import { ChevronLeft, Loader2 } from "lucide-react";
import NewsVideo from "@/components/NewsVideo";

export default function ArticleDetail() {
  const { id } = useParams();
  const [article, setArticle] = useState(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    let active=true;setArticle(null);setError(false);
    axios.get(`${API}/news/${id}`).then(response=>{if(active)setArticle(response.data)}).catch(()=>{if(active)setError(true)});
    return()=>{active=false};
  }, [id]);

  if(error)return <div role="alert" className="max-w-3xl mx-auto px-4 py-6">Notizia non disponibile. <Link to="/notizie" className="underline">Torna alle notizie</Link></div>;
  if (!article) return <div role="status" aria-label="Caricamento notizia" className="max-w-3xl mx-auto px-4 py-8 flex items-center justify-center"><Loader2 className="w-8 h-8 animate-spin text-[#0B2545]" /></div>;

  return (
    <article>
      {article.cover_image && (
        <div className="relative h-56 sm:h-80 w-full overflow-hidden bg-[#0B2545]">
          <img src={article.cover_image} alt="" className="w-full h-full object-cover opacity-90" />
          <div className="absolute inset-0 bg-gradient-to-t from-[#0B2545] via-transparent to-transparent"></div>
        </div>
      )}

      <div className={"max-w-3xl mx-auto px-4 sm:px-6 relative z-10 "+(article.cover_image?"-mt-12 sm:-mt-16":"pt-6")}>
        <div className="bg-[#FAF8F5] rounded-md p-6 sm:p-10 border border-[#E5DFD3]">
          <Link to="/notizie" data-testid="back-to-news" className="inline-flex items-center gap-1 font-mono-accent text-[10px] uppercase tracking-widest text-[#D9531E] hover:underline mb-4">
            <ChevronLeft className="w-3 h-3" /> Torna alle notizie
          </Link>
          <div className="font-mono-accent text-[10px] uppercase tracking-widest text-[#0B2545]/70">
            {article.category} · {new Date(article.published_at).toLocaleDateString("it-IT", { day: "numeric", month: "long", year: "numeric" })}
          </div>
          <h1 className="font-serif-display text-3xl sm:text-4xl lg:text-5xl font-black text-[#0B2545] tracking-tight leading-[1.1] mt-3">{article.title}</h1>
          {article.subtitle && <p className="mt-4 font-serif-display text-lg sm:text-xl text-[#0B2545]/70 italic leading-relaxed">{article.subtitle}</p>}
          <div className="mt-4 pb-4 border-b border-[#E5DFD3] font-mono-accent text-[10px] uppercase tracking-widest text-[#0B2545]/60">
            Di {article.author} · {article.views} letture
          </div>
          {article.source_url && <a href={article.source_url} target="_blank" rel="noopener noreferrer" className="inline-block mt-3 text-sm text-[#D9531E] underline">Leggi la fonte originale ↗</a>}

          {/* BODY */}
          {article.video_url && <div className="mt-6"><NewsVideo url={article.video_url} title={article.title} poster={article.cover_image}/></div>}
          <div className="mt-8 font-body text-base sm:text-lg leading-[1.85] text-[#111827] space-y-5">
            {(article.body||'').split("\n").filter(Boolean).map((p, i) => <p key={i}>{p}</p>)}
          </div>

          {article.quartiere && (
            <div className="mt-8 pt-6 border-t border-[#E5DFD3]">
              <span className="font-mono-accent text-[10px] uppercase tracking-widest text-[#0B2545]/60">Quartiere: </span>
              <Link to={`/quartieri?q=${encodeURIComponent(article.quartiere)}`} className="font-serif-display font-bold text-[#D9531E] hover:underline">{article.quartiere}</Link>
            </div>
          )}
        </div>

<p className="mt-8 pb-10 text-sm text-slate-500">Commenti non attivi in questa versione.</p>
      </div>
    </article>
  );
}
