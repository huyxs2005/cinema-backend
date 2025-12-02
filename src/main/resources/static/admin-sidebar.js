(() => {
    document.addEventListener("DOMContentLoaded", () => {
        const layout = document.getElementById("adminDashboardLayout");
        const toggleBtn = document.getElementById("sidebarToggleBtn");
        const peekToggleBtn = document.getElementById("sidebarPeekToggleBtn");
        if (!layout || !toggleBtn) {
            return;
        }

        function setSidebarState(collapsed) {
            layout.classList.toggle("sidebar-collapsed", collapsed);
            toggleBtn.classList.toggle("is-active", collapsed);
            toggleBtn.setAttribute("aria-expanded", String(!collapsed));
            toggleBtn.setAttribute("aria-pressed", String(collapsed));
            toggleBtn.setAttribute("aria-label", collapsed ? "Mở menu quản trị" : "Thu gọn menu quản trị");
            if (peekToggleBtn) {
                peekToggleBtn.setAttribute("aria-expanded", String(!collapsed));
                peekToggleBtn.setAttribute("aria-label", collapsed ? "Mở menu quản trị" : "Thu gọn menu quản trị");
            }
        }

        toggleBtn.addEventListener("click", () => {
            const collapsed = !layout.classList.contains("sidebar-collapsed");
            setSidebarState(collapsed);
        });

        if (peekToggleBtn) {
            peekToggleBtn.addEventListener("click", () => {
                setSidebarState(false);
            });
        }

        setSidebarState(false);
    });
})();
