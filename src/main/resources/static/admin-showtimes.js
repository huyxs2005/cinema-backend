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

const showtimeApi = {
    list: "/api/admin/showtimes",
    grouped: "/api/admin/showtimes/grouped",
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

const showtimeState = {
    movies: [],
    auditoriums: [],
    movieMap: new Map(),
    auditoriumMap: new Map(),
    submitting: false,
    repeatDayButtons: [],
    groupedShowtimes: [],
    selectedMovieId: null,
    filterSelectedMovieId: null,
    timeSuggestionButtons: []
};

const repeatDayPresets = {
    NONE: [],
    WHOLE_WEEK: [1, 2, 3, 4, 5, 6, 7],
    WEEKDAY: [1, 2, 3, 4, 5],
    WEEKEND: [6, 7]
};

const SHOWTIME_TIME_MIN = "08:00";
const SHOWTIME_TIME_MAX = "22:00";
const SHOWTIME_MINUTES_MIN = timeStringToMinutes(SHOWTIME_TIME_MIN);
const SHOWTIME_MINUTES_MAX = timeStringToMinutes(SHOWTIME_TIME_MAX);

const escapeHtml = (value = "") =>
    String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");

document.addEventListener("DOMContentLoaded", () => {
    if (document.getElementById("showtimeForm")) {
        initShowtimeAdmin();
    }
    initRepeatControls();
    showtimeDataBus.subscribe("movies", () => loadMovieOptionsForShowtimes(true));
    showtimeDataBus.subscribe("auditoriums", () => loadAuditoriumOptions(true));
});

window.refreshShowtimeAuditoriums = () => loadAuditoriumOptions(true);
window.refreshShowtimeSchedule = () => fetchShowtimes();

function showShowtimeError(message) {
    const finalMessage = message || "Không thể thực hiện thao tác với suất chiếu.";
    if (typeof window.openAdminNotice === "function") {
        openAdminNotice({
            title: "Lỗi suất chiếu",
            message: finalMessage,
            variant: "warning"
        });
    } else {
        alert(finalMessage);
    }
}

async function initShowtimeAdmin() {
    initShowtimeMovieSearch();
    initFilterShowtimeMovieSearch();
    await loadShowtimeOptions();
    bindShowtimeForm();
    initTimeSuggestions();
    bindShowtimeFilters();
    fetchShowtimes();
}

function initRepeatControls() {
    const modeSelect = document.getElementById("showtimeRepeatMode");
    const startTimeInput = document.getElementById("showtimeStartTime");
    const startDateInput = document.getElementById("showtimeStartDate");
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
        btn.addEventListener("click", () => toggleRepeatDay(btn));
    });
    const highlightIfNeeded = () => {
        if (modeSelect.value === "NONE") {
            highlightStartDay();
        }
    };
    startTimeInput?.addEventListener("change", highlightIfNeeded);
    startDateInput?.addEventListener("change", highlightIfNeeded);
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
    const startDateInput = document.getElementById("showtimeStartDate");
    const startDateValue = startDateInput?.value;
    clearRepeatDaySelection();
    if (!startDateValue) {
        return;
    }
    const dayNumber = getDayNumberFromDate(startDateValue);
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

function getDayNumberFromDate(value) {
    const date = new Date(`${value}T00:00:00`);
    if (Number.isNaN(date.getTime())) {
        return null;
    }
    const jsDay = date.getDay();
    return ((jsDay + 6) % 7) + 1;
}

function initTimeSuggestions() {
    const container = document.getElementById("showtimeTimeSuggestions");
    const timeInput = document.getElementById("showtimeStartTime");
    if (!container || !timeInput) {
        showtimeState.timeSuggestionButtons = [];
        return;
    }
    showtimeState.timeSuggestionButtons = Array.from(
        container.querySelectorAll("[data-time-suggestion]")
    );
    if (!showtimeState.timeSuggestionButtons.length) {
        return;
    }
    showtimeState.timeSuggestionButtons.forEach((button) => {
        button.addEventListener("click", () => {
            const value = button.dataset.timeSuggestion;
            if (!value || !timeInput) {
                return;
            }
            timeInput.value = value;
            timeInput.dispatchEvent(new Event("input", { bubbles: true }));
            timeInput.dispatchEvent(new Event("change", { bubbles: true }));
            syncTimeSuggestionButtons();
        });
    });
    timeInput.addEventListener("input", syncTimeSuggestionButtons);
    timeInput.addEventListener("change", syncTimeSuggestionButtons);
    syncTimeSuggestionButtons();
}

function syncTimeSuggestionButtons() {
    if (!Array.isArray(showtimeState.timeSuggestionButtons)) {
        return;
    }
    const timeInput = document.getElementById("showtimeStartTime");
    const currentValue = timeInput?.value?.slice(0, 5) || "";
    showtimeState.timeSuggestionButtons.forEach((button) => {
        const buttonValue = button.dataset.timeSuggestion || "";
        button.classList.toggle("active", buttonValue === currentValue);
    });
}

function bindShowtimeForm() {
    const form = document.getElementById("showtimeForm");
    const resetBtn = document.getElementById("showtimeResetBtn");
    form.addEventListener("submit", submitShowtimeForm);
    resetBtn?.addEventListener("click", resetShowtimeForm);
}

function bindShowtimeFilters() {
    const filterForm = document.getElementById("showtimeFilterForm");
    if (!filterForm) {
        return;
    }
    filterForm.addEventListener("submit", (event) => {
        event.preventDefault();
        fetchShowtimes();
    });
    filterForm.querySelectorAll("select, input").forEach((field) => {
        if (field.id === "filterShowtimeKeyword") {
            return;
        }
        field.addEventListener("change", () => fetchShowtimes());
    });
    document.getElementById("showtimeFilterReset")?.addEventListener("click", () => {
        filterForm.reset();
        setFilterSelectedMovie(null);
        hideFilterShowtimeMovieSuggestions();
        const filterError = document.getElementById("filterShowtimeMovieError");
        if (filterError) {
            filterError.textContent = "";
        }
        fetchShowtimes();
    });
}

async function loadShowtimeOptions() {
    await Promise.all([loadMovieOptionsForShowtimes(), loadAuditoriumOptions()]);
}

async function loadMovieOptionsForShowtimes(preserveSelection = false) {
    const hiddenInput = document.getElementById("filterShowtimeMovie");
    const prevFilterValue = preserveSelection && hiddenInput ? hiddenInput.value : "";
    try {
        const response = await fetch("/api/movies/options");
        if (!response.ok) throw new Error("Không thể tải danh sách phim");
        const data = await response.json();
        showtimeState.movies = Array.isArray(data) ? data : [];
        showtimeState.movieMap.clear();
        showtimeState.movies.forEach((movie) => {
            showtimeState.movieMap.set(String(movie.id), movie);
        });
        if (prevFilterValue) {
            setFilterSelectedMovie(Number(prevFilterValue));
        } else if (showtimeState.filterSelectedMovieId) {
            setFilterSelectedMovie(showtimeState.filterSelectedMovieId);
        } else {
            setFilterSelectedMovie(null);
        }
        if (showtimeState.selectedMovieId) {
            setFormSelectedMovie(showtimeState.selectedMovieId);
        }
        renderShowtimeMovieSuggestions(document.getElementById("showtimeMovieSearch")?.value || "");
        renderFilterShowtimeMovieSuggestions(document.getElementById("filterShowtimeKeyword")?.value || "");
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
        if (!response.ok) throw new Error("Không thể tải danh sách phòng chiếu");
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

async function fetchShowtimes() {
    const container = document.getElementById("showtimeGroupedContainer");
    const counter = document.getElementById("showtimeCount");
    const filterForm = document.getElementById("showtimeFilterForm");
    if (!container || !counter || !filterForm) {
        return;
    }
    const formData = new FormData(filterForm);
    const keywordValue = (formData.get("keyword") || "").toString().trim();
    let movieId = (formData.get("movieId") || "").toString().trim();
    if (!movieId && keywordValue) {
        const match = findMovieByKeyword(keywordValue);
        if (match) {
            movieId = String(match.id);
            setFilterSelectedMovie(match.id);
        }
    }
    const filterMovieError = document.getElementById("filterShowtimeMovieError");
    if (filterMovieError) {
        filterMovieError.textContent = "";
    }
    const params = new URLSearchParams();
    if (movieId) {
        params.set("movieId", movieId);
    }
    const auditoriumId = (formData.get("auditoriumId") || "").toString().trim();
    if (auditoriumId) {
        params.set("auditoriumId", auditoriumId);
    }
    const activeValue = (formData.get("active") || "").toString().trim();
    if (activeValue) {
        params.set("active", activeValue);
    }
    const fromDate = (formData.get("fromDate") || "").toString().trim();
    if (fromDate) {
        params.set("fromDate", fromDate);
    }
    const toDate = (formData.get("toDate") || "").toString().trim();
    if (toDate) {
        params.set("toDate", toDate);
    }
    if (keywordValue) {
        params.set("keyword", keywordValue);
    }
    container.innerHTML = `<div class="showtime-empty-state">Đang tải dữ liệu...</div>`;

    try {
        const response = await fetch(`${showtimeApi.grouped}?${params.toString()}`);
        if (!response.ok) throw new Error("Không thể tải danh sách suất chiếu.");
        const data = await response.json();
        showtimeState.groupedShowtimes = Array.isArray(data) ? data : [];
        const total = showtimeState.groupedShowtimes.reduce(
            (sum, group) => sum + (group.totalShowtimes ?? 0),
            0
        );
        counter.textContent = `${total} suất`;
        renderGroupedShowtimes(showtimeState.groupedShowtimes);
    } catch (error) {
        container.innerHTML = `<div class="showtime-empty-state text-danger">${error.message}</div>`;
        counter.textContent = "0 suất";
    }
}

function renderGroupedShowtimes(groups) {
    const container = document.getElementById("showtimeGroupedContainer");
    if (!container) {
        return;
    }
    if (!Array.isArray(groups) || groups.length === 0) {
        container.innerHTML = `<div class="showtime-empty-state">Không có suất chiếu nào cho phim này trong khoảng thời gian đã chọn.</div>`;
        return;
    }
    container.innerHTML = groups
        .map((group, index) => renderTimeGroup(group, index))
        .join("");
    container.querySelectorAll("[data-group-toggle]").forEach((btn) => {
        btn.addEventListener("click", () => toggleTimeGroup(btn));
    });
    bindGroupActionButtons(container);
}

function renderTimeGroup(group, index) {
    const dayGroups = Array.isArray(group.dayGroups) ? group.dayGroups : [];
    const flattenedIds = dayGroups
        .flatMap((day) => (Array.isArray(day.showtimes) ? day.showtimes : []))
        .map((item) => item.id)
        .filter((id) => Number.isInteger(id));
    const timeLabel = group.startTimeLabel ?? "--:--";
    const safeTimeLabel = escapeHtml(timeLabel);
    const deleteDisabledAttr = flattenedIds.length ? "" : "disabled";
    const dayMarkup = dayGroups.length
        ? dayGroups.map((day) => renderDayGroup(day)).join("")
        : `<div class="showtime-empty-state">Không có suất nào trong khung giờ này.</div>`;
    return `
        <div class="showtime-time-group" data-time-group="${index}">
            <div class="showtime-time-header">
                <div>
                    <p class="showtime-time-label mb-0">Khung giờ ${timeLabel}</p>
                    <span class="showtime-time-count">${group.totalShowtimes ?? 0} suất</span>
                </div>
                <div class="showtime-time-actions">
                    <button type="button"
                        class="btn btn-outline-danger btn-sm"
                        data-time-delete="${flattenedIds.join(",")}"
                        data-time-label="${safeTimeLabel}"
                        ${deleteDisabledAttr}>
                        Xóa khung giờ
                    </button>
                    <button type="button" class="btn btn-outline-light btn-sm" data-group-toggle="${index}">Mở rộng</button>
                </div>
            </div>
            <div class="showtime-day-groups collapsed" data-group-body="${index}">
                ${dayMarkup}
            </div>
        </div>
    `;
}

function renderDayGroup(dayGroup) {
    const occurrences = Array.isArray(dayGroup.showtimes) ? dayGroup.showtimes : [];
    const idList = occurrences.map((item) => item.id).filter((id) => Number.isInteger(id));
    const disabledAttr = idList.length ? "" : "disabled";
    const hasInactive = occurrences.some((occurrence) => !occurrence.active);
    const toggleTargetActive = hasInactive ? "true" : "false";
    const toggleLabel = hasInactive ? "Kích họat tất cả" : "Vô hiệu hóa";
    const safeDayLabel = escapeHtml(dayGroup.dayLabel ?? "");
    const occurrenceMarkup = occurrences.length
        ? occurrences.map((occurrence) => renderShowtimeOccurrence(occurrence)).join("")
        : `<li class="showtime-empty-state">Không có suất nào.</li>`;
    return `
        <div class="showtime-day-group" data-day="${dayGroup.dayOfWeek}">
            <div class="showtime-day-header">
                <div>
                    <p class="mb-0">${dayGroup.dayLabel ?? "Không rõ"}</p>
                    <span class="badge bg-secondary">${dayGroup.totalShowtimes ?? 0} suất</span>
                </div>
                <div class="showtime-day-actions">
                    <button type="button"
                        class="btn btn-outline-warning btn-sm"
                        data-bulk-toggle="${idList.join(",")}"
                        data-day-label="${safeDayLabel}"
                        data-target-active="${toggleTargetActive}"
                        ${disabledAttr}>
                        ${toggleLabel}
                    </button>
                    <button type="button"
                        class="btn btn-outline-danger btn-sm"
                        data-bulk-delete="${idList.join(",")}"
                        data-day-label="${safeDayLabel}"
                        ${disabledAttr}>
                        Xóa toàn bộ
                    </button>
                </div>
            </div>
            <ul class="showtime-occurrence-list">
                ${occurrenceMarkup}
            </ul>
        </div>
    `;
}

function renderShowtimeOccurrence(occurrence) {
    const statusClass = occurrence.active ? "status-active" : "status-inactive";
    const statusLabel = occurrence.active ? "Đang mở" : "Tạm tắt";
    const dateLabel = occurrence.showDateLabel || formatShowDate(occurrence.showDate);
    const roomLabel = occurrence.auditoriumName ? `Phòng ${occurrence.auditoriumName}` : "Chưa rõ phòng";
    const moviePrimary = occurrence.movieTitle ? `<span class="showtime-movie-primary">${occurrence.movieTitle}</span>` : "";
    const movieSecondary = occurrence.movieOriginalTitle
        ? `<span class="showtime-movie-secondary">${occurrence.movieOriginalTitle}</span>`
        : "";
    const movieLine = moviePrimary
        ? `<div class="showtime-movie-line">${moviePrimary}${movieSecondary}</div>`
        : "";
    const toggleTargetActive = occurrence.active ? "false" : "true";
    const toggleButtonLabel = occurrence.active ? "Vô hiệu hóa" : "Kích hoạt";
    const safeShowtimeLabel = escapeHtml(`${dateLabel} - ${roomLabel}`);
    return `
        <li class="showtime-occurrence-item">
            <div>
                ${movieLine}
                <p class="mb-1 fw-semibold">${dateLabel}</p>
                <div class="showtime-occurrence-meta">
                    <span>${roomLabel}</span>
                    <span class="showtime-status-badge ${statusClass}">${statusLabel}</span>
                </div>
            </div>
            <div class="d-flex gap-2 flex-wrap">
                <button type="button"
                    class="btn btn-outline-warning btn-sm"
                    data-showtime-toggle="${occurrence.id}"
                    data-target-active="${toggleTargetActive}"
                    data-showtime-label="${safeShowtimeLabel}">
                    ${toggleButtonLabel}
                </button>
                <button type="button" class="btn btn-outline-light btn-sm" data-edit="${occurrence.id}">Sửa</button>
                <button type="button" class="btn btn-outline-danger btn-sm" data-delete="${occurrence.id}">Xóa</button>
            </div>
        </li>
    `;
}

function toggleTimeGroup(button) {
    const target = document.querySelector(`[data-group-body="${button.dataset.groupToggle}"]`);
    if (!target) return;
    target.classList.toggle("collapsed");
    button.textContent = target.classList.contains("collapsed") ? "Mở rộng" : "Thu gọn";
}

function bindGroupActionButtons(container) {
    container.querySelectorAll("[data-edit]").forEach((btn) => {
        btn.addEventListener("click", () => editShowtime(Number(btn.dataset.edit)));
    });
    container.querySelectorAll("[data-delete]").forEach((btn) => {
        btn.addEventListener("click", () => deleteShowtime(Number(btn.dataset.delete)));
    });
    container.querySelectorAll("[data-showtime-toggle]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const id = Number(btn.dataset.showtimeToggle);
            if (!Number.isInteger(id)) {
                return;
            }
            const shouldActivate = btn.dataset.targetActive === "true";
            handleSingleToggle(id, shouldActivate, btn.dataset.showtimeLabel || "");
        });
    });
    container.querySelectorAll("[data-bulk-toggle]").forEach((btn) => {
        btn.addEventListener("click", () => handleBulkToggle(btn.dataset.bulkToggle, btn.dataset.dayLabel, btn.dataset.targetActive));
    });
    container.querySelectorAll("[data-bulk-delete]").forEach((btn) => {
        btn.addEventListener("click", () => handleBulkDelete(btn.dataset.bulkDelete, btn.dataset.dayLabel));
    });
    container.querySelectorAll("[data-time-delete]").forEach((btn) => {
        btn.addEventListener("click", () => handleBulkDelete(btn.dataset.timeDelete, btn.dataset.timeLabel));
    });
}

function handleBulkDelete(idList, label) {
    const ids = parseIdList(idList);
    if (!ids.length) {
        return;
    }
    const confirmAction = () => performBulkDelete(ids);
    const targetLabel = label || "nhóm này";
    const message = `Xóa ${ids.length} suất thuộc ${targetLabel}?`;
    if (typeof openAdminConfirmDialog === "function") {
        openAdminConfirmDialog({
            title: "Xóa nhóm suất chiếu",
            message,
            confirmLabel: "Xóa",
            confirmVariant: "danger",
            cancelLabel: "Hủy",
            onConfirm: confirmAction
        });
    } else if (confirm(message)) {
        confirmAction();
    }
}

async function performBulkDelete(ids) {
    for (const id of ids) {
        try {
            const response = await fetch(showtimeApi.delete(id), { method: "DELETE" });
            if (!response.ok) {
                throw new Error("Không thể xóa một số suất chiếu.");
            }
        } catch (error) {
            alert(error.message);
            break;
        }
    }
    fetchShowtimes();
    showtimeDataBus.dispatch("showtimes");
}

function handleBulkToggle(idList, label, targetActive) {
    const ids = parseIdList(idList);
    if (!ids.length) {
        return;
    }
    const shouldActivate = targetActive === "true";
    const actionLabel = shouldActivate ? "Kích hoạt" : "Vô hiệu hóa";
    const targetLabel = label || "nhóm này";
    const confirmAction = () => performBulkToggle(ids, shouldActivate);
    const message = `${actionLabel} ${ids.length} suất thuộc ${targetLabel}?`;
    if (typeof openAdminConfirmDialog === "function") {
        openAdminConfirmDialog({
            title: `${actionLabel} suất chiếu`,
            message,
            confirmLabel: actionLabel,
            confirmVariant: shouldActivate ? "primary" : "danger",
            cancelLabel: "Huỷ",
            onConfirm: confirmAction
        });
    } else if (confirm(message)) {
        confirmAction();
    }
}

function handleSingleToggle(id, shouldActivate, label) {
    if (!Number.isInteger(id)) {
        return;
    }
    const actionLabel = shouldActivate ? "Kích hoạt" : "Vô hiệu hóa";
    const targetLabel = label || `suất #${id}`;
    const confirmAction = () => performBulkToggle([id], shouldActivate);
    const message = `${actionLabel} ${targetLabel}?`;
    if (typeof openAdminConfirmDialog === "function") {
        openAdminConfirmDialog({
            title: `${actionLabel} suất chiếu`,
            message,
            confirmLabel: actionLabel,
            confirmVariant: shouldActivate ? "primary" : "danger",
            cancelLabel: "Huỷ",
            onConfirm: confirmAction
        });
    } else if (confirm(message)) {
        confirmAction();
    }
}

async function performBulkToggle(ids, shouldActivate) {
    for (const id of ids) {
        try {
            await toggleShowtimeActive(id, shouldActivate);
        } catch (error) {
            showShowtimeError(error.message);
            break;
        }
    }
    fetchShowtimes();
    showtimeDataBus.dispatch("showtimes");
}

async function toggleShowtimeActive(id, shouldActivate) {
    const response = await fetch(showtimeApi.toggleActive(id, shouldActivate), { method: "PATCH" });
    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "Không thể cập nhật trạng thái suất chiếu.");
    }
}

function parseIdList(idList) {
    if (!idList) {
        return [];
    }
    return idList
        .split(",")
        .map((value) => Number(value.trim()))
        .filter((id) => Number.isInteger(id) && id > 0);
}

function initShowtimeMovieSearch() {
    const searchInput = document.getElementById("showtimeMovieSearch");
    const clearBtn = document.getElementById("showtimeMovieClear");
    if (!searchInput) {
        return;
    }
    const debounced = createDebounce(() => {
        renderShowtimeMovieSuggestions(searchInput.value || "");
    }, 200);
    searchInput.addEventListener("input", debounced);
    searchInput.addEventListener("focus", () => {
        renderShowtimeMovieSuggestions(searchInput.value || "");
    });
    searchInput.addEventListener("blur", (event) => {
        const related = event.relatedTarget;
        if (related && related.classList?.contains("movie-suggestion-item")) {
            return;
        }
        hideShowtimeMovieSuggestions();
    });
    clearBtn?.addEventListener("click", () => {
        setFormSelectedMovie(null);
        hideShowtimeMovieSuggestions();
    });
    updateShowtimeMovieClearButtonState();
}

function renderShowtimeMovieSuggestions(keyword = "") {
    const list = document.getElementById("showtimeMovieSuggestions");
    if (!list) {
        return;
    }
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) {
        hideShowtimeMovieSuggestions();
        return;
    }
    const matches = showtimeState.movies
            .filter((movie) => {
                const title = (movie.title || "").toLowerCase();
                const original = (movie.originalTitle || "").toLowerCase();
                return title.includes(normalized) || original.includes(normalized);
            })
            .slice(0, 8);
    if (!matches.length) {
        list.innerHTML = `<p class="text-warning small mb-0">Không tìm thấy phim "${keyword}".</p>`;
        list.classList.remove("movie-suggestion-list--hidden");
        return;
    }
    list.innerHTML = matches
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
            setFormSelectedMovie(Number.isFinite(movieId) ? movieId : null);
            hideShowtimeMovieSuggestions();
        });
    });
    list.classList.remove("movie-suggestion-list--hidden");
}

function hideShowtimeMovieSuggestions() {
    const list = document.getElementById("showtimeMovieSuggestions");
    if (!list) {
        return;
    }
    list.innerHTML = "";
    list.classList.add("movie-suggestion-list--hidden");
}

function setFormSelectedMovie(movieId) {
    showtimeState.selectedMovieId = movieId ?? null;
    const hiddenInput = document.getElementById("showtimeMovieId");
    if (hiddenInput) {
        hiddenInput.value = movieId ?? "";
    }
    const searchInput = document.getElementById("showtimeMovieSearch");
    if (searchInput) {
        if (movieId) {
            const movie = showtimeState.movieMap.get(String(movieId));
            searchInput.value = movie
                    ? movie.title || movie.originalTitle || ""
                    : "";
        } else {
            searchInput.value = "";
        }
    }
    updateShowtimeMoviePreview("showtimeMoviePreview", movieId);
    updateShowtimeMovieClearButtonState();
}

function updateShowtimeMovieClearButtonState() {
    const clearBtn = document.getElementById("showtimeMovieClear");
    if (!clearBtn) {
        return;
    }
    const hasSelection = !!showtimeState.selectedMovieId;
    clearBtn.disabled = !hasSelection;
    clearBtn.classList.toggle("btn-danger", hasSelection);
    clearBtn.classList.toggle("btn-outline-secondary", !hasSelection);
}

function initFilterShowtimeMovieSearch() {
    const searchInput = document.getElementById("filterShowtimeKeyword");
    const clearBtn = document.getElementById("filterShowtimeMovieClear");
    if (!searchInput) {
        return;
    }
    const debounced = createDebounce(() => {
        renderFilterShowtimeMovieSuggestions(searchInput.value || "");
    }, 200);
    searchInput.addEventListener("input", debounced);
    searchInput.addEventListener("focus", () => {
        renderFilterShowtimeMovieSuggestions(searchInput.value || "");
    });
    searchInput.addEventListener("blur", (event) => {
        const related = event.relatedTarget;
        if (related && related.classList?.contains("movie-suggestion-item")) {
            return;
        }
        hideFilterShowtimeMovieSuggestions();
    });
    clearBtn?.addEventListener("click", () => {
        setFilterSelectedMovie(null, { triggerFetch: true });
        hideFilterShowtimeMovieSuggestions();
    });
    updateFilterShowtimeMovieClearButtonState();
}

function renderFilterShowtimeMovieSuggestions(keyword = "") {
    const list = document.getElementById("filterShowtimeMovieSuggestions");
    if (!list) {
        return;
    }
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) {
        hideFilterShowtimeMovieSuggestions();
        return;
    }
    const matches = showtimeState.movies
        .filter((movie) => {
            const title = (movie.title || "").toLowerCase();
            const original = (movie.originalTitle || "").toLowerCase();
            return title.includes(normalized) || original.includes(normalized);
        })
        .slice(0, 8);
    if (!matches.length) {
        list.innerHTML = `<p class="text-warning small mb-0">Không tìm thấy phim "${keyword}".</p>`;
        list.classList.remove("movie-suggestion-list--hidden");
        return;
    }
    list.innerHTML = matches
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
            setFilterSelectedMovie(Number.isFinite(movieId) ? movieId : null, { triggerFetch: true });
            hideFilterShowtimeMovieSuggestions();
        });
    });
    list.classList.remove("movie-suggestion-list--hidden");
}

function hideFilterShowtimeMovieSuggestions() {
    const list = document.getElementById("filterShowtimeMovieSuggestions");
    if (!list) {
        return;
    }
    list.innerHTML = "";
    list.classList.add("movie-suggestion-list--hidden");
}

function setFilterSelectedMovie(movieId, options = {}) {
    const shouldTriggerFetch = Boolean(options.triggerFetch);
    showtimeState.filterSelectedMovieId = movieId ?? null;
    const hiddenInput = document.getElementById("filterShowtimeMovie");
    if (hiddenInput) {
        hiddenInput.value = movieId ?? "";
    }
    const searchInput = document.getElementById("filterShowtimeKeyword");
    if (searchInput) {
        if (movieId) {
            const movie = showtimeState.movieMap.get(String(movieId));
            searchInput.value = movie ? movie.title || movie.originalTitle || "" : "";
        } else {
            searchInput.value = "";
        }
    }
    const errorEl = document.getElementById("filterShowtimeMovieError");
    if (errorEl) {
        errorEl.textContent = "";
    }
    updateShowtimeMoviePreview("filterShowtimeMoviePreview", movieId);
    updateFilterShowtimeMovieClearButtonState();
    if (shouldTriggerFetch) {
        fetchShowtimes();
    }
}

function updateFilterShowtimeMovieClearButtonState() {
    const clearBtn = document.getElementById("filterShowtimeMovieClear");
    if (!clearBtn) {
        return;
    }
    const hasSelection = !!showtimeState.filterSelectedMovieId;
    clearBtn.disabled = !hasSelection;
    clearBtn.classList.toggle("btn-danger", hasSelection);
    clearBtn.classList.toggle("btn-outline-secondary", !hasSelection);
}

function updateShowtimeMoviePreview(containerId, movieId) {
    const preview = document.getElementById(containerId);
    if (!preview) {
        return;
    }
    if (!movieId) {
        preview.innerHTML = "";
        preview.classList.add("movie-preview--hidden");
        return;
    }
    const movie = showtimeState.movieMap.get(String(movieId));
    preview.classList.remove("movie-preview--hidden");
    if (!movie) {
        preview.innerHTML = `<p class="text-warning mb-0">Không tìm thấy phim #${movieId}.</p>`;
        return;
    }
    const poster = movie.posterUrl || "https://via.placeholder.com/64x96.png?text=Poster";
    const originalLine = movie.originalTitle
        ? `<span class="movie-preview-original">${movie.originalTitle}</span>`
        : "";
    const statusLine = movie.status ? `<span>Trạng thái: ${statusLabel(movie.status)}</span>` : "";
    preview.innerHTML = `
        <img src="${poster}" alt="${movie.title || ""}">
        <div class="movie-preview-info">
            <h6>${movie.title ?? "-"}</h6>
            ${originalLine}
            ${statusLine}
        </div>
    `;
}

function findMovieByKeyword(keyword) {
    if (!keyword) {
        return null;
    }
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) {
        return null;
    }
    return showtimeState.movies.find((movie) => {
        const title = (movie.title || "").toLowerCase();
        const original = (movie.originalTitle || "").toLowerCase();
        return title.includes(normalized) || original.includes(normalized);
    }) || null;
}

async function submitShowtimeForm(event) {
    event.preventDefault();
    if (showtimeState.submitting) return;

    const payload = buildShowtimePayload();
    const startDateValue = document.getElementById("showtimeStartDate")?.value || "";
    const startTimeValue = document.getElementById("showtimeStartTime")?.value || "";
    const validationErrors = validateShowtimePayload(payload, { startDateValue, startTimeValue });
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
            throw new Error(error.message || "Không thể lưu suất chiếu.");
        }
        showSuccessToast("Lưu thành công!");
        showtimeDataBus.dispatch("showtimes");
        resetShowtimeForm();
        fetchShowtimes();
        window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
        messageBox.textContent = error.message;
        messageBox.classList.add("text-warning");
    } finally {
        setSubmittingState(false, submitBtn);
    }
}

function buildShowtimePayload() {
    const startDateValue = document.getElementById("showtimeStartDate")?.value || "";
    const startTimeValue = document.getElementById("showtimeStartTime")?.value || "";
    const normalizedStart = combineDateAndTime(startDateValue, startTimeValue);
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

function validateShowtimePayload(payload, options = {}) {
    const errors = [];
    const startDateValue = options.startDateValue || "";
    const startTimeValue = options.startTimeValue || "";
    if (!payload.movieId) {
        errors.push({ field: "showtimeMovieSearch", message: "Vui lòng chọn phim *" });
    }
    if (!payload.auditoriumId) {
        errors.push({ field: "showtimeAuditoriumId", message: "Vui lòng chọn phòng chiếu *" });
    }
    if (!startDateValue) {
        errors.push({ field: "showtimeStartDate", message: "Vui lòng chọn ngày bắt đầu *" });
    }
    if (!startTimeValue) {
        errors.push({ field: "showtimeStartTime", message: "Vui lòng chọn giờ bắt đầu *" });
    } else if (!isWithinAllowedStartTime(startTimeValue)) {
        errors.push({
            field: "showtimeStartTime",
            message: "Giờ bắt đầu phải nằm trong khoảng 08:00 - 22:00"
        });
    }
    if (!payload.startTime && startDateValue && startTimeValue) {
        errors.push({ field: "showtimeStartTime", message: "Giờ bắt đầu không hợp lệ" });
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
    [
        "showtimeMovieSearch",
        "showtimeAuditoriumId",
        "showtimeStartDate",
        "showtimeStartTime",
        "showtimeRepeatUntil"
    ].forEach((id) => {
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
    setFormSelectedMovie(null);
    hideShowtimeMovieSuggestions();
    clearShowtimeErrors();
    syncTimeSuggestionButtons();
}

async function editShowtime(id) {
    try {
        const response = await fetch(showtimeApi.detail(id));
        if (!response.ok) throw new Error("Không tìm thấy suất chiếu.");
        const showtime = await response.json();
        document.getElementById("showtimeId").value = showtime.id;
        document.getElementById("showtimeFormTitle").textContent = `Chỉnh sửa suất chiếu #${showtime.id}`;
        setFormSelectedMovie(showtime.movieId ?? null);
        document.getElementById("showtimeAuditoriumId").value = showtime.auditoriumId ?? "";
        const startParts = splitDateAndTime(showtime.startTime);
        const startDateInput = document.getElementById("showtimeStartDate");
        if (startDateInput) {
            startDateInput.value = startParts.datePart;
        }
        document.getElementById("showtimeStartTime").value = startParts.timePart;
        syncTimeSuggestionButtons();
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

function deleteShowtime(id) {
    const confirmAction = () => performDeleteShowtime(id);
    if (typeof openAdminConfirmDialog === "function") {
        openAdminConfirmDialog({
            title: "Xóa suất chiếu",
            message: "Bạn chắc chắn?",
            confirmLabel: "Xóa",
            confirmVariant: "danger",
            cancelLabel: "Hủy",
            onConfirm: confirmAction
        });
    } else if (confirm("Bạn chắc chắn?")) {
        confirmAction();
    }
}

async function performDeleteShowtime(id) {
    try {
        const response = await fetch(showtimeApi.delete(id), { method: "DELETE" });
        if (!response.ok) throw new Error("Không thể xóa suất chiếu.");
        fetchShowtimes();
        showtimeDataBus.dispatch("showtimes");
    } catch (error) {
        alert(error.message);
    }
}

function formatShowDate(value) {
    if (!value) return "-";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleDateString("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric"
    });
}

function splitDateAndTime(value) {
    if (!value) {
        return { datePart: "", timePart: "" };
    }
    const normalized = value.replace(" ", "T");
    const [datePart = "", rest = ""] = normalized.split("T");
    if (!rest) {
        return { datePart, timePart: "" };
    }
    const timeMatch = rest.match(/^(\d{2}:\d{2})/);
    return {
        datePart,
        timePart: timeMatch ? timeMatch[1] : ""
    };
}

function combineDateAndTime(dateValue, timeValue) {
    if (!dateValue || !timeValue) {
        return null;
    }
    const normalizedTime = normalizeTimeValue(timeValue);
    if (!normalizedTime) {
        return null;
    }
    return `${dateValue}T${normalizedTime}`;
}

function normalizeTimeValue(value) {
    if (!value) {
        return null;
    }
    const segments = value.split(":");
    if (segments.length < 2) {
        return null;
    }
    const [hours, minutes, seconds = "00"] = segments;
    const trimmedSeconds = seconds.split(".")[0];
    if (!hours || !minutes) {
        return null;
    }
    return `${hours.padStart(2, "0")}:${minutes.padStart(2, "0")}:${trimmedSeconds.padStart(2, "0")}`;
}

function valueOrNull(value) {
    return value ? Number(value) : null;
}

function isWithinAllowedStartTime(value) {
    const minutes = timeStringToMinutes(value);
    if (minutes === null) {
        return false;
    }
    return minutes >= SHOWTIME_MINUTES_MIN && minutes <= SHOWTIME_MINUTES_MAX;
}

function timeStringToMinutes(value) {
    if (!value) {
        return null;
    }
    const [hours, minutes] = value.split(":");
    if (hours === undefined || minutes === undefined) {
        return null;
    }
    const h = Number(hours);
    const m = Number(minutes);
    if (Number.isNaN(h) || Number.isNaN(m)) {
        return null;
    }
    return h * 60 + m;
}
