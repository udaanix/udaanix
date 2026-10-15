
/* UDAANIX — SHARK TANK EDITION INTERACTIONS */

const $ = (s, p=document) => p.querySelector(s);
const $$ = (s, p=document) => [...p.querySelectorAll(s)];

/* Loader */
window.addEventListener("load", () => {
  setTimeout(() => $("#preloader")?.classList.add("hide"), 650);
});

/* Scroll progress + nav shadow + back top */
const scrollLine=$("#scrollLine"), nav=$("#navbar"), backTop=$("#backTop");
window.addEventListener("scroll",()=>{
  const max=document.documentElement.scrollHeight-innerHeight;
  scrollLine.style.width=(max ? scrollY/max*100 : 0)+"%";
  nav.style.boxShadow=scrollY>12 ? "0 8px 30px rgba(29,48,82,.07)" : "none";
  backTop.classList.toggle("show",scrollY>650);
},{passive:true});
backTop?.addEventListener("click",()=>scrollTo({top:0,behavior:"smooth"}));

/* Cursor glow */
const glow=$("#cursorGlow");
if(matchMedia("(pointer:fine)").matches){
 document.addEventListener("mousemove",e=>{
   glow.style.left=e.clientX+"px"; glow.style.top=e.clientY+"px"; glow.style.opacity="1";
 });
}

/* Reveal */
const revealObserver=new IntersectionObserver(entries=>{
 entries.forEach(e=>{if(e.isIntersecting){e.target.classList.add("visible");revealObserver.unobserve(e.target)}})
},{threshold:.12});
$$(".reveal").forEach(x=>revealObserver.observe(x));

/* Number counters */
const counters=$$("[data-count]");
const counterObserver=new IntersectionObserver(entries=>{
 if(!entries[0].isIntersecting)return;
 counters.forEach(el=>{
   const target=+el.dataset.count, suffix=el.dataset.suffix||"";
   const start=performance.now(), duration=1300;
   const tick=now=>{
     const p=Math.min((now-start)/duration,1);
     const eased=1-Math.pow(1-p,3);
     el.textContent=Math.floor(target*eased).toLocaleString()+suffix;
     if(p<1)requestAnimationFrame(tick);
   };
   requestAnimationFrame(tick);
 });
 counterObserver.disconnect();
},{threshold:.35});
const statRoot=$(".hero-stats"); if(statRoot)counterObserver.observe(statRoot);

/* Mobile menu */
$("#menuToggle")?.addEventListener("click",()=>{
 $("#mobileNav").classList.toggle("open");
});
$$(".mobile-nav a").forEach(a=>a.addEventListener("click",()=>$("#mobileNav").classList.remove("open")));

/* Search: locates sections/cards, not just a decorative field */
const searchable=[
 {name:"Science Stream",hint:"PCM · PCB · STEM · engineering · medicine",target:"#streams"},
 {name:"Commerce Stream",hint:"Finance · CA · business · economics · management",target:"#streams"},
 {name:"Arts & Humanities",hint:"Design · law · psychology · media · literature",target:"#streams"},
 {name:"Career Intelligence — 500+ profiles",hint:"Software · doctor · data science · CA · law · design · 500+ career profiles",href:"streams/career-explorer.html"},
 {name:"Career Paths",hint:"Explore detailed career profiles, routes, skills and opportunities",href:"streams/career-explorer.html"},
 {name:"Career Assessment",hint:"Interest · aptitude · personality · stream selection",target:"#assessment"},
 {name:"Counsellors",hint:"Human guidance · career planning · experts",target:"#counsellors"},
 {name:"Udaanix Community",hint:"Questions · discussions · students · experts",target:"#community"},
 {name:"Resources",hint:"Guides · skills · exams · career advice",target:"#resources"}
];
const search=$("#careerSearch"), pop=$("#searchPop");
function renderSearch(){
 const q=(search.value||"").trim().toLowerCase();
 let matches=q ? searchable.filter(x=>(x.name+" "+x.hint).toLowerCase().includes(q)) : searchable.slice(0,5);

 // If the user types a specific career that is not a homepage section,
 // offer the full 500+ Career Intelligence library with the query preserved.
 if(q && !matches.length){
   matches=[{
     name:"Search 500+ career profiles",
     hint:`Open Career Intelligence for “${q}”`,
     href:`streams/career-explorer.html?q=${encodeURIComponent(q)}`
   }];
 } else if(q && matches.length && !matches.some(x=>x.href)){
   matches.push({
     name:`Explore “${q}” in Career Intelligence`,
     hint:"Search the complete 500+ career profile library",
     href:`streams/career-explorer.html?q=${encodeURIComponent(q)}`
   });
 }

 pop.innerHTML=matches.length ? matches.map(x=>`<button data-target="${x.target||""}" data-href="${x.href||""}"><i class="fa-solid fa-location-arrow"></i><span><b>${x.name}</b><small>${x.hint}</small></span></button>`).join("") : `<div style="padding:13px;font-size:10px;color:#8290a5">No matching Udaanix section yet.</div>`;
 pop.classList.add("open");
 $$(".search-pop button").forEach(b=>b.onclick=()=>{
   if(b.dataset.href){ window.location.href=b.dataset.href; return; }
   document.querySelector(b.dataset.target)?.scrollIntoView({behavior:"smooth",block:"start"});
   pop.classList.remove("open");
 });
}
search?.addEventListener("focus",renderSearch);
search?.addEventListener("input",renderSearch);
search?.addEventListener("keydown",e=>{if(e.key==="Enter"){e.preventDefault();const first=$(".search-pop button");if(first)first.click()}});
document.addEventListener("click",e=>{if(!e.target.closest(".search-wrap"))pop?.classList.remove("open")});
document.addEventListener("keydown",e=>{
 if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==="k"){e.preventDefault();search?.focus()}
 if(e.key==="Escape")pop?.classList.remove("open");
});

/* Career path data */
const careerData={
 science:[
  ["Software Engineer","Technology · AI · Software","fa-code","22%","₹6–25 LPA","4 yrs","Programming"],
  ["Medical Doctor","Healthcare · Research","fa-stethoscope","15%","₹10–30 LPA","5.5 yrs","Patient care"],
  ["Data Scientist","Analytics · AI · ML","fa-chart-line","28%","₹12–30 LPA","4 yrs","Python + Stats"],
  ["Civil Engineer","Infrastructure · Design","fa-building","12%","₹6–20 LPA","4 yrs","AutoCAD"]
 ],
 commerce:[
  ["Chartered Accountant","Finance · Audit · Tax","fa-calculator","18%","₹7–35 LPA","4.5 yrs","Accounting"],
  ["Investment Banker","Finance · Markets","fa-money-bill-trend-up","14%","₹15–50 LPA","3–5 yrs","Financial modelling"],
  ["Financial Analyst","Analytics · Business","fa-chart-pie","10%","₹5–18 LPA","3 yrs","Excel + Stats"],
  ["Marketing Manager","Growth · Brand · Strategy","fa-bullhorn","16%","₹6–22 LPA","3–4 yrs","Digital marketing"]
 ],
 arts:[
  ["Lawyer / Advocate","Legal · Corporate · Policy","fa-scale-balanced","10%","₹4–25 LPA","5 yrs","Research + Argument"],
  ["Journalist / Reporter","Media · Writing","fa-newspaper","5%","₹3–12 LPA","3 yrs","Writing"],
  ["Psychologist / Counsellor","People · Research","fa-brain","20%","₹4–15 LPA","2–4 yrs","Empathy"],
  ["UX/UI Designer","Product · Design","fa-pen-ruler","25%","₹6–20 LPA","3 yrs","User research"]
 ]
};
function renderPaths(stream){
 const grid=$("#pathGrid"); if(!grid)return;
 grid.innerHTML=careerData[stream].map((c,i)=>`
  <article class="path-card">
   <span class="path-icon"><i class="fa-solid ${c[2]}"></i></span>
   <h3>${c[0]} <span class="tag">${i===0||i===2?"HIGH DEMAND":"POPULAR"}</span></h3>
   <span class="stream-label">${c[1]}</span>
   <p>Explore the role, skills, study route and growth signals before you decide.</p>
   <div class="path-details">
    <div><i class="fa-solid fa-arrow-trend-up"></i> Growth <b>${c[3]} yearly</b></div>
    <div><i class="fa-solid fa-indian-rupee-sign"></i> Salary <b>${c[4]}</b></div>
    <div><i class="fa-solid fa-clock"></i> Duration <b>${c[5]}</b></div>
    <div><i class="fa-solid fa-gears"></i> Skills <b>${c[6]}</b></div>
   </div>
  </article>`).join("");
 $("#careerCount").textContent=careerData[stream].length+" paths";
}
renderPaths("science");
$$(".career-tab").forEach(tab=>tab.addEventListener("click",()=>{
 $$(".career-tab").forEach(t=>t.classList.remove("active"));tab.classList.add("active");renderPaths(tab.dataset.stream);
}));

/* Counsellor uploaded-image rule:
   If backend later provides data-profile-image, it replaces the silhouette.
   Broken/empty image always falls back to local silhouette. */
$$(".counsellor-card").forEach(card=>{
 const img=$("img",card);
 const supplied=card.dataset.profileImage?.trim();
 if(supplied){img.src=supplied;$(".photo-note",card)?.remove();}
 img.addEventListener("error",()=>{img.src="images/counsellor-silhouette.svg";});
});

/* Community likes */
$$(".like-btn").forEach(btn=>btn.addEventListener("click",()=>{
 const old=btn.textContent.match(/\d+/)?.[0]||"0";
 const liked=btn.classList.toggle("liked");
 const n=Math.max(0,+old+(liked?1:-1));
 btn.innerHTML=`<i class="${liked?"fa-solid":"fa-regular"} fa-heart"></i> ${n}`;
}));

/* Magnetic buttons */
if(matchMedia("(pointer:fine)").matches){
 $$(".magnetic").forEach(el=>{
  el.addEventListener("mousemove",e=>{
   const r=el.getBoundingClientRect(),x=(e.clientX-r.left-r.width/2)*.08,y=(e.clientY-r.top-r.height/2)*.08;
   el.style.transform=`translate(${x}px,${y}px)`;
  });
  el.addEventListener("mouseleave",()=>el.style.transform="");
 });
 const stage=$(".hero-stage");
 document.addEventListener("mousemove",e=>{
  if(!stage)return;
  const x=(e.clientX-innerWidth/2)/innerWidth,y=(e.clientY-innerHeight/2)/innerHeight;
  stage.style.transform=`translate(${x*7}px,${y*5}px)`;
 });
}

/* Smooth nav highlighting */
const navAnchors=$$(".desktop-nav a");
const observed=$$(".section[id],.hero[id]");
const sectionObserver=new IntersectionObserver(entries=>{
 entries.forEach(e=>{
  if(e.isIntersecting){
   navAnchors.forEach(a=>a.classList.toggle("active",a.getAttribute("href")==="#"+e.target.id));
  }
 })
},{rootMargin:"-35% 0px -55% 0px"});
observed.forEach(s=>sectionObserver.observe(s));

/* Toast */
let toastTimer;
function showToast(message){
 const t=$("#toast"); if(!t)return;
 t.textContent=message;t.classList.add("show");
 clearTimeout(toastTimer);toastTimer=setTimeout(()=>t.classList.remove("show"),2400);
}
window.showToast=showToast;
