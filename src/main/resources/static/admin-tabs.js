(function () {
    const state = {
        buttons: [],
        panels: [],
        activeTab: null
    };

    function updateTabs() {
        const placeholderPanel = document.getElementById("crud-placeholder");
        state.buttons.forEach((btn) => {
            const isActive = state.activeTab != null && btn.dataset.tab === state.activeTab;
            btn.classList.toggle("active", isActive);
            btn.setAttribute("aria-pressed", String(isActive));
        });
        state.panels.forEach((panel) => panel.classList.remove("active"));
        if (state.activeTab) {
            const targetPanel = document.getElementById(state.activeTab);
            targetPanel?.classList.add("active");
        } else {
            placeholderPanel?.classList.add("active");
        }
    }

    function showAdminTab(targetId) {
        if (!targetId || !state.buttons.length || !state.panels.length) {
            return;
        }
        state.activeTab = state.activeTab === targetId ? null : targetId;
        updateTabs();
    }

    function activateAdminTab(targetId) {
        if (!targetId || !state.buttons.length || !state.panels.length) {
            return;
        }
        state.activeTab = targetId;
        updateTabs();
    }

    document.addEventListener("DOMContentLoaded", () => {
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
        updateTabs();
    });

    window.showAdminTab = showAdminTab;
    window.activateAdminTab = activateAdminTab;
})();
