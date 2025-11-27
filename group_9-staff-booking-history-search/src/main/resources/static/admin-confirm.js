(function () {
    window.openAdminConfirmDialog = function ({
        title = "X\u00e1c nh\u1eadn",
        message = "b\u1ea1n ch\u1eafc ch\u1ee9 ?",
        confirmLabel = "X\u00e1c nh\u1eadn",
        cancelLabel = "Hu\u1ef7",
        confirmVariant = "danger",
        onConfirm = () => {}
    } = {}) {
        const overlay = document.createElement("div");
        overlay.className = "admin-confirm-overlay";
        overlay.innerHTML = `
            <div class="admin-confirm-dialog">
                <div class="admin-confirm-title">${title}</div>
                <div class="admin-confirm-message">${message}</div>
                <div class="admin-confirm-actions">
                    <button type="button" class="btn btn-outline-light btn-sm" data-role="cancel">${cancelLabel}</button>
                    <button type="button" class="btn btn-${confirmVariant} btn-sm" data-role="confirm">${confirmLabel}</button>
                </div>
            </div>
        `;
        document.body.appendChild(overlay);

        const cleanup = () => {
            overlay.classList.remove("show");
            setTimeout(() => overlay.remove(), 180);
        };

        overlay.addEventListener("click", (event) => {
            if (event.target === overlay) {
                cleanup();
            }
        });

        overlay.querySelector("[data-role='cancel']").addEventListener("click", cleanup);
        overlay.querySelector("[data-role='confirm']").addEventListener("click", () => {
            try {
                onConfirm?.();
            } finally {
                cleanup();
            }
        });

        requestAnimationFrame(() => overlay.classList.add("show"));
    };
})();
