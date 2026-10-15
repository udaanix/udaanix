/* SIYA Career Knowledge Adapter — Step 6
 * Source: Udaanix Career Explorer's canonical career profiles.
 * This is public reference data; it is not a substitute for current official admission rules.
 */
(function(){
  const KNOWLEDGE_URL = (window.UDAANIX_SIYA_CAREER_KNOWLEDGE_URL || '/career-knowledge.json');
  let knowledge = null;

  async function load(){
    if(knowledge) return knowledge;
    try {
      const r = await fetch(KNOWLEDGE_URL, {cache:'no-store'});
      if(!r.ok) throw new Error('career knowledge unavailable');
      knowledge = await r.json();
    } catch(e){
      console.warn('SIYA career knowledge could not load:', e);
      knowledge = {science:[], commerce:[], arts:[]};
    }
    return knowledge;
  }

  function score(item, q){
    const hay = [item[0],item[1],item[2],item[3],item[4],item[5]].join(' ').toLowerCase();
    const terms = q.toLowerCase().split(/[^a-z0-9+#]+/).filter(Boolean);
    return terms.reduce((n,t)=>n+(hay.includes(t)?1:0),0);
  }

  async function findRelevant(query, limit=8){
    const data = await load();
    const all = Object.entries(data).flatMap(([stream,items])=>items.map(x=>({stream,raw:x})));
    return all.map(x=>({
      stream:x.stream,
      title:x.raw[0], domain:x.raw[1], education:x.raw[2], skills:x.raw[3], opportunities:x.raw[4], mode:x.raw[5],
      relevance:score(x.raw,query)
    })).filter(x=>x.relevance>0).sort((a,b)=>b.relevance-a.relevance).slice(0,limit);
  }

  window.UdaanixSiyaCareerKnowledge = {load, findRelevant};
})();
