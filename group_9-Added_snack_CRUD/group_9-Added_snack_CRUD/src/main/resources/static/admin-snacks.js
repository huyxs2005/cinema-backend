const snackApi = {
    list: "/api/admin/snacks",
    detail: (id) => `/api/admin/snacks/${id}`,
    create: "/api/admin/snacks",
    update: (id) => `/api/admin/snacks/${id}`,
    delete: (id) => `/api/admin/snacks/${id}`,
    upload: "/api/admin/uploads/snack"
};

const snackState = {
    page: 0,
    totalPages: 0,
    submitting: false,
    filters: {
        keyword: "",
        category: "",
        available: ""
    }
};

const snackDebounceFactory = typeof window.__ADMIN_DEBOUNCE_FACTORY__ === "function"
    ? window.__ADMIN_DEBOUNCE_FACTORY__
    : function (fn, delay = 300) {
        let timer;
        return function (...args) {
            clearTimeout(timer);
            timer = setTimeout(() => fn.apply(this, args), delay);
        };
    };

document.addEventListener("DOMContentLoaded", () => {
    if (document.getElementById("snackForm")) {
        initSnackAdminSection();
    }
});

function initSnackAdminSection() {
    bindSnackForm();
    bindSnackFilters();
    bindSnackUpload();
    fetchSnacks();
}

function bindSnackForm() {
    const form = document.getElementById("snackForm");
    const resetBtn = document.getElementById("snackResetBtn");
    form?.addEventListener("submit", submitSnackForm);
    resetBtn?.addEventListener("click", resetSnackForm);
}

function bindSnackFilters() {
    const filterForm = document.getElementById("snackFilterForm");
    if (!filterForm) return;
    const keywordInput = document.getElementById("snackKeyword");
    const resetBtn = document.getElementById("snackFilterResetBtn");
    filterForm.addEventListener("submit", (event) => {
        event.preventDefault();
        applySnackFilters();
    });
    resetBtn?.addEventListener("click", () => {
        filterForm.reset();
        snackState.filters = { keyword: "", category: "", available: "" };
        fetchSnacks(0);
    });
    keywordInput?.addEventListener("input", snackDebounceFactory(() => {
        snackState.filters.keyword = keywordInput.value.trim();
        fetchSnacks(0);
    }, 400));
}

function bindSnackUpload() {
    const button = document.getElementById("snackImageBtn");
    const fileInput = document.getElementById("snackImageFile");
    button?.addEventListener("click", () => fileInput?.click());
    fileInput?.addEventListener("change", () => handleSnackUpload(fileInput));
}

async function fetchSnacks(page = snackState.page || 0) {
    const tableBody = document.querySelector("#snackTable tbody");
    const counter = document.getElementById("snackCount");
    if (tableBody) {
        tableBody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-light">Đang tải dữ liệu...</td></tr>`;
    }
    const params = new URLSearchParams();
    params.set("page", page);
    params.set("size", 10);
    params.set("sort", "displayOrder,asc");
    if (snackState.filters.keyword) {
        params.set("keyword", snackState.filters.keyword);
    }
    if (snackState.filters.category) {
        params.set("category", snackState.filters.category);
    }
    if (snackState.filters.available) {
        params.set("available", snackState.filters.available);
    }
    try {
        const response = await fetch(`${snackApi.list}?${params.toString()}`);
        if (!response.ok) {
            throw new Error("Không thể tải danh sách snack");
        }
        const data = await response.json();
        snackState.page = data.page ?? 0;
        snackState.totalPages = data.totalPages ?? 0;
        renderSnackTable(Array.isArray(data.content) ? data.content : []);
        renderSnackPagination(snackState.page, snackState.totalPages);
        if (counter) {
            counter.textContent = `${data.totalElements ?? 0} mục`;
        }
    } catch (error) {
        if (tableBody) {
            tableBody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4">${error.message}</td></tr>`;
        }
        const pagination = document.getElementById("snackPagination");
        if (pagination) pagination.innerHTML = "";
        if (counter) counter.textContent = "0 mục";
    }
}

function renderSnackTable(items) {
    const tbody = document.querySelector("#snackTable tbody");
    if (!tbody) return;
    if (!items.length) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-light">Chưa có dữ liệu</td></tr>`;
        return;
    }
    tbody.innerHTML = "";
    items.forEach((item, index) => {
        const rowNumber = index + 1 + snackState.page * 10;
        const status = item.available
            ? '<span class="badge bg-success">ON</span>'
            : '<span class="badge bg-secondary">OFF</span>';
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${rowNumber}</td>
            <td>${item.name ?? "-"}</td>
            <td>${item.category ?? "-"}</td>
            <td>${formatCurrency(item.price)}</td>
            <td>${status}</td>
            <td>${formatDateTime(item.updatedAt)}</td>
            <td>
                <div class="d-flex gap-2">
                    <button type="button" class="btn btn-outline-light btn-sm" data-snack-edit="${item.id}">Sửa</button>
                    <button type="button" class="btn btn-outline-danger btn-sm" data-snack-delete="${item.id}">Xóa</button>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
    tbody.querySelectorAll("[data-snack-edit]").forEach((btn) => {
        btn.addEventListener("click", () => editSnack(Number(btn.dataset.snackEdit)));
    });
    tbody.querySelectorAll("[data-snack-delete]").forEach((btn) => {
        btn.addEventListener("click", () => deleteSnack(Number(btn.dataset.snackDelete)));
    });
}

function renderSnackPagination(page, totalPages) {
    const container = document.getElementById("snackPagination");
    if (!container) return;
    if (totalPages <= 1) {
        container.innerHTML = "";
        return;
    }
    const prevDisabled = page <= 0 ? "disabled" : "";
    const nextDisabled = page + 1 >= totalPages ? "disabled" : "";
    container.innerHTML = `
        <button class="btn btn-outline-light btn-sm" data-snack-page="${page - 1}" ${prevDisabled}>Trang trước</button>
        <span class="small">Trang ${page + 1}/${totalPages}</span>
        <button class="btn btn-outline-light btn-sm" data-snack-page="${page + 1}" ${nextDisabled}>Trang sau</button>
    `;
    container.querySelectorAll("[data-snack-page]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const target = Number(btn.dataset.snackPage);
            if (!Number.isNaN(target)) {
                fetchSnacks(target);
            }
        });
    });
}

function applySnackFilters() {
    snackState.filters.category = document.getElementById("snackCategoryFilter")?.value ?? "";
    snackState.filters.available = document.getElementById("snackAvailabilityFilter")?.value ?? "";
    fetchSnacks(0);
}

async function submitSnackForm(event) {
    event.preventDefault();
    if (snackState.submitting) return;
    const payload = buildSnackPayload();
    const errors = validateSnackPayload(payload);
    const messageBox = document.getElementById("snackFormMessage");
    messageBox.textContent = "";
    if (errors.length) {
        messageBox.textContent = errors.join(". ");
        return;
    }
    const currentId = document.getElementById("snackId").value;
    const method = currentId ? "PUT" : "POST";
    const url = currentId ? snackApi.update(currentId) : snackApi.create;
    snackState.submitting = true;
    setSnackSubmitting(true);
    try {
        const response = await fetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || "Không thể lưu snack");
        }
        showSuccessToast?.("Lưu thành công!");
        resetSnackForm();
        fetchSnacks(0);
    } catch (error) {
        messageBox.textContent = error.message;
    } finally {
        snackState.submitting = false;
        setSnackSubmitting(false);
    }
}

function buildSnackPayload() {
    const priceValue = parseFloat(document.getElementById("snackPrice").value);
    const orderValue = parseInt(document.getElementById("snackDisplayOrder").value, 10);
    const payload = {
        name: document.getElementById("snackName").value.trim(),
        category: document.getElementById("snackCategory").value,
        price: Number.isFinite(priceValue) ? Number(priceValue.toFixed(2)) : null,
        description: document.getElementById("snackDescription").value.trim() || null,
        servingSize: document.getElementById("snackServingSize").value.trim() || null,
        imageUrl: document.getElementById("snackImageUrl").value.trim() || null,
        displayOrder: Number.isInteger(orderValue) && orderValue > 0 ? orderValue : 1,
        available: document.getElementById("snackAvailable").checked
    };
    return payload;
}

function validateSnackPayload(payload) {
    const errors = [];
    if (!payload.name) {
        errors.push("Nhập tên sản phẩm");
    }
    if (!payload.category) {
        errors.push("Chọn phân loại");
    }
    if (payload.price === null || payload.price <= 0) {
        errors.push("Giá bán không hợp lệ");
    }
    if (!payload.displayOrder || payload.displayOrder < 1) {
        errors.push("Thứ tự phải ≥ 1");
    }
    return errors;
}

function setSnackSubmitting(isSubmitting) {
    const submitBtn = document.querySelector("#snackForm button[type='submit']");
    if (!submitBtn) return;
    submitBtn.disabled = isSubmitting;
    submitBtn.innerHTML = isSubmitting ? "Đang lưu..." : "Lưu sản phẩm";
}

function resetSnackForm() {
    const form = document.getElementById("snackForm");
    if (!form) return;
    form.reset();
    document.getElementById("snackId").value = "";
    document.getElementById("snackFormTitle").textContent = "Thêm sản phẩm quầy snack";
    document.getElementById("snackFormMessage").textContent = "";
    document.getElementById("snackDisplayOrder").value = 1;
    document.getElementById("snackAvailable").checked = true;
}

async function editSnack(id) {
    try {
        const response = await fetch(snackApi.detail(id));
        if (!response.ok) {
            throw new Error("Không thể tải snack");
        }
        const data = await response.json();
        document.getElementById("snackId").value = data.id ?? "";
        document.getElementById("snackName").value = data.name ?? "";
        document.getElementById("snackCategory").value = data.category ?? "";
        document.getElementById("snackPrice").value = data.price ?? "";
        document.getElementById("snackDescription").value = data.description ?? "";
        document.getElementById("snackServingSize").value = data.servingSize ?? "";
        document.getElementById("snackImageUrl").value = data.imageUrl ?? "";
        document.getElementById("snackDisplayOrder").value = data.displayOrder ?? 1;
        document.getElementById("snackAvailable").checked = Boolean(data.available);
        document.getElementById("snackFormTitle").textContent = `Chỉnh sửa - ${data.name ?? ""}`;
        window.activateAdminTab?.("crud-snacks");
        document.getElementById("snackForm").scrollIntoView({ behavior: "smooth", block: "start" });
    } catch (error) {
        openAdminNotice?.({
            title: "Thông báo",
            message: error.message || "Không thể tải snack",
            variant: "warning"
        });
    }
}

function deleteSnack(id) {
    openAdminConfirmDialog?.({
        title: "Xóa sản phẩm",
        message: "Bạn chắc chắn muốn xóa snack này?",
        confirmVariant: "danger",
        confirmLabel: "Xóa",
        onConfirm: async () => {
            try {
                const response = await fetch(snackApi.delete(id), { method: "DELETE" });
                if (!response.ok) {
                    throw new Error("Không thể xóa snack");
                }
                fetchSnacks(snackState.page);
            } catch (error) {
                openAdminNotice?.({
                    title: "Thông báo",
                    message: error.message || "Không thể xóa snack",
                    variant: "warning"
                });
            }
        }
    });
}

async function handleSnackUpload(fileInput) {
    if (!fileInput?.files?.length) return;
    const file = fileInput.files[0];
    const formData = new FormData();
    formData.append("file", file);
    try {
        const response = await fetch(snackApi.upload, {
            method: "POST",
            body: formData
        });
        if (!response.ok) {
            throw new Error("Không thể tải ảnh");
        }
        const data = await response.json();
        document.getElementById("snackImageUrl").value = data.path ?? "";
    } catch (error) {
        openAdminNotice?.({
            title: "Thông báo",
            message: error.message || "Không thể tải ảnh",
            variant: "warning"
        });
    } finally {
        fileInput.value = "";
    }
}

function formatCurrency(value) {
    if (value === null || value === undefined) return "-";
    try {
        return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value);
    } catch {
        return value;
    }
}

function formatDateTime(value) {
    if (!value) return "-";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "-";
    return date.toLocaleString("vi-VN");
}
