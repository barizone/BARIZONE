import React,{useLayoutEffect,useRef,useState} from 'react';
import './NewsTicker.css';

// A common distance/time ratio keeps both tickers at 40 pixels per second.
export default function InfoTicker({label,labelHref,labelTitle,ariaLabel,text,contentHref,className=''}){
 const [paused,setPaused]=useState(false),[duration,setDuration]=useState(45),group=useRef(null);
 useLayoutEffect(()=>{const element=group.current;if(!element)return;const measure=()=>{const width=element.getBoundingClientRect().width;if(width>0)setDuration(width/40)};measure();const observer=new ResizeObserver(measure);observer.observe(element);return()=>observer.disconnect()},[text]);
 const external=labelHref.startsWith('https://');
 return <div className={`news-ticker ${className}`} aria-label={ariaLabel}>
  <a className="news-ticker-label" href={labelHref} title={labelTitle} target={external?'_blank':undefined} rel={external?'noopener noreferrer':undefined}>{label}</a>
  <div className="news-ticker-window"><div className={`news-ticker-track${paused?' is-paused':''}`} style={{'--ticker-duration':`${duration}s`}}>
   {[0,1].map(i=><div className="news-ticker-group" key={i} ref={i===0?group:undefined} aria-hidden={i===1?'true':undefined}>{contentHref?<a className="news-ticker-item" href={contentHref} target="_blank" rel="noopener noreferrer" tabIndex={i===1?-1:undefined}>{text}</a>:<span className="news-ticker-item">{text}</span>}</div>)}
  </div></div>
  <button type="button" className="news-ticker-control" aria-pressed={paused} aria-label={`${paused?'Riprendi':'Metti in pausa'} ${ariaLabel.startsWith('Meteo')?'il meteo':'la viabilità'}`} onClick={()=>setPaused(v=>!v)}>{paused?'▶':'Ⅱ'}</button>
 </div>;
}
