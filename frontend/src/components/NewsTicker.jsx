import {API} from '@/context/AuthContext';
import React, {useEffect,useState} from 'react';
import InfoTicker from './InfoTicker';
export default function NewsTicker(){
 const [text,setText]=useState('Caricamento meteo Bari…');
 useEffect(()=>{let active=true;const refresh=async()=>{try{
 const r=await fetch(`${API}/weather`,{signal:AbortSignal.timeout(12000)});
 if(!r.ok)throw Error();const {current:c}=await r.json();if(!c||![c.temperature_2m,c.precipitation,c.wind_speed_10m].every(Number.isFinite))throw Error();
 const condition=c.weather_code===0?'Sereno':c.weather_code<=3?'Nuvoloso':c.weather_code>=95?'Temporali':(c.weather_code>=71&&c.weather_code<=77)||c.weather_code===85||c.weather_code===86?'Neve':c.weather_code>=51?'Pioggia':'Condizioni variabili';
 if(active)setText(`Bari · ${condition} · ${c.temperature_2m} °C · Pioggia ${c.precipitation} mm · Vento ${c.wind_speed_10m} km/h (${c.wind_direction_10m}°) · Dati ${c.time.replace('T',' ')} · Open-Meteo`);
 }catch{if(active)setText('Meteo Bari temporaneamente non disponibile · Nuovo tentativo automatico tra 15 minuti');}};
 refresh();const t=setInterval(refresh,900000);return()=>{active=false;clearInterval(t);};},[]);
 return <InfoTicker label="METEO BARI · 7 GIORNI" labelHref="/#meteo-settimana" ariaLabel="Meteo Bari a scorrimento" text={text}/>;
}
