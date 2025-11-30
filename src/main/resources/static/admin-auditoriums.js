const auditoriumApi = {
    list: "/api/admin/auditoriums",
    detail: (id) => `/api/admin/auditoriums/${id}`,
    create: "/api/admin/auditoriums",
    update: (id) => `/api/admin/auditoriums/${id}`,
    delete: (id) => `/api/admin/auditoriums/${id}`,
    toggleActive: (id, active) => `/api/admin/auditoriums/${id}/active?active=${active}`
};

const auditoriumDataBus = window.AdminDataBus || {
    dispatch: () => {},
    subscribe: () => () => {}
};

const AUD_PAGE_SIZE = 10;
const AUD_ROWS_MIN = 1;
const AUD_ROWS_MAX = 26;
const AUD_COLUMNS_MIN = 1;
const AUD_COLUMNS_MAX = 30;
const auditoriumState = {
    page: 0,
    totalPages: 0,
    submitting: false
};
const auditoriumTextCompare = new Intl.Collator("vi", { sensitivity: "base" });
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

document.addEventListener("DOMContentLoaded", () => {
    if (document.getElementById("auditoriumForm")) {
        initAuditoriumAdmin();
    }
});

async function initAuditoriumAdmin() {
    initNumericStepperControls();
    bindAuditoriumForm();
    bindAuditoriumFilters();
    fetchAuditoriums();
}

function bindAuditoriumForm() {
    const form = document.getElementById("auditoriumForm");
    const resetBtn = document.getElementById("auditoriumResetBtn");
    form.addEventListener("submit", submitAuditoriumForm);
    resetBtn?.addEventListener("click", () => resetAuditoriumForm());
}

function bindAuditoriumFilters() {
    const filterForm = document.getElementById("auditoriumFilterForm");
    const nameInput = document.getElementById("filterAuditoriumName");
    const statusSelect = document.getElementById("filterAuditoriumStatus");
    const debouncedSearch = createDebounce(() => fetchAuditoriums(0));
    filterForm.addEventListener("submit", (event) => {
        event.preventDefault();
        fetchAuditoriums(0);
    });
    nameInput?.addEventListener("input", debouncedSearch);
    statusSelect?.addEventListener("change", () => fetchAuditoriums(0));
}

async function fetchAuditoriums(page = 0) {
    const tbody = document.querySelector("#auditoriumTable tbody");
    const counter = document.getElementById("auditoriumCount");
    if (!tbody || !counter) return;
    tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-light">Đang tải dữ liệu...</td></tr>`;

    const params = new URLSearchParams();
    const filterForm = document.getElementById("auditoriumFilterForm");
    const formData = new FormData(filterForm);
    for (const [key, value] of formData.entries()) {
        if (value) {
            params.append(key, value);
        }
    }
    params.set("page", page);
    params.set("size", AUD_PAGE_SIZE);
    params.set("sort", "name,asc");

    try {
        const response = await fetch(`${auditoriumApi.list}?${params.toString()}`);
        if (!response.ok) throw new Error("Không thể tải danh sách phòng chiếu");
        const data = await response.json();
        auditoriumState.page = data.page ?? 0;
        auditoriumState.totalPages = data.totalPages ?? 0;
        counter.textContent = `${data.totalElements ?? 0} items`;
        renderAuditoriumTable(Array.isArray(data.content) ? data.content : []);
        renderAuditoriumPagination(auditoriumState.page, auditoriumState.totalPages);
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4">${error.message}</td></tr>`;
        counter.textContent = "0 items";
        document.getElementById("auditoriumPagination").innerHTML = "";
    }
}

function renderAuditoriumTable(items) {
    const tbody = document.querySelector("#auditoriumTable tbody");
    if (!tbody) return;
    if (!items.length) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-light">Chưa có phòng chiếu nào</td></tr>`;
        return;
    }
    tbody.innerHTML = "";
    const sortedItems = items.slice().sort((a, b) =>
        auditoriumTextCompare.compare(a?.name || "", b?.name || "")
    );
    sortedItems.forEach((item, index) => {
        const totalSeats = calculateTotalSeats(item.numberOfRows, item.numberOfColumns);
        const toggleLabel = item.active ? "Vô hiệu hóa" : "Kích hoạt";
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${index + 1 + auditoriumState.page * AUD_PAGE_SIZE}</td>
            <td>${item.name ?? "-"}</td>
            <td>${item.numberOfRows ?? "-"}</td>
            <td>${item.numberOfColumns ?? "-"}</td>
            <td>${totalSeats ?? "-"}</td>
            <td>${renderAuditoriumStatus(item.active)}</td>
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
                                data-toggle="${item.id}"
                                data-target-active="${item.active ? "false" : "true"}"
                                data-menu-role="toggle">${toggleLabel}</button>
                        <button type="button" data-delete="${item.id}" data-menu-role="delete">Xóa</button>
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
            editAuditorium(Number(btn.dataset.edit));
        })
    );
    tbody.querySelectorAll("[data-toggle]").forEach((btn) =>
        btn.addEventListener("click", () => {
            const id = Number(btn.dataset.toggle);
            const shouldActivate = btn.dataset.targetActive === "true";
            window.AdminActionMenus?.closeAll();
            confirmToggleAuditorium(id, shouldActivate);
        })
    );
    tbody.querySelectorAll("[data-delete]").forEach((btn) =>
        btn.addEventListener("click", () => {
            window.AdminActionMenus?.closeAll();
            deleteAuditorium(Number(btn.dataset.delete));
        })
    );
}

function renderAuditoriumPagination(page, totalPages) {
    const container = document.getElementById("auditoriumPagination");
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
            if (!Number.isNaN(targetPage)) {
                fetchAuditoriums(targetPage);
            }
        });
    });
}

async function submitAuditoriumForm(event) {
    event.preventDefault();
    if (auditoriumState.submitting) return;
    const payload = buildAuditoriumPayload();
    const errors = validateAuditoriumPayload(payload);
    clearAuditoriumErrors();
    const messageBox = document.getElementById("auditoriumFormMessage");
    messageBox.textContent = "";

    if (errors.length) {
        showAuditoriumErrors(errors);
        const firstField = document.getElementById(errors[0].field);
        firstField?.scrollIntoView({ behavior: "smooth", block: "center" });
        firstField?.focus({ preventScroll: true });
        return;
    }

    const submitBtn = document.querySelector("#auditoriumForm button[type='submit']");
    if (submitBtn && !submitBtn.dataset.originalHtml) {
        submitBtn.dataset.originalHtml = submitBtn.innerHTML;
    }
    setAuditoriumSubmitting(true, submitBtn);

    const currentId = document.getElementById("auditoriumId").value;
    const url = currentId ? auditoriumApi.update(currentId) : auditoriumApi.create;
    const method = currentId ? "PUT" : "POST";

    try {
        const response = await fetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || "Không thể lưu phòng chiếu");
        }
        showSuccessToast("Lưu thành công!");
        auditoriumDataBus.dispatch("auditoriums");
        resetAuditoriumForm();
        fetchAuditoriums(auditoriumState.page);
        if (typeof window.refreshShowtimeAuditoriums === "function") {
            window.refreshShowtimeAuditoriums();
        }
        window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
        messageBox.textContent = error.message;
        messageBox.classList.add("text-warning");
    } finally {
        setAuditoriumSubmitting(false, submitBtn);
    }
}

function buildAuditoriumPayload() {
    return {
        name: document.getElementById("auditoriumName").value.trim(),
        numberOfRows: numberFromInput(document.getElementById("auditoriumRows").value),
        numberOfColumns: numberFromInput(document.getElementById("auditoriumColumns").value),
        active: document.getElementById("auditoriumActive").checked
    };
}

function validateAuditoriumPayload(payload) {
    const errors = [];
    if (!payload.name) {
        errors.push({ field: "auditoriumName", message: "Vui lòng nhập tên phòng chiếu *" });
    }
    if (!payload.numberOfRows || payload.numberOfRows < AUD_ROWS_MIN) {
        errors.push({ field: "auditoriumRows", message: `Số hàng ghế phải từ ${AUD_ROWS_MIN} trở lên *` });
    } else if (payload.numberOfRows > AUD_ROWS_MAX) {
        errors.push({ field: "auditoriumRows", message: `Số hàng ghế tối đa ${AUD_ROWS_MAX} (A-Z) *` });
    }
    if (!payload.numberOfColumns || payload.numberOfColumns < AUD_COLUMNS_MIN) {
        errors.push({ field: "auditoriumColumns", message: `Số ghế mỗi hàng phải từ ${AUD_COLUMNS_MIN} trở lên *` });
    } else if (payload.numberOfColumns > AUD_COLUMNS_MAX) {
        errors.push({ field: "auditoriumColumns", message: `Số ghế mỗi hàng tối đa ${AUD_COLUMNS_MAX} *` });
    }
    return errors;
}

function clearAuditoriumErrors() {
    ["auditoriumName", "auditoriumRows", "auditoriumColumns"].forEach((id) => {
        document.getElementById(id)?.classList.remove("is-invalid");
        const errorBox = document.getElementById(`${id}Error`);
        if (errorBox) errorBox.textContent = "";
    });
}

function showAuditoriumErrors(errors) {
    errors.forEach((err) => {
        document.getElementById(err.field)?.classList.add("is-invalid");
        const errorBox = document.getElementById(`${err.field}Error`);
        if (errorBox) errorBox.textContent = err.message;
    });
}

function setAuditoriumSubmitting(state, button) {
    auditoriumState.submitting = state;
    if (!button) return;
    if (state) {
        button.disabled = true;
        button.innerHTML = `<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang lưu...`;
    } else {
        button.disabled = false;
        button.innerHTML = button.dataset.originalHtml || "Lưu phòng chiếu";
    }
}

function resetAuditoriumForm() {
    const form = document.getElementById("auditoriumForm");
    if (!form) return;
    form.reset();
    document.getElementById("auditoriumId").value = "";
    document.getElementById("auditoriumFormTitle").textContent = "Thêm phòng chiếu";
    document.getElementById("auditoriumFormMessage").textContent = "";
    clearAuditoriumErrors();
    document.getElementById("auditoriumActive").checked = true;
}

async function editAuditorium(id) {
    try {
        const response = await fetch(auditoriumApi.detail(id));
        if (!response.ok) throw new Error("Không tìm thấy phòng chiếu");
        const data = await response.json();
        document.getElementById("auditoriumId").value = data.id ?? "";
        document.getElementById("auditoriumName").value = data.name ?? "";
        document.getElementById("auditoriumRows").value = data.numberOfRows ?? "";
        document.getElementById("auditoriumColumns").value = data.numberOfColumns ?? "";
        document.getElementById("auditoriumActive").checked = Boolean(data.active);
        const titleText = data.name ? `Chỉnh sửa phòng chiếu - ${data.name}` : "Chỉnh sửa phòng chiếu";
        document.getElementById("auditoriumFormTitle").textContent = titleText;
        document.getElementById("auditoriumFormMessage").textContent = "";
        clearAuditoriumErrors();
        window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
        alert(error.message);
    }
}

function deleteAuditorium(id) {
    const confirmAction = () => performDeleteAuditorium(id);
    if (typeof openAdminConfirmDialog === "function") {
        openAdminConfirmDialog({
            title: "Xóa phòng chiếu",
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

function confirmToggleAuditorium(id, shouldActivate) {
    const actionLabel = shouldActivate ? "kích hoạt" : "vô hiệu hóa";
    const confirmAction = () => toggleAuditoriumActive(id, shouldActivate);
    if (typeof openAdminConfirmDialog === "function") {
        openAdminConfirmDialog({
            title: `${shouldActivate ? "Kích hoạt" : "Vô hiệu hóa"} phòng chiếu`,
            message: `Bạn có chắc muốn ${actionLabel} phòng chiếu này?`,
            confirmLabel: "Xác nhận",
            confirmVariant: shouldActivate ? "primary" : "danger",
            cancelLabel: "Hủy",
            onConfirm: confirmAction
        });
    } else if (confirm(`Bạn có chắc muốn ${actionLabel} phòng chiếu này?`)) {
        confirmAction();
    }
}

async function toggleAuditoriumActive(id, shouldActivate) {
    try {
        const response = await fetch(auditoriumApi.toggleActive(id, shouldActivate), { method: "PATCH" });
        if (!response.ok) {
            throw new Error("Không thể cập nhật trạng thái phòng chiếu");
        }
        fetchAuditoriums(auditoriumState.page);
        auditoriumDataBus.dispatch("auditoriums");
    } catch (error) {
        alert(error.message);
    }
}

async function performDeleteAuditorium(id) {
    try {
        const response = await fetch(auditoriumApi.delete(id), { method: "DELETE" });
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            let message = error.message || "Không thể xóa phòng chiếu";
            if (response.status === 409 && !error.message) {
                message = "Phòng chiếu này vẫn còn suất chiếu đang sử dụng.";
            }
            const err = new Error(message);
            err.status = response.status;
            throw err;
        }
        showSuccessToast("Đã xóa phòng chiếu");
        auditoriumDataBus.dispatch("auditoriums");
        fetchAuditoriums(auditoriumState.page);
        if (typeof window.refreshShowtimeAuditoriums === "function") {
            window.refreshShowtimeAuditoriums();
        }
    } catch (error) {
        const conflictDetected =
            error.status === 409
            || (error.message && error.message.includes("FK_Showtimes_Auditoriums"))
            || (error.message && error.message.toLowerCase().includes("suất chiếu"));
        if (conflictDetected) {
            openAdminNotice?.({
                title: "Không thể xóa",
                message: error.message || "Phòng chiếu này vẫn còn suất chiếu đang sử dụng. Hãy xóa hoặc chuyển tất cả suất chiếu trước khi xóa phòng.",
                variant: "warning"
            });
        } else {
            alert(error.message);
        }
    }
}

function renderAuditoriumStatus(active) {
    return active ? '<span class="badge bg-success">ON</span>' : '<span class="badge bg-secondary">OFF</span>';
}

function calculateTotalSeats(rows, columns) {
    if (rows == null || columns == null) {
        return null;
    }
    if (Number.isNaN(Number(rows)) || Number.isNaN(Number(columns))) {
        return null;
    }
    const totalRows = Number(rows);
    const totalCols = Number(columns);
    let totalSeats = totalRows * totalCols;
    if (totalCols % 2 !== 0) {
        totalSeats -= determineCoupleRows(totalRows);
    }
    return Math.max(0, totalSeats);
}

function determineCoupleRows(totalRows) {
    if (totalRows >= 20) {
        return 2;
    }
    if (totalRows >= 10) {
        return 1;
    }
    return 0;
}

function numberFromInput(value) {
    if (value === null || value === undefined || value === "") {
        return null;
    }
    const num = Number(value);
    return Number.isNaN(num) ? null : num;
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
