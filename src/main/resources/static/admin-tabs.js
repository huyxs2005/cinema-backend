(function () {
    const state = {
        buttons: [],
        panels: [],
        activeTab: null,
        defaultTab: null
    };

    function updateTabs() {
        state.buttons.forEach((btn) => {
            const isActive = state.activeTab != null && btn.dataset.tab === state.activeTab;
            btn.classList.toggle("active", isActive);
            btn.setAttribute("aria-pressed", String(isActive));
        });
        state.panels.forEach((panel) => {
            const isActive = panel.id === state.activeTab;
            panel.classList.toggle("active", isActive);
        });
    }

    function ensureActiveTab() {
        if (!state.activeTab) {
            state.activeTab = state.defaultTab || (state.buttons[0]?.dataset.tab ?? null);
        }
    }

    function showAdminTab(targetId) {
        if (!targetId || !state.buttons.length || !state.panels.length) {
            return;
        }
        state.activeTab = targetId;
        updateTabs();
        scrollToAdminTop();
    }

    function activateAdminTab(targetId) {
        if (!targetId || !state.buttons.length || !state.panels.length) {
            return;
        }
        state.activeTab = targetId;
        updateTabs();
        scrollToAdminTop();
    }

    function scrollToAdminTop() {
        const content = document.querySelector(".admin-content");
        const targetTop = content ? content.offsetTop - 40 : 0;
        window.scrollTo({
            top: Math.max(targetTop, 0),
            behavior: "smooth"
        });
    }

    document.addEventListener("DOMContentLoaded", () => {
        const layout = document.getElementById("adminDashboardLayout");
        state.defaultTab = layout?.dataset.defaultTab || null;
        state.buttons = Array.from(document.querySelectorAll(".admin-tabs .tab-button"));
        state.panels = Array.from(document.querySelectorAll(".admin-tab-panel"));
        if (!state.buttons.length || !state.panels.length) {
            return;
        }
        state.buttons.forEach((button) => {
            button.type = "button";
            button.setAttribute("role", "tab");
            button.addEventListener("click", (event) => {
                event.preventDefault();
                showAdminTab(button.dataset.tab);
            });
        });
        ensureActiveTab();
        updateTabs();
    });

    window.showAdminTab = showAdminTab;
    window.activateAdminTab = activateAdminTab;
})();
