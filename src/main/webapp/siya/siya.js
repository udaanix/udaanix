/* SIYA AI — Udaanix | Step 7: Personalized Roadmap Engine */
(function(){
  const launcher=document.getElementById('siyaLauncher'), win=document.getElementById('siyaWindow'), close=document.getElementById('siyaClose');
  const input=document.getElementById('siyaInput'), send=document.getElementById('siyaSend'), messages=document.getElementById('siyaMessages');
  const handoffBtn=document.getElementById('siyaCounsellorBtn'), handoffModal=document.getElementById('siyaHandoffModal'), handoffClose=document.getElementById('siyaHandoffClose'), handoffSubmit=document.getElementById('siyaHandoffSubmit'), handoffReason=document.getElementById('siyaHandoffReason'), handoffMode=document.getElementById('siyaHandoffMode');
  const historyBtn=document.getElementById('siyaHistory'), historyPanel=document.getElementById('siyaHistoryPanel'), historyList=document.getElementById('siyaHistoryList'), historyClose=document.getElementById('siyaHistoryClose');
  if(!launcher||!win||!input||!send||!messages)return;
  const API='/UdaanixSIYA/api/siya/chat', ROADMAP_API='/UdaanixSIYA/api/siya/roadmap', HANDOFF_API='/UdaanixSIYA/api/siya/counsellor-handoff';
  let profile=window.UdaanixSiyaProfile?window.UdaanixSiyaProfile():{};
  let conversationId='';
  const toggle=open=>{win.classList.toggle('open',open);if(open)setTimeout(()=>input.focus(),150)};
  launcher.addEventListener('click',()=>toggle(!win.classList.contains('open'))); close&&close.addEventListener('click',()=>toggle(false));
  document.addEventListener('keydown',e=>{if(e.key==='Escape')toggle(false)});
  function scroll(){messages.parentElement.scrollTop=messages.parentElement.scrollHeight}
  function studentId(){ profile=window.UdaanixSiyaProfile?window.UdaanixSiyaProfile():profile; return String(profile?.studentId||profile?.id||profile?.userId||'').trim(); }
  function openHistory(){ if(!historyPanel)return; historyPanel.hidden=false; loadHistory(); }
  function closeHistory(){ if(historyPanel)historyPanel.hidden=true; }
  async function loadHistory(){
    if(!historyList)return; const sid=studentId();
    if(!sid){historyList.innerHTML='<div class="siya-history-empty">Log in to save and view your SIYA conversations.</div>';return;}
    historyList.innerHTML='<div class="siya-history-empty">Loading history…</div>';
    try{const r=await fetch('/UdaanixSIYA/api/siya/history?studentId='+encodeURIComponent(sid));const d=await r.json();if(!r.ok||!d.success)throw new Error(d.error||'History unavailable');
      historyList.innerHTML=''; const items=d.conversations||[]; if(!items.length){historyList.innerHTML='<div class="siya-history-empty">No saved conversations yet. Start chatting with SIYA.</div>';return;}
      items.forEach(c=>{const b=document.createElement('button');b.className='siya-history-item';b.dataset.id=c.conversationId;const st=document.createElement('strong');st.textContent=c.title;const sm=document.createElement('small');sm.textContent=c.updatedAt||'';b.append(st,sm);b.addEventListener('click',()=>loadConversation(c.conversationId));historyList.appendChild(b);});
    }catch(e){console.error(e);historyList.innerHTML='<div class="siya-history-empty">History is unavailable. Check the SIYA backend and database.</div>';}
  }
  async function loadConversation(id){
    try{const sid=studentId();if(!sid)return;const r=await fetch('/UdaanixSIYA/api/siya/history/messages?studentId='+encodeURIComponent(sid)+'&conversationId='+encodeURIComponent(id));const d=await r.json();if(!r.ok||!d.success)throw new Error(d.error||'Unable to load conversation');
      conversationId=id;messages.innerHTML='';(d.messages||[]).forEach(m=>msg(m.message,String(m.sender).toLowerCase()==='user'?'user':'bot'));closeHistory();scroll();
    }catch(e){console.error(e);msg('I could not load that conversation right now.','bot');}
  }

  function msg(text,type){const el=document.createElement('div');el.className='siya-msg '+type;el.textContent=text;messages.appendChild(el);scroll();return el}
  function load(on){send.disabled=on;input.disabled=on;if(on){const e=document.createElement('div');e.className='siya-msg bot siya-loading';e.textContent='SIYA is thinking…';e.id='siyaLoading';messages.appendChild(e);scroll()}else document.getElementById('siyaLoading')?.remove()}
  async function careerRefs(q){return window.UdaanixSiyaCareerKnowledge?await window.UdaanixSiyaCareerKnowledge.findRelevant(q,8):[]}
  function openHandoff(reason=''){ if(!handoffModal)return; handoffModal.hidden=false; if(reason&&handoffReason)handoffReason.value=reason; setTimeout(()=>handoffReason?.focus(),50); }
  function closeHandoff(){ if(handoffModal)handoffModal.hidden=true; }
  async function submitHandoff(){ const sid=studentId(), reason=(handoffReason?.value||'').trim(), mode=handoffMode?.value||'ANY'; if(!sid){msg('Please log in to Udaanix before requesting a counsellor.','bot');closeHandoff();return;} if(reason.length<8){handoffReason?.focus();return;} if(handoffSubmit)handoffSubmit.disabled=true; try{const r=await fetch(HANDOFF_API,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({studentId:sid,conversationId,reason,preferredMode:mode})});const d=await r.json().catch(()=>({}));if(!r.ok||!d.success)throw new Error(d.error||'Request unavailable');closeHandoff();msg('Your counsellor request has been sent to Udaanix. A counsellor will confirm the next step with you.','bot');}catch(e){console.error(e);msg('I could not send the counsellor request right now. Please try again.','bot')}finally{if(handoffSubmit)handoffSubmit.disabled=false;} }

  function list(parent,items){if(!Array.isArray(items)||!items.length)return;const ul=document.createElement('ul');items.forEach(x=>{const li=document.createElement('li');li.textContent=x;ul.appendChild(li)});parent.appendChild(ul)}
  function renderRoadmap(r){
    const box=document.createElement('div');box.className='siya-roadmap';
    const h=document.createElement('h3');h.textContent='🧭 '+(r.goal||'Your Career Roadmap');box.appendChild(h);
    if(r.summary){const p=document.createElement('p');p.className='roadmap-summary';p.textContent=r.summary;box.appendChild(p)}
    (r.stages||[]).forEach(s=>{const card=document.createElement('div');card.className='roadmap-stage';const h4=document.createElement('h4');h4.textContent=s.title||'Stage';card.appendChild(h4);if(s.timeframe){const sm=document.createElement('small');sm.textContent=s.timeframe;card.appendChild(sm)}if(s.objective){const p=document.createElement('p');p.textContent=s.objective;card.appendChild(p)}list(card,s.actions);list(card,s.skills);list(card,s.milestones);box.appendChild(card)});
    if(Array.isArray(r.next_steps)&&r.next_steps.length){const n=document.createElement('div');n.className='roadmap-next';n.textContent='Next steps';box.appendChild(n);list(box,r.next_steps)}
    messages.appendChild(box);scroll();
  }
  async function roadmap(goal){
    goal=goal.trim();if(!goal||send.disabled)return;msg('Build a roadmap for: '+goal,'user');load(true);
    try{profile=window.UdaanixSiyaProfile?window.UdaanixSiyaProfile():profile;const refs=await careerRefs(goal);const res=await fetch(ROADMAP_API,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({goal,profile,careerContext:refs})});const data=await res.json().catch(()=>({}));if(!res.ok||!data.success)throw new Error(data.error||'Roadmap unavailable');renderRoadmap(data.roadmap)}catch(e){console.error(e);msg('I could not generate the roadmap right now. Please make sure the SIYA backend is running and try again.','bot')}finally{load(false);input.focus()}
  }
  async function chat(text){text=text.trim();if(!text||send.disabled)return;msg(text,'user');input.value='';load(true);try{profile=window.UdaanixSiyaProfile?window.UdaanixSiyaProfile():profile;const refs=await careerRefs(text);const res=await fetch(API,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({message:text,profile,careerContext:refs,conversationId})});const data=await res.json().catch(()=>({}));if(!res.ok||!data.success)throw new Error(data.error||'SIYA unavailable');if(data.conversationId)conversationId=data.conversationId;msg(data.reply||'Please try again.','bot')}catch(e){console.error(e);msg('I’m having trouble connecting to SIYA right now. Please make sure the Udaanix SIYA backend is running and try again.','bot')}finally{load(false);input.focus()}}
  function handle(text){if(/^\s*(build|create|make|generate)\s+(my\s+)?(a\s+)?(career\s+)?roadmap/i.test(text)||/roadmap/i.test(text)&&text.length>12){roadmap(text.replace(/^(build|create|make|generate)\s+(my\s+)?(a\s+)?(career\s+)?roadmap\s*(for)?\s*:?/i,'').trim()||text);return}chat(text)}
  send.addEventListener('click',()=>handle(input.value));
  handoffBtn&&handoffBtn.addEventListener('click',()=>openHandoff());
  handoffClose&&handoffClose.addEventListener('click',closeHandoff);
  handoffSubmit&&handoffSubmit.addEventListener('click',submitHandoff);
  handoffModal&&handoffModal.addEventListener('click',e=>{if(e.target===handoffModal)closeHandoff()});input.addEventListener('keydown',e=>{if(e.key==='Enter'&&!e.shiftKey){e.preventDefault();handle(input.value)}});
  document.querySelectorAll('.siya-quick').forEach(b=>b.addEventListener('click',()=>{const p=b.dataset.prompt||b.textContent.trim();if(/roadmap/i.test(p)){input.value='';roadmap(p.replace(/.*roadmap\s*(for)?\s*/i,'')||'my career')}else handle(p)}));
  window.addEventListener('udaanix:assessment-updated',()=>{profile=window.UdaanixSiyaProfile?window.UdaanixSiyaProfile():profile});
  historyBtn&&historyBtn.addEventListener('click',()=>historyPanel?.hidden?openHistory():closeHistory());
  historyClose&&historyClose.addEventListener('click',closeHistory);
  window.UdaanixSiyaBuildRoadmap=roadmap;
})();
