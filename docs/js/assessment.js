
document.addEventListener("DOMContentLoaded", () => {
  const revealItems = document.querySelectorAll(".reveal");
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(e => { if(e.isIntersecting){ e.target.classList.add("visible"); observer.unobserve(e.target); }});
  }, {threshold:.12});
  revealItems.forEach(el => observer.observe(el));

  const mobileBtn = document.getElementById("mobileMenuBtn");
  const navLinks = document.getElementById("mobileLinks");
  if(mobileBtn && navLinks){
    mobileBtn.addEventListener("click", () => navLinks.classList.toggle("hidden"));
  }

  document.querySelectorAll("[data-start]").forEach(btn => {
    btn.addEventListener("click", () => {
      const target = btn.dataset.start;
      window.location.href = target;
    });
  });

  document.querySelectorAll("[data-toast]").forEach(el => {
    el.addEventListener("click", () => showToast(el.dataset.toast));
  });
});

function showToast(message){
  let toast = document.getElementById("toast");
  if(!toast){
    toast = document.createElement("div");
    toast.id="toast"; toast.className="toast";
    document.body.appendChild(toast);
  }
  toast.textContent=message;
  toast.classList.add("show");
  clearTimeout(window.__toastTimer);
  window.__toastTimer=setTimeout(()=>toast.classList.remove("show"),2600);
}

function saveAssessmentResult(type, answers, result){
  const record = {
    assessmentType:type,
    answers,
    result,
    completedAt:new Date().toISOString()
  };
  localStorage.setItem("udaanix_last_assessment", JSON.stringify(record));
}
