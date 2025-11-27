(function () {
    const state = {
        buttons: [],
        panels: []
    };

    function showAdminTab(targetId) {
        if (!targetId || !state.buttons.length || !state.panels.length) {
            return;
        }
        const targetPanel = document.getElementById(targetId);
        if (!targetPanel) {
            return;
        }
        state.buttons.forEach((btn) => {
            const isActive = btn.dataset.tab === targetId;
            btn.classList.toggle("active", isActive);
            btn.setAttribute("aria-pressed", String(isActive));
        });
        state.panels.forEach((panel) => panel.classList.toggle("active", panel.id === targetId));
        targetPanel.scrollIntoView({ behavior: "smooth", block: "start" });
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
    });

    window.showAdminTab = showAdminTab;
})();
