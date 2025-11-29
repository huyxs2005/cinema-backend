const adminBannersApi = {
    list: "/api/admin/banners",
    detail: (id) => `/api/admin/banners/${id}`,
    create: "/api/admin/banners",
    update: (id) => `/api/admin/banners/${id}`,
    delete: (id) => `/api/admin/banners/${id}`,
    toggleActive: (id, active) => `/api/admin/banners/${id}/active?active=${active}`
};

const bannerDataBus = window.AdminDataBus || {
    dispatch: () => {},
    subscribe: () => () => {}
};

const bannerTextCompare = new Intl.Collator("vi", { sensitivity: "base" });

const movieOptionsMap = new Map();
let allMovieOptions = [];
let movieSelectReady = false;
let pendingMovieSelection = null;
let selectedMovieId = null;
const promotionOptionsMap = new Map();
let allPromotionOptions = [];
let promotionSelectReady = false;
let pendingPromotionSelection = null;
let selectedPromotionId = null;
let isEditing = false;
let isSubmittingBanner = false;
var createDebounce = window.__ADMIN_DEBOUNCE_FACTORY__;
if (typeof createDebounce !== "function") {
    createDebounce = function (fn, delay = 300) {
        let timer;
        return function (...args) {
            clearTimeout(timer);
            timer = setTimeout(() => fn.apply(this, args), delay);
        };
    };
    window.__ADMIN_DEBOUNCE_FACTORY__ = createDebounce;
}

function resolveBannerDisplayName(banner) {
    if (!banner) return "Banner";
    const linkType = (banner.linkType || "").toUpperCase();
    if (linkType === "MOVIE") {
        if (banner.movieTitle) {
            return banner.movieTitle;
        }
        if (banner.movieId) {
            return `Phim #${banner.movieId}`;
        }
        return "Banner phim";
    }
    if (linkType === "PROMO") {
        if (banner.promotionTitle) {
            return banner.promotionTitle;
        }
        if (banner.promotionSlug) {
            return `Khuyến mãi ${banner.promotionSlug}`;
        }
        return "Banner khuyến mãi";
    }
    if (banner.targetUrl) {
        return banner.targetUrl;
    }
    return "Khuyến mãi HUB";
}

function buildBannerTitleHtml(banner, primaryText) {
    const primary = primaryText || "Banner";
    const secondary =
        banner && banner.linkType === "MOVIE" && banner.movieOriginalTitle
            ? `<div class="banner-title-secondary">${banner.movieOriginalTitle}</div>`
            : "";
    return `
        <div class="banner-title-primary">${primary}</div>
        ${secondary}
    `;
}

document.addEventListener("DOMContentLoaded", () => {
    initImageUploadControls();
    initTargetUrlPreview();
    initMovieClearButton();
    initMovieSearchInput();
    initPromotionSearchInput();
    initPromotionClearButton();
    attachNumericOnlyHandlers();
    initNumericStepperControls();
    loadMovieOptions();
    loadPromotionOptions();
    updatePromotionClearButtonState();

    const form = document.getElementById("bannerForm");
    const resetBtn = document.getElementById("resetFormBtn");
    const linkTypeSelect = document.getElementById("linkType");

    if (!form || !resetBtn || !linkTypeSelect) {
        return;
    }

    bindBannerFilters();
    fetchBanners();
    form.addEventListener("submit", submitBannerForm);
    resetBtn.addEventListener("click", resetFormState);
    linkTypeSelect.addEventListener("change", toggleLinkInputs);
    toggleLinkInputs();
    updateImageHint(document.getElementById("imagePath")?.value ?? "");
    updateMovieClearButtonState();
    bannerDataBus.subscribe("movies", refreshMovieOptionsFromBus);
    bannerDataBus.subscribe("promotions", refreshPromotionOptionsFromBus);
});

function initImageUploadControls() {
    const selectBtn = document.getElementById("selectImageBtn");
    const fileInput = document.getElementById("bannerImageFile");
    if (!selectBtn || !fileInput) return;
    selectBtn.addEventListener("click", () => fileInput.click());
    fileInput.addEventListener("change", handleBannerImageSelection);
}

function initTargetUrlPreview() {
    const previewBtn = document.getElementById("previewTargetUrl");
    const targetField = document.getElementById("targetUrl");
    if (!previewBtn || !targetField) return;
    previewBtn.addEventListener("click", () => {
        const url = targetField.value.trim();
        if (!url) {
            alert("Hãy nhập URL trước khi mở thử.");
            return;
        }
        const fullUrl = url.startsWith("http")
            ? url
            : `${window.location.origin}${url.startsWith("/") ? "" : "/"}${url}`;
        window.open(fullUrl, "_blank");
    });
}

function initMovieClearButton() {
    const clearBtn = document.getElementById("movieClearBtn");
    if (!clearBtn) return;
    clearBtn.addEventListener("click", () => {
        setMovieSelection(null);
        hideMovieSuggestions();
        updateMovieClearButtonState();
    });
}

function updateMovieClearButtonState() {
    const clearBtn = document.getElementById("movieClearBtn");
    if (!clearBtn) return;
    const hasSelection = !!getSelectedMovieId();
    clearBtn.disabled = !hasSelection;
    clearBtn.classList.toggle("btn-danger", hasSelection);
    clearBtn.classList.toggle("btn-outline-secondary", !hasSelection);
}

function initMovieSearchInput() {
    const searchInput = document.getElementById("movieSearchInput");
    if (!searchInput) return;
    const debounced = createDebounce(() => {
        renderMovieSuggestions(searchInput.value || "");
    }, 200);
    searchInput.addEventListener("input", debounced);
    searchInput.addEventListener("focus", () => {
        renderMovieSuggestions(searchInput.value || "");
    });
    searchInput.addEventListener("blur", (event) => {
        const nextFocus = event.relatedTarget;
        const suggestionFocused = nextFocus?.classList?.contains("movie-suggestion-item");
        if (suggestionFocused) {
            return;
        }
        hideMovieSuggestions();
    });
}

function initPromotionClearButton() {
    const clearBtn = document.getElementById("promotionClearBtn");
    if (!clearBtn) return;
    clearBtn.addEventListener("click", () => {
        setPromotionSelection(null);
        hidePromotionSuggestions();
        updatePromotionClearButtonState();
    });
}

function updatePromotionClearButtonState() {
    const clearBtn = document.getElementById("promotionClearBtn");
    if (!clearBtn) return;
    const hasSelection = !!getSelectedPromotionId();
    clearBtn.disabled = !hasSelection;
    clearBtn.classList.toggle("btn-danger", hasSelection);
    clearBtn.classList.toggle("btn-outline-secondary", !hasSelection);
}

function refreshMovieOptionsFromBus() {
    pendingMovieSelection = getSelectedMovieId();
    loadMovieOptions();
    fetchBanners();
}

function refreshPromotionOptionsFromBus() {
    pendingPromotionSelection = getSelectedPromotionId();
    loadPromotionOptions();
}

async function loadMovieOptions() {
    const suggestionList = document.getElementById("movieSuggestionList");
    if (!suggestionList) return;
    suggestionList.innerHTML = `<p class="text-muted small mb-0">Đang tải danh sách phim...</p>`;
    movieOptionsMap.clear();
    allMovieOptions = [];
    movieSelectReady = false;
    try {
        const response = await fetch("/api/movies/options");
        if (!response.ok) throw new Error("Không thể tải danh sách phim");
        const data = await response.json();
        allMovieOptions = Array.isArray(data) ? data : [];
        allMovieOptions.forEach((movie) => {
            movieOptionsMap.set(String(movie.id), movie);
        });
        renderMovieSuggestions(document.getElementById("movieSearchInput")?.value || "");
    } catch (error) {
        suggestionList.innerHTML = `<p class="text-warning small mb-0">${error.message}</p>`;
    } finally {
        movieSelectReady = true;
        if (pendingMovieSelection !== null) {
            setMovieSelection(pendingMovieSelection);
            pendingMovieSelection = null;
        } else {
            updateMoviePreview(getSelectedMovieId());
        }
    }
}

function populateMovieSelect(movies) {
    allMovieOptions = Array.isArray(movies) ? movies.slice() : [];
    const searchTerm = document.getElementById("movieSearchInput")?.value || "";
    renderMovieSuggestions(searchTerm);
}

function renderMovieSuggestions(filterKeyword = "") {
    const list = document.getElementById("movieSuggestionList");
    if (!list) return;
    const normalized = (filterKeyword || "").trim().toLowerCase();
    if (!normalized) {
        list.innerHTML = "";
        list.classList.add("movie-suggestion-list--hidden");
        return;
    }

    const matches = allMovieOptions.filter((movie) => {
        const title = (movie.title || "").toLowerCase();
        const original = (movie.originalTitle || "").toLowerCase();
        return title.includes(normalized) || original.includes(normalized);
    });

    if (!matches.length) {
        list.innerHTML = `<p class="text-warning small mb-0">Không tìm thấy phim "${filterKeyword}".</p>`;
        list.classList.remove("movie-suggestion-list--hidden");
        return;
    }

    list.innerHTML = matches
        .slice(0, 8)
        .map((movie) => {
            const original = movie.originalTitle
                ? `<span class="movie-suggestion-secondary">${movie.originalTitle}</span>`
                : "";
            return `
                <button type="button" class="movie-suggestion-item" data-movie-id="${movie.id}">
                    <span class="movie-suggestion-primary">${movie.title || "Phim không tên"}</span>
                    ${original}
                    <span class="movie-suggestion-status">${statusLabel(movie.status)}</span>
                </button>
            `;
        })
        .join("");

    list.querySelectorAll(".movie-suggestion-item").forEach((button) => {
        button.addEventListener("click", () => {
            const movieId = Number(button.dataset.movieId);
            setMovieSelection(Number.isFinite(movieId) ? movieId : null);
            hideMovieSuggestions();
        });
    });
    list.classList.remove("movie-suggestion-list--hidden");
}

function hideMovieSuggestions() {
    const list = document.getElementById("movieSuggestionList");
    if (!list) return;
    list.innerHTML = "";
    list.classList.add("movie-suggestion-list--hidden");
}

function statusLabel(status) {
    switch ((status || "").toLowerCase()) {
        case "nowshowing":
            return "Đang chiếu";
        case "comingsoon":
            return "Sắp chiếu";
        case "stopped":
            return "Ngừng chiếu";
        default:
            return status || "";
    }
}

function getSelectedMovieId() {
    if (!movieSelectReady) {
        return pendingMovieSelection ?? null;
    }
    return selectedMovieId;
}

function setMovieSelection(movieId) {
    if (!movieSelectReady) {
        pendingMovieSelection = movieId ?? null;
        return;
    }
    selectedMovieId = movieId ?? null;
    const searchInput = document.getElementById("movieSearchInput");
    if (searchInput) {
        if (movieId) {
            const movie = movieOptionsMap.get(String(movieId));
            searchInput.value = movie
                ? movie.title || movie.originalTitle || ""
                : "";
        } else {
            searchInput.value = "";
        }
    }
    updateMoviePreview(movieId);
    updateMovieClearButtonState();
}

function updateMoviePreview(movieId) {
    const preview = document.getElementById("moviePreview");
    if (!preview) return;
    if (!movieId) {
        preview.classList.add("movie-preview--hidden");
        preview.innerHTML = "";
        return;
    }
    preview.classList.remove("movie-preview--hidden");
    const movie = movieOptionsMap.get(String(movieId));
    if (!movie) {
        preview.innerHTML = `<p class="text-warning mb-0">Không tìm thấy phim #${movieId}. Hãy chọn lại.</p>`;
        return;
    }
    const poster = movie.posterUrl || "https://via.placeholder.com/64x96.png?text=Poster";
    const originalLine = movie.originalTitle && movie.originalTitle.trim()
        ? `<span class="movie-preview-original">${movie.originalTitle}</span>`
        : "";
    preview.innerHTML = `
        <img src="${poster}" alt="${movie.title}">
        <div class="movie-preview-info">
            <h6>${movie.title}</h6>
            ${originalLine}
            <span>Trạng thái: ${statusLabel(movie.status)}</span>
        </div>
    `;
}

async function loadPromotionOptions() {
    const list = document.getElementById("promotionSuggestionList");
    if (!list) return;
    list.innerHTML = `<p class="text-muted small mb-0">Đang tải danh sách khuyến mãi...</p>`;
    promotionOptionsMap.clear();
    allPromotionOptions = [];
    promotionSelectReady = false;
    try {
        const response = await fetch("/api/admin/promotions/options");
        if (!response.ok) {
            throw new Error("Không thể tải danh sách khuyến mãi");
        }
        const data = await response.json();
        allPromotionOptions = Array.isArray(data) ? data : [];
        allPromotionOptions.forEach((promo) => {
            promotionOptionsMap.set(String(promo.id), promo);
        });
        renderPromotionSuggestions(document.getElementById("promotionSearchInput")?.value || "");
    } catch (error) {
        list.innerHTML = `<p class="text-warning small mb-0">${error.message}</p>`;
    } finally {
        promotionSelectReady = true;
        if (pendingPromotionSelection !== null) {
            setPromotionSelection(pendingPromotionSelection);
            pendingPromotionSelection = null;
        } else {
            updatePromotionPreview(getSelectedPromotionId());
        }
    }
}

function renderPromotionSuggestions(filterKeyword = "") {
    const list = document.getElementById("promotionSuggestionList");
    if (!list) return;
    const normalized = (filterKeyword || "").trim().toLowerCase();
    if (!normalized) {
        list.innerHTML = "";
        list.classList.add("movie-suggestion-list--hidden");
        return;
    }

    const matches = allPromotionOptions.filter((promo) => {
        const title = (promo.title || "").toLowerCase();
        const slug = (promo.slug || "").toLowerCase();
        return title.includes(normalized) || slug.includes(normalized);
    });

    if (!matches.length) {
        list.innerHTML = `<p class="text-warning small mb-0">Không tìm thấy khuyến mãi "${filterKeyword}".</p>`;
        list.classList.remove("movie-suggestion-list--hidden");
        return;
    }

    list.innerHTML = matches
        .slice(0, 8)
        .map((promo) => `
            <button type="button" class="movie-suggestion-item" data-promo-id="${promo.id}">
                <span class="movie-suggestion-primary">${promo.title || `Khuyến mãi #${promo.id}`}</span>
            </button>
        `)
        .join("");

    list.querySelectorAll(".movie-suggestion-item").forEach((button) => {
        button.addEventListener("click", () => {
            const promoId = Number(button.dataset.promoId);
            setPromotionSelection(Number.isFinite(promoId) ? promoId : null);
            hidePromotionSuggestions();
        });
    });
    list.classList.remove("movie-suggestion-list--hidden");
}

function hidePromotionSuggestions() {
    const list = document.getElementById("promotionSuggestionList");
    if (!list) return;
    list.innerHTML = "";
    list.classList.add("movie-suggestion-list--hidden");
}

function initPromotionSearchInput() {
    const searchInput = document.getElementById("promotionSearchInput");
    if (!searchInput) return;
    const debounced = createDebounce(() => {
        renderPromotionSuggestions(searchInput.value || "");
    }, 200);
    searchInput.addEventListener("input", debounced);
    searchInput.addEventListener("focus", () => {
        renderPromotionSuggestions(searchInput.value || "");
    });
    searchInput.addEventListener("blur", (event) => {
        const nextFocus = event.relatedTarget;
        if (nextFocus && nextFocus.classList?.contains("movie-suggestion-item")) {
            return;
        }
        hidePromotionSuggestions();
    });
}

function getSelectedPromotionId() {
    if (!promotionSelectReady) {
        return pendingPromotionSelection ?? null;
    }
    return selectedPromotionId;
}

function setPromotionSelection(promotionId) {
    if (!promotionSelectReady) {
        pendingPromotionSelection = promotionId ?? null;
        return;
    }
    selectedPromotionId = promotionId ?? null;
    const searchInput = document.getElementById("promotionSearchInput");
    if (searchInput) {
        if (promotionId) {
            const promo = promotionOptionsMap.get(String(promotionId));
            if (promo) {
                searchInput.value = promo.title || `Khuyến mãi #${promo.id}`;
            } else {
                searchInput.value = "";
            }
        } else {
            searchInput.value = "";
        }
    }
    updatePromotionPreview(promotionId);
    updatePromotionClearButtonState();
}

function updatePromotionPreview(promotionId) {
    const preview = document.getElementById("promotionPreview");
    if (!preview) return;
    if (!promotionId) {
        preview.classList.add("movie-preview--hidden");
        preview.innerHTML = "";
        return;
    }
    preview.classList.remove("movie-preview--hidden");
    const promotion = promotionOptionsMap.get(String(promotionId));
    if (!promotion) {
        preview.innerHTML = `<p class="text-warning mb-0">Không tìm thấy khuyến mãi #${promotionId}.</p>`;
        return;
    }
    preview.innerHTML = `
        <div class="promo-preview-info">
            <h6>${promotion.title ?? "-"}</h6>
        </div>
    `;
}

function toggleLinkInputs() {
    const linkType = document.getElementById("linkType")?.value ?? "MOVIE";
    const movieGroup = document.getElementById("movieSelectGroup");
    const promotionGroup = document.getElementById("promotionSelectGroup");
    const targetGroup = document.getElementById("targetUrlGroup");
    const targetField = document.getElementById("targetUrl");
    const showMovie = linkType === "MOVIE";
    const showPromo = linkType === "PROMO";
    const showUrl = linkType === "URL";

    toggleGroupVisibility(movieGroup, showMovie);
    toggleGroupVisibility(promotionGroup, showPromo);
    toggleGroupVisibility(targetGroup, showUrl);

    if (!showMovie) {
        setMovieSelection(null);
        hideMovieSuggestions();
    }
    if (!showPromo) {
        setPromotionSelection(null);
        hidePromotionSuggestions();
    }

    if (targetField) {
        if (showUrl) {
            targetField.removeAttribute("disabled");
        } else {
            targetField.value = "";
            targetField.setAttribute("disabled", "disabled");
        }
    }
    updateMovieClearButtonState();
    updatePromotionClearButtonState();
}

function toggleGroupVisibility(group, shouldShow) {
    if (!group) return;
    if (shouldShow) {
        group.classList.remove("d-none");
    } else {
        group.classList.add("d-none");
    }
}

async function fetchBanners() {
    const tbody = document.querySelector("#bannerTable tbody");
    const counter = document.getElementById("bannerCount");
    if (!tbody || !counter) return;

    tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">Đang tải dữ liệu...</td></tr>`;
    try {
        const keywordInput = document.getElementById("bannerSearchKeyword");
        const statusSelect = document.getElementById("bannerStatusFilter");
        const keyword = keywordInput?.value.trim();
        const params = new URLSearchParams();
        if (keyword) {
            params.append("keyword", keyword);
        }
        if (statusSelect?.value) {
            params.append("active", statusSelect.value);
        }
        const url = params.toString() ? `${adminBannersApi.list}?${params.toString()}` : adminBannersApi.list;
        const response = await fetch(url);
        if (!response.ok) throw new Error("Không thể tải danh sách banner");
        const data = await response.json();
        renderBannerTable(data);
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4">${error.message}</td></tr>`;
        counter.textContent = "0 items";
    }
}

function renderBannerTable(banners) {
    const tbody = document.querySelector("#bannerTable tbody");
    const counter = document.getElementById("bannerCount");
    if (!tbody || !counter) return;

    if (!Array.isArray(banners) || banners.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-white">Chưa có banner nào</td></tr>`;
        counter.textContent = "0 items";
        return;
    }

    counter.textContent = `${banners.length} item(s)`;
    tbody.innerHTML = "";
    const sortedBanners = banners.slice().sort((a, b) =>
        bannerTextCompare.compare(resolveBannerDisplayName(a) || "", resolveBannerDisplayName(b) || "")
    );
    sortedBanners.forEach((banner, index) => {
        const displayText = resolveBannerDisplayName(banner);
        const titleHtml = buildBannerTitleHtml(banner, displayText);
        const encodedName = encodeURIComponent(displayText || "");
        const toggleLabel = banner.isActive ? "Vô hiệu hóa" : "Kích hoạt";
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${index + 1}</td>
            <td>${titleHtml}</td>
            <td>${banner.linkType || "-"}</td>
            <td>${banner.sortOrder ?? "-"}</td>
            <td>${banner.isActive ? '<span class="badge bg-success">ON</span>' : '<span class="badge bg-secondary">OFF</span>'}</td>
            <td>${formatDateRange(banner.startDate, banner.endDate)}</td>
            <td>
                <div class="user-action-menu-wrapper">
                    <button type="button"
                            class="btn btn-outline-light btn-sm action-menu-toggle"
                            aria-haspopup="true"
                            aria-expanded="false"
                            title="Mở hành động">⋮</button>
                    <div class="user-action-menu" role="menu">
                        <button type="button" data-banner-edit="${banner.id}" data-menu-role="edit">Sửa</button>
                        <button type="button"
                                data-banner-toggle="${banner.id}"
                                data-banner-target-active="${banner.isActive ? "false" : "true"}"
                                data-banner-name="${encodedName}"
                                data-menu-role="toggle">${toggleLabel}</button>
                        <button type="button"
                                data-banner-delete="${banner.id}"
                                data-banner-name="${encodedName}"
                                data-menu-role="delete">Xóa</button>
                    </div>
                </div>
            </td>`;
        tbody.appendChild(tr);
    });
    window.AdminActionMenus?.init(tbody);
    tbody.querySelectorAll("[data-banner-edit]").forEach((btn) => {
        btn.addEventListener("click", () => {
            window.AdminActionMenus?.closeAll();
            editBanner(Number(btn.dataset.bannerEdit));
        });
    });
    tbody.querySelectorAll("[data-banner-toggle]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const id = Number(btn.dataset.bannerToggle);
            const shouldActivate = btn.dataset.bannerTargetActive === "true";
            const title = decodeURIComponent(btn.dataset.bannerName || "");
            window.AdminActionMenus?.closeAll();
            confirmToggleBanner(id, shouldActivate, title);
        });
    });
    tbody.querySelectorAll("[data-banner-delete]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const id = Number(btn.dataset.bannerDelete);
            const title = decodeURIComponent(btn.dataset.bannerName || "");
            window.AdminActionMenus?.closeAll();
            deleteBanner(id, title);
        });
    });
}

function formatDateRange(start, end) {
    if (!start && !end) return "-";
    const formattedStart = formatDateDisplay(start);
    const formattedEnd = formatDateDisplay(end);
    if (start && end) return `${formattedStart} -> ${formattedEnd}`;
    return start ? `Từ ${formattedStart}` : `Đến ${formattedEnd}`;
}

function formatDateDisplay(value) {
    if (!value) {
        return "-";
    }
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
    if (match) {
        return `${match[3]}/${match[2]}/${match[1]}`;
    }
    return value;
}
async function submitBannerForm(event) {
    event.preventDefault();
    if (isSubmittingBanner) return;
    isSubmittingBanner = true;

    const submitBtn = document.querySelector("#bannerForm button[type='submit']");
    if (submitBtn && !submitBtn.dataset.originalHtml) {
        submitBtn.dataset.originalHtml = submitBtn.innerHTML;
    }
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = `<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang lưu...`;
    }

    clearBannerErrors();
    const payload = buildPayloadFromForm();
    const validation = validateBannerForm(payload);
    const messageEl = document.getElementById("formMessage");
    messageEl.textContent = "";

    if (validation.errors.length > 0) {
        showBannerErrors(validation.errors);
        if (validation.firstInvalid) {
            validation.firstInvalid.scrollIntoView({ behavior: "smooth", block: "center" });
            validation.firstInvalid.focus({ preventScroll: true });
        }
        isSubmittingBanner = false;
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = submitBtn.dataset.originalHtml || "Lưu banner";
        }
        return;
    }

    const bannerId = document.getElementById("bannerId").value;
    const url = bannerId ? adminBannersApi.update(bannerId) : adminBannersApi.create;
    const method = bannerId ? "PUT" : "POST";

    try {
        const response = await fetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || "Không thể lưu banner");
        }
        resetFormState();
        fetchBanners();
        bannerDataBus.dispatch("banners");
        showSuccessToast("Lưu thành công!");
        window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
        messageEl.textContent = error.message;
        messageEl.classList.remove("text-success");
        messageEl.classList.add("text-warning");
    } finally {
        isSubmittingBanner = false;
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = submitBtn.dataset.originalHtml || "Lưu banner";
        }
    }
}

function buildPayloadFromForm() {
    const linkType = document.getElementById("linkType").value;
    const selectedMovieId = getSelectedMovieId();
    const selectedPromotionId = getSelectedPromotionId();
    const targetValue = document.getElementById("targetUrl").value.trim();
    return {
        imagePath: document.getElementById("imagePath").value.trim(),
        linkType,
        movieId: linkType === "MOVIE" ? selectedMovieId : null,
        promotionId: linkType === "PROMO" ? selectedPromotionId : null,
        targetUrl: linkType === "URL" ? (targetValue || null) : null,
        sortOrder: Number(document.getElementById("sortOrder").value) || null,
        isActive: document.getElementById("isActive").checked,
        startDate: document.getElementById("startDate").value || null,
        endDate: document.getElementById("endDate").value || null
    };
}

async function editBanner(id) {
    window.activateAdminTab?.("crud-banners");
    try {
        const response = await fetch(adminBannersApi.detail(id));
        if (!response.ok) throw new Error("Không tìm thấy banner");
        const data = await response.json();
        fillFormWithBanner(data);
        isEditing = true;
        const bannerName = resolveBannerDisplayName(data);
        document.getElementById("formTitle").textContent = `Chỉnh sửa banner - ${bannerName}`;
        document.getElementById("formMessage").textContent = "";
    } catch (error) {
        alert(error.message);
    }
}

function fillFormWithBanner(banner) {
    document.getElementById("bannerId").value = banner.id;
    document.getElementById("imagePath").value = banner.imagePath || "";
    document.getElementById("linkType").value = banner.linkType || "MOVIE";
    document.getElementById("targetUrl").value = banner.targetUrl || "";
    document.getElementById("sortOrder").value = banner.sortOrder ?? 1;
    document.getElementById("isActive").checked = !!banner.isActive;
    document.getElementById("startDate").value = banner.startDate || "";
    document.getElementById("endDate").value = banner.endDate || "";
    toggleLinkInputs();
    setMovieSelection(banner.linkType === "MOVIE" ? (banner.movieId ?? null) : null);
    setPromotionSelection(banner.linkType === "PROMO" ? (banner.promotionId ?? null) : null);
    updateImageHint(banner.imagePath || "");
    updateMovieClearButtonState();
    updatePromotionClearButtonState();
}

function resetFormState() {
    isEditing = false;
    const form = document.getElementById("bannerForm");
    if (!form) return;
    form.reset();
    document.getElementById("bannerId").value = "";
    document.getElementById("formTitle").textContent = "Thêm banner mới";
    document.getElementById("formMessage").textContent = "";
    document.getElementById("isActive").checked = true;
    setMovieSelection(null);
    setPromotionSelection(null);
    toggleLinkInputs();
    updateImageHint("");
    clearBannerErrors();
    updateMovieClearButtonState();
    updatePromotionClearButtonState();
}

function confirmToggleBanner(id, shouldActivate, title) {
    const bannerName = title || "banner này";
    const actionLabel = shouldActivate ? "kích hoạt" : "vô hiệu hóa";
    const confirmAction = () => toggleBannerActive(id, shouldActivate);
    if (typeof openAdminConfirmDialog === "function") {
        openAdminConfirmDialog({
            title: `${shouldActivate ? "Kích hoạt" : "Vô hiệu hóa"} banner`,
            message: `Bạn có chắc muốn ${actionLabel} ${bannerName}?`,
            confirmLabel: "Xác nhận",
            confirmVariant: shouldActivate ? "primary" : "danger",
            cancelLabel: "Hủy",
            onConfirm: confirmAction
        });
    } else if (confirm(`Bạn có chắc muốn ${actionLabel} ${bannerName}?`)) {
        confirmAction();
    }
}

async function toggleBannerActive(id, shouldActivate) {
    try {
        const response = await fetch(adminBannersApi.toggleActive(id, shouldActivate), { method: "PATCH" });
        if (!response.ok) {
            throw new Error("Không thể cập nhật trạng thái banner");
        }
        fetchBanners();
        bannerDataBus.dispatch("banners");
    } catch (error) {
        alert(error.message);
    }
}

function deleteBanner(id, title) {
    const displayName = title || "banner này";
    if (typeof openAdminConfirmDialog === "function") {
        openAdminConfirmDialog({
            title: "Xóa banner",
            message: "bạn chắc chứ ?",
            confirmLabel: "Xác nhận",
            confirmVariant: "danger",
            cancelLabel: "Hủy",
            onConfirm: () => performDeleteBanner(id)
        });
    } else if (confirm("bạn chắc chứ ?")) {
        performDeleteBanner(id);
    }
}

async function performDeleteBanner(id) {
    try {
        const response = await fetch(adminBannersApi.delete(id), { method: "DELETE" });
        if (!response.ok) throw new Error("Xóa thất bại");
        fetchBanners();
        bannerDataBus.dispatch("banners");
    } catch (error) {
        alert(error.message);
    }
}

async function handleBannerImageSelection(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    const hint = document.getElementById("imagePathHint");
    if (hint) {
        hint.textContent = "Đang tải ảnh...";
        hint.classList.remove("text-warning");
    }
    try {
        const path = await uploadBannerImage(file);
        document.getElementById("imagePath").value = path;
        updateImageHint(path);
        event.target.value = "";
    } catch (error) {
        if (hint) {
            hint.textContent = error.message || "Không thể tải ảnh.";
            hint.classList.add("text-warning");
        }
    }
}

async function uploadBannerImage(file) {
    const formData = new FormData();
    formData.append("file", file);
    const response = await fetch("/api/admin/uploads/banner", {
        method: "POST",
        body: formData
    });
    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "Không thể tải ảnh.");
    }
    const data = await response.json();
    return data.path;
}

function updateImageHint(path) {
    const hint = document.getElementById("imagePathHint");
    if (!hint) return;
    if (!path) {
        hint.textContent = "Chưa có ảnh nào được tải lên.";
        hint.classList.remove("text-warning");
        return;
    }
    hint.textContent = `Ảnh hiện tại: ${path}`;
    hint.classList.remove("text-warning");
}

function validateBannerForm(payload) {
    const errors = [];
    let firstInvalid = null;
    const registerError = (fieldId, errorId, message) => {
        errors.push({ fieldId, errorId, message });
        if (!firstInvalid) {
            firstInvalid = document.getElementById(fieldId);
        }
    };
    if (!payload.imagePath) {
        registerError("imagePath", "imagePathError", "Vui lòng chọn Đường dẫn ảnh *");
    }
    if (!payload.linkType) {
        registerError("linkType", "linkTypeError", "Vui lòng chọn Loại liên kết *");
    } else if (payload.linkType === "MOVIE" && !payload.movieId) {
        registerError("movieSearchInput", "movieSelectError", "Vui lòng chọn phim *");
    } else if (payload.linkType === "PROMO" && !payload.promotionId) {
        registerError("promotionSearchInput", "promotionSelectError", "Vui lòng chọn khuyến mãi *");
    } else if (payload.linkType === "URL" && !payload.targetUrl) {
        registerError("targetUrl", "targetUrlError", "Vui lòng nhập Target URL *");
    }
    if (payload.sortOrder === null || Number.isNaN(payload.sortOrder)) {
        registerError("sortOrder", "sortOrderError", "Vui lòng chọn thứ tự");
    } else if (payload.sortOrder < 1) {
        registerError("sortOrder", "sortOrderError", "Thứ tự phải lớn hơn hoặc bằng 1*");
    } else if (payload.sortOrder > 100) {
        registerError("sortOrder", "sortOrderError", "Thứ tự tối đa là 100*");
    }
    return { errors, firstInvalid };
}

function clearBannerErrors() {
    ["imagePath", "linkType", "movieSearchInput", "promotionSearchInput", "targetUrl", "sortOrder"].forEach((id) => {
        document.getElementById(id)?.classList.remove("is-invalid");
    });
    ["imagePathError", "linkTypeError", "movieSelectError", "promotionSelectError", "targetUrlError", "sortOrderError"].forEach((id) => {
        const el = document.getElementById(id);
        if (el) {
            el.textContent = "";
        }
    });
}

function showBannerErrors(errors) {
    errors.forEach((err) => {
        document.getElementById(err.fieldId)?.classList.add("is-invalid");
        const hint = document.getElementById(err.errorId);
        if (hint) {
            hint.textContent = err.message;
        }
    });
}

function attachNumericOnlyHandlers(context = document) {
    context.querySelectorAll("[data-numeric-only='true']").forEach((input) => {
        if (input.dataset.numericBound === "true") {
            return;
        }
        input.dataset.numericBound = "true";
        input.addEventListener("input", () => {
            const digits = input.value.replace(/\D+/g, "");
            const maxLength = input.getAttribute("maxlength");
            const limited = maxLength ? digits.slice(0, Number(maxLength)) : digits;
            if (input.value !== limited) {
                input.value = limited;
            }
        });
    });
}

function initNumericStepperControls(context = document) {
    context.querySelectorAll(".numeric-stepper").forEach((wrapper) => {
        if (wrapper.dataset.stepperInit === "true") {
            return;
        }
        const input = wrapper.querySelector("input[type='number']");
        if (!input) {
            return;
        }
        wrapper.dataset.stepperInit = "true";
        wrapper.querySelectorAll(".numeric-stepper-btn").forEach((button) => {
            const direction = Number(button.dataset.step || 1);
            button.addEventListener("click", () => adjustNumericInputValue(input, direction));
        });
    });
}

function adjustNumericInputValue(input, direction) {
    const inputStep = Number(input.step) || 1;
    const min = input.min !== "" ? Number(input.min) : null;
    const max = input.max !== "" ? Number(input.max) : null;
    let currentValue = Number(input.value);
    if (Number.isNaN(currentValue)) {
        currentValue = min !== null ? min : 0;
    }
    let nextValue = currentValue + direction * inputStep;
    if (min !== null) {
        nextValue = Math.max(nextValue, min);
    }
    if (max !== null) {
        nextValue = Math.min(nextValue, max);
    }
    input.value = String(nextValue);
    input.dispatchEvent(new Event("input", { bubbles: true }));
    input.dispatchEvent(new Event("change", { bubbles: true }));
}

function bindBannerFilters() {
    const keywordInput = document.getElementById("bannerSearchKeyword");
    const statusSelect = document.getElementById("bannerStatusFilter");
    const debouncedSearch = createDebounce(() => fetchBanners(), 300);
    keywordInput?.addEventListener("input", debouncedSearch);
    statusSelect?.addEventListener("change", () => fetchBanners());
}
