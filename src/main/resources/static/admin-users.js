if (window.__ADMIN_USERS_LOADED__) {
    console.warn("admin-users.js already initialized. Skipping duplicate load.");
} else {
window.__ADMIN_USERS_LOADED__ = true;

const userApi = {
    list: "/api/admin/users",
    detail: (id) => `/api/admin/users/${id}`,
    create: "/api/admin/users",
    update: (id) => `/api/admin/users/${id}`,
    status: (id) => `/api/admin/users/${id}/status`,
    delete: (id) => `/api/admin/users/${id}`,
    roles: "/api/admin/roles"
};

const userState = {
    page: 0,
    totalPages: 0,
    submitting: false,
    roles: []
};

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
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
const userTextCompare = new Intl.Collator("vi", { sensitivity: "base" });
const coalesce = (value, fallback) => (value === undefined || value === null ? fallback : value);
const safeText = (value) => {
    if (value === undefined || value === null) {
        return "-";
    }
    const trimmed = typeof value === "string" ? value.trim() : value;
    return trimmed === "" ? "-" : trimmed;
};

const escapeHtml = (value) => {
    if (value === undefined || value === null) {
        return "";
    }
    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
};

function bootUserAdmin() {
    if (document.getElementById("userForm")) {
        initUserAdmin().catch((error) => {
            console.error("Không thể khởi tạo CRUD khách hàng:", error);
        });
    }
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", bootUserAdmin);
} else {
    bootUserAdmin();
}

const withCredentials = (options = {}) => ({
    credentials: "same-origin",
    ...options
});

async function initUserAdmin() {
    console.info("Khởi động module CRUD khách hàng");
    await loadRoleOptions();
    bindUserForm();
    bindUserFilters();
    fetchUsers();
}

function bindUserForm() {
    const form = document.getElementById("userForm");
    if (!form) {
        console.warn("Không tìm thấy form người dùng");
        return;
    }
    const resetBtn = document.getElementById("userResetBtn");
    form.addEventListener("submit", submitUserForm);
    if (resetBtn) {
        resetBtn.addEventListener("click", () => resetUserForm());
    }
}

function bindUserFilters() {
    const filterForm = document.getElementById("userFilterForm");
    if (!filterForm) {
        console.warn("Không tìm thấy form lọc khách hàng");
        return;
    }
    const resetBtn = document.getElementById("userFilterReset");
    const keywordInput = document.getElementById("userSearchKeyword");
    const debouncedSearch = createDebounce(() => fetchUsers(0), 300);
    filterForm.addEventListener("submit", (event) => {
        event.preventDefault();
        fetchUsers(0);
    });
    if (resetBtn) {
        resetBtn.addEventListener("click", () => {
            filterForm.reset();
            fetchUsers(0);
        });
    }
    keywordInput?.addEventListener("input", debouncedSearch);
}

async function loadRoleOptions() {
    try {
        const response = await fetch(userApi.roles, withCredentials());
        if (!response.ok) throw new Error("Không thể tải danh sách vai trò");
        const roles = await response.json();
        userState.roles = Array.isArray(roles) ? roles : [];
        populateRoleSelect("userRole", userState.roles, "-- Chọn vai trò --");
        populateRoleSelect("filterUserRole", userState.roles, "Tất cả");
    } catch (error) {
        console.error(error);
    }
}

function populateRoleSelect(elementId, roles, placeholder) {
    const select = document.getElementById(elementId);
    if (!select) return;
    select.innerHTML = "";
    if (placeholder) {
        const opt = document.createElement("option");
        opt.value = "";
        opt.textContent = placeholder;
        select.appendChild(opt);
    }
    roles.forEach((role) => {
        const option = document.createElement("option");
        option.value = role.name;
        option.textContent = role.name;
        select.appendChild(option);
    });
}

async function fetchUsers(page = 0) {
    const tbody = document.querySelector("#userTable tbody");
    const counter = document.getElementById("userCount");
    if (!tbody || !counter) return;
    tbody.innerHTML = `<tr><td colspan="9" class="text-center py-4 text-light">Đang tải dữ liệu...</td></tr>`;

    const params = new URLSearchParams();
    const filterForm = document.getElementById("userFilterForm");
    if (filterForm) {
        const formData = new FormData(filterForm);
        for (const pair of formData.entries()) {
            const key = pair[0];
            const value = pair[1];
            if (value) {
                params.append(key, value);
            }
        }
    }
    params.set("page", page);
    params.set("size", 10);
    params.set("sort", "createdAt,desc");

    try {
        const response = await fetch(`${userApi.list}?${params.toString()}`, withCredentials());
        if (!response.ok) throw new Error("Không thể tải danh sách người dùng");
        const data = await response.json();
        userState.page = coalesce(data.page, 0);
        userState.totalPages = coalesce(data.totalPages, 0);
        counter.textContent = `${coalesce(data.totalElements, 0)} items`;
        renderUserTable(Array.isArray(data.content) ? data.content : []);
        renderUserPagination(userState.page, userState.totalPages);
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center text-danger py-4">${error.message}</td></tr>`;
        counter.textContent = "0 items";
        document.getElementById("userPagination").innerHTML = "";
        console.error("Lỗi tải danh sách người dùng:", error);
    }
}

function renderUserTable(items) {
    const tbody = document.querySelector("#userTable tbody");
    if (!tbody) return;
    if (!items.length) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center py-4 text-light">Chưa có người dùng nào</td></tr>`;
        return;
    }
    const currentUser = typeof window !== "undefined" ? window.CURRENT_USER ?? null : null;
    const currentUserId = currentUser?.userId ?? null;
    tbody.innerHTML = "";
    const sortedItems = items.slice().sort((a, b) =>
        userTextCompare.compare(a?.fullName || "", b?.fullName || "")
    );
    sortedItems.forEach((item, index) => {
        const isSelf = currentUserId != null && item.userId === currentUserId;
        const tr = document.createElement("tr");
        const toggleLabel = item.active ? "Vô hiệu hóa" : "Kích hoạt";
        const targetActive = item.active ? "false" : "true";
        const displayName = safeText(item.fullName);
        const displayEmail = safeText(item.email);
        const identityTitle = `${displayName === "-" ? "" : displayName}${displayEmail !== "-" ? ` | ${displayEmail}` : ""}`;
        const identityCell = `
            <div class="user-identity" title="${escapeHtml(identityTitle.trim())}">
                <span class="user-name">${escapeHtml(displayName)}</span>
                <span class="user-email">${escapeHtml(displayEmail)}</span>
            </div>
        `;
        const phoneTitle = safeText(item.phone);
        const lastLoginCell = buildRelativeTimeCell(item.lastLoginAt);
        const createdCell = buildRelativeTimeCell(item.createdAt);

        tr.innerHTML = `
            <td>${index + 1 + userState.page * 10}</td>
            <td>${identityCell}</td>
            <td title="${escapeHtml(phoneTitle)}">${escapeHtml(phoneTitle)}</td>
            <td>${safeText(item.role)}</td>
            <td>${renderUserStatus(item.active)}</td>
            <td>${lastLoginCell}</td>
            <td>${createdCell}</td>
            <td>
                <div class="user-action-menu-wrapper">
                    <button type="button"
                            class="btn btn-outline-light btn-sm action-menu-toggle"
                            aria-haspopup="true"
                            aria-expanded="false"
                            title="Mở hành động"
                            data-user-id="${item.userId}">⋮</button>
                    <div class="user-action-menu" role="menu">
                        <button type="button" data-user-menu-action="edit" data-user-menu-id="${item.userId}">Sửa</button>
                        <button type="button"
                                data-user-menu-action="toggle"
                                data-user-menu-id="${item.userId}"
                                data-user-target-active="${targetActive}">${toggleLabel}</button>
                        <button type="button"
                                data-user-menu-action="delete"
                                data-user-menu-id="${item.userId}"
                                ${isSelf ? "disabled title='Không thể xóa tài khoản đang đăng nhập'" : ""}>Xóa</button>
                    </div>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
    bindUserActionMenus(tbody);
}

function renderUserStatus(active) {
    return active ? '<span class="badge bg-success">ON</span>' : '<span class="badge bg-secondary">OFF</span>';
}

function renderUserPagination(page, totalPages) {
    const container = document.getElementById("userPagination");
    if (!container) return;
    if (totalPages <= 1) {
        container.innerHTML = "";
        return;
    }
    const prevDisabled = page <= 0 ? "disabled" : "";
    const nextDisabled = page + 1 >= totalPages ? "disabled" : "";
    container.innerHTML = `
        <button class="btn btn-outline-light btn-sm" data-user-page="${page - 1}" ${prevDisabled}>Trang trước</button>
        <span class="small">Trang ${page + 1}/${totalPages}</span>
        <button class="btn btn-outline-light btn-sm" data-user-page="${page + 1}" ${nextDisabled}>Trang sau</button>
    `;
    container.querySelectorAll("[data-user-page]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const target = Number(btn.dataset.userPage);
            if (!Number.isNaN(target)) {
                fetchUsers(target);
            }
        });
    });
}

async function submitUserForm(event) {
    event.preventDefault();
    if (userState.submitting) return;
    const payload = buildUserPayload();
    const errors = validateUserPayload(payload);
    clearUserErrors();
    const messageBox = document.getElementById("userFormMessage");
    messageBox.textContent = "";

    if (errors.length) {
        showUserErrors(errors);
        const first = document.getElementById(errors[0].field);
        if (first) {
            first.scrollIntoView({ behavior: "smooth", block: "center" });
            if (first.focus) {
                first.focus({ preventScroll: true });
            }
        }
        return;
    }

    const submitBtn = document.querySelector("#userForm button[type='submit']");
    if (submitBtn && !submitBtn.dataset.originalHtml) {
        submitBtn.dataset.originalHtml = submitBtn.innerHTML;
    }
    setUserSubmitting(true, submitBtn);
    const currentId = document.getElementById("userId").value;
    const url = currentId ? userApi.update(currentId) : userApi.create;
    const method = currentId ? "PUT" : "POST";

    const requestPayload = { ...payload };
    delete requestPayload.confirmPassword;

    try {
        const response = await fetch(url, withCredentials({
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(requestPayload)
        }));
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || "Không thể lưu người dùng");
        }
        showSuccessToast("Lưu thành công!");
        resetUserForm();
        fetchUsers(userState.page);
        window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
        messageBox.textContent = error.message;
        messageBox.classList.add("text-warning");
    } finally {
        setUserSubmitting(false, submitBtn);
    }
}

function buildUserPayload() {
    return {
        email: document.getElementById("userEmail").value.trim(),
        fullName: document.getElementById("userFullName").value.trim(),
        phone: document.getElementById("userPhone").value.trim(),
        role: document.getElementById("userRole").value,
        password: document.getElementById("userPassword").value,
        confirmPassword: document.getElementById("userConfirmPassword").value,
        active: document.getElementById("userActive").checked
    };
}

function validateUserPayload(payload) {
    const errors = [];
    const userIdInput = document.getElementById("userId");
    const isNewUser = !userIdInput || !userIdInput.value;

    if (!payload.email || !EMAIL_PATTERN.test(payload.email)) {
        errors.push({ field: "userEmail", message: "Email không hợp lệ" });
    }
    if (!payload.fullName) {
        errors.push({ field: "userFullName", message: "Vui lòng nhập họ tên" });
    } else if (payload.fullName.length > 22) {
        errors.push({ field: "userFullName", message: "Họ tên tối đa 22 ký tự" });
    }
    if (!payload.phone || payload.phone.length < 10 || payload.phone.length > 11 || !/^\d+$/.test(payload.phone)) {
        errors.push({ field: "userPhone", message: "Số điện thoại 10-11 chữ số" });
    }
    if (!payload.role) {
        errors.push({ field: "userRole", message: "Vui lòng chọn vai trò" });
    }

    if (isNewUser) {
        if (!payload.password) {
            errors.push({ field: "userPassword", message: "Vui lòng nhập mật khẩu" });
        } else if (payload.password.length < 8) {
            errors.push({ field: "userPassword", message: "Mật khẩu phải từ 8 kí tự" });
        }
        if (!payload.confirmPassword) {
            errors.push({ field: "userConfirmPassword", message: "Vui lòng xác nhận mật khẩu" });
        } else if (payload.password !== payload.confirmPassword) {
            errors.push({ field: "userConfirmPassword", message: "Mật khẩu xác nhận không khớp" });
        }
    } else {
        const wantsPasswordChange = Boolean(payload.password) || Boolean(payload.confirmPassword);
        if (wantsPasswordChange) {
            if (!payload.password) {
                errors.push({ field: "userPassword", message: "Vui lòng nhập mật khẩu mới" });
            } else if (payload.password.length < 8) {
                errors.push({ field: "userPassword", message: "Mật khẩu phải từ 8 kí tự" });
            }
            if (!payload.confirmPassword) {
                errors.push({ field: "userConfirmPassword", message: "Vui lòng xác nhận mật khẩu" });
            } else if (payload.password && payload.password !== payload.confirmPassword) {
                errors.push({ field: "userConfirmPassword", message: "Mật khẩu xác nhận không khớp" });
            }
        }
    }
    return errors;
}

function clearUserErrors() {
    ["userEmail", "userFullName", "userPhone", "userRole", "userPassword", "userConfirmPassword"].forEach((id) => {
        const element = document.getElementById(id);
        if (element) {
            element.classList.remove("is-invalid");
        }
        const errorBox = document.getElementById(`${id}Error`);
        if (errorBox) errorBox.textContent = "";
    });
}

function showUserErrors(errors) {
    errors.forEach((err) => {
        const element = document.getElementById(err.field);
        if (element) {
            element.classList.add("is-invalid");
        }
        const errorBox = document.getElementById(`${err.field}Error`);
        if (errorBox) errorBox.textContent = err.message;
    });
}

function setUserSubmitting(state, button) {
    userState.submitting = state;
    if (!button) return;
    if (state) {
        button.disabled = true;
        button.innerHTML = `<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang lưu...`;
    } else {
        button.disabled = false;
        button.innerHTML = button.dataset.originalHtml || "Lưu người dùng";
    }
}

function resetUserForm() {
    const form = document.getElementById("userForm");
    if (!form) return;
    form.reset();
    document.getElementById("userId").value = "";
    document.getElementById("userFormTitle").textContent = "Thêm người dùng";
    document.getElementById("userFormMessage").textContent = "";
    clearUserErrors();
    document.getElementById("userPassword").value = "";
    document.getElementById("userConfirmPassword").value = "";
    document.getElementById("userActive").checked = true;
}

async function editUser(id) {
    try {
        const response = await fetch(userApi.detail(id), withCredentials());
        if (!response.ok) throw new Error("Không tìm thấy người dùng");
        const data = await response.json();
        document.getElementById("userId").value = coalesce(data.userId, "");
        document.getElementById("userEmail").value = safeText(data.email) === "-" ? "" : data.email;
        document.getElementById("userFullName").value = safeText(data.fullName) === "-" ? "" : data.fullName;
        document.getElementById("userPhone").value = safeText(data.phone) === "-" ? "" : data.phone;
        document.getElementById("userRole").value = safeText(data.role) === "-" ? "" : data.role;
        document.getElementById("userActive").checked = Boolean(data.active);
        document.getElementById("userPassword").value = "";
        document.getElementById("userConfirmPassword").value = "";
        const identifierLabel = safeText(data.email) !== "-" ? data.email : safeText(data.phone);
        const title = identifierLabel && identifierLabel !== "-" ? `Chỉnh sửa tài khoản - ${identifierLabel}` : "Chỉnh sửa tài khoản";
        document.getElementById("userFormTitle").textContent = title;
        window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
        alert(error.message);
    }
}

function toggleUserActive(id, shouldActivate) {
    const confirmAction = () => changeUserStatus(id, shouldActivate);
    const message = shouldActivate ? "Kích hoạt người dùng này?" : "Vô hiệu hóa người dùng này?";
    if (typeof openAdminConfirmDialog === "function") {
        openAdminConfirmDialog({
            title: "Xác nhận",
            message,
            confirmLabel: "Xác nhận",
            cancelLabel: "Hủy",
            onConfirm: confirmAction
        });
    } else if (confirm(message)) {
        confirmAction();
    }
}

async function changeUserStatus(id, active) {

    try {

        const url = `${userApi.status(id)}?active=${active}`;

        const response = await fetch(url, withCredentials({ method: "PATCH" }));

        if (!response.ok) {

            const data = await response.json().catch(() => ({}));

            throw new Error(data.message || "Kh\xf4ng th\u1ec3 c\u1eadp nh\u1eadt tr\u1ea1ng th\xe1i");

        }

        fetchUsers(userState.page);

    } catch (error) {

        showUserErrorDialog(error.message);

    }

}


function confirmDeleteUser(id) {
    const proceed = () => deleteUser(id);
    if (typeof openAdminConfirmDialog === "function") {
        openAdminConfirmDialog({
            title: "Xác nhận",
            message: "Bạn có chắc muốn xóa tài khoản này?",
            confirmLabel: "Xóa",
            confirmVariant: "danger",
            cancelLabel: "Hủy",
            onConfirm: proceed
        });
    } else if (confirm("Bạn có chắc muốn xóa tài khoản này?")) {
        proceed();
    }
}

async function deleteUser(id) {

    try {

        const response = await fetch(userApi.delete(id), withCredentials({ method: "DELETE" }));

        if (!response.ok) {

            const data = await response.json().catch(() => ({}));

            throw new Error(data.message || "Kh\xf4ng th\u1ec3 x\xf3a ng\u01b0\u1eddi d\xf9ng");

        }

        showSuccessToast("\u0110\xe3 x\xf3a t\xe0i kho\u1ea3n");

        fetchUsers(userState.page);

    } catch (error) {

        showUserErrorDialog(error.message);

    }

}


function showUserErrorDialog(message) {
    const content = message || "\u0110\xe3 x\u1ea3y ra l\u1ed7i, vui l\u00f2ng th\u1eed l\u1ea1i.";

    if (typeof window.openAdminNotice === "function") {

        openAdminNotice({

            title: "Kh\u00f4ng th\u1ec3 th\u1ef1c hi\u1ec7n",

            message: content,

            buttonLabel: "\u0110\xe3 hi\u1ec3u",

            variant: "warning"

        });

    } else {

        alert(content);

    }

}



function formatDateTimeDisplay(value) {
    if (!value) return "-";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "-";
    return date.toLocaleString("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false
    });
}

function buildRelativeTimeCell(value) {
    const relative = formatRelativeTime(value);
    const absolute = formatDateTimeDisplay(value);
    const cssClass = relative === "-" ? "relative-time muted" : "relative-time";
    return `<span class="${cssClass}" title="${escapeHtml(absolute)}">${escapeHtml(relative)}</span>`;
}

function formatRelativeTime(value) {
    if (!value) {
        return "-";
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return "-";
    }
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMinutes = Math.floor(diffMs / 60000);
    if (diffMinutes < 1) {
        return "Vừa xong";
    }
    if (diffMinutes < 60) {
        return `${diffMinutes} phút trước`;
    }
    const diffHours = Math.floor(diffMinutes / 60);
    if (diffHours < 24) {
        return `${diffHours} giờ trước`;
    }
    const diffDays = Math.floor(diffHours / 24);
    if (diffDays < 7) {
        return `${diffDays} ngày trước`;
    }
    return date.toLocaleDateString("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric"
    });
}

function bindUserActionMenus(container) {
    if (!container) {
        return;
    }
    if (window.AdminActionMenus) {
        window.AdminActionMenus.init(container);
    }
    container.querySelectorAll("[data-user-menu-action]").forEach((button) => {
        button.addEventListener("click", (event) => {
            event.preventDefault();
            event.stopPropagation();
            if (button.disabled) {
                return;
            }
            const userId = Number(button.dataset.userMenuId);
            const action = button.dataset.userMenuAction;
            window.AdminActionMenus?.closeAll();
            if (action === "edit") {
                editUser(userId);
            } else if (action === "toggle") {
                const shouldActivate = button.dataset.userTargetActive === "true";
                toggleUserActive(userId, shouldActivate);
            } else if (action === "delete") {
                confirmDeleteUser(userId);
            }
        });
    });
}

} // end guard
