document.addEventListener("DOMContentLoaded", () => {
    const tabButtons = document.querySelectorAll(".about-tabs .tab-button");
    const panels = document.querySelectorAll(".about-tab-panel");
    if (!tabButtons.length || !panels.length) {
        return;
    }

    tabButtons.forEach((button) => {
        button.addEventListener("click", () => {
            const targetId = button.dataset.tab;
            tabButtons.forEach((btn) => btn.classList.toggle("active", btn === button));
            panels.forEach((panel) => panel.classList.toggle("active", panel.id === targetId));
        });
    });
});
