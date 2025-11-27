const AuthAPI = {
    login: "/api/auth/login",
    register: "/api/auth/register",
    forgot: "/api/auth/forgot-password",
    reset: "/api/auth/reset-password",
    verifyReset: "/api/auth/verify-reset-token"
};

const ProfileAPI = {
    detail: (userId) => `/api/profile/${userId}`,
    update: (userId) => `/api/profile/${userId}`,
    changePassword: (userId) => `/api/profile/${userId}/change-password`
};
const CountdownDefaults = {
    forgot: 120
};

const PASSWORD_RULE = /^(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;
const PASSWORD_RULE_MESSAGE = "Mật khẩu phải có ít nhất 8 ký tự, bao gồm số và ký tự đặc biệt";
const PHONE_RULE = /^\d{10,11}$/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const USER_STORAGE_KEY = "cinemaHubUser";
let modalOverlayRef = null;
const modalMap = {};
let userMenuInitialized = false;
let profileToastTimeout = null;
let overlayMouseDown = false;

document.addEventListener("DOMContentLoaded", () => {
    initModals();
    initNumericOnlyInputs();
    bindLoginForm();
    bindRegisterForm();
    bindForgotForm();
    bindResetForm();
    initializeUserState();
    initializeProfilePage();
    initPasswordToggles();
});

function initModals() {
    modalOverlayRef = document.getElementById("modalOverlay");
    modalMap.login = document.getElementById("loginModal");
    modalMap.register = document.getElementById("registerModal");

    if (!modalOverlayRef) {
        return;
    }

    document.getElementById("openLoginModal")?.addEventListener("click", () => showModal("login"));
    document.getElementById("openRegisterModal")?.addEventListener("click", () => showModal("register"));

    modalOverlayRef.addEventListener("mousedown", (event) => {
        overlayMouseDown = event.target === modalOverlayRef;
    });

    modalOverlayRef.addEventListener("mouseup", (event) => {
        if (overlayMouseDown && event.target === modalOverlayRef) {
            hideModals();
        }
        overlayMouseDown = false;
    });

    document.querySelectorAll("[data-modal-close]").forEach((btn) =>
        btn.addEventListener("click", hideModals)
    );

    document.querySelectorAll("[data-open-modal]").forEach((btn) => {
        const target = btn.getAttribute("data-open-modal");
        btn.addEventListener("click", () => showModal(target));
    });
}

function showModal(name) {
    if (!modalOverlayRef || !modalMap[name]) return;
    Object.values(modalMap).forEach((modal) => modal?.classList.remove("show"));
    modalMap[name].classList.add("show");
    modalOverlayRef.classList.add("show");
}

function hideModals() {
    modalOverlayRef?.classList.remove("show");
    Object.values(modalMap).forEach((modal) => modal?.classList.remove("show"));
}

function getStoredUser() {
    try {
        const raw = localStorage.getItem(USER_STORAGE_KEY);
        return raw ? JSON.parse(raw) : null;
    } catch {
        return null;
    }
}

function setStoredUser(user) {
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
}

function clearStoredUser() {
    localStorage.removeItem(USER_STORAGE_KEY);
}

function initializeUserState() {
    const user = getStoredUser();
    const authButtons = document.getElementById("authActionButtons");
    const userMenuWrapper = document.getElementById("userMenuWrapper");
    const userNameLabel = document.getElementById("userMenuName");

    if (authButtons) {
        authButtons.classList.toggle("hidden", !!user);
    }
    if (userMenuWrapper && userNameLabel) {
        const displayName = user?.fullName || user?.email || "Tài khoản";
        userNameLabel.textContent = displayName;
        userMenuWrapper.style.display = user ? "flex" : "none";
    }

    updateAdminMenuVisibility(user);
    setupUserMenuHandlers();
}

function updateAdminMenuVisibility(user) {
    const adminButton = document.getElementById("adminDashboardLink");
    if (!adminButton) {
        return;
    }
    const isAdmin = (user?.role || "").toUpperCase() === "ADMIN";
    adminButton.classList.toggle("hidden", !isAdmin);
}

function setupUserMenuHandlers() {
    const toggle = document.getElementById("userMenuToggle");
    const dropdown = document.getElementById("userMenuDropdown");
    const wrapper = document.getElementById("userMenuWrapper");

    if (!toggle || !dropdown || !wrapper || userMenuInitialized) {
        return;
    }
    userMenuInitialized = true;

    toggle.addEventListener("click", (event) => {
        event.stopPropagation();
        dropdown.classList.toggle("show");
        wrapper.classList.toggle("open");
    });

    document.addEventListener("click", (event) => {
        if (!wrapper.contains(event.target)) {
            dropdown.classList.remove("show");
            wrapper.classList.remove("open");
        }
    });

    dropdown.querySelectorAll("button[data-user-action]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const action = btn.getAttribute("data-user-action");
            handleUserMenuAction(action);
            dropdown.classList.remove("show");
            wrapper.classList.remove("open");
        });
    });
}

function handleUserMenuAction(action) {
    if (action === "profile") {
        window.location.href = "/profile";
        return;
    }
    if (action === "admin") {
        window.location.href = "/admin/dashboard";
        return;
    }
    if (action === "logout") {
        clearStoredUser();
        initializeUserState();
        hideModals();
        window.location.href = "/";
    }
}

function bindLoginForm() {
    const form = document.getElementById("loginForm");
    const messageEl = document.getElementById("loginMessage");
    if (!form) return;

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const payload = {
            email: document.getElementById("loginEmail").value.trim(),
            password: document.getElementById("loginPassword").value
        };
        clearFieldErrors(["loginEmail", "loginPassword"]);
        let hasError = false;
        if (!payload.email || !EMAIL_PATTERN.test(payload.email)) {
            setFieldError("loginEmail", "Email không hợp lệ");
            hasError = true;
        }
        if (!payload.password) {
            setFieldError("loginPassword", "Vui lòng nhập mật khẩu");
            hasError = true;
        }
        if (hasError) {
            setFormMessage(messageEl, "");
            return;
        }
        await handleRequest(AuthAPI.login, payload, messageEl, (data) => completeAuthSession(data));
    });
}

function bindRegisterForm() {
    const form = document.getElementById("registerForm");
    const messageEl = document.getElementById("registerMessage");
    if (!form) return;

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const firstName = document.getElementById("registerFirstName")?.value.trim() ?? "";
        const lastName = document.getElementById("registerLastName")?.value.trim() ?? "";
        let fullName = document.getElementById("registerFullName")?.value.trim() ?? "";
        if (!fullName) {
            fullName = `${firstName} ${lastName}`.trim();
        }
        const email = document.getElementById("registerEmail").value.trim();
        const phoneInput = document.getElementById("registerPhone");
        const sanitizedPhone = sanitizePhone(phoneInput.value.trim());
        phoneInput.value = sanitizedPhone;
        const password = document.getElementById("registerPassword").value;
        const confirmPassword = document.getElementById("registerConfirmPassword").value;

        clearFieldErrors([
            "registerFirstName",
            "registerLastName",
            "registerEmail",
            "registerPhone",
            "registerPassword",
            "registerConfirmPassword"
        ]);

        let hasError = false;
        if (!firstName) {
            setFieldError("registerFirstName", "Họ không được để trống");
            hasError = true;
        }
        if (!lastName) {
            setFieldError("registerLastName", "Tên không được để trống");
            hasError = true;
        }
        if (!email || !EMAIL_PATTERN.test(email)) {
            setFieldError("registerEmail", "Email không hợp lệ");
            hasError = true;
        }
        if (!sanitizedPhone) {
            setFieldError("registerPhone", "Số điện thoại không được để trống");
            hasError = true;
        } else if (!PHONE_RULE.test(sanitizedPhone)) {
            setFieldError("registerPhone", "Số điện thoại phải gồm 10-11 chữ số");
            hasError = true;
        }
        if (!validatePasswordStrength(password)) {
            setFieldError("registerPassword", PASSWORD_RULE_MESSAGE);
            hasError = true;
        }
        if (password !== confirmPassword) {
            setFieldError("registerConfirmPassword", "Mật khẩu xác nhận không khớp");
            hasError = true;
        }
        if (hasError) {
            setFormMessage(messageEl, "");
            return;
        }

        const payload = { fullName, email, phone: sanitizedPhone, password, confirmPassword };
        await handleRequest(AuthAPI.register, payload, messageEl, (data) => completeAuthSession(data));
    });
}

function bindForgotForm() {
    const emailForm = document.getElementById("forgotPasswordForm");
    const verifyForm = document.getElementById("forgotVerificationForm");
    const resetForm = document.getElementById("forgotResetForm");
    const messageEl = document.getElementById("forgotMessage");
    const stepEmail = document.getElementById("forgotStepEmail");
    const stepCode = document.getElementById("forgotStepCode");
    const stepReset = document.getElementById("forgotStepReset");
    const emailDisplay = document.getElementById("forgotEmailDisplay");
    const emailHidden = document.getElementById("forgotEmailHidden");
    const countdownLabel = document.getElementById("forgotCountdown");
    const resendBtn = document.getElementById("forgotResendBtn");
    const resetEmailLabel = document.getElementById("forgotResetEmail");
    const resetEmailHidden = document.getElementById("forgotResetEmailHidden");
    const resetTokenHidden = document.getElementById("forgotResetTokenHidden");
    if (!emailForm) return;

    let cachedEmail = "";
    let lastToken = "";
    let countdownTimer = null;

    emailForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        cachedEmail = document.getElementById("forgotEmail").value.trim();
        if (!cachedEmail) {
            setFormMessage(messageEl, "Vui lòng nhập email đăng ký.");
            return;
        }
        await handleRequest(AuthAPI.forgot, { email: cachedEmail }, messageEl, () => {
            if (emailHidden) {
                emailHidden.value = cachedEmail;
            }
            if (emailDisplay) {
                emailDisplay.textContent = cachedEmail;
            }
            switchForgotStep(stepEmail, stepCode, stepReset);
            startForgotCountdown(countdownLabel, resendBtn, () => {
                resendBtn?.removeAttribute("disabled");
            });
            setFormMessage(messageEl, "Đã gửi mã xác thực, vui lòng kiểm tra email.", "success");
        });
    });

    if (resendBtn) {
        resendBtn.addEventListener("click", async () => {
            if (!cachedEmail) return;
            resendBtn.setAttribute("disabled", "true");
            await handleRequest(AuthAPI.forgot, { email: cachedEmail }, messageEl, () => {
                startForgotCountdown(countdownLabel, resendBtn, () => {
                    resendBtn?.removeAttribute("disabled");
                });
                setFormMessage(messageEl, "Đã gửi lại mã xác thực.", "success");
            });
        });
    }

    if (verifyForm) {
        verifyForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const email = cachedEmail || emailHidden?.value || "";
            const token = document.getElementById("forgotToken").value.trim();
            if (!email) {
                setFormMessage(messageEl, "Vui lòng nhập email trước khi đổi mật khẩu.");
                return;
            }
            if (!token) {
                setFieldError("forgotToken", "Xin vui lòng nhập mã xác nhận");
                return;
            }
            setFieldError("forgotToken", "");
            await handleRequest(AuthAPI.verifyReset, { email, token }, messageEl, () => {
                lastToken = token;
                if (resetEmailHidden) {
                    resetEmailHidden.value = email;
                }
                if (resetTokenHidden) {
                    resetTokenHidden.value = token;
                }
                if (resetEmailLabel) {
                    resetEmailLabel.textContent = email;
                }
                clearInterval(countdownTimer);
                switchForgotStep(stepCode, stepReset, stepEmail);
                setFormMessage(messageEl, "Mã hợp lệ, hãy đặt mật khẩu mới.", "success");
            });
        });
    }

    if (resetForm) {
        resetForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const email = resetEmailHidden?.value || cachedEmail;
            const token = resetTokenHidden?.value || lastToken;
            const newPassword = document.getElementById("forgotNewPassword").value;
            const confirmPassword = document.getElementById("forgotConfirmPassword").value;

            clearFieldErrors(["forgotNewPassword", "forgotConfirmPassword"]);
            if (!validatePasswordStrength(newPassword)) {
                setFieldError("forgotNewPassword", PASSWORD_RULE_MESSAGE);
                return;
            }
            if (newPassword !== confirmPassword) {
                setFieldError("forgotConfirmPassword", "Mật khẩu xác nhận không khớp");
                return;
            }

            const payload = { email, token, newPassword, confirmPassword };
            await handleRequest(AuthAPI.reset, payload, messageEl, async () => {
                setFormMessage(messageEl, "Đổi mật khẩu thành công! Đang đăng nhập...", "success");
                await loginAfterRecovery(email, newPassword);
            });
        });
    }

    function startForgotCountdown(label, button, onExpire) {
        if (!label) return;
        let remaining = CountdownDefaults.forgot;
        label.textContent = formatCountdown(remaining);
        button?.setAttribute("disabled", "true");
        clearInterval(countdownTimer);
        countdownTimer = setInterval(() => {
            remaining -= 1;
            if (remaining <= 0) {
                clearInterval(countdownTimer);
                label.textContent = "00:00";
                onExpire?.();
            } else {
                label.textContent = formatCountdown(remaining);
            }
        }, 1000);
    }

    function formatCountdown(seconds) {
        const mm = String(Math.floor(seconds / 60)).padStart(2, "0");
        const ss = String(seconds % 60).padStart(2, "0");
        return `${mm}:${ss}`;
    }

    function switchForgotStep(hide, show, other) {
        hide?.classList.add("hidden");
        show?.classList.remove("hidden");
        other?.classList.add("hidden");
    }
}

function bindResetForm() {
    const form = document.getElementById("resetPasswordForm");
    const messageEl = document.getElementById("resetMessage");
    if (!form) return;

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const newPassword = document.getElementById("resetPassword").value;
        const confirmPassword = document.getElementById("resetConfirmPassword").value;

        if (!validatePasswordStrength(newPassword)) {
            setFormMessage(messageEl, PASSWORD_RULE_MESSAGE);
            return;
        }
        if (newPassword !== confirmPassword) {
            setFormMessage(messageEl, "Mật khẩu xác nhận không khớp.");
            return;
        }

        const payload = {
            email: document.getElementById("resetEmail").value.trim(),
            token: document.getElementById("resetToken").value.trim(),
            newPassword,
            confirmPassword
        };
        await handleRequest(AuthAPI.reset, payload, messageEl, () => {
            setFormMessage(messageEl, "Đổi mật khẩu thành công! Đang chuyển về trang đăng nhập...", "success");
            setTimeout(() => window.location.href = "/login", 1500);
        });
    });
}

function initializeProfilePage() {
    const profileTabs = document.getElementById("profileTabs");
    if (!profileTabs) {
        return;
    }

    const user = getStoredUser();
    if (!user) {
        window.location.href = "/";
        return;
    }

    loadProfileData(user);

    profileTabs.querySelectorAll(".tab-button").forEach((btn) => {
        btn.addEventListener("click", () => switchProfileTab(btn.getAttribute("data-tab-target")));
    });

    const profileForm = document.getElementById("profileForm");
    if (profileForm) {
        profileForm.addEventListener("submit", async (event) => {
            event.preventDefault();
        const phoneInput = document.getElementById("profilePhone");
        const sanitizedPhone = sanitizePhone(phoneInput.value.trim());
        phoneInput.value = sanitizedPhone;

        if (!PHONE_RULE.test(sanitizedPhone)) {
            showProfileToast("Số điện thoại phải gồm 10-11 chữ số.", "error");
            phoneInput.focus();
            return;
        }

        const payload = {
            firstName: document.getElementById("profileFirstName").value.trim(),
            lastName: document.getElementById("profileLastName").value.trim(),
            phone: sanitizedPhone
        };
            try {
                const updated = await apiRequest(ProfileAPI.update(user.userId), {
                    method: "PUT",
                    headers: {"Content-Type": "application/json"},
                    body: JSON.stringify(payload)
                });
                showProfileToast("Lưu thành công !");
                const fullName = buildFullName(payload.firstName, payload.lastName) || user.fullName;
                const syncedUser = {
                    ...user,
                    fullName,
                    phone: updated.phone
                };
                setStoredUser(syncedUser);
                Object.assign(user, syncedUser);
                initializeUserState();
            } catch (error) {
                showProfileToast(error.message, "error");
            }
        });
    }

    setupChangePasswordModal(user);

    renderTicketHistory(user.ticketHistory || []);
}

function switchProfileTab(targetId) {
    document.querySelectorAll(".profile-section").forEach((section) => {
        section.classList.toggle("active", section.id === targetId);
    });
    document.querySelectorAll(".tab-button").forEach((btn) => {
        btn.classList.toggle("active", btn.getAttribute("data-tab-target") === targetId);
    });
}

function renderTicketHistory(entries) {
    const tbody = document.getElementById("ticketHistoryBody");
    if (!tbody) return;
    tbody.innerHTML = "";
    if (!entries.length) {
        const row = document.createElement("tr");
        const col = document.createElement("td");
        col.colSpan = 4;
        col.className = "text-center text-muted";
        col.textContent = "Chưa có dữ liệu";
        row.appendChild(col);
        tbody.appendChild(row);
        return;
    }
    entries.forEach((item) => {
        const row = document.createElement("tr");
        ["date", "movie", "tickets", "amount"].forEach((field) => {
            const cell = document.createElement("td");
            cell.textContent = item[field] ?? "";
            row.appendChild(cell);
        });
        tbody.appendChild(row);
    });
}

async function loadProfileData(user) {
    try {
        const profile = await apiRequest(ProfileAPI.detail(user.userId));
        fillProfileForm(profile, user);
        const fullName = buildFullName(profile.firstName, profile.lastName);
        const syncedUser = {
            ...user,
            fullName: fullName || user.fullName,
            phone: profile.phone
        };
        setStoredUser(syncedUser);
        Object.assign(user, syncedUser);
        initializeUserState();
    } catch (error) {
        showProfileToast(error.message || "Không thể tải thông tin tài khoản", "error");
    }
}

function fillProfileForm(profile, user) {
    const normalizedPhone = sanitizePhone(profile.phone);
    document.getElementById("profileFirstName").value = profile.firstName || "";
    document.getElementById("profileLastName").value = profile.lastName || "";
    document.getElementById("profilePhone").value = normalizedPhone || "";
    document.getElementById("profileUsername").value = user.email?.split("@")[0] || "";
    document.getElementById("profileEmail").value = user.email || "";
}

function setupChangePasswordModal(user) {
    const openBtn = document.getElementById("changePasswordBtn");
    const closeBtn = document.getElementById("closeChangePasswordModal");
    const cancelBtn = document.getElementById("cancelChangePassword");
    const modal = document.getElementById("changePasswordModal");
    const form = document.getElementById("changePasswordForm");

    if (!openBtn || !modal || !form) {
        return;
    }

    const closeModal = () => toggleChangePasswordModal(false);

    openBtn.addEventListener("click", () => toggleChangePasswordModal(true));
    closeBtn?.addEventListener("click", closeModal);
    cancelBtn?.addEventListener("click", closeModal);
    modal.addEventListener("click", (event) => {
        if (event.target === modal) {
            closeModal();
        }
    });

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const payload = {
            currentPassword: document.getElementById("currentPassword").value,
            newPassword: document.getElementById("newPassword").value,
            confirmPassword: document.getElementById("confirmPassword").value
        };
        clearFieldErrors(["currentPassword", "newPassword", "confirmPassword"]);
        let hasError = false;
        if (!payload.currentPassword?.trim()) {
            setFieldError("currentPassword", "Vui lòng nhập mật khẩu hiện tại");
            hasError = true;
        }
        if (!validatePasswordStrength(payload.newPassword)) {
            setFieldError("newPassword", PASSWORD_RULE_MESSAGE);
            hasError = true;
        }
        if (payload.newPassword !== payload.confirmPassword) {
            setFieldError("confirmPassword", "Mật khẩu xác nhận không khớp");
            hasError = true;
        }
        if (hasError) {
            return;
        }
        try {
            await apiRequest(ProfileAPI.changePassword(user.userId), {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(payload)
            });
            showProfileToast("Đổi mật khẩu thành công!");
            form.reset();
            closeModal();
        } catch (error) {
            showProfileToast(error.message, "error");
        }
    });
}

function toggleChangePasswordModal(show) {
    const modal = document.getElementById("changePasswordModal");
    if (!modal) return;
    modal.classList.toggle("show", show);
    modal.setAttribute("aria-hidden", show ? "false" : "true");
    if (!show) {
        document.getElementById("changePasswordForm")?.reset();
    }
}

function showProfileToast(message, variant = "success") {
    const toast = document.getElementById("profileToast");
    if (!toast) return;
    toast.textContent = message;
    toast.classList.remove("error");
    if (variant === "error") {
        toast.classList.add("error");
    }
    toast.classList.add("show");
    clearTimeout(profileToastTimeout);
    profileToastTimeout = setTimeout(() => toast.classList.remove("show"), 3200);
}

function buildFullName(firstName, lastName) {
    return [firstName, lastName]
        .map((part) => (part || "").trim())
        .filter(Boolean)
        .join(" ")
        .trim();
}

function validatePasswordStrength(password) {
    return PASSWORD_RULE.test(password);
}

function sanitizePhone(value) {
    return (value || "").replace(/\D+/g, "");
}

function initNumericOnlyInputs(root = document) {
    root.querySelectorAll("[data-numeric-only='true']").forEach((input) => {
        if (input.dataset.numericBound === "true") {
            return;
        }
        input.dataset.numericBound = "true";
        input.addEventListener("input", () => {
            const digits = sanitizePhone(input.value);
            const maxLength = input.getAttribute("maxlength");
            const limited = maxLength ? digits.slice(0, Number(maxLength)) : digits;
            if (input.value !== limited) {
                input.value = limited;
            }
        });
    });
}

function setFormMessage(element, text, variant) {
    if (!element) return;
    element.textContent = text;
    element.classList.remove("success", "error");
    if (variant === "success" || variant === "error") {
        element.classList.add(variant);
    }
}

function setFieldError(fieldId, message) {
    const hint = document.querySelector(`[data-error-for="${fieldId}"]`);
    if (hint) {
        hint.textContent = message || "";
    }
}

function clearFieldErrors(ids = []) {
    ids.forEach((id) => setFieldError(id, ""));
}

function initPasswordToggles() {
    document.querySelectorAll(".password-toggle").forEach((btn) => {
        if (btn.dataset.bound === "true") return;
        btn.dataset.bound = "true";
        btn.addEventListener("click", () => {
            const targetId = btn.getAttribute("data-toggle-target");
            const input = document.getElementById(targetId);
            if (!input) {
                return;
            }
            const isPassword = input.type === "password";
            input.type = isPassword ? "text" : "password";
            btn.setAttribute("aria-pressed", isPassword ? "true" : "false");
        });
    });
}

function completeAuthSession(data) {
    if (!data) {
        return;
    }
    setStoredUser({
        userId: data.userId,
        fullName: data.fullName,
        email: data.email,
        role: data.role,
        phone: data.phone || ""
    });
    initializeUserState();
    hideModals();
    redirectByRole(data.role);
}

function redirectByRole(role) {
    const normalized = (role || "").toUpperCase();
    if (normalized === "ADMIN") {
        window.location.href = "/admin/dashboard";
    } else if (normalized === "STAFF" || normalized === "EMPLOYEE") {
        window.location.href = "/staff/portal";
    } else {
        window.location.href = "/";
    }
}

async function loginAfterRecovery(email, password) {
    if (!email || !password) return;
    try {
        const data = await apiRequest(AuthAPI.login, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ email, password })
        });
        completeAuthSession(data);
    } catch (error) {
        console.error("Auto login failed", error);
        window.location.href = "/login";
    }
}

async function handleRequest(url, payload, messageEl, onSuccess) {
    try {
        setFormMessage(messageEl, "Đang xử lý...");
        const data = await apiRequest(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });
        if (typeof onSuccess === "function") {
            await onSuccess(data);
        }
    } catch (error) {
        console.error(error);
        setFormMessage(messageEl, error.message, "error");
    }
}

async function apiRequest(url, options = {}) {
    const response = await fetch(url, options);
    let data = {};
    const rawText = await response.text();
    if (rawText) {
        try {
            data = JSON.parse(rawText);
        } catch (err) {
            console.warn("Response is not JSON", err);
        }
    }
    if (!response.ok) {
        throw new Error(data.message || data.error || "Đã xảy ra lỗi");
    }
    return data;
}














