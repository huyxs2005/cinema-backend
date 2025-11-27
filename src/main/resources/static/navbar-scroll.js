document.addEventListener("DOMContentLoaded", function () {
    const navbar = document.querySelector(".cinema-navbar");
    if (!navbar) return;

    function handleScroll() {
        if (window.scrollY > 5) {
            navbar.classList.add("scrolled");
        } else {
            navbar.classList.remove("scrolled");
        }
    }

    handleScroll();
    window.addEventListener("scroll", handleScroll);
});