(function () {
    if (window.AdminActionMenus) {
        return;
    }

    function init(root = document) {
        if (!root) {
            return;
        }
        root.querySelectorAll(".user-action-menu-wrapper").forEach((wrapper) => {
            if (wrapper.dataset.actionMenuBound === "true") {
                return;
            }
            const toggle = wrapper.querySelector(".action-menu-toggle");
            if (!toggle) {
                return;
            }
            wrapper.dataset.actionMenuBound = "true";
            toggle.addEventListener("click", (event) => {
                event.stopPropagation();
                const isOpen = wrapper.classList.contains("open");
                closeAll();
                if (!isOpen) {
                    wrapper.classList.add("open");
                    toggle.setAttribute("aria-expanded", "true");
                }
            });
        });
    }

    function closeAll() {
        document.querySelectorAll(".user-action-menu-wrapper.open").forEach((wrapper) => {
            wrapper.classList.remove("open");
            const toggle = wrapper.querySelector(".action-menu-toggle");
            if (toggle) {
                toggle.setAttribute("aria-expanded", "false");
            }
        });
    }

    document.addEventListener("click", () => closeAll());
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closeAll();
        }
    });

    window.AdminActionMenus = {
        init,
        closeAll
    };
})();
