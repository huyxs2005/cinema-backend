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
