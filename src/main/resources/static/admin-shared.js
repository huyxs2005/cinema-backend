(function () {
    if (window.AdminDataBus) {
        return;
    }

    const EVENT_NAME = "admin:data-changed";

    function dispatch(resource, payload) {
        window.dispatchEvent(
            new CustomEvent(EVENT_NAME, {
                detail: { resource, payload }
            })
        );
    }

    function subscribe(resource, handler) {
        if (typeof handler !== "function") {
            return () => {};
        }
        const listener = (event) => {
            const target = event.detail?.resource;
            if (!resource || resource === target) {
                handler(event.detail);
            }
        };
        window.addEventListener(EVENT_NAME, listener);
        return () => window.removeEventListener(EVENT_NAME, listener);
    }

    window.AdminDataBus = {
        dispatch,
        subscribe,
        EVENT_NAME
    };
})();

(function () {
    function initManualCollapseButtons() {
        const buttons = document.querySelectorAll("[data-bs-toggle='collapse']");
        if (!buttons.length) {
            return;
        }
        const CollapseConstructor =
            window.bootstrap && typeof window.bootstrap.Collapse === "function"
                ? window.bootstrap.Collapse
                : null;

        buttons.forEach((button) => {
            if (button.dataset.collapseBound === "true") {
                return;
            }
            const selector = button.getAttribute("data-bs-target") || button.getAttribute("href");
            if (!selector) {
                return;
            }
            const target = document.querySelector(selector);
            if (!target) {
                return;
            }
            button.dataset.collapseBound = "true";
            if (CollapseConstructor) {
                const instance = CollapseConstructor.getOrCreateInstance(target, { toggle: false });
                const setExpanded = (state) => button.setAttribute("aria-expanded", state ? "true" : "false");
                setExpanded(target.classList.contains("show"));
                target.addEventListener("shown.bs.collapse", () => setExpanded(true));
                target.addEventListener("hidden.bs.collapse", () => setExpanded(false));
                button.addEventListener("click", (event) => {
                    event.preventDefault();
                    event.stopPropagation();
                    instance.toggle();
                });
                continue;
            }

            target.classList.remove("collapsing");
            target.classList.remove("collapse-horizontal");
            if (!target.classList.contains("show")) {
                target.style.display = "none";
            }

            button.addEventListener("click", (event) => {
                event.preventDefault();
                event.stopPropagation();
                const isVisible = target.classList.toggle("show");
                target.style.display = isVisible ? "block" : "none";
                button.setAttribute("aria-expanded", isVisible ? "true" : "false");
            });
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initManualCollapseButtons);
    } else {
        initManualCollapseButtons();
    }
})();
