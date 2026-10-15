
"use strict";

/* =========================================================
   UDAANIX - CAREER INTELLIGENCE
   File: js/career-explorer.js
   ========================================================= */

const $ = id => document.getElementById(id);

const icons = ["✦", "◈", "⌘", "✺", "◎", "✎", "◇", "✹", "⬡", "☼"];

/* ================= CAREER DATABASE ================= */

const seed = {
    science: [
        ["Software Engineer", "Technology · AI · Software", "B.Tech CSE/IT, BCA or B.Sc CS + projects", "Coding, DSA, web development, cloud", "IT firms, startups, MNCs and remote work", "Degree", "Private", "Popular"],
        ["Medical Doctor", "Healthcare · Clinical", "MBBS, internship, registration and specialisation", "Biology, patient care, clinical reasoning", "Government and private hospitals", "Degree", "Government", "Popular"],
        ["Data Scientist", "Analytics · AI · ML", "Degree in Data Science, Mathematics, Statistics or CS", "Python, statistics, SQL, machine learning", "Analytics firms, fintech and research labs", "Degree", "Private", "Emerging"],
        ["Civil Engineer", "Infrastructure · Design", "B.E./B.Tech Civil Engineering", "AutoCAD, structures, project management", "PWD, railways, construction and infrastructure firms", "Degree", "Government", "Popular"],
        ["Research Scientist", "Research · Laboratory", "B.Sc, M.Sc and potentially PhD", "Research methods, lab skills, scientific writing", "Universities and research laboratories", "Degree", "International", "Emerging"],
        ["Science Teacher", "Education · Teaching", "Relevant science degree and required teaching qualification", "Subject mastery, pedagogy, communication", "Schools, coaching and education platforms", "Degree", "Government", "Popular"],
        ["Pilot", "Aviation · Transport", "Commercial Pilot Licence training and medical clearance", "Physics, maths, communication, discipline", "Airlines, cargo and aviation services", "Certification", "Private", "Popular"],
        ["Environmental Scientist", "Climate · Sustainability", "B.Sc/M.Sc Environmental Science or related field", "GIS, fieldwork, sustainability reporting", "Government bodies, NGOs and consulting", "Degree", "International", "Emerging"],
        ["Defence Officer", "Public Service · Defence", "NDA after 12th or eligible graduate entry", "Fitness, leadership and current affairs", "Armed forces and defence services", "Professional Exam", "Government", "Popular"],
        ["Cybersecurity Analyst", "Security · Technology", "Computer Science degree or relevant training", "Networking, Linux and security fundamentals", "Banks, cyber teams and security firms", "Certification", "Private", "Emerging"],
        ["Biotechnologist", "Life Science · Research", "B.Sc/B.Tech Biotechnology; further study as needed", "Molecular biology, lab techniques, bioinformatics", "Biotech, pharma and research companies", "Degree", "International", "Emerging"],
        ["Pharmacist", "Healthcare · Medicines", "D.Pharm/B.Pharm and applicable registration", "Pharmacology, patient safety, documentation", "Hospitals, pharmacies and pharmaceutical firms", "Degree", "Government", "Popular"],
        ["Architect", "Design · Built Environment", "B.Arch and applicable registration requirements", "Design, drawing, CAD, sustainability", "Architecture firms and urban design studios", "Degree", "Private", "Popular"],
        ["Agriculture Scientist", "Agriculture · Food Systems", "B.Sc Agriculture; postgraduate study for specialised roles", "Agronomy, soil science, data and fieldwork", "Agriculture departments, agri-tech and research", "Degree", "Government", "Emerging"],
        ["Science Communicator", "Media · Education", "Science background with writing or media portfolio", "Writing, visualisation, public speaking", "Publishers, museums, edtech and science media", "Certification", "International", "Emerging"]
    ],

    commerce: [
        ["Chartered Accountant", "Accounting · Audit · Tax", "CA Foundation, Intermediate, practical training and Final", "Accounting, audit, taxation, Excel", "Audit firms, corporates and independent practice", "Professional Exam", "Private", "Popular"],
        ["Business Manager", "Management · Strategy", "BBA/BMS/B.Com; MBA optional", "Leadership, sales, strategy, communication", "Companies, startups and entrepreneurship", "Degree", "Private", "Popular"],
        ["Banking Officer", "Banking · Public Service", "Graduation and applicable banking recruitment exams", "Quantitative aptitude, finance, customer service", "Public-sector banks, private banks and fintech", "Professional Exam", "Government", "Popular"],
        ["Investment Analyst", "Finance · Markets", "Finance, Commerce or Economics degree", "Financial modelling, markets and analysis", "Asset managers, investment banks and wealth firms", "Certification", "International", "Emerging"],
        ["Company Secretary", "Corporate Law · Compliance", "Company Secretary professional qualification route", "Company law, governance, documentation", "Listed companies and compliance teams", "Professional Exam", "Private", "Popular"],
        ["Economist", "Economics · Policy", "Economics degree; postgraduate study for specialist roles", "Econometrics, statistics, policy writing", "Government, banks and research organisations", "Degree", "Government", "Emerging"],
        ["Digital Marketing Manager", "Marketing · Growth", "Relevant degree with practical marketing skills", "SEO, ads, content and analytics", "Agencies, brands and ecommerce", "Certification", "Private", "Emerging"],
        ["Actuary", "Risk · Insurance · Mathematics", "Actuarial examinations and mathematical preparation", "Probability, statistics and finance", "Insurance, consulting and risk management", "Professional Exam", "International", "Emerging"],
        ["Entrepreneur", "Business · Innovation", "Any suitable educational route with business preparation", "Problem-solving, finance, sales and leadership", "Own venture, family business and startups", "Certification", "Entrepreneurship", "Emerging"],
        ["Financial Planner", "Personal Finance · Advisory", "Relevant finance education and applicable qualifications", "Financial planning, investments, communication", "Banks, wealth firms and advisory practices", "Certification", "Private", "Popular"],
        ["Tax Consultant", "Tax · Compliance", "Commerce, accounting or law background with tax specialisation", "Income tax, GST and documentation", "Tax firms, corporates and independent practice", "Certification", "Private", "Popular"],
        ["Supply Chain Analyst", "Operations · Logistics", "Commerce, Business or Operations degree", "Excel, analytics, procurement and planning", "Manufacturing, ecommerce and logistics", "Degree", "International", "Emerging"],
        ["HR Professional", "People · Organisation", "Business, Commerce or related degree", "Communication, HR processes and analytics", "Corporates and consulting organisations", "Degree", "Private", "Popular"],
        ["Business Data Analyst", "Analytics · Business", "Business, Commerce or Economics degree with analytics training", "Excel, SQL, Power BI and statistics", "Banks, fintech and consulting", "Certification", "International", "Emerging"],
        ["Stock Market Professional", "Markets · Research", "Finance education and applicable regulatory qualifications", "Market analysis, risk management and discipline", "Brokerages, research firms and investment platforms", "Certification", "Private", "Emerging"]
    ],

    arts: [
        ["Lawyer", "Law · Justice · Corporate", "Integrated law degree after 12th or eligible LLB route", "Legal reasoning, drafting and research", "Courts, law firms and corporate legal teams", "Degree", "Private", "Popular"],
        ["Civil Services Officer", "Public Administration · Government", "Graduation followed by eligible civil services examinations", "Current affairs, writing, analysis and leadership", "Central and state government administration", "Professional Exam", "Government", "Popular"],
        ["Psychologist", "Psychology · Behaviour", "Psychology degree followed by appropriate further training", "Listening, research, assessment and ethics", "Schools, research and eligible wellbeing settings", "Degree", "Private", "Popular"],
        ["Journalist", "Media · Communication", "Journalism, English or Media degree; portfolio", "Writing, research and multimedia", "Newsrooms, digital media and publishers", "Degree", "International", "Popular"],
        ["UX/UI Designer", "Design · Technology", "Design education or a strong practical portfolio", "Design thinking, Figma and user research", "Technology companies, studios and agencies", "Certification", "Private", "Emerging"],
        ["Teacher", "Education · Teaching", "Relevant degree and required teacher education qualification", "Subject knowledge, pedagogy and communication", "Schools, colleges and education platforms", "Degree", "Government", "Popular"],
        ["Policy Analyst", "Public Policy · Research", "Political Science, Economics or related degree", "Research, policy writing and analysis", "Think tanks, NGOs and policy organisations", "Degree", "International", "Emerging"],
        ["Film Producer", "Media · Creative Industry", "Film, Media or related education and practical experience", "Storytelling, production and project management", "Production houses, streaming and advertising", "Degree", "Private", "Emerging"],
        ["Social Worker", "Development · Community", "BSW or related degree; MSW for specialised roles", "Community work, empathy and project management", "NGOs, CSR and development organisations", "Degree", "Government", "Popular"],
        ["Fashion Designer", "Creative · Fashion", "Fashion Design degree, diploma or portfolio route", "Textiles, illustration and trend research", "Fashion houses, brands and independent labels", "Degree", "Entrepreneurship", "Emerging"],
        ["Foreign Language Specialist", "Languages · Global Work", "Language study and relevant proficiency qualifications", "Language fluency, cultural awareness and translation", "MNCs, tourism and international organisations", "Certification", "International", "Emerging"],
        ["Public Relations Manager", "Communication · Branding", "PR, Media, English or Marketing education", "Writing, media relations and communication", "Agencies, corporates and public organisations", "Degree", "Private", "Popular"],
        ["Historian", "History · Research · Heritage", "History degree; postgraduate study for specialist research", "Archival research, writing and interpretation", "Museums, universities and archives", "Degree", "International", "Emerging"],
        ["International Relations Specialist", "Diplomacy · Global Affairs", "International Relations, Political Science or related degree", "Geopolitics, languages and research", "Think tanks, NGOs and international organisations", "Degree", "International", "Emerging"],
        ["Content Creator", "Digital Media · Storytelling", "Any educational route with a strong portfolio", "Video, writing, editing and audience analytics", "Creator economy, brands and agencies", "Certification", "Entrepreneurship", "Emerging"]
    ]
};


/* ================= CAREER DATA ================= */

function buildCareers(stream) {
    return seed[stream].map((item, index) => ({
        id: `${stream}-${index}`,
        title: item[0],
        domain: item[1],
        education: item[2],
        skills: item[3],
        opportunities: item[4],
        mode: item[5],
        sector: item[6],
        demand: item[7],
        stage: "After 12th",
        duration: item[5] === "Degree" ? "3–5 years" : "Varies by route",
        icon: icons[index % icons.length],
        description:
            `Explore ${item[0]}, compare education options, develop relevant skills and research suitable opportunities.`
    }));
}

const data = {
    science: {
        title: "Science career pathways",
        intro: "Explore medicine, engineering, technology, research, education, aviation, public service and emerging scientific careers.",
        careers: buildCareers("science"),
        road: [
            "After 10th: compare PCM, PCB, PCMB and vocational science routes.",
            "After 12th: shortlist degree, diploma, certification and entrance-exam routes.",
            "Build projects, internships and communication skills; compare career opportunities."
        ]
    },

    commerce: {
        title: "Commerce career pathways",
        intro: "Explore accounting, finance, banking, business, economics, marketing, analytics and entrepreneurship.",
        careers: buildCareers("commerce"),
        road: [
            "After 10th: choose Commerce with or without Mathematics according to your goals.",
            "After 12th: compare B.Com, BBA/BMS, professional examinations and specialised degrees.",
            "Develop Excel, analytics, communication and practical business skills."
        ]
    },

    arts: {
        title: "Arts & Humanities career pathways",
        intro: "Explore law, public service, psychology, media, design, education, languages, culture and global careers.",
        careers: buildCareers("arts"),
        road: [
            "After 10th: select Humanities subjects that match your interests and strengths.",
            "After 12th: compare BA, law, design, media, psychology and policy routes.",
            "Build a portfolio, writing samples, internships and communication skills."
        ]
    }
};


/* ================= STATE ================= */

let current = "science";
let shown = 12;
let quickFilter = "all";


/* ================= FILTERING ================= */

function getFilteredCareers() {
    const query = $("search").value.trim().toLowerCase();
    const sector = $("sector").value;
    const stage = $("stage").value;
    const mode = $("mode").value;

    return data[current].careers.filter(career => {
        const searchable = [
            career.title,
            career.domain,
            career.education,
            career.skills,
            career.opportunities,
            career.mode,
            career.sector,
            career.demand,
            career.stage
        ].join(" ").toLowerCase();

        const searchMatch = !query || searchable.includes(query);
        const sectorMatch = sector === "all" || career.sector === sector;
        const stageMatch = stage === "all" || career.stage === stage;
        const modeMatch = mode === "all" || career.mode === mode;

        let quickMatch = true;

        if (quickFilter === "popular") {
            quickMatch = career.demand === "Popular";
        } else if (quickFilter === "government") {
            quickMatch = career.sector === "Government";
        } else if (quickFilter === "international") {
            quickMatch = career.sector === "International";
        } else if (quickFilter === "emerging") {
            quickMatch = career.demand === "Emerging";
        }

        return searchMatch && sectorMatch && stageMatch &&
            modeMatch && quickMatch;
    });
}


/* ================= RENDER CARDS ================= */

function render() {
    const d = data[current];
    const careers = getFilteredCareers();
    const visible = careers.slice(0, shown);

    $("streamEyebrow").textContent =
        current === "arts" ? "ARTS & HUMANITIES" : current.toUpperCase();

    $("streamTitle").textContent = d.title;
    $("streamIntro").textContent = d.intro;
    $("count").textContent = `${careers.length} career profiles`;

    $("careerGrid").innerHTML = visible.length
        ? visible.map(c => `
            <article class="card">
                <div class="card-top">
                    <div class="icon">${c.icon}</div>
                    <span class="pill">${c.demand}</span>
                </div>

                <h3>${c.title}</h3>
                <small>${c.domain}</small>
                <p>${c.description}</p>

                <div class="mini">
                    <span>⌁ ${c.duration}</span>
                    <span>▣ ${c.mode}</span>
                </div>

                <button type="button" data-id="${c.id}">
                    Explore career →
                </button>
            </article>
        `).join("")
        : "<p>No matching careers found. Try changing your filters.</p>";

    $("loadMore").style.display =
        shown < careers.length ? "block" : "none";

    renderRoadmap();

    document.querySelectorAll("#streamTabs button").forEach(button => {
        const active = button.dataset.stream === current;
        button.classList.toggle("active", active);
        button.setAttribute("aria-pressed", String(active));
    });
}


/* ================= CAREER DETAIL POPUP ================= */

function openDetail(career) {
    if (!career) return;

    $("detailBody").innerHTML = `
        <div class="detail">
            <p class="eyebrow">${current.toUpperCase()} / CAREER PROFILE</p>

            <h2>${career.icon} ${career.title}</h2>
            <p class="lead">${career.description}</p>

            <div class="detail-grid">
                <div>
                    <b>Education route</b>
                    <p>${career.education}</p>
                </div>
                <div>
                    <b>Study stage</b>
                    <p>${career.stage}</p>
                </div>
                <div>
                    <b>Skills to build</b>
                    <p>${career.skills}</p>
                </div>
                <div>
                    <b>Duration</b>
                    <p>${career.duration}</p>
                </div>
                <div>
                    <b>Opportunity type</b>
                    <p>${career.sector}</p>
                </div>
                <div>
                    <b>Where you can work</b>
                    <p>${career.opportunities}</p>
                </div>
            </div>

            <h3>Student action plan</h3>
            <ol>
                <li>Check eligibility and entrance requirements on official websites.</li>
                <li>Compare courses and institutions with a counsellor.</li>
                <li>Build a relevant project, portfolio or internship.</li>
                <li>Verify current fees, recognition and professional requirements.</li>
            </ol>
        </div>
    `;

    $("detail").showModal();
}


/* ================= EVENT HANDLERS ================= */

function setupEvents() {
    document.querySelectorAll("#streamTabs button").forEach(button => {
        button.addEventListener("click", () => {
            current = button.dataset.stream;
            shown = 12;
            quickFilter = "all";
            render();
        });
    });

    ["search", "sector", "stage", "mode"].forEach(id => {
        $(id).addEventListener(
            id === "search" ? "input" : "change",
            () => {
                shown = 12;
                render();
            }
        );
    });

    $("careerGrid").addEventListener("click", event => {
        const button = event.target.closest("[data-id]");
        if (!button) return;

        const career = data[current].careers.find(
            item => item.id === button.dataset.id
        );

        openDetail(career);
    });

    $("loadMore").addEventListener("click", () => {
        shown += 12;
        render();
    });

    $("clearFilters").addEventListener("click", () => {
        $("search").value = "";
        $("sector").value = "all";
        $("stage").value = "all";
        $("mode").value = "all";

        quickFilter = "all";
        shown = 12;
        render();
    });

    document.querySelectorAll("[data-quick]").forEach(button => {
        button.addEventListener("click", () => {
            quickFilter = button.dataset.quick;

            $("search").value = "";
            $("sector").value = "all";
            $("stage").value = "all";
            $("mode").value = "all";

            shown = 12;
            render();
        });
    });

    $("close").addEventListener("click", () => {
        $("detail").close();
    });

    $("detail").addEventListener("click", event => {
        if (event.target === $("detail")) {
            $("detail").close();
        }
    });

    $("contributorForm").addEventListener("submit", event => {
        event.preventDefault();

        const form = event.currentTarget;
        const item = Object.fromEntries(new FormData(form).entries());

        let saved = [];

        try {
            saved = JSON.parse(
                localStorage.getItem("udaanix_submissions") || "[]"
            );

            if (!Array.isArray(saved)) saved = [];

            saved.push({
                ...item,
                submittedAt: new Date().toISOString()
            });

            localStorage.setItem(
                "udaanix_submissions",
                JSON.stringify(saved)
            );

            $("submitStatus").textContent =
                "Thank you! Your career insight has been saved for demo review.";

            form.reset();
        } catch (error) {
            console.error("Could not save submission:", error);

            $("submitStatus").textContent =
                "Unable to save this submission in browser storage.";
        }
    });
}


/* ================= ROADMAP ================= */

function renderRoadmap() {
    $("roadmap").innerHTML = `
        <div class="steps">
            ${data[current].road.map((step, index) => `
                <div class="step">
                    <b>Step ${index + 1}</b>
                    <span>${step}</span>
                </div>
            `).join("")}
        </div>
    `;
}


/* ================= INITIALISE ================= */

function initialiseCareerExplorer() {
    const required = [
        "streamTabs", "search", "sector", "stage", "mode",
        "streamEyebrow", "streamTitle", "streamIntro", "count",
        "careerGrid", "loadMore", "roadmap", "detail",
        "detailBody", "close", "contributorForm",
        "submitStatus", "clearFilters"
    ];

    const missing = required.filter(id => !$(id));

    if (missing.length) {
        console.error("Career Explorer missing HTML elements:", missing);
        return;
    }

    setupEvents();
    render();

    console.info("Udaanix Career Explorer loaded successfully.");
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initialiseCareerExplorer);
} else {
    initialiseCareerExplorer();
}
