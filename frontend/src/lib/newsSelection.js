// Keep a single card for the same article, even with tracking URLs or different IDs.
export function uniqueNews(items) {
 const ids=new Set(),sources=new Set(),titles=new Set();
 return [...items].sort((a,b)=>Date.parse(b.published_at)-Date.parse(a.published_at)).filter(item=>{
  let source='';try{const u=new URL(item.source_url);for(const key of [...u.searchParams.keys()])if(/^utm_/i.test(key)||['fbclid','gclid'].includes(key))u.searchParams.delete(key);u.searchParams.sort();source=u.hostname.replace(/^www\./,'')+u.pathname.replace(/\/$/,'')+u.search;}catch{}
  const title=(item.title||'').normalize('NFKC').toLocaleLowerCase('it').replace(/[^\p{L}\p{N}]+/gu,' ').trim();
  if(ids.has(item.id)||(source&&sources.has(source))||(title&&titles.has(title)))return false;
  ids.add(item.id);if(source)sources.add(source);if(title)titles.add(title);return true;
 });
}
