const $=(s,p=document)=>p.querySelector(s);
const $$=(s,p=document)=>[...p.querySelectorAll(s)];

const silhouette="images/counsellor-silhouette.svg";

/* Public expert catalogue. New registered experts are merged from localStorage
   so the profile-picture rule works immediately in the static/demo build. */
const demoExperts=[
 {id:"ex01",name:"Aarav Mehta",role:"Senior Software Engineer",company:"Product Technology",stream:"Science",career:"Software Engineer",age:34,experience:11,expertise:["AI / ML","Software","Cloud"],rank:1,rating:4.9,reviews:318,students:1850,fee:999,duration:45,mode:["Chat","Video call"],city:"Bengaluru",bio:"Helps students understand what software careers actually look like beyond a degree — from projects and interviews to AI-era skills.",video:"",verified:true,availability:"Available today"},
 {id:"ex02",name:"Riya Kapoor",role:"Product & UX Leader",company:"Digital Products",stream:"Arts & Humanities",career:"UX/UI Designer",age:32,experience:9,expertise:["UX / UI","Design","Product"],rank:2,rating:4.9,reviews:276,students:1420,fee:899,duration:45,mode:["Chat","Video call"],city:"Mumbai",bio:"Breaks down design careers, portfolios, product thinking and the difference between learning tools and becoming a strong designer.",video:"",verified:true,availability:"Available today"},
 {id:"ex03",name:"Vikram Shah",role:"Chartered Accountant & Finance Mentor",company:"Finance & Advisory",stream:"Commerce",career:"Chartered Accountant",age:41,experience:17,expertise:["CA","Tax","Finance"],rank:3,rating:4.8,reviews:241,students:1260,fee:799,duration:30,mode:["Chat","Video call"],city:"Mumbai",bio:"Makes professional finance pathways easier to understand — including CA, audit, tax, corporate finance and building a long-term career.",video:"",verified:true,availability:"Available tomorrow"},
 {id:"ex04",name:"Dr. Neha Iyer",role:"Doctor & Healthcare Mentor",company:"Clinical Medicine",stream:"Science",career:"Medical Doctor",age:38,experience:13,expertise:["Medicine","NEET","Healthcare"],rank:4,rating:4.9,reviews:205,students:1130,fee:1099,duration:45,mode:["Chat","Video call"],city:"Pune",bio:"Offers a realistic view of medical education, clinical life, specialisation and the skills students should develop early.",video:"",verified:true,availability:"Available today"},
 {id:"ex05",name:"Kabir Malhotra",role:"Investment Banking Professional",company:"Capital Markets",stream:"Commerce",career:"Investment Analyst",age:36,experience:12,expertise:["Markets","Banking","Finance"],rank:5,rating:4.8,reviews:187,students:940,fee:1299,duration:45,mode:["Chat","Video call"],city:"Gurugram",bio:"Explains finance careers through the lens of real work: financial modelling, markets, communication and the skills recruiters notice.",video:"",verified:true,availability:"Available this week"},
 {id:"ex06",name:"Ananya Rao",role:"Psychology & Career Specialist",company:"People & Learning",stream:"Arts & Humanities",career:"Psychologist",age:35,experience:10,expertise:["Psychology","People","Career"],rank:6,rating:4.9,reviews:164,students:870,fee:749,duration:30,mode:["Chat","Video call"],city:"Hyderabad",bio:"Helps students explore psychology and people-focused careers with clarity around education, specialisation and real-world roles.",video:"",verified:true,availability:"Available today"},
 {id:"ex07",name:"Siddharth Jain",role:"Data & AI Leader",company:"Analytics & AI",stream:"Science",career:"Data Scientist",age:39,experience:15,expertise:["Data Science","AI","Python"],rank:7,rating:4.8,reviews:151,students:820,fee:1199,duration:45,mode:["Chat","Video call"],city:"Bengaluru",bio:"Shows students how mathematics, coding, statistics and business thinking come together in modern data and AI careers.",video:"",verified:true,availability:"Available tomorrow"},
 {id:"ex08",name:"Meera Nair",role:"Media & Communications Director",company:"Media & Entertainment",stream:"Arts & Humanities",career:"Journalist",age:37,experience:14,expertise:["Media","Writing","Communication"],rank:8,rating:4.7,reviews:138,students:690,fee:699,duration:30,mode:["Chat","Video call"],city:"Delhi",bio:"Shares practical insight into journalism, digital media, storytelling, portfolios and how the industry is evolving.",video:"",verified:true,availability:"Available today"},
 {id:"ex09",name:"Arjun Desai",role:"Civil Infrastructure Consultant",company:"Infrastructure & Projects",stream:"Science",career:"Civil Engineer",age:44,experience:20,expertise:["Civil","Infrastructure","Projects"],rank:9,rating:4.8,reviews:119,students:610,fee:699,duration:30,mode:["Chat","Video call"],city:"Ahmedabad",bio:"Connects classroom concepts with infrastructure projects, engineering careers, government opportunities and project management.",video:"",verified:true,availability:"Available this week"},
 {id:"ex10",name:"Simran Bedi",role:"Digital Growth Strategist",company:"Brand & Growth",stream:"Commerce",career:"Digital Marketing Manager",age:31,experience:8,expertise:["Marketing","Growth","Brand"],rank:10,rating:4.8,reviews:127,students:730,fee:699,duration:30,mode:["Chat","Video call"],city:"Delhi",bio:"Helps students understand digital marketing beyond social media — including analytics, growth, performance marketing and strategy.",video:"",verified:true,availability:"Available today"},
 {id:"ex11",name:"Kunal Verma",role:"Cybersecurity Architect",company:"Cyber Defence",stream:"Science",career:"Cybersecurity Analyst",age:40,experience:16,expertise:["Cybersecurity","Linux","Networking"],rank:11,rating:4.9,reviews:109,students:560,fee:999,duration:45,mode:["Chat","Video call"],city:"Noida",bio:"Explains cybersecurity roles, learning paths, certifications, projects and what security teams expect from fresh talent.",video:"",verified:true,availability:"Available tomorrow"},
 {id:"ex12",name:"Ishita Sen",role:"Public Policy Consultant",company:"Policy & Development",stream:"Arts & Humanities",career:"Policy Analyst",age:33,experience:9,expertise:["Policy","Research","Public Affairs"],rank:12,rating:4.8,reviews:96,students:480,fee:749,duration:30,mode:["Chat","Video call"],city:"Kolkata",bio:"Guides students interested in public policy, research, governance, think tanks and international development careers.",video:"",verified:true,availability:"Available this week"}
];

function getRegisteredExpert(){
 const name=localStorage.getItem("udaanixExpertName");
 if(!name) return null;
 const fee=Number(localStorage.getItem("udaanixExpertFee")||799);
 const duration=Number(localStorage.getItem("udaanixExpertDuration")||30);
 const age=Number(localStorage.getItem("udaanixExpertAge")||0);
 const expRaw=localStorage.getItem("udaanixExpertExperience")||"";
 const exp=Number((expRaw.match(/\d+/)||["0"])[0]);
 const stream=localStorage.getItem("udaanixExpertStream")||"Science";
 const career=localStorage.getItem("udaanixExpertCareer")||localStorage.getItem("udaanixExpertise")||"Industry Expert";
 const expertise=(localStorage.getItem("udaanixExpertise")||"Industry expertise").split(",").map(x=>x.trim()).filter(Boolean);
 const pic=localStorage.getItem("udaanixExpertProfilePicture")||"";
 const video=localStorage.getItem("udaanixExpertIntroVideo")||"";
 return {
   id:"registered-expert",name,role:localStorage.getItem("udaanixExpertJobTitle")||"Industry Expert",
   company:localStorage.getItem("udaanixExpertCompany")||"Independent Professional",stream,career,age,experience:exp,
   expertise,rank:0,rating:5,reviews:0,students:0,fee,duration,mode:["Chat","Video call"],city:localStorage.getItem("udaanixExpertCity")||"India",
   bio:"Your Udaanix expert profile is ready. Add your professional story, career insights and intro video to help students understand your path.",
   video,verified:false,availability:"Profile available",picture:pic
 };
}
let experts=[...demoExperts];
const registered=getRegisteredExpert();
if(registered) experts.unshift(registered);

let filtered=[...experts];

function imageFor(e){return e.picture||silhouette}
function render(){
 const q=($("#expertSearch").value||"").trim().toLowerCase();
 const stream=$("#streamFilter").value;
 const career=$("#careerFilter").value;
 const sort=$("#sortFilter").value;
 filtered=experts.filter(e=>{
   const text=[e.name,e.role,e.company,e.stream,e.career,...e.expertise].join(" ").toLowerCase();
   return (!q||text.includes(q))&&(stream==="all"||e.stream===stream)&&(career==="all"||e.career===career);
 });
 filtered.sort((a,b)=>{
   if(sort==="experience") return b.experience-a.experience;
   if(sort==="rating") return b.rating-a.rating;
   if(sort==="price-low") return a.fee-b.fee;
   return (a.rank||999)-(b.rank||999);
 });
 $("#expertCount").textContent=filtered.length;
 $("#emptyState").hidden=filtered.length!==0;
 $("#expertGrid").innerHTML=filtered.map(card).join("");
 $$(".expert-card .view").forEach(b=>b.onclick=()=>openProfile(experts.find(e=>e.id===b.dataset.id)));
 $$(".expert-card .chat").forEach(b=>b.onclick=()=>toast("Sign in to start a chat with this expert."));
}
function card(e){
 const isYou=e.id==="registered-expert";
 const tags=e.expertise.slice(0,3).map(t=>`<span>${escapeHtml(t)}</span>`).join("");
 return `<article class="expert-card">
  <div class="expert-photo">
   <img src="${imageFor(e)}" alt="${escapeHtml(e.name)} profile photo">
   <span class="rank-badge">${isYou?'<i class="fa-solid fa-user"></i> YOUR PROFILE':'<i class="fa-solid fa-ranking-star"></i> #'+e.rank}</span>
   <span class="online-badge"><i class="fa-solid fa-circle"></i> ${escapeHtml(e.availability)}</span>
   ${e.video?'<span class="video-badge"><i class="fa-solid fa-play"></i> Intro video</span>':''}
  </div>
  <div class="expert-body">
   <div class="expert-meta"><div><span class="expert-role">${escapeHtml(e.role)}</span><h3>${escapeHtml(e.name)} ${e.verified?'<i class="fa-solid fa-circle-check" title="Udaanix verified"></i>':''}</h3><span class="expert-company">${escapeHtml(e.company)} · ${escapeHtml(e.city)}</span></div><span class="rating"><i class="fa-solid fa-star"></i> ${e.rating.toFixed(1)}</span></div>
   <p class="expert-summary">${escapeHtml(e.bio)}</p>
   <div class="chips">${tags}</div>
   <div class="expert-facts">
    <div><span>Stream / career</span><b>${escapeHtml(e.stream)} · ${escapeHtml(e.career)}</b></div>
    <div><span>Experience · Age</span><b>${e.experience||"—"} yrs · ${e.age||"—"}</b></div>
    <div><span>Session</span><b>₹${e.fee} · ${e.duration} min</b></div>
    <div><span>Student impact</span><b>${e.students.toLocaleString()} mentees</b></div>
   </div>
   <div class="expert-actions">
    <button class="chat" type="button"><i class="fa-regular fa-comment-dots"></i> Chat</button>
    <a href="auth/login.html"><i class="fa-solid fa-video"></i> Book video</a>
    <button class="view" type="button" data-id="${e.id}">View full profile <i class="fa-solid fa-arrow-right"></i></button>
   </div>
  </div>
 </article>`;
}
function openProfile(e){
 const video=e.video?`<div class="modal-video"><video controls preload="metadata" src="${e.video}"></video></div>`:`<div class="modal-video"><div class="video-placeholder"><i class="fa-regular fa-circle-play"></i><b>Career explainer video</b><div>This expert has not uploaded an intro video yet.</div></div></div>`;
 const tags=e.expertise.map(t=>`<span class="chips"><span>${escapeHtml(t)}</span></span>`).join("");
 $("#modalBody").innerHTML=`<div class="modal-profile">
  <aside class="modal-visual"><span class="modal-rank">${e.id==="registered-expert"?"YOUR PROFILE":"#"+e.rank+" · UDAANIX RANKING"}</span><img src="${imageFor(e)}" alt="${escapeHtml(e.name)} profile photo"><b>${e.verified?"✓ Udaanix Verified":"Industry Expert"}</b><p>${escapeHtml(e.stream)} · ${escapeHtml(e.career)}</p></aside>
  <div class="modal-main">
   <span class="role">${escapeHtml(e.role)}</span><h2>${escapeHtml(e.name)}</h2><div class="company">${escapeHtml(e.company)} · ${escapeHtml(e.city)}</div>
   <p class="modal-lead">${escapeHtml(e.bio)}</p>
   <div class="modal-facts">
    <div><small>AGE</small><b>${e.age||"Not added"}</b></div><div><small>EXPERIENCE</small><b>${e.experience||"Not added"} yrs</b></div><div><small>RATING</small><b>${e.rating.toFixed(1)} ★</b></div>
    <div><small>STUDENTS</small><b>${e.students.toLocaleString()}</b></div><div><small>SESSION</small><b>₹${e.fee}</b></div><div><small>DURATION</small><b>${e.duration} min</b></div>
   </div>
   <div class="chips" style="margin-top:15px">${e.expertise.map(t=>`<span>${escapeHtml(t)}</span>`).join("")}</div>
   ${video}
   <div class="modal-actions"><button onclick="toast('Sign in to start a chat with this expert.')"><i class="fa-regular fa-comment-dots"></i> Chat</button><a href="auth/login.html"><i class="fa-solid fa-video"></i> Zoom / Video call</a><a href="auth/login.html"><i class="fa-solid fa-calendar-check"></i> Book · ₹${e.fee}</a></div>
  </div>
 </div>`;
 $("#expertModal").showModal();
}
function populateCareers(){
 const careers=[...new Set(experts.map(e=>e.career))].sort();
 $("#careerFilter").innerHTML='<option value="all">All careers</option>'+careers.map(c=>`<option value="${escapeAttr(c)}">${escapeHtml(c)}</option>`).join("");
}
function escapeHtml(s){return String(s??"").replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[m]))}
function escapeAttr(s){return escapeHtml(s)}
function toast(msg){const t=$("#expertToast");t.textContent=msg;t.classList.add("show");clearTimeout(window._toast);window._toast=setTimeout(()=>t.classList.remove("show"),2400)}
window.toast=toast;

populateCareers();
["expertSearch","streamFilter","careerFilter","sortFilter"].forEach(id=>$( "#"+id).addEventListener(id==="expertSearch"?"input":"change",render));
$("#modalClose").onclick=()=>$("#expertModal").close();
$("#expertModal").addEventListener("click",e=>{if(e.target===e.currentTarget)e.currentTarget.close()});
render();
