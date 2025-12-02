const promotionApi = {
    list: "/api/admin/promotions",
    detail: (id) => `/api/admin/promotions/${id}`,
    create: "/api/admin/promotions",
    update: (id) => `/api/admin/promotions/${id}`,
    delete: (id) => `/api/admin/promotions/${id}`,
    upload: "/api/admin/uploads/promotion"
};

const promotionDataBus = window.AdminDataBus || {
    dispatch: () => {},
    subscribe: () => () => {}
};

const PROMOTION_PAGE_SIZE = 8;

const promotionState = {
    page: 0,
    totalPages: 0,
    submitting: false,
    filters: {
        keyword: "",
        status: "",
        fromDate: "",
        toDate: ""
    }
};

var promotionDebounceFactory = window.__ADMIN_DEBOUNCE_FACTORY__;
if (typeof promotionDebounceFactory !== "function") {
    promotionDebounceFactory = function (fn, delay = 300) {
        let timer;
        return function (...args) {
            clearTimeout(timer);
            timer = setTimeout(() => fn.apply(this, args), delay);
        };
    };
    window.__ADMIN_DEBOUNCE_FACTORY__ = promotionDebounceFactory;
}

document.addEventListener("DOMContentLoaded", () => {
    if (document.getElementById("promotionForm")) {
        initPromotionAdminSection();
    }
});

function initPromotionAdminSection() {
    const form = document.getElementById("promotionForm");
    const resetBtn = document.getElementById("promotionResetBtn");
    const keywordInput = document.getElementById("promotionKeyword");
    const filterApplyBtn = document.getElementById("promotionFilterApplyBtn");
    const filterResetBtn = document.getElementById("promotionFilterResetBtn");
    const thumbBtn = document.getElementById("selectPromotionThumbnailBtn");
    const thumbFile = document.getElementById("promotionThumbnailFile");
    const imgContentBtn = document.getElementById("selectPromotionImgContentBtn");
    const imgContentFile = document.getElementById("promotionImgContentFile");

    fetchPromotions();

    form?.addEventListener("submit", submitPromotionForm);
    resetBtn?.addEventListener("click", resetPromotionForm);
    filterApplyBtn?.addEventListener("click", applyPromotionFilters);
    filterResetBtn?.addEventListener("click", resetPromotionFilters);
    if (keywordInput) {
        keywordInput.addEventListener("input", promotionDebounceFactory(() => {
            promotionState.filters.keyword = keywordInput.value.trim();
            promotionState.page = 0;
            fetchPromotions();
        }, 400));
    }
    if (thumbBtn && thumbFile) {
        thumbBtn.addEventListener("click", () => thumbFile.click());
        thumbFile.addEventListener("change", () => handlePromotionUpload(thumbFile, "promotionThumbnail"));
    }
    if (imgContentBtn && imgContentFile) {
        imgContentBtn.addEventListener("click", () => imgContentFile.click());
        imgContentFile.addEventListener("change", () => handlePromotionUpload(imgContentFile, "promotionImgContent"));
    }
}

async function fetchPromotions(page = promotionState.page || 0) {
    const params = new URLSearchParams();
    params.set("page", page);
    params.set("size", PROMOTION_PAGE_SIZE);
    params.set("sort", "publishedDate,desc");
    if (promotionState.filters.keyword) {
        params.set("keyword", promotionState.filters.keyword);
    }
    if (promotionState.filters.status) {
        params.set("active", promotionState.filters.status);
    }
    if (promotionState.filters.fromDate) {
        params.set("fromDate", promotionState.filters.fromDate);
    }
    if (promotionState.filters.toDate) {
        params.set("toDate", promotionState.filters.toDate);
    }

    const tableBody = document.querySelector("#promotionTable tbody");
    const counter = document.getElementById("promotionCount");
    if (tableBody) {
        tableBody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-light">Đang tải dữ liệu...</td></tr>`;
    }

    try {
        const response = await fetch(`${promotionApi.list}?${params.toString()}`);
        if (!response.ok) {
            throw new Error("Không thể tải khuyến mãi");
        }
        const data = await response.json();
        promotionState.page = data.page ?? 0;
        promotionState.totalPages = data.totalPages ?? 0;
        renderPromotionTable(data.content ?? []);
        renderPromotionPagination(promotionState.page, promotionState.totalPages);
        if (counter) {
            counter.textContent = `${data.totalElements ?? 0} items`;
        }
    } catch (error) {
        if (tableBody) {
            tableBody.innerHTML = `<tr><td colspan="6" class="text-center text-danger py-4">${error.message}</td></tr>`;
        }
        const pagination = document.getElementById("promotionPagination");
        if (pagination) pagination.innerHTML = "";
    }
}

function renderPromotionTable(items) {
    const tbody = document.querySelector("#promotionTable tbody");
    if (!tbody) return;
    if (!items.length) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-light">Chưa có khuyến mãi nào</td></tr>`;
        return;
    }
    tbody.innerHTML = "";
    items.forEach((item, index) => {
        const rowNumber = index + 1 + promotionState.page * PROMOTION_PAGE_SIZE;
        const statusBadge = item.active
            ? '<span class="badge bg-success">ON</span>'
            : '<span class="badge bg-secondary">OFF</span>';
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${rowNumber}</td>
            <td>
                <div class="fw-semibold">${item.title ?? "-"}</div>
            </td>
            <td>${formatDateDisplay(item.publishedDate)}</td>
            <td>${statusBadge}</td>
            <td>${formatDateTimeDisplay(item.updatedAt)}</td>
            <td>
                <div class="d-flex gap-2">
                    <button type="button" class="btn btn-outline-light btn-sm" data-edit="${item.id}">Sửa</button>
                    <button type="button" class="btn btn-outline-danger btn-sm" data-delete="${item.id}">Xóa</button>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
    tbody.querySelectorAll("[data-edit]").forEach((btn) => {
        btn.addEventListener("click", () => editPromotion(Number(btn.dataset.edit)));
    });
    tbody.querySelectorAll("[data-delete]").forEach((btn) => {
        btn.addEventListener("click", () => deletePromotion(Number(btn.dataset.delete)));
    });
}

function renderPromotionPagination(page, totalPages) {
    const container = document.getElementById("promotionPagination");
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
            if (Number.isNaN(targetPage) || targetPage < 0 || targetPage >= totalPages) {
                return;
            }
            fetchPromotions(targetPage);
        });
    });
}

function applyPromotionFilters() {
    promotionState.filters.status = document.getElementById("promotionStatus").value;
    promotionState.filters.fromDate = document.getElementById("promotionFromDate").value;
    promotionState.filters.toDate = document.getElementById("promotionToDate").value;
    promotionState.page = 0;
    fetchPromotions();
}

function resetPromotionFilters() {
    const status = document.getElementById("promotionStatus");
    const from = document.getElementById("promotionFromDate");
    const to = document.getElementById("promotionToDate");
    const keyword = document.getElementById("promotionKeyword");
    if (status) status.value = "";
    if (from) from.value = "";
    if (to) to.value = "";
    if (keyword) keyword.value = "";
    promotionState.filters = { keyword: "", status: "", fromDate: "", toDate: "" };
    promotionState.page = 0;
    fetchPromotions();
}

async function submitPromotionForm(event) {
    event.preventDefault();
    if (promotionState.submitting) return;
    const payload = buildPromotionPayload();
    const validationErrors = validatePromotionPayload(payload);
    const messageBox = document.getElementById("promotionFormMessage");
    messageBox.textContent = "";
    messageBox.classList.remove("text-success", "text-warning");
    if (validationErrors.length > 0) {
        messageBox.textContent = validationErrors.map((error) => error.message).join(". ");
        const firstErrorField = document.getElementById(validationErrors[0].fieldId ?? "");
        firstErrorField?.scrollIntoView({ behavior: "smooth", block: "center" });
        firstErrorField?.focus();
        return;
    }
    const currentId = document.getElementById("promotionId").value;
    const method = currentId ? "PUT" : "POST";
    const url = currentId ? promotionApi.update(currentId) : promotionApi.create;

    promotionState.submitting = true;
    setPromotionSubmitting(true);
    try {
        const response = await fetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || "Không thể lưu khuyến mãi");
        }
        resetPromotionForm();
        fetchPromotions();
        promotionDataBus.dispatch("promotions");
        showSuccessToast?.("Lưu thành công!");
        window.scrollTo({ top: 0, behavior: "smooth" });
        messageBox.textContent = "";
    } catch (error) {
        messageBox.classList.remove("text-success");
        messageBox.classList.add("text-warning");
        messageBox.textContent = error.message;
    } finally {
        promotionState.submitting = false;
        setPromotionSubmitting(false);
    }
}

function buildPromotionPayload() {
    return {
        title: document.getElementById("promotionTitle").value.trim(),
        thumbnailUrl: document.getElementById("promotionThumbnail").value.trim() || null,
        imgContentUrl: document.getElementById("promotionImgContent").value.trim() || null,
        content: document.getElementById("promotionContent").value.trim(),
        publishedDate: document.getElementById("promotionPublishedDate").value || null,
        active: document.getElementById("promotionActive").checked
    };
}

function validatePromotionPayload(payload) {
    const errors = [];
    if (!payload.title) {
        errors.push({ message: "Vui lòng nhập tiêu đề", fieldId: "promotionTitle" });
    }
    if (!payload.content) {
        errors.push({ message: "Vui lòng nhập nội dung chi tiết", fieldId: "promotionContent" });
    }
    if (!payload.publishedDate) {
        errors.push({ message: "Chọn ngày phát hành", fieldId: "promotionPublishedDate" });
    }
    return errors;
}

function setPromotionSubmitting(isSubmitting) {
    const submitBtn = document.querySelector("#promotionForm button[type='submit']");
    if (submitBtn) {
        submitBtn.disabled = isSubmitting;
        submitBtn.innerHTML = isSubmitting ? "Đang lưu..." : "Lưu khuyến mãi";
    }
}

function resetPromotionForm() {
    const form = document.getElementById("promotionForm");
    form?.reset();
    document.getElementById("promotionId").value = "";
    document.getElementById("promotionFormMessage").textContent = "";
}

async function editPromotion(id) {
    try {
        const response = await fetch(promotionApi.detail(id));
        if (!response.ok) {
            throw new Error("Không thể tải khuyến mãi");
        }
        const data = await response.json();
        document.getElementById("promotionId").value = data.id;
        document.getElementById("promotionTitle").value = data.title ?? "";
        document.getElementById("promotionThumbnail").value = data.thumbnailUrl ?? "";
        document.getElementById("promotionImgContent").value = data.imgContentUrl ?? "";
        document.getElementById("promotionContent").value = data.content ?? "";
        document.getElementById("promotionPublishedDate").value = (data.publishedDate ?? "").substring(0, 10);
        document.getElementById("promotionActive").checked = Boolean(data.active);
        document.getElementById("promotionFormTitle").textContent = `Chỉnh sửa khuyến mãi`;
        window.activateAdminTab ? window.activateAdminTab("crud-promotions") : window.showAdminTab?.("crud-promotions");
        document.getElementById("promotionForm").scrollIntoView({ behavior: "smooth", block: "start" });
    } catch (error) {
        openAdminNotice?.({
            title: "Thông báo",
            message: error.message || "Không thể tải khuyến mãi",
            variant: "warning"
        });
    }
}

function deletePromotion(id) {
    openAdminConfirmDialog?.({
        title: "Xóa khuyến mãi",
        message: "Bạn có chắc muốn xóa khuyến mãi này?",
        onConfirm: async () => {
            try {
                const response = await fetch(promotionApi.delete(id), { method: "DELETE" });
                  if (!response.ok) {
                      throw new Error("Không thể xóa khuyến mãi");
                  }
                  fetchPromotions();
                  promotionDataBus.dispatch("promotions");
              } catch (error) {
                  openAdminNotice?.({
                      title: "Thông báo",
                      message: error.message || "Không thể xóa khuyến mãi",
                    variant: "warning"
                });
            }
        }
    });
}

async function handlePromotionUpload(fileInput, targetInputId) {
    if (!fileInput?.files?.length) return;
    const file = fileInput.files[0];
    const formData = new FormData();
    formData.append("file", file);
    try {
        const response = await fetch(promotionApi.upload, {
            method: "POST",
            body: formData
        });
        if (!response.ok) {
            throw new Error("Không thể tải ảnh");
        }
        const data = await response.json();
        document.getElementById(targetInputId).value = data.path ?? "";
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

function formatDateDisplay(value) {
    if (!value) return "-";
    try {
        const date = new Date(value);
        return date.toLocaleDateString("vi-VN");
    } catch {
        return value;
    }
}

function formatDateTimeDisplay(value) {
    if (!value) return "-";
    try {
        const date = new Date(value);
        return date.toLocaleString("vi-VN");
    } catch {
        return value;
    }
}
