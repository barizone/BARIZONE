import React, {useEffect,useState} from 'react';
import InfoTicker from './InfoTicker';

// Fonte: ViaMichelin (consultazione del 04/11/2026). Snapshot editoriale,
// non feed live. La scadenza impedisce di presentare avvisi vecchi come attuali.
// Per aggiornamenti automatici serve un feed/API con autorizzazione d'uso.
const SOURCE='https://www.viamichelin.it/mappe-piantine/traffico/italia/puglia/bari/bari-70121';
const VALID_UNTIL=Date.parse('2026-11-05T08:00:00+01:00');
const REPORTS=[
 'Via Francesco Crispi: lavori in corso',
 'Via Giuseppe Capruzzi: lavori in corso',
 'Viale Papa Giovanni XXIII: lavori in corso',
 'SS16 Adriatica: lavori in corso',
 'Viale delle Regioni: chiusura di tratto segnalata',
 'Via di Maratona: chiusura di tratto segnalata',
 'Via Alessandro Manzoni: chiusura di tratto segnalata',
 'Corso Vittorio Veneto: lavori in corso'
];
export default function TrafficTicker(){
 const [now,setNow]=useState(()=>Date.now());
 useEffect(()=>{const t=setInterval(()=>setNow(Date.now()),60000);return()=>clearInterval(t)},[]);
 const fresh=now<VALID_UNTIL;
 const text=fresh?REPORTS.join('   ·   '):'Le segnalazioni precedenti sono scadute. Consulta ViaMichelin per verificare lavori, chiusure e restringimenti aggiornati.';
 return <InfoTicker className="traffic-ticker" label="🚧 VIABILITÀ BARI ↗" labelHref={SOURCE} labelTitle="Apri ViaMichelin" ariaLabel="Viabilità Bari: segnalazioni stradali a scorrimento" contentHref={SOURCE} text={`${text} · Fonte ViaMichelin ${fresh?'· rilevazione 04/11/2026':''} ↗`}/>;
}
