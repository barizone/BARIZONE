import React from "react";
import SocialLinks from "./SocialLinks";
import { Link } from "react-router-dom";

export default function Footer() {
  return (
    <footer className="bg-[#0B2545] text-[#F8F5EE] mt-3 relative overflow-hidden">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 relative z-10">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-5">
          <div className="md:col-span-2">
            <div className="flex items-center gap-3">
              <img src="/logo-224.webp" alt="Barizone" className="h-14 w-14 object-contain bg-[#F8F5EE] rounded p-1" />
              <div>
                <div className="font-serif-display font-black text-2xl">Barizone</div>
                <div className="font-mono-accent text-[10px] uppercase tracking-widest text-[#D9A036]">La prima app dei Baresi</div>
              </div>
            </div>
            <p className="mt-6 font-body text-sm text-[#F8F5EE]/80 max-w-md leading-relaxed">
              Tutta Bari a portata di mano. Notizie, eventi, nuove aperture e la voce dei quartieri — dal Molo Sant'Antonio a Torre a Mare.
            </p>
          </div>

          <div>
            <div className="font-mono-accent text-[10px] uppercase tracking-widest text-[#D9A036] mb-4">La città</div>
            <ul className="space-y-2 font-ui text-sm">
              <li><Link to="/iscriviti-news" className="link-underline">Iscriviti alle news</Link></li>
              <li><Link to="/notizie" className="link-underline">Notizie</Link></li>
              <li><Link to="/eventi" className="link-underline">Eventi</Link></li>
              
              <li><Link to="/aziende" className="link-underline">Aziende</Link></li>
            </ul>
          </div>

          <div>
            <div className="font-mono-accent text-[10px] uppercase tracking-widest text-[#D9A036] mb-4">Barizone</div>
            <ul className="space-y-2 font-ui text-sm">
              <li><Link to="/contatti" className="link-underline">Scrivi a Barizone · Email e WhatsApp</Link></li>
              <li><Link to="/promuovi" className="link-underline">Promuovi la tua attività</Link></li>
              <li><span className="block text-xs text-[#F8F5EE]/80 mb-1">Comunicazioni e news</span><a href="mailto:info@barizone.it" className="font-semibold underline underline-offset-4 break-all">info@barizone.it</a></li>
              <li><Link to="/login" className="link-underline">Accesso redazione</Link></li>
              <li><span className="text-[#F8F5EE]/60">Privacy · Cookie · Termini</span></li>
            </ul>
          </div>
        </div>

        <div className="mt-5"><SocialLinks/></div>
        <div className="mt-5 pt-4 border-t border-[#F8F5EE]/15 flex flex-col md:flex-row justify-between gap-4 font-mono-accent text-[10px] uppercase tracking-widest text-[#F8F5EE]/60">
          <span>© 2026 Barizone · Made in Bari con orgoglio 🌊</span>
          <span>v0.1 · Beta</span>
        </div>
      </div>
    </footer>
  );
}

