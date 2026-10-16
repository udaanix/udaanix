
"use strict";

const $ = (s, p = document) => p.querySelector(s);
const $$ = (s, p = document) => [...p.querySelectorAll(s)];

const seedPosts = [
    { id: 1, role: "Student", name: "Aarav Mehta", meta: "Class 12 · Science · 18 min ago", type: "question", label: "QUESTION", topic: "Technology", text: "PCM ke baad AI/ML aur engineering ke beech confused hoon. Skills kis direction mein build karun — coding first, maths first ya entrance prep?", likes: 42, comments: 14, tags: ["AI & ML", "Engineering", "Class 12"] },
    { id: 2, role: "Industry Expert", name: "Rahul Verma", meta: "AI & Technology · Industry Expert · 41 min ago", type: "insight", label: "EXPERT INSIGHT", topic: "Technology", text: "If I were starting college again, I would build 5 things before chasing a job title: problem solving, communication, one strong technical skill, projects and the habit of learning in public.", likes: 127, comments: 32, tags: ["AI", "Skills", "Career Growth"], expert: true },
    { id: 3, role: "Counsellor", name: "Dr. Priya Sharma", meta: "Career Counsellor · 1 hr ago", type: "pathway", label: "PATHWAY", topic: "Pathways", text: "A career pathway is not one straight line. For students exploring psychology, here is a simple lens: school → undergraduate foundation → specialisation → supervised experience → chosen practice/research direction.", likes: 89, comments: 21, tags: ["Psychology", "Career Pathway", "Guidance"] },
    { id: 4, role: "Student", name: "Shreya Kapoor", meta: "College Student · Design · 2 hrs ago", type: "pathway", label: "ROADMAP", topic: "Careers", text: "Design isn't only drawing! The roadmap that finally helped me understand UX/UI was: user research → problem framing → wireframes → prototypes → testing → portfolio → internships.", likes: 74, comments: 19, tags: ["UX/UI", "Portfolio", "Design"] },
    { id: 5, role: "Industry Expert", name: "Neha Iyer", meta: "Doctor · Healthcare · 3 hrs ago", type: "insight", label: "EXPERT INSIGHT", topic: "Careers", text: "A useful question before choosing medicine: do you enjoy the responsibility of solving human problems consistently, not just the idea of becoming a doctor? Talk to professionals and observe the day-to-day reality.", likes: 103, comments: 27, tags: ["Medicine", "NEET", "Healthcare"], expert: true },
    { id: 6, role: "Student", name: "Karan Shah", meta: "Commerce · 4 hrs ago", type: "study", label: "TRENDING STUDY", topic: "Education", text: "Seeing everyone talk about AI + finance. Is FinTech a separate career path or can someone reach it through CA, economics, BBA/BCom or computer science?", likes: 61, comments: 18, tags: ["FinTech", "Commerce", "AI"] },
    { id: 7, role: "Counsellor", name: "Ananya Rao", meta: "Career Counsellor · 6 hrs ago", type: "question", label: "DISCUSSION", topic: "Study Abroad", text: "What is one thing students wish they had known before choosing a study-abroad university? Let's make a checklist around cost, employability, curriculum, visa realities and alumni outcomes.", likes: 38, comments: 11, tags: ["Study Abroad", "College Choice", "Planning"] },
    { id: 8, role: "Industry Expert", name: "Siddharth Jain", meta: "Data & AI Leader · 8 hrs ago", type: "study", label: "TECH TREND", topic: "Technology", text: "The interesting AI career shift is not simply 'learn AI'. Teams increasingly need people who can combine domain knowledge + data + communication + responsible AI thinking. What domain would you pair with AI?", likes: 146, comments: 37, tags: ["Data Science", "AI", "Future of Work"], expert: true }
];

let posts = [...seedPosts];
let activeTopic = "all";
let activeFilter = "all";
let visible = 5;
let toastTimer;

function avatarClass(role) {
    return role === "Industry Expert" ? "expert" : role.toLowerCase();
}

function initials(name) {
    return name.split(" ").map(x => x[0]).slice(0, 2).join("");
}

function esc(s) {
    return String(s).replace(/[&<>"']/g, m => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#039;"
    })[m]);
}

function formatText(s) {
    return esc(s).replace(
        /(AI\/ML|AI|FinTech|UX\/UI|NEET|study-abroad)/gi,
        "<strong>$1</strong>"
    );
}

/* Reusable toast for Save, Share and other small messages. */
function toast(message) {
    let box = $("#communityToast");

    if (!box) {
        box = document.createElement("div");
        box.id = "communityToast";
        box.setAttribute("role", "status");
        box.setAttribute("aria-live", "polite");

        box.style.cssText = `
            position: fixed;
            left: 50%;
            bottom: 24px;
            transform: translateX(-50%);
            z-index: 100001;
            background: #123568;
            color: #fff;
            padding: 14px 22px;
            border-radius: 12px;
            box-shadow: 0 8px 28px rgba(0,0,0,.2);
            font-size: 14px;
            max-width: calc(100% - 32px);
            text-align: center;
            display: none;
        `;

        document.body.appendChild(box);
    }

    clearTimeout(toastTimer);
    box.textContent = message;
    box.style.display = "block";

    toastTimer = setTimeout(() => {
        box.style.display = "none";
    }, 3000);
}

/* Accessible validation and success popup. */
function showValidationPopup(title, message, options = {}) {
    let popup = $("#communityValidationPopup");

    if (!popup) {
        popup = document.createElement("div");
        popup.id = "communityValidationPopup";

        popup.innerHTML = `
            <div class="community-validation-card"
                 role="alertdialog"
                 aria-modal="true"
                 aria-labelledby="communityPopupTitle"
                 aria-describedby="communityPopupMessage">

                <div class="community-validation-icon"
                     aria-hidden="true">i</div>

                <h3 id="communityPopupTitle"></h3>
                <p id="communityPopupMessage"></p>

                <button type="button" id="communityPopupOK">
                    Got it
                </button>
            </div>
        `;

        popup.style.cssText = `
            position: fixed;
            inset: 0;
            z-index: 100000;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
            background: rgba(10, 31, 66, .58);
            backdrop-filter: blur(5px);
        `;

        const card = $(".community-validation-card", popup);
        card.style.cssText = `
            width: 100%;
            max-width: 420px;
            padding: 30px;
            border-radius: 22px;
            background: #fff;
            color: #17365f;
            text-align: center;
            box-shadow: 0 20px 60px rgba(0,0,0,.22);
            font-family: inherit;
            animation: communityPopupIn .2s ease-out;
        `;

        const icon = $(".community-validation-icon", popup);
        icon.style.cssText = `
            width: 54px;
            height: 54px;
            margin: 0 auto 16px;
            display: grid;
            place-items: center;
            border-radius: 50%;
            background: #eaf0ff;
            color: #4568d8;
            font-size: 28px;
            font-weight: 800;
        `;

        $("h3", popup).style.cssText =
            "margin:0 0 12px;font-size:23px;";

        $("p", popup).style.cssText =
            "margin:0 0 24px;color:#63748b;line-height:1.7;font-size:14px;";

        const okButton = $("#communityPopupOK", popup);
        okButton.style.cssText = `
            border: 0;
            border-radius: 12px;
            padding: 12px 28px;
            background: #123568;
            color: #fff;
            font-weight: 700;
            cursor: pointer;
        `;

        okButton.addEventListener("click", closePopup);

        popup.addEventListener("click", event => {
            if (event.target === popup) closePopup();
        });

        popup.addEventListener("keydown", event => {
            if (event.key === "Escape") {
                event.preventDefault();
                closePopup();
            }

            if (event.key === "Tab") {
                event.preventDefault();
                $("#communityPopupOK", popup).focus();
            }
        });

        document.body.appendChild(popup);
    }

    $("#communityPopupTitle", popup).textContent = title;
    $("#communityPopupMessage", popup).textContent = message;

    popup.style.display = "flex";
    $("#communityPopupOK", popup).focus();

    function closePopup() {
        if (popup && popup.isConnected) {
            popup.remove();
        }

        if (typeof options.onClose === "function") {
            options.onClose();
        }
    }
}

function render() {
    const feed = $("#postFeed");
    if (!feed) return;

    const searchInput = $("#expertSearch");
    const sortSelect = $("#sortPosts");
    const loadMoreButton = $("#loadMore");

    const q = (searchInput?.value || "").trim().toLowerCase();
    const sort = sortSelect?.value || "latest";

    let list = posts.filter(p => {
        const searchableText = [
            p.name, p.role, p.meta, p.type, p.topic,
            p.text, ...(p.tags || [])
        ].join(" ").toLowerCase();

        const topicOk =
            activeTopic === "all" || p.topic === activeTopic;

        const filterOk =
            activeFilter === "all" ||
            (activeFilter === "expert" && p.expert) ||
            p.type === activeFilter;

        return topicOk && filterOk &&
            (!q || searchableText.includes(q));
    });

    if (sort === "popular") {
        list.sort((a, b) => b.likes - a.likes);
    } else if (sort === "expert") {
        list.sort((a, b) =>
            Number(Boolean(b.expert)) - Number(Boolean(a.expert))
        );
    } else {
        list.sort((a, b) => b.id - a.id);
    }

    const shown = list.slice(0, visible);
    feed.innerHTML = shown.map(postCard).join("");

    if (loadMoreButton) {
        loadMoreButton.style.display =
            shown.length < list.length ? "block" : "none";
    }

    bindPostActions();
}

function postCard(p) {
    const liked =
        localStorage.getItem("udaanixLikedPost" + p.id) === "1";

    const saved =
        localStorage.getItem("udaanixSavedPost" + p.id) === "1";

    const media = p.type === "study"
        ? `<div class="post-media study-media">
               <div>
                   <i class="fa-solid fa-chart-line"></i>
                   <b>Career &amp; technology trend</b>
                   <span>Discussion starter · source-friendly knowledge</span>
               </div>
           </div>`
        : "";

    return `
        <article class="post" data-id="${esc(p.id)}">
            <div class="post-head">
                <span class="post-avatar ${avatarClass(p.role)}">
                    ${esc(initials(p.name))}
                </span>

                <div class="post-user">
                    <b>
                        ${esc(p.name)}
                        ${p.expert
                            ? '<i class="fa-solid fa-circle-check" title="Expert"></i>'
                            : ""}
                    </b>
                    <small>${esc(p.meta)}</small>
                </div>

                <span class="post-type ${p.expert ? "expert" : ""}">
                    ${esc(p.label)}
                </span>
            </div>

            <p class="post-content">${formatText(p.text)}</p>

            ${media}

            <div class="post-tags">
                ${(p.tags || []).map(tag =>
                    `<span>#${esc(tag)}</span>`
                ).join("")}
            </div>

            <div class="post-actions">
                <button type="button" class="like ${liked ? "liked" : ""}">
                    <i class="${liked ? "fa-solid" : "fa-regular"} fa-heart"></i>
                    <span>${p.likes}</span>
                </button>

                <button type="button" class="comment">
                    <i class="fa-regular fa-comment"></i>
                    ${p.comments} replies
                </button>

                <button type="button" class="save ${saved ? "saved" : ""}">
                    <i class="${saved ? "fa-solid" : "fa-regular"} fa-bookmark"></i>
                    ${saved ? "Saved" : "Save"}
                </button>

                <button type="button" class="share">
                    <i class="fa-solid fa-share-nodes"></i>
                    Share
                </button>
            </div>
        </article>
    `;
}

function bindPostActions() {
    $$(".like").forEach(btn => {
        btn.onclick = () => {
            const article = btn.closest(".post");
            const id = Number(article?.dataset.id);
            const post = posts.find(p => p.id === id);

            if (!post) return;

            const key = "udaanixLikedPost" + id;
            const liked = localStorage.getItem(key) === "1";

            post.likes += liked ? -1 : 1;
            localStorage.setItem(key, liked ? "0" : "1");

            render();
        };
    });

    $$(".save").forEach(btn => {
        btn.onclick = () => {
            const id = Number(btn.closest(".post")?.dataset.id);
            const key = "udaanixSavedPost" + id;
            const saved = localStorage.getItem(key) === "1";

            localStorage.setItem(key, saved ? "0" : "1");
            render();

            toast(saved
                ? "Removed from saved posts"
                : "Post saved to your Udaanix collection"
            );
        };
    });

    $$(".comment").forEach(btn => {
        btn.onclick = () => toast("Replies will open after sign in.");
    });

    $$(".share").forEach(btn => {
        btn.onclick = async () => {
            const id = btn.closest(".post")?.dataset.id;
            const url = location.href.split("#")[0] + "#post-" + id;

            try {
                await navigator.clipboard.writeText(url);
                toast("Discussion link copied");
            } catch {
                toast("Copy unavailable. Discussion link: " + url);
            }
        };
    });
}

function openComposer() {
    const composer = $("#composer");

    if (!composer) {
        showValidationPopup(
            "Composer unavailable",
            "The post form could not be found. Please refresh the page."
        );
        return;
    }

    if (typeof composer.showModal === "function") {
        if (!composer.open) composer.showModal();
    } else {
        composer.setAttribute("open", "");
    }

    setTimeout(() => $("#postText")?.focus(), 100);
}

/* Open composer buttons. */
["openComposer", "composerTrigger", "rightCompose"].forEach(id => {
    const element = $("#" + id);
    if (element) element.addEventListener("click", openComposer);
});

$("#closeComposer")?.addEventListener("click", () => {
    $("#composer")?.close();
});

$("#cancelComposer")?.addEventListener("click", () => {
    $("#composer")?.close();
});

/* Profile role selection. */
$$(".role").forEach(button => {
    button.addEventListener("click", () => {
        $$(".role").forEach(item => item.classList.remove("active"));
        button.classList.add("active");
    });
});

/* Publish post with clear validation popup. */
$("#publishPost")?.addEventListener("click", event => {
    event.preventDefault();

    const textInput = $("#postText");
    const topicInput = $("#postTopic");
    const typeInput = $("#postType");

    if (!textInput || !topicInput || !typeInput) {
        showValidationPopup(
            "Form unavailable",
            "Some post form fields are missing. Please refresh the page and try again."
        );
        return;
    }

    const text = textInput.value.trim();
    const topic = topicInput.value.trim() || "Careers";
    const type = typeInput.value;

    if (text.length < 12) {
        showValidationPopup(
            "Your post is too short!",
            `You have entered ${text.length} characters. Please write at least 12 characters so the community can understand your post.`,
            {
                onClose: () => {
                    textInput.focus();
                    const end = textInput.value.length;
                    textInput.setSelectionRange?.(end, end);
                }
            }
        );
        return;
    }

    const activeRole = $(".role.active");
    const role = activeRole?.dataset.role;

    if (!role) {
        showValidationPopup(
            "Select your profile",
            "Please select Student, Counsellor or Industry Expert before publishing."
        );
        return;
    }

    const allowedRoles = ["Student", "Counsellor", "Industry Expert"];

    if (!allowedRoles.includes(role)) {
        showValidationPopup(
            "Invalid profile",
            "Please select a valid community profile."
        );
        return;
    }

    const nameKey = role === "Industry Expert"
        ? "udaanixExpertName"
        : role === "Counsellor"
            ? "udaanixCounsellorName"
            : "udaanixStudentName";

    const name = localStorage.getItem(nameKey) || `Udaanix ${role}`;

    const id = Date.now();

    const labels = {
        study: "TRENDING STUDY",
        pathway: "PATHWAY",
        insight: "INSIGHT",
        win: "CAREER WIN",
        question: "QUESTION"
    };

    posts.unshift({
        id,
        role,
        name,
        meta: "Just now · Udaanix Community",
        type,
        label: labels[type] || "QUESTION",
        topic,
        text,
        likes: 0,
        comments: 0,
        tags: [topic],
        expert: role === "Industry Expert"
    });

    textInput.value = "";
    topicInput.value = "";

    const composer = $("#composer");
    if (composer?.open) composer.close();

    visible = Math.max(visible, 6);
    activeTopic = "all";
    activeFilter = "all";

    $$(".topic").forEach(button => {
        button.classList.toggle("active", button.dataset.topic === "all");
    });

    $$(".pill").forEach(button => {
        button.classList.toggle("active", button.dataset.filter === "all");
    });

    render();

    showValidationPopup(
        "Post published!",
        "Your post has been added to the community feed."
    );
});

/* Topic filters. */
$$(".topic").forEach(button => {
    button.addEventListener("click", () => {
        $$(".topic").forEach(item => item.classList.remove("active"));
        button.classList.add("active");

        activeTopic = button.dataset.topic || "all";
        visible = 5;
        render();
    });
});

/* Post type filters. */
$$(".pill").forEach(button => {
    button.addEventListener("click", () => {
        $$(".pill").forEach(item => item.classList.remove("active"));
        button.classList.add("active");

        activeFilter = button.dataset.filter || "all";
        visible = 5;
        render();
    });
});

$("#sortPosts")?.addEventListener("change", () => {
    visible = 5;
    render();
});

$("#expertSearch")?.addEventListener("input", () => {
    visible = 5;
    render();
});

$("#loadMore")?.addEventListener("click", () => {
    visible += 4;
    render();
});

/* Initial render. */
if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", render, { once: true });
} else {
    render();
}
