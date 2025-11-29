document.addEventListener("DOMContentLoaded", () => {
    const carouselInner = document.getElementById("bannerCarouselInner");
    const carouselElement = document.getElementById("mainBannerCarousel");
    if (!carouselInner) {
        return;
    }

    const renderPlaceholder = (message) => {
        carouselInner.innerHTML = "";
        const wrapper = document.createElement("div");
        wrapper.className = "carousel-item active";

        const placeholder = document.createElement("div");
        placeholder.className = "banner-placeholder";
        placeholder.textContent = message;

        wrapper.appendChild(placeholder);
        carouselInner.appendChild(wrapper);
    };

    fetch("/api/banners/active")
        .then((response) => {
            if (!response.ok) {
                throw new Error("Failed to load banners");
            }
            return response.json();
        })
        .then((data) => {
            if (!Array.isArray(data) || data.length === 0) {
                renderPlaceholder("Không có banner nào dang họat động");
                return;
            }

            carouselInner.innerHTML = "";
            const sorted = [...data].sort((a, b) => {
                const orderA = typeof a.sortOrder === "number" ? a.sortOrder : Number.MAX_SAFE_INTEGER;
                const orderB = typeof b.sortOrder === "number" ? b.sortOrder : Number.MAX_SAFE_INTEGER;
                if (orderA === orderB) {
                    return (a.id || 0) - (b.id || 0);
                }
                return orderA - orderB;
            });

            sorted.forEach((banner, index) => {
                const item = document.createElement("div");
                item.className = `carousel-item${index === 0 ? " active" : ""}`;

                const link = document.createElement("a");
                link.className = "banner-link";
                link.href = buildBannerUrl(banner);
                link.target = "_self";
                if (banner.imagePath) {
                    link.style.setProperty("--banner-bg", `url("${banner.imagePath}")`);
                }

                                const img = document.createElement("img");
                img.className = "d-block w-100 banner-image";
                img.src = banner.imagePath;
                let altText = "Banner Cinema HUB";
                if (banner.linkType === "MOVIE") {
                    altText = `Banner phim ${banner.movieId ?? ""}`.trim();
                } else if (banner.linkType === "PROMO") {
                    altText = banner.promotionTitle
                        ? `Khuyến mãi ${banner.promotionTitle}`
                        : "Banner khuyến mãii HUB";
                } else if (banner.linkType === "URL") {
                    altText = banner.targetUrl || altText;
                }
                img.alt = altText;

                link.appendChild(img);
                item.appendChild(link);

                carouselInner.appendChild(item);
            });

            if (carouselElement && window.bootstrap) {
                const existing = window.bootstrap.Carousel.getInstance(carouselElement);
                if (existing) {
                    existing.dispose();
                }
                new window.bootstrap.Carousel(carouselElement, {
                    interval: 5000,
                    ride: "carousel",
                    pause: false,
                    wrap: true
                });
            }
        })
        .catch(() => renderPlaceholder("Không thể tải banner. Vui lòng thử lại sau."));
});

function buildBannerUrl(banner) {
    if (banner.linkType === "MOVIE" && banner.movieId) {
        return `/movies/${banner.movieId}`;
    }
    if (banner.linkType === "PROMO" && banner.promotionSlug) {
        return `/khuyen-mai/${banner.promotionSlug}`;
    }
    if (banner.linkType === "URL" && banner.targetUrl) {
        return banner.targetUrl;
    }
    return "#";
}



