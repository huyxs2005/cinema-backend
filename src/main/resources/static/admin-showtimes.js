const showtimeApi = {
    list: "/api/admin/showtimes",
    detail: (id) => `/api/admin/showtimes/${id}`,
    create: "/api/admin/showtimes",
    update: (id) => `/api/admin/showtimes/${id}`,
    delete: (id) => `/api/admin/showtimes/${id}`,
    toggleActive: (id, active) => `/api/admin/showtimes/${id}/active?active=${active}`,
    auditoriums: "/api/admin/showtime-options/auditoriums"
};

const showtimeDataBus = window.AdminDataBus || {
    dispatch: () => {},
    subscribe: () => () => {}
};

const SHOWTIME_PAGE_SIZE = 10;
const showtimeState = {
    page: 0,
    totalPages: 0,
    movies: [],
    auditoriums: [],
    movieMap: new Map(),
    auditoriumMap: new Map(),
    submitting: false,
    repeatDayButtons: []
};
const showtimeTextCompare = new Intl.Collator("vi", { sensitivity: "base" });
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

const repeatDayPresets = {
    NONE: [],
    WHOLE_WEEK: [1, 2, 3, 4, 5, 6, 7],
    WEEKDAY: [1, 2, 3, 4, 5],
    WEEKEND: [6, 7]
};

document.addEventListener("DOMContentLoaded", () => {
    if (document.getElementById("showtimeForm")) {
        initShowtimeAdmin();
    }
    initRepeatControls();
    showtimeDataBus.subscribe("movies", () => loadMovieOptionsForShowtimes(true));
    showtimeDataBus.subscribe("auditoriums", () => loadAuditoriumOptions(true));
});

window.refreshShowtimeAuditoriums = () => loadAuditoriumOptions(true);

async function initShowtimeAdmin() {
    await loadShowtimeOptions();
    bindShowtimeForm();
    bindShowtimeFilters();
    fetchShowtimes();
}

function initRepeatControls() {
    const modeSelect = document.getElementById("showtimeRepeatMode");
    const startInput = document.getElementById("showtimeStartTime");
    showtimeState.repeatDayButtons = Array.from(document.querySelectorAll("#repeatDayButtons button"));
    if (!modeSelect || showtimeState.repeatDayButtons.length === 0) {
        return;
    }
    modeSelect.addEventListener("change", () => {
        if (modeSelect.value === "CUSTOM") {
            return;
        }
        applyRepeatModePreset(modeSelect.value);
    });
    showtimeState.repeatDayButtons.forEach((btn) => {
        btn.addEventListener("click", () => {
            toggleRepeatDay(btn);
        });
    });
    startInput?.addEventListener("change", () => {
        const currentMode = modeSelect.value;
        if (currentMode === "NONE") {
            highlightStartDay();
        }
    });
    applyRepeatModePreset(modeSelect.value);
}

function applyRepeatModePreset(mode) {
    if (mode === "CUSTOM") {
        return;
    }
    if (mode === "NONE") {
        clearRepeatDaySelection();
        highlightStartDay();
        return;
    }
    const presetDays = repeatDayPresets[mode] || [];
    setRepeatDaySelection(presetDays);
}

function clearRepeatDaySelection() {
    showtimeState.repeatDayButtons.forEach((btn) => btn.classList.remove("active"));
}

function setRepeatDaySelection(days) {
    const normalized = Array.isArray(days) ? days.map(Number) : [];
    showtimeState.repeatDayButtons.forEach((btn) => {
        const day = Number(btn.dataset.repeatDay);
        btn.classList.toggle("active", normalized.includes(day));
    });
}

function highlightStartDay() {
    const startInput = document.getElementById("showtimeStartTime");
    const startValue = startInput?.value;
    clearRepeatDaySelection();
    if (!startValue) {
        return;
    }
    const dayNumber = getDayNumberFromDateString(startValue);
    if (dayNumber) {
        setRepeatDaySelection([dayNumber]);
    }
}

function toggleRepeatDay(button) {
    button.classList.toggle("active");
    const modeSelect = document.getElementById("showtimeRepeatMode");
    if (modeSelect) {
        modeSelect.value = "CUSTOM";
    }
}

function getSelectedRepeatDays() {
    return showtimeState.repeatDayButtons
        .filter((btn) => btn.classList.contains("active"))
        .map((btn) => Number(btn.dataset.repeatDay))
        .filter((num) => !Number.isNaN(num));
}

function getDayNumberFromDateString(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return null;
    }
    const jsDay = date.getDay(); // 0 (Sun) - 6 (Sat)
    return ((jsDay + 6) % 7) + 1; // Convert to 1 (Mon) - 7 (Sun)
}

function bindShowtimeForm() {
    const form = document.getElementById("showtimeForm");
    const resetBtn = document.getElementById("showtimeResetBtn");
    form.addEventListener("submit", submitShowtimeForm);
    resetBtn?.addEventListener("click", resetShowtimeForm);
}

function bindShowtimeFilters() {
    const filterForm = document.getElementById("showtimeFilterForm");
    const resetBtn = document.getElementById("showtimeFilterReset");
    const keywordInput = document.getElementById("filterShowtimeKeyword");
    const debouncedSearch = createDebounce(() => fetchShowtimes(0));
    filterForm.addEventListener("submit", (event) => {
        event.preventDefault();
        fetchShowtimes(0);
    });
    resetBtn?.addEventListener("click", () => {
        filterForm.reset();
        fetchShowtimes(0);
    });
    keywordInput?.addEventListener("input", debouncedSearch);
}

async function loadShowtimeOptions() {
    await Promise.all([loadMovieOptionsForShowtimes(), loadAuditoriumOptions()]);
}

async function loadMovieOptionsForShowtimes(preserveSelection = false) {
    const formSelect = document.getElementById("showtimeMovieId");
    const filterSelect = document.getElementById("filterShowtimeMovie");
    const prevFormValue = preserveSelection && formSelect ? formSelect.value : "";
    const prevFilterValue = preserveSelection && filterSelect ? filterSelect.value : "";
    try {
        const response = await fetch("/api/movies/options");
        if (!response.ok) throw new Error("Không thể tải danh sách phim");
        const data = await response.json();
        showtimeState.movies = Array.isArray(data) ? data : [];
        showtimeState.movieMap.clear();
        showtimeState.movies.forEach((movie) => {
            showtimeState.movieMap.set(String(movie.id), movie);
        });
        populateSelect("showtimeMovieId", showtimeState.movies, "-- Chọn phim --");
        populateSelect("filterShowtimeMovie", showtimeState.movies, "Tất cả");
        if (preserveSelection && formSelect) {
            formSelect.value = prevFormValue;
        }
        if (preserveSelection && filterSelect) {
            filterSelect.value = prevFilterValue;
        }
    } catch (error) {
        console.error(error);
    }
}

async function loadAuditoriumOptions(preserveSelection = false) {
    const formSelect = document.getElementById("showtimeAuditoriumId");
    const filterSelect = document.getElementById("filterShowtimeAuditorium");
    const prevFormValue = preserveSelection && formSelect ? formSelect.value : "";
    const prevFilterValue = preserveSelection && filterSelect ? filterSelect.value : "";
    try {
        const response = await fetch(showtimeApi.auditoriums);
        if (!response.ok) throw new Error("Không thể tải danh sách phòng");
        const data = await response.json();
        showtimeState.auditoriums = Array.isArray(data) ? data : [];
        showtimeState.auditoriumMap.clear();
        showtimeState.auditoriums.forEach((auditorium) => {
            showtimeState.auditoriumMap.set(String(auditorium.id), auditorium);
        });
        populateSelect("showtimeAuditoriumId", showtimeState.auditoriums, "-- Chọn phòng --");
        populateSelect("filterShowtimeAuditorium", showtimeState.auditoriums, "Tất cả");
        if (preserveSelection && formSelect && prevFormValue) {
            formSelect.value = prevFormValue;
        }
        if (preserveSelection && filterSelect && prevFilterValue) {
            filterSelect.value = prevFilterValue;
        }
    } catch (error) {
        console.error(error);
    }
}

function populateSelect(elementId, options, placeholder) {
    const select = document.getElementById(elementId);
    if (!select) return;
    select.innerHTML = "";
    if (placeholder) {
        const defaultOpt = document.createElement("option");
        defaultOpt.value = "";
        defaultOpt.textContent = placeholder;
        select.appendChild(defaultOpt);
    }
    options.forEach((item) => {
        const option = document.createElement("option");
        option.value = item.id;
        option.textContent = item.title || item.name;
        select.appendChild(option);
    });
}

async function fetchShowtimes(page = 0) {
    const tbody = document.querySelector("#showtimeTable tbody");
    const counter = document.getElementById("showtimeCount");
    if (!tbody || !counter) return;
    tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-light">Đang tải dữ liệu...</td></tr>`;

    const params = new URLSearchParams();
    const filterForm = document.getElementById("showtimeFilterForm");
    const formData = new FormData(filterForm);
    for (const [key, value] of formData.entries()) {
        if (value) params.append(key, value);
    }
    params.set("page", page);
    params.set("size", SHOWTIME_PAGE_SIZE);
    params.set("sort", "startTime,asc");

    try {
        const response = await fetch(`${showtimeApi.list}?${params.toString()}`);
        if (!response.ok) throw new Error("Không thể tải danh sách suất chiếu");
        const data = await response.json();
        showtimeState.page = data.page ?? 0;
        showtimeState.totalPages = data.totalPages ?? 0;
        counter.textContent = `${data.totalElements ?? 0} items`;
        renderShowtimeTable(Array.isArray(data.content) ? data.content : []);
        renderShowtimePagination(data.page ?? 0, data.totalPages ?? 0);
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4">${error.message}</td></tr>`;
        counter.textContent = "0 items";
        document.getElementById("showtimePagination").innerHTML = "";
    }
}

function renderShowtimeTable(items) {
    const tbody = document.querySelector("#showtimeTable tbody");
    if (!tbody) return;
    if (!items.length) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-light">Chưa có suất chiếu nào</td></tr>`;
        return;
    }
    tbody.innerHTML = "";
    const sorted = items.slice().sort((a, b) =>
        showtimeTextCompare.compare(a?.movieTitle || "", b?.movieTitle || "")
    );
    sorted.forEach((item, index) => {
        const tr = document.createElement("tr");
        const toggleLabel = item.active ? "Vô hiệu hóa" : "Kích hoạt";
        const targetActive = item.active ? "false" : "true";
        tr.innerHTML = `
            <td>${index + 1 + showtimeState.page * SHOWTIME_PAGE_SIZE}</td>
            <td>
                <strong>${item.movieTitle ?? "-"}</strong>
            </td>
            <td>${item.auditoriumName ?? "-"}</td>
            <td>${formatDateTimeDisplay(item.startTime)}</td>
            <td>${formatDateTimeDisplay(item.endTime)}</td>
            <td>${renderShowtimeStatus(item.active)}</td>
            <td>
                <div class="user-action-menu-wrapper">
                    <button type="button"
                            class="btn btn-outline-light btn-sm action-menu-toggle"
                            aria-haspopup="true"
                            aria-expanded="false"
                            title="Mở hành động">⋮</button>
                    <div class="user-action-menu" role="menu">
                        <button type="button" data-edit="${item.id}" data-menu-role="edit">Sửa</button>
                        <button type="button"
                                data-showtime-toggle="${item.id}"
                                data-target-active="${targetActive}"
                                data-menu-role="toggle">${toggleLabel}</button>
                        <button type="button"
                                data-showtime-delete="${item.id}"
                                data-menu-role="delete">Xóa</button>
                    </div>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
    window.AdminActionMenus?.init(tbody);
    tbody.querySelectorAll("[data-edit]").forEach((btn) =>
        btn.addEventListener("click", () => {
            window.AdminActionMenus?.closeAll();
            editShowtime(Number(btn.dataset.edit));
        })
    );
    tbody.querySelectorAll("[data-showtime-toggle]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const id = Number(btn.dataset.showtimeToggle);
            const targetActive = btn.dataset.targetActive === "true";
            window.AdminActionMenus?.closeAll();
            confirmToggleShowtime(id, targetActive);
        });
    });
    tbody.querySelectorAll("[data-showtime-delete]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const id = Number(btn.dataset.showtimeDelete);
            window.AdminActionMenus?.closeAll();
            confirmDeleteShowtime(id);
        });
    });
}

function renderShowtimeStatus(active) {
    return active ? '<span class="badge bg-success">ON</span>' : '<span class="badge bg-secondary">OFF</span>';
}

function renderShowtimePagination(page, totalPages) {
    const container = document.getElementById("showtimePagination");
    if (!container) return;
    if (totalPages <= 1) {
        container.innerHTML = "";
        return;
    }
    const prevDisabled = page <= 0 ? "disabled" : "";
    const nextDisabled = page + 1 >= totalPages ? "disabled" : "";
    container.innerHTML = `
        <button class="btn btn-outline-light btn-sm" data-page="${page - 1}" ${prevDisabled}>Trang trước</button>
        <span class="small">Trang ${page + 1}/${totalPages}</span>
        <button class="btn btn-outline-light btn-sm" data-page="${page + 1}" ${nextDisabled}>Trang sau</button>
    `;
    container.querySelectorAll("button[data-page]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const targetPage = Number(btn.dataset.page);
            if (Number.isNaN(targetPage)) return;
            fetchShowtimes(targetPage);
        });
    });
}

async function submitShowtimeForm(event) {
    event.preventDefault();
    if (showtimeState.submitting) return;

    const payload = buildShowtimePayload();
    const validationErrors = validateShowtimePayload(payload);
    clearShowtimeErrors();
    const messageBox = document.getElementById("showtimeFormMessage");
    messageBox.textContent = "";

    if (validationErrors.length > 0) {
        showShowtimeErrors(validationErrors);
        const firstField = document.getElementById(validationErrors[0].field);
        firstField?.scrollIntoView({ behavior: "smooth", block: "center" });
        firstField?.focus({ preventScroll: true });
        return;
    }

    const submitBtn = document.querySelector("#showtimeForm button[type='submit']");
    if (submitBtn && !submitBtn.dataset.originalHtml) {
        submitBtn.dataset.originalHtml = submitBtn.innerHTML;
    }
    setSubmittingState(true, submitBtn);

    const currentId = document.getElementById("showtimeId").value;
    const url = currentId ? showtimeApi.update(currentId) : showtimeApi.create;
    const method = currentId ? "PUT" : "POST";

    try {
        const response = await fetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || "Không thể lưu suất chiếu");
        }
        showtimeDataBus.dispatch("showtimes");
        showSuccessToast("Lưu thành công!");
        resetShowtimeForm();
        fetchShowtimes(showtimeState.page);
        window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
        messageBox.textContent = error.message;
        messageBox.classList.add("text-warning");
    } finally {
        setSubmittingState(false, submitBtn);
    }
}

function buildShowtimePayload() {
    const startValue = document.getElementById("showtimeStartTime").value;
    const normalizedStart = startValue ? `${startValue}${startValue.length === 16 ? ":00" : ""}` : null;
    return {
        movieId: valueOrNull(document.getElementById("showtimeMovieId").value),
        auditoriumId: valueOrNull(document.getElementById("showtimeAuditoriumId").value),
        startTime: normalizedStart,
        repeatUntil: document.getElementById("showtimeRepeatUntil")?.value || null,
        repeatDays: getSelectedRepeatDays(),
        repeatMode: document.getElementById("showtimeRepeatMode")?.value || "NONE",
        active: document.getElementById("showtimeActive")?.checked ?? true
    };
}

function validateShowtimePayload(payload) {
    const errors = [];
    if (!payload.movieId) {
        errors.push({ field: "showtimeMovieId", message: "Vui lòng chọn phim *" });
    }
    if (!payload.auditoriumId) {
        errors.push({ field: "showtimeAuditoriumId", message: "Vui lòng chọn phòng chiếu *" });
    }
    if (!payload.startTime) {
        errors.push({ field: "showtimeStartTime", message: "Vui lòng chọn giờ bắt đầu *" });
    }
    if (payload.repeatUntil) {
        if (!payload.startTime) {
            errors.push({ field: "showtimeRepeatUntil", message: "Vui lòng chọn giờ bắt đầu trước *" });
        } else if (isRepeatUntilBeforeStart(payload.startTime, payload.repeatUntil)) {
            errors.push({ field: "showtimeRepeatUntil", message: "Ngày kết thúc phải từ ngày bắt đầu trở đi *" });
        }
    }
    return errors;
}

function clearShowtimeErrors() {
    ["showtimeMovieId", "showtimeAuditoriumId", "showtimeStartTime", "showtimeRepeatUntil"].forEach((id) => {
        document.getElementById(id)?.classList.remove("is-invalid");
        const errorBox = document.getElementById(`${id}Error`);
        if (errorBox) errorBox.textContent = "";
    });
}

function isRepeatUntilBeforeStart(startDateTime, repeatUntilDate) {
    const startDate = new Date(startDateTime);
    if (Number.isNaN(startDate.getTime())) {
        return false;
    }
    startDate.setHours(0, 0, 0, 0);
    const untilDate = new Date(`${repeatUntilDate}T00:00:00`);
    if (Number.isNaN(untilDate.getTime())) {
        return false;
    }
    return untilDate.getTime() < startDate.getTime();
}

function showShowtimeErrors(errors) {
    errors.forEach((err) => {
        document.getElementById(err.field)?.classList.add("is-invalid");
        const errorBox = document.getElementById(`${err.field}Error`);
        if (errorBox) errorBox.textContent = err.message;
    });
}

function setSubmittingState(state, button) {
    showtimeState.submitting = state;
    if (!button) return;
    if (state) {
        button.disabled = true;
        button.innerHTML = `<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang lưu...`;
    } else {
        button.disabled = false;
        button.innerHTML = button.dataset.originalHtml || "Lưu suất chiếu";
    }
}

function resetShowtimeForm() {
    const form = document.getElementById("showtimeForm");
    if (!form) return;
    form.reset();
    const repeatSelect = document.getElementById("showtimeRepeatMode");
    if (repeatSelect) {
        repeatSelect.value = "NONE";
    }
    applyRepeatModePreset("NONE");
    document.getElementById("showtimeId").value = "";
    document.getElementById("showtimeFormTitle").textContent = "Thêm suất chiếu";
    const messageBox = document.getElementById("showtimeFormMessage");
    messageBox.textContent = "";
    const activeToggle = document.getElementById("showtimeActive");
    if (activeToggle) {
        activeToggle.checked = true;
    }
    clearShowtimeErrors();
}

async function editShowtime(id) {
    try {
        const response = await fetch(showtimeApi.detail(id));
        if (!response.ok) throw new Error("Không tìm thấy suất chiếu");
        const showtime = await response.json();
        document.getElementById("showtimeId").value = showtime.id;
        document.getElementById("showtimeFormTitle").textContent = "Chỉnh sửa suất chiếu";
        document.getElementById("showtimeMovieId").value = showtime.movieId ?? "";
        document.getElementById("showtimeAuditoriumId").value = showtime.auditoriumId ?? "";
        document.getElementById("showtimeStartTime").value = formatDateTimeForInput(showtime.startTime);
        const repeatSelect = document.getElementById("showtimeRepeatMode");
        if (repeatSelect) {
            repeatSelect.value = "NONE";
        }
        applyRepeatModePreset("NONE");
        const activeToggle = document.getElementById("showtimeActive");
        if (activeToggle) {
            activeToggle.checked = Boolean(showtime.active);
        }
        document.getElementById("showtimeFormMessage").textContent = "";
        clearShowtimeErrors();
        window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
        alert(error.message);
    }
}

function confirmToggleShowtime(id, shouldActivate) {
    const actionLabel = shouldActivate ? "kích hoạt" : "xóa";
    const confirmAction = () => toggleShowtimeActive(id, shouldActivate);
    if (typeof openAdminConfirmDialog === "function") {
        openAdminConfirmDialog({
            title: `${shouldActivate ? "Kích hoạt" : "Xóa"} suất chiếu`,
            message: `Bạn có chắc muốn ${actionLabel} suất chiếu này?`,
            confirmLabel: "Xác nhận",
            confirmVariant: shouldActivate ? "primary" : "danger",
            cancelLabel: "Hủy",
            onConfirm: confirmAction
        });
    } else if (confirm(`Bạn có chắc muốn ${actionLabel} suất chiếu này?`)) {
        confirmAction();
    }
}

async function toggleShowtimeActive(id, shouldActivate) {
    try {
        const response = await fetch(showtimeApi.toggleActive(id, shouldActivate), { method: "PATCH" });
        if (!response.ok) {
            throw new Error("Không thể cập nhật trạng thái suất chiếu");
        }
        fetchShowtimes(showtimeState.page);
        showtimeDataBus.dispatch("showtimes");
    } catch (error) {
        alert(error.message);
    }
}

function confirmDeleteShowtime(id) {
    const confirmAction = () => performDeleteShowtime(id);
    if (typeof openAdminConfirmDialog === "function") {
        openAdminConfirmDialog({
            title: "Xóa suất chiếu",
            message: "Bạn chắc chứ?",
            confirmLabel: "Xác nhận",
            confirmVariant: "danger",
            cancelLabel: "Hủy",
            onConfirm: confirmAction
        });
    } else if (confirm("Bạn chắc chứ?")) {
        confirmAction();
    }
}

async function performDeleteShowtime(id) {
    try {
        const response = await fetch(showtimeApi.delete(id), { method: "DELETE" });
        if (!response.ok) throw new Error("Không thể xóa suất chiếu");
        fetchShowtimes(showtimeState.page);
        showtimeDataBus.dispatch("showtimes");
    } catch (error) {
        alert(error.message);
    }
}

function formatDateTimeDisplay(value) {
    if (!value) return "-";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleString("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false
    });
}

function formatDateTimeForInput(value) {
    if (!value) return "";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "";
    const tzOffset = date.getTimezoneOffset() * 60000;
    const local = new Date(date.getTime() - tzOffset);
    return local.toISOString().slice(0, 16);
}

function valueOrNull(value) {
    return value ? Number(value) : null;
}
