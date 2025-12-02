const movieAdminApi = {
    list: "/api/admin/movies",
    detail: (id) => `/api/admin/movies/${id}`,
    create: "/api/admin/movies",
    update: (id) => `/api/admin/movies/${id}`,
    delete: (id) => `/api/admin/movies/${id}`,
    genres: "/api/admin/movies/genres/all"
};

const movieDataBus = window.AdminDataBus || {
    dispatch: () => {},
    subscribe: () => () => {}
};

const AdminDataBus = window.AdminDataBus || {
    dispatch: () => {},
    subscribe: () => () => {}
};

const FALLBACK_GENRES = [
    "Hành động",
    "Phiêu lưu",
    "Hoạt hình",
    "Hài",
    "Tội phạm",
    "Chính kịch",
    "Gia đình",
    "Kinh dị",
    "Ca nhạc",
    "Bí ẩn",
    "Thần thoại",
    "Tâm lý",
    "Tình cảm",
    "Khoa học viễn tưởng",
    "Hồi hộp (Suspense)",
    "Giật gân (Thriller)"
];

let genreOptions = [];
let genreDropdownInitialized = false;

const movieTextCompare = new Intl.Collator("vi", { sensitivity: "base" });

let isSubmittingMovie = false;
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

document.addEventListener("DOMContentLoaded", async () => {
    if (document.getElementById("movieForm")) {
        attachNumericOnlyHandlers();
        await loadGenreOptions();
        initMovieAdminSection();
    }
});

function initMovieAdminSection() {
    const form = document.getElementById("movieForm");
    const resetBtn = document.getElementById("resetMovieFormBtn");
    const posterBtn = document.getElementById("selectMoviePosterBtn");
    const posterFileInput = document.getElementById("moviePosterFile");

    renderGenreCheckboxes();
    fetchMovies();
    bindMovieFilters();

    form?.addEventListener("submit", submitMovieForm);
    resetBtn?.addEventListener("click", resetMovieForm);

    if (posterBtn && posterFileInput) {
        posterBtn.addEventListener("click", () => posterFileInput.click());
        posterFileInput.addEventListener("change", handlePosterUpload);
    }
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

async function fetchMovies() {
    const tbody = document.querySelector("#moviesTable tbody");
    const counter = document.getElementById("movieCount");
    if (!tbody || !counter) return;

    tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-muted">Đang tải dữ liệu...</td></tr>`;
    try {
        const keywordInput = document.getElementById("movieSearchKeyword");
        const statusSelect = document.getElementById("movieStatusFilter");
        const genreSelect = document.getElementById("movieGenreFilter");
        const orderSelect = document.getElementById("movieDurationOrder");
        const ageSelect = document.getElementById("movieAgeFilter");
        const keyword = keywordInput?.value.trim();
        const params = new URLSearchParams();
        if (keyword) {
            params.append("keyword", keyword);
        }
        if (statusSelect?.value) {
            params.append("status", statusSelect.value);
        }
        if (genreSelect?.value) {
            params.append("genre", genreSelect.value);
        }
        if (orderSelect?.value) {
            params.append("durationOrder", orderSelect.value);
        }
        if (ageSelect?.value) {
            params.append("ageRating", ageSelect.value);
        }
        const url = params.toString() ? `${movieAdminApi.list}?${params.toString()}` : movieAdminApi.list;
        const response = await fetch(url);
        if (!response.ok) throw new Error("Không thể tải danh sách phim");
        const data = await response.json();
        renderMovieTable(data);
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger py-4">${error.message}</td></tr>`;
        counter.textContent = "0 items";
    }
}

function renderMovieTable(movies) {
    const tbody = document.querySelector("#moviesTable tbody");
    const counter = document.getElementById("movieCount");
    if (!tbody || !counter) return;

    if (!Array.isArray(movies) || movies.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-white">Chưa có phim nào</td></tr>`;
        counter.textContent = "0 items";
        return;
    }

    counter.textContent = `${movies.length} items`;
    tbody.innerHTML = "";

    const sortedMovies = movies.slice().sort((a, b) =>
        movieTextCompare.compare(a?.title || "", b?.title || "")
    );
    sortedMovies.forEach((movie, index) => {
        const tr = document.createElement("tr");
        const safeTitle = JSON.stringify(movie.title || "");
        const releaseDate = formatDateDisplay(movie.releaseDate);
        const endDate = formatDateDisplay(movie.endDate);
        const originalTitle = movie.originalTitle && movie.originalTitle.trim()
            ? `<div class="movie-table-secondary">${movie.originalTitle}</div>`
            : "";
        tr.innerHTML = `
            <td>${index + 1}</td>
            <td>
                <div class="movie-table-primary">${movie.title || "-"}</div>
                ${originalTitle}
                <div class="small">
                    <span class="badge ${statusBadgeClass(movie.status)}">${statusLabel(movie.status)}</span>
                </div>
            </td>
            <td>${movie.durationMinutes ?? "-"} phút</td>
            <td>${formatAgeRating(movie.ageRating)}</td>
            <td>${releaseDate}</td>
            <td>${endDate}</td>
            <td>${(movie.genres || []).join(", ") || "-"}</td>
            <td>
                <div class="user-action-menu-wrapper">
                    <button type="button"
                            class="btn btn-outline-light btn-sm action-menu-toggle"
                            aria-haspopup="true"
                            aria-expanded="false"
                            title="Mở hành động">⋮</button>
                    <div class="user-action-menu" role="menu">
                        <button type="button" data-movie-edit="${movie.id}" data-menu-role="edit">Sửa</button>
                        <button type="button"
                                data-movie-delete="${movie.id}"
                                data-menu-role="delete">Xóa</button>
                    </div>
                </div>
            </td>`;
        tbody.appendChild(tr);
    });
    window.AdminActionMenus?.init(tbody);
    tbody.querySelectorAll("[data-movie-edit]").forEach((btn) => {
        btn.addEventListener("click", () => {
            window.AdminActionMenus?.closeAll();
            editMovie(Number(btn.dataset.movieEdit));
        });
    });
    tbody.querySelectorAll("[data-movie-delete]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const id = Number(btn.dataset.movieDelete);
            window.AdminActionMenus?.closeAll();
            deleteMovie(id);
        });
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

function statusBadgeClass(status) {
    switch ((status || "").toLowerCase()) {
        case "nowshowing":
            return "bg-success";
        case "comingsoon":
            return "bg-info text-dark";
        case "stopped":
            return "bg-danger";
        default:
            return "bg-secondary";
    }
}

async function editMovie(id) {
    try {
        const response = await fetch(movieAdminApi.detail(id));
        if (!response.ok) throw new Error("Không tìm thấy phim");
        const data = await response.json();
        fillMovieForm(data);
    } catch (error) {
        alert(error.message);
    }
}

function fillMovieForm(movie) {
    const movieTitle = movie.title || movie.originalTitle || "Chỉnh sửa phim";
    document.getElementById("movieFormTitle").textContent = `Chỉnh sửa phim - ${movieTitle}`;
    document.getElementById("movieFormMessage").textContent = "";
    document.getElementById("movieId").value = movie.id;
    document.getElementById("movieTitle").value = movie.title || "";
    document.getElementById("movieOriginalTitle").value = movie.originalTitle || "";
    document.getElementById("movieDescription").value = movie.description || "";
    document.getElementById("movieDuration").value = movie.durationMinutes ?? "";
    document.getElementById("movieAgeRating").value = movie.ageRating || "";
    document.getElementById("movieReleaseDate").value = movie.releaseDate || "";
    document.getElementById("movieEndDate").value = movie.endDate || "";
    document.getElementById("movieImdbUrl").value = movie.imdbUrl || "";
    document.getElementById("movieTrailerUrl").value = movie.trailerUrl || "";
    document.getElementById("movieTrailerEmbedUrl").value = movie.trailerEmbedUrl || "";
    document.getElementById("moviePoster").value = movie.posterUrl || "";
    setSelectedGenres(movie.genres || []);
    document.getElementById("movieDirectors").value = (movie.directors || []).join(", ");
    document.getElementById("movieActors").value = (movie.actors || []).join(", ");
    const hint = document.getElementById("moviePosterHint");
    if (hint) hint.textContent = movie.posterUrl ? `Đang dùng: ${movie.posterUrl}` : "Chưa có poster nào";
}

function resetMovieForm() {
    const form = document.getElementById("movieForm");
    if (!form) return;
    form.reset();
    document.getElementById("movieId").value = "";
    document.getElementById("movieFormTitle").textContent = "Thêm phim mới";
    document.getElementById("movieFormMessage").textContent = "";
    document.getElementById("moviePoster").value = "";
    document.getElementById("moviePosterHint").textContent = "Chưa có poster nào";
    setSelectedGenres([]);
    clearFieldErrors();
}

async function submitMovieForm(event) {
    event.preventDefault();
    if (isSubmittingMovie) return;
    isSubmittingMovie = true;

    const submitBtn = document.querySelector("#movieForm button[type='submit']");
    if (submitBtn && !submitBtn.dataset.originalHtml) {
        submitBtn.dataset.originalHtml = submitBtn.innerHTML;
    }
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = `<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang lưu...`;
    }

    const messageBox = document.getElementById("movieFormMessage");
    messageBox.textContent = "";
    clearFieldErrors();

    const validation = validateRequiredFields();
    if (validation.errors.length > 0) {
        showFieldErrors(validation.errors);
        if (validation.firstInvalid) {
            validation.firstInvalid.scrollIntoView({ behavior: "smooth", block: "center" });
            validation.firstInvalid.focus({ preventScroll: true });
        }
        isSubmittingMovie = false;
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = submitBtn.dataset.originalHtml || "Lưu phim";
        }
        return;
    }

    const payload = buildMoviePayload();
    const movieId = document.getElementById("movieId").value;
    const url = movieId ? movieAdminApi.update(movieId) : movieAdminApi.create;
    const method = movieId ? "PUT" : "POST";

    try {
        const response = await fetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || "Không thể lưu phim");
        }
        await fetchMovies();
        resetMovieForm();
        movieDataBus.dispatch("movies");
        showSuccessToast("Lưu thành công!");
        window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
        messageBox.textContent = error.message;
        messageBox.classList.remove("text-success");
        messageBox.classList.add("text-warning");
    } finally {
        isSubmittingMovie = false;
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = submitBtn.dataset.originalHtml || "Lưu phim";
        }
    }
}

function buildMoviePayload() {
    return {
        title: document.getElementById("movieTitle").value.trim(),
        originalTitle: document.getElementById("movieOriginalTitle").value.trim(),
        description: document.getElementById("movieDescription").value.trim(),
        durationMinutes: Number(document.getElementById("movieDuration").value) || null,
        ageRating: document.getElementById("movieAgeRating").value.trim(),
        posterUrl: document.getElementById("moviePoster").value.trim(),
        trailerUrl: document.getElementById("movieTrailerUrl").value.trim(),
        trailerEmbedUrl: document.getElementById("movieTrailerEmbedUrl").value.trim(),
        imdbUrl: document.getElementById("movieImdbUrl").value.trim(),
        releaseDate: document.getElementById("movieReleaseDate").value || null,
        endDate: document.getElementById("movieEndDate").value || null,
        genres: getSelectedGenres(),
        directors: splitList(document.getElementById("movieDirectors").value),
        actors: splitList(document.getElementById("movieActors").value)
    };
}

function validateRequiredFields() {
    const errors = [];
    const titleInput = document.getElementById("movieTitle");
    const descriptionInput = document.getElementById("movieDescription");
    const durationInput = document.getElementById("movieDuration");
    const posterInput = document.getElementById("moviePoster");
    const ageSelect = document.getElementById("movieAgeRating");
    const releaseInput = document.getElementById("movieReleaseDate");
    const endInput = document.getElementById("movieEndDate");
    const directorsInput = document.getElementById("movieDirectors");
    const actorsInput = document.getElementById("movieActors");

    const title = titleInput.value.trim();
    const description = descriptionInput?.value.trim() ?? "";
    const duration = Number(durationInput.value);
    const poster = posterInput.value.trim();
    const ageValue = ageSelect?.value.trim();
    const releaseValue = releaseInput.value;
    const endValue = endInput.value;
    const selectedGenres = getSelectedGenres();
    const directors = directorsInput?.value.trim() ?? "";
    const actors = actorsInput?.value.trim() ?? "";

    if (!title) {
        errors.push({ field: "movieTitle", message: "Vui lòng nhập tên phim *" });
    }
    if (!duration || duration <= 0) {
        errors.push({ field: "movieDuration", message: "Vui lòng nhập thời lượng (phút) * lớn hơn 0" });
    } else if (duration > 900) {
        errors.push({ field: "movieDuration", message: "Thời lượng tối đa 900 phút" });
    }
    if (!poster) {
        errors.push({ field: "moviePoster", message: "Vui lòng chọn Poster *" });
    }
    if (!ageValue) {
        errors.push({ field: "movieAgeRating", message: "Vui lòng chọn giới hạn độ tuổi *" });
    }
    if (!releaseValue) {
        errors.push({ field: "movieReleaseDate", message: "Vui lòng chọn ngày khởi chiếu *" });
    }
    if (!endValue) {
        errors.push({ field: "movieEndDate", message: "Vui lòng chọn ngày ngừng chiếu *" });
    }
    if (releaseValue && endValue) {
        const releaseDate = new Date(releaseValue);
        const endDate = new Date(endValue);
        if (endDate < releaseDate) {
            errors.push({ field: "movieEndDate", message: "Ngày ngừng chiếu phải sau hoặc bằng ngày khởi chiếu" });
        }
    }
    if (!selectedGenres.length) {
        errors.push({ field: "genreDropdownBtn", message: "Vui lòng chọn ít nhất một thể loại *" });
    }

    const firstInvalid = errors.length ? document.getElementById(errors[0].field) : null;
    return { errors, firstInvalid };
}
function clearFieldErrors() {
    [
        "movieTitle",
        "movieDuration",
        "moviePoster",
        "movieAgeRating",
        "movieReleaseDate",
        "movieEndDate",
        "genreDropdownBtn"
    ].forEach((id) => {
        document.getElementById(id)?.classList.remove("is-invalid");
        const errorBox = document.getElementById(`${id}Error`);
        if (errorBox) errorBox.textContent = "";
    });
}

function showFieldErrors(errors) {
    errors.forEach((err) => {
        document.getElementById(err.field)?.classList.add("is-invalid");
        const errorBox = document.getElementById(`${err.field}Error`);
        if (errorBox) errorBox.textContent = err.message;
    });
}

function splitList(value) {
    return value
        .split(",")
        .map((item) => item.trim())
        .filter((item) => item.length > 0);
}

function deleteMovie(id, title) {
    if (typeof openAdminConfirmDialog === "function") {
        openAdminConfirmDialog({
            title: "Xóa phim",
            message: "bạn chắc chứ ?",
            confirmLabel: "Xác nhận",
            confirmVariant: "danger",
            cancelLabel: "Hủy",
            onConfirm: () => performDeleteMovie(id)
        });
    } else if (confirm("bạn chắc chứ ?")) {
        performDeleteMovie(id);
    }
}

async function performDeleteMovie(id) {
    try {
        const response = await fetch(movieAdminApi.delete(id), { method: "DELETE" });
        if (!response.ok) {
            let fallback = "Không thể xóa phim";
            if (response.status === 409) {
                fallback = "CONFLICT";
            } else {
                const error = await response.json().catch(() => ({}));
                fallback = error.message || fallback;
            }
            throw new Error(fallback);
        }
        fetchMovies();
        const currentId = document.getElementById("movieId").value;
        if (String(currentId) === String(id)) {
            resetMovieForm();
        }
        movieDataBus.dispatch("movies");
    } catch (error) {
        const conflictDetected =
            error.message === "CONFLICT"
            || (error.message && error.message.includes("FK_Showtimes"))
            || (error.message && error.message.includes("suất chiếu"));
        if (conflictDetected) {
            openAdminNotice?.({
                title: "Không thể xóa",
                message: "Phim này vẫn đang có suất chiếu. Hãy xóa hoặc chuyển tất cả suất chiếu liên quan trước khi xóa phim.",
                variant: "warning"
            });
        } else {
            alert(error.message);
        }
    }
}

async function handlePosterUpload(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    const hint = document.getElementById("moviePosterHint");
    if (hint) {
        hint.textContent = "Đang tải poster...";
        hint.classList.remove("text-danger");
    }
    try {
        const formData = new FormData();
        formData.append("file", file);
        const response = await fetch("/api/admin/uploads/movie-poster", {
            method: "POST",
            body: formData
        });
        if (!response.ok) throw new Error("Không thể tải poster");
        const data = await response.json();
        document.getElementById("moviePoster").value = data.path;
        if (hint) hint.textContent = `Đã tải: ${data.path}`;
    } catch (error) {
        if (hint) {
            hint.textContent = error.message;
            hint.classList.add("text-danger");
        }
    } finally {
        event.target.value = "";
    }
}

async function loadGenreOptions() {
    try {
        const response = await fetch(movieAdminApi.genres, { credentials: "same-origin" });
        if (!response.ok) {
            throw new Error("Không thể tải danh sách thể loại");
        }
        const data = await response.json();
        const normalized = Array.isArray(data)
            ? data
                  .map((name) => (typeof name === "string" ? name.trim() : ""))
                  .filter((name) => name.length > 0)
            : [];
        genreOptions = normalized.length ? normalized : [...FALLBACK_GENRES];
    } catch (error) {
        console.error(error);
        genreOptions = [...FALLBACK_GENRES];
    }
}

function renderGenreCheckboxes() {
    const list = document.getElementById("genreCheckboxList");
    if (!list) return;
    list.innerHTML = "";
    const options = genreOptions.length ? genreOptions.slice() : [...FALLBACK_GENRES];
    options
        .sort((a, b) => a.localeCompare(b, "vi"))
        .forEach((name) => {
            const id = `genre-${name.toLowerCase().replace(/[^a-z0-9]+/g, "-")}`;
            const wrapper = document.createElement("div");
            wrapper.className = "form-check";
            wrapper.innerHTML = `
            <input class="form-check-input" type="checkbox" value="${name}" id="${id}">
            <label class="form-check-label" for="${id}">${name}</label>
        `;
            list.appendChild(wrapper);
        });
    updateGenreDropdownLabel();
    list.addEventListener("change", updateGenreDropdownLabel);
    ensureGenreDropdownBehavior();
}

function getSelectedGenres() {
    const checked = document.querySelectorAll("#genreCheckboxList input[type='checkbox']:checked");
    return Array.from(checked)
        .map((cb) => cb.value)
        .sort((a, b) => a.localeCompare(b, "vi"));
}

function setSelectedGenres(genres) {
    const normalized = (genres || []).map((g) => (g || "").trim().toLowerCase());
    document.querySelectorAll("#genreCheckboxList input[type='checkbox']").forEach((cb) => {
        cb.checked = normalized.includes(cb.value.toLowerCase());
    });
    updateGenreDropdownLabel();
}

function updateGenreDropdownLabel() {
    const label = document.getElementById("genreDropdownLabel");
    if (!label) return;
    const selected = getSelectedGenres();
    label.textContent = selected.length ? selected.join(", ") : "Chọn thể loại";
}

function ensureGenreDropdownBehavior() {
    if (genreDropdownInitialized) return;
    const button = document.getElementById("genreDropdownBtn");
    const menu = document.getElementById("genreCheckboxList");
    if (!button || !menu) return;
    genreDropdownInitialized = true;
    const closeMenu = () => {
        menu.classList.remove("show");
    };
    button.addEventListener("click", (event) => {
        event.preventDefault();
        event.stopPropagation();
        menu.classList.toggle("show");
    });
    menu.addEventListener("click", (event) => {
        event.stopPropagation();
    });
    document.addEventListener("click", closeMenu);
}

function bindMovieFilters() {
    const form = document.getElementById("movieFilterForm");
    const resetBtn = document.getElementById("movieFilterReset");
    const searchInput = document.getElementById("movieSearchKeyword");
    const statusSelect = document.getElementById("movieStatusFilter");
    const genreSelect = document.getElementById("movieGenreFilter");
    const orderSelect = document.getElementById("movieDurationOrder");
    const ageSelect = document.getElementById("movieAgeFilter");
    const debouncedSearch = createDebounce(() => fetchMovies(), 300);
    form?.addEventListener("submit", (event) => {
        event.preventDefault();
        fetchMovies();
    });
    searchInput?.addEventListener("input", debouncedSearch);
    resetBtn?.addEventListener("click", () => {
        const searchInput = document.getElementById("movieSearchKeyword");
        if (searchInput) {
            searchInput.value = "";
        }
        if (statusSelect) {
            statusSelect.value = "";
        }
        if (genreSelect) {
            genreSelect.value = "";
        }
        if (orderSelect) {
            orderSelect.value = "";
        }
        if (ageSelect) {
            ageSelect.value = "";
        }
        fetchMovies();
    });
}

function showSuccessToast(message) {
    const toast = document.createElement("div");
    toast.className = "admin-toast-success";
    toast.textContent = message;
    document.body.appendChild(toast);
    requestAnimationFrame(() => toast.classList.add("show"));
    setTimeout(() => {
        toast.classList.remove("show");
        setTimeout(() => toast.remove(), 400);
    }, 2000);
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

function formatAgeRating(value) {
    if (!value) {
        return "-";
    }
    switch (value.toUpperCase()) {
        case "P":
            return "P";
        case "K":
            return "K";
        case "T13":
        case "T16":
        case "T18":
            return value.toUpperCase();
        default:
            return value;
    }
}
