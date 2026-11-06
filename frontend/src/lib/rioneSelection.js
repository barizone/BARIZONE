const normalize=value=>value==='Bari Vecchia'?'San Nicola / Bari Vecchia':value;
const belongs=(item,q)=>!q||q==='Tutta Bari'||normalize(item.quartiere)===normalize(q)||(item.neighborhoods||[]).map(normalize).includes(normalize(q));
export function selectRioneNews(items,q,section='news') {
 return items.filter(item=>item.source_url&&belongs(item,q)&&((item.category||'').trim().toLowerCase()==='eventi')===(section==='eventi'));
}
export function selectRioneRestaurants(items,q) {
 return items.filter(item=>belongs(item,q)&&(item.directory_category||item.category||'').trim().toLowerCase()==='ristoranti');
}
