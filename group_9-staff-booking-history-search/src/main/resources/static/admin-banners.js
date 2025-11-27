const adminBannersApi = {
    list: "/api/admin/banners",
    detail: (id) => `/api/admin/banners/${id}`,
    create: "/api/admin/banners",
    update: (id) => `/api/admin/banners/${id}`,
    delete: (id) => `/api/admin/banners/${id}`
};

const movieOptionsMap = new Map();
let movieSelectReady = false;
let pendingMovieSelection = null;
let isEditing = false;
let isSubmittingBanner = false;

document.addEventListener("DOMContentLoaded", () => {
    initImageUploadControls();
    initTargetUrlPreview();
    initMovieClearButton();
    attachNumericOnlyHandlers();
    loadMovieOptions();

    const form = document.getElementById("bannerForm");
    const resetBtn = document.getElementById("resetFormBtn");
    const linkTypeSelect = document.getElementById("linkType");

    if (!form || !resetBtn || !linkTypeSelect) {
        return;
    }

    fetchBanners();
    form.addEventListener("submit", submitBannerForm);
    resetBtn.addEventListener("click", resetFormState);
    linkTypeSelect.addEventListener("change", toggleLinkInputs);
    toggleLinkInputs();
    updateImageHint(document.getElementById("imagePath")?.value ?? "");
    updateClearButtonState();
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
        updateClearButtonState();
    });
}

function updateClearButtonState() {
    const clearBtn = document.getElementById("movieClearBtn");
    if (!clearBtn) return;
    const hasSelection = !!getSelectedMovieId();
    clearBtn.disabled = !hasSelection;
    clearBtn.classList.toggle("btn-danger", hasSelection);
    clearBtn.classList.toggle("btn-outline-secondary", !hasSelection);
}

async function loadMovieOptions() {
    const select = document.getElementById("movieSelect");
    if (!select) return;
    select.innerHTML = `<option value="">-- Đang tải danh sách phim --</option>`;
    movieOptionsMap.clear();
    movieSelectReady = false;
    try {
        const response = await fetch("/api/movies/options");
        if (!response.ok) throw new Error("Không thể tải danh sách phim");
        const data = await response.json();
        populateMovieSelect(data);
    } catch (error) {
        select.innerHTML = `<option value="">${error.message}</option>`;
    } finally {
        movieSelectReady = true;
        if (pendingMovieSelection !== null) {
            setMovieSelection(pendingMovieSelection);
            pendingMovieSelection = null;
        }
    }
}

function populateMovieSelect(movies) {
    const select = document.getElementById("movieSelect");
    if (!select) return;
    select.innerHTML = `<option value="">-- Chọn phim đang chiếu hoặc sắp chiếu --</option>`;
    if (!Array.isArray(movies) || movies.length === 0) {
        select.innerHTML = `<option value="">Không có phim phù hợp</option>`;
        return;
    }
    movies.forEach((movie) => {
        movieOptionsMap.set(String(movie.id), movie);
        const option = document.createElement("option");
        option.value = movie.id;
        option.textContent = `${movie.title} (${statusLabel(movie.status)})`;
        select.appendChild(option);
    });
    select.addEventListener("change", () => {
        updateMoviePreview(getSelectedMovieId());
        updateClearButtonState();
    });
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
    const select = document.getElementById("movieSelect");
    if (!select) return null;
    const value = select.value;
    return value ? Number(value) : null;
}

function setMovieSelection(movieId) {
    const select = document.getElementById("movieSelect");
    if (!select) return;
    if (!movieSelectReady) {
        pendingMovieSelection = movieId ?? null;
        return;
    }
    select.value = movieId ? String(movieId) : "";
    updateMoviePreview(movieId);
}

function updateMoviePreview(movieId) {
    const preview = document.getElementById("moviePreview");
    if (!preview) return;
    if (!movieId) {
        preview.innerHTML = `<p class="text-muted mb-0">Chưa chọn phim.</p>`;
        return;
    }
    const movie = movieOptionsMap.get(String(movieId));
    if (!movie) {
        preview.innerHTML = `<p class="text-warning mb-0">Không tìm thấy phim #${movieId}. Hãy chọn lại.</p>`;
        return;
    }
    const poster = movie.posterUrl || "https://via.placeholder.com/64x96.png?text=Poster";
    preview.innerHTML = `
        <img src="${poster}" alt="${movie.title}">
        <div class="movie-preview-info">
            <h6>${movie.title}</h6>
            <span>Trạng thái: ${statusLabel(movie.status)}</span>
            <span>ID: ${movie.id}</span>
        </div>
    `;
}

function toggleLinkInputs() {
    const linkType = document.getElementById("linkType")?.value ?? "MOVIE";
    const movieGroup = document.getElementById("movieSelectGroup");
    const targetGroup = document.getElementById("targetUrlGroup");
    const targetField = document.getElementById("targetUrl");

    if (linkType === "MOVIE") {
        if (movieGroup) movieGroup.style.display = "block";
        if (targetGroup) targetGroup.style.display = "none";
        if (targetField) {
            targetField.value = "";
            targetField.setAttribute("disabled", "disabled");
        }
    } else {
        if (movieGroup) movieGroup.style.display = "none";
        if (targetGroup) targetGroup.style.display = "block";
        if (targetField) {
            targetField.removeAttribute("disabled");
        }
    }
}

async function fetchBanners() {
    const tbody = document.querySelector("#bannerTable tbody");
    const counter = document.getElementById("bannerCount");
    if (!tbody || !counter) return;

    tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">Đang tải dữ liệu...</td></tr>`;
    try {
        const response = await fetch(adminBannersApi.list);
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
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">Chưa có banner nào</td></tr>`;
        counter.textContent = "0 items";
        return;
    }

    counter.textContent = `${banners.length} item(s)`;
    tbody.innerHTML = "";
    banners.forEach((banner, index) => {
        const displayText =
            banner.linkType === "MOVIE"
                ? (banner.movieTitle || `Phim #${banner.movieId ?? "-"}`)
                : (banner.targetUrl || "Khuyến mãi HUB");
        const safeDisplay = JSON.stringify(displayText || "");
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${index + 1}</td>
            <td><div class="fw-semibold">${displayText}</div></td>
            <td>${banner.linkType || "-"}</td>
            <td>${banner.sortOrder ?? "-"}</td>
            <td>${banner.isActive ? '<span class="badge bg-success">ON</span>' : '<span class="badge bg-secondary">OFF</span>'}</td>
            <td>${formatDateRange(banner.startDate, banner.endDate)}</td>
            <td>
                <div class="d-flex gap-2">
                    <button type="button" class="btn btn-outline-light btn-sm" onclick='editBanner(${banner.id})'>Sửa</button>
                    <button type="button" class="btn btn-outline-danger btn-sm" onclick='deleteBanner(${banner.id}, ${safeDisplay})'>Xóa</button>
                </div>
            </td>`;
        tbody.appendChild(tr);
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
    const targetValue = document.getElementById("targetUrl").value.trim();
    return {
        imagePath: document.getElementById("imagePath").value.trim(),
        linkType,
        movieId: linkType === "MOVIE" ? selectedMovieId : null,
        targetUrl: linkType === "URL" ? (targetValue || null) : null,
        sortOrder: Number(document.getElementById("sortOrder").value) || null,
        isActive: document.getElementById("isActive").checked,
        startDate: document.getElementById("startDate").value || null,
        endDate: document.getElementById("endDate").value || null
    };
}

async function editBanner(id) {
    try {
        const response = await fetch(adminBannersApi.detail(id));
        if (!response.ok) throw new Error("Không tìm thấy banner");
        const data = await response.json();
        fillFormWithBanner(data);
        isEditing = true;
        document.getElementById("formTitle").textContent = "Chỉnh sửa banner";
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
    setMovieSelection(banner.movieId ?? null);
    toggleLinkInputs();
    updateImageHint(banner.imagePath || "");
    updateClearButtonState();
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
    toggleLinkInputs();
    updateImageHint("");
    clearBannerErrors();
    updateClearButtonState();
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
        registerError("movieSelect", "movieSelectError", "Vui lòng chọn phim *");
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
    ["imagePath", "linkType", "movieSelect", "targetUrl", "sortOrder"].forEach((id) => {
        document.getElementById(id)?.classList.remove("is-invalid");
    });
    ["imagePathError", "linkTypeError", "movieSelectError", "targetUrlError", "sortOrderError"].forEach((id) => {
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
