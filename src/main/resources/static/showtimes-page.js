const SHOWTIME_RANGE_DAYS = 7;
const SEAT_INTENT_STORAGE_KEY = "cinemaSeatIntent";
const SEAT_INTENT_TTL = 5 * 60 * 1000;
const seatSelectionState = {
    pendingShowtimeId: null,
    pendingMovieId: null
};

document.addEventListener("DOMContentLoaded", () => {
    const tabsContainer = document.getElementById("showtimeDateTabs");
    const movieListContainer = document.getElementById("showtimeMovieList");
    if (!tabsContainer || !movieListContainer) {
        return;
    }

    const today = new Date();
    let currentDateKey = formatDateKey(today);
    let movies = [];

    const dateRange = Array.from({ length: SHOWTIME_RANGE_DAYS }, (_, index) => {
        const date = new Date(today);
        date.setDate(today.getDate() + index);
        return date;
    });

    renderDateTabs(dateRange, tabsContainer, currentDateKey, (key) => {
        currentDateKey = key;
        fetchAndRenderShowtimes(key);
    });

    Promise.all([fetchNowShowingMovies(), fetchShowtimesByDate(currentDateKey)])
        .then(([movieData, showtimeData]) => {
            movies = movieData;
            renderMovieCards(
                movieListContainer,
                movies,
                groupShowtimesByMovie(showtimeData),
                currentDateKey
            );
        })
        .catch(() => {
            movieListContainer.innerHTML = `<div class="showtime-empty-state">Không thể tải dữ liệu lịch chiếu.</div>`;
        });

    function fetchAndRenderShowtimes(dateKey) {
        movieListContainer.innerHTML = `<div class="showtime-empty-state">Đang tải lịch chiếu ${formatHumanDate(dateKey)}...</div>`;
        fetchShowtimesByDate(dateKey)
            .then((showtimes) => {
                renderMovieCards(movieListContainer, movies, groupShowtimesByMovie(showtimes), dateKey);
            })
            .catch(() => {
                movieListContainer.innerHTML = `<div class="showtime-empty-state">Không thể tải lịch chiếu cho ${formatHumanDate(dateKey)}.</div>`;
            });
    }
});

function renderDateTabs(dates, container, activeKey, onSelect) {
    container.innerHTML = "";
    dates.forEach((date) => {
        const key = formatDateKey(date);
        const button = document.createElement("button");
        button.type = "button";
        button.className = "showtime-date-card";
        button.dataset.dateKey = key;
        if (key === activeKey) {
            button.classList.add("active");
        }
        button.innerHTML = `
            <span class="weekday">${date.toLocaleDateString("vi-VN", { weekday: "short" })}</span>
            <span class="date-number">${String(date.getDate()).padStart(2, "0")}-${String(date.getMonth() + 1).padStart(2, "0")}</span>
        `;
        button.addEventListener("click", () => {
            container.querySelectorAll(".showtime-date-card").forEach((el) => el.classList.remove("active"));
            button.classList.add("active");
            onSelect(key);
        });
        container.appendChild(button);
    });
}

function fetchNowShowingMovies() {
    return fetch("/api/movies/now-showing")
        .then((response) => {
            if (!response.ok) {
                throw new Error("Failed to fetch movies");
            }
            return response.json();
        });
}

function fetchShowtimesByDate(dateKey) {
    return fetch(`/api/showtimes/day?date=${dateKey}`)
        .then((response) => {
            if (!response.ok) {
                throw new Error("Failed to fetch showtimes");
            }
            return response.json();
        });
}

function groupShowtimesByMovie(showtimes) {
    return showtimes.reduce((acc, item) => {
        if (!item.movieId) {
            return acc;
        }
        if (!acc[item.movieId]) {
            acc[item.movieId] = [];
        }
        acc[item.movieId].push(item);
        acc[item.movieId].sort((a, b) => new Date(a.startTime) - new Date(b.startTime));
        return acc;
    }, {});
}

function renderMovieCards(container, movies, showtimesMap, dateKey) {
    if (!movies.length) {
        container.innerHTML = `<div class="showtime-empty-state">Không có phim nào đang chiếu.</div>`;
        return;
    }
    const cards = movies
        .map((movie) => buildMovieCard(movie, showtimesMap[movie.id] || []))
        .filter(Boolean);
    if (!cards.length) {
        container.innerHTML = `<div class="showtime-empty-state">Không có suất chiếu cho ngày ${formatHumanDate(dateKey)}.</div>`;
        return;
    }
    container.innerHTML = cards.join("");
    bindShowtimeButtons(container);
}

function bindShowtimeButtons(container) {
    if (!container) {
        return;
    }
    const buttons = container.querySelectorAll(".showtime-slot");
    buttons.forEach((button) => {
        if (button.dataset.bound === "true") {
            return;
        }
        button.dataset.bound = "true";
        button.addEventListener("click", () => handleShowtimeSelection(button));
    });
}

function handleShowtimeSelection(button) {
    const showtimeId = button.dataset.showtimeId;
    const movieId = button.dataset.movieId;
    if (!showtimeId || !movieId) {
        return;
    }
    const detailUrl = `/movies/${movieId}?showtimeId=${showtimeId}`;
    rememberSeatIntent(movieId, showtimeId);
    const currentUser = typeof getCurrentUser === "function" ? getCurrentUser() : window.CURRENT_USER;
    if (currentUser) {
        window.location.href = detailUrl;
        return;
    }
    if (typeof window.setPostLoginRedirect === "function") {
        window.setPostLoginRedirect(detailUrl, { ttl: SEAT_INTENT_TTL });
    }
    if (typeof window.setLoginPromptMessage === "function") {
        window.setLoginPromptMessage("Bạn cần phải đăng nhập để có thể đặt vé");
        window.loginPromptLocked = true;
    }
    document.getElementById("openLoginModal")?.click();
}

function rememberSeatIntent(movieId, showtimeId) {
    seatSelectionState.pendingShowtimeId = String(showtimeId);
    seatSelectionState.pendingMovieId = String(movieId);
    try {
        const storage = window.sessionStorage;
        if (!storage) {
            return;
        }
        const payload = {
            movieId: String(movieId),
            showtimeId: String(showtimeId),
            expiresAt: Date.now() + SEAT_INTENT_TTL
        };
        storage.setItem(SEAT_INTENT_STORAGE_KEY, JSON.stringify(payload));
    } catch (error) {
        // ignore storage issues
    }
}

function buildMovieCard(movie, showtimes) {
    if (!showtimes.length) {
        return "";
    }
    const duration = movie.durationMinutes ? `${movie.durationMinutes} phút` : "";
    const releaseDate = movie.releaseDate ? `Khởi chiếu: ${formatDisplayDate(movie.releaseDate)}` : "";
    const ageRating = movie.ageRatingDescription || describeAgeRating(movie.ageRating);
    const genres = Array.isArray(movie.genres) && movie.genres.length ? movie.genres.join(", ") : "";
    const showtimeButtons = showtimes
        .map((showtime) => {
            const timeLabel = formatTime(new Date(showtime.startTime));
            return `<button type="button" class="showtime-slot showtime-slot-compact" data-showtime-id="${showtime.id}" data-movie-id="${movie.id}"><span class="showtime-time">${timeLabel}</span></button>`;
        })
        .join("");
    const metaSegments = [];
    if (genres) {
        metaSegments.push(`<span class="showtimes-genre">${genres}</span>`);
    }
    if (duration) {
        metaSegments.push(`<span class="showtimes-duration">${duration}</span>`);
    }
    const ageBadge = (movie.ageRating || "P").toUpperCase();
    const detailUrl = `/movies/${movie.id}`;
    return `
        <article class="showtimes-card">
            <div class="showtimes-format-badge">${ageBadge}</div>
            <a class="showtimes-card-poster" href="${detailUrl}">
                <img src="${movie.posterUrl || '/images/default-poster.jpg'}" alt="${movie.title}">
            </a>
            <div class="showtimes-card-body">
                <div class="showtimes-card-meta">${metaSegments.join("")}</div>
                <h3><a href="${detailUrl}">${movie.title}</a></h3>
                <ul class="showtimes-card-info">
                    ${releaseDate ? `<li class="showtimes-release">${releaseDate}</li>` : ""}
                    ${ageRating ? `<li class="showtimes-age">${ageRating}</li>` : ""}
                </ul>
                <div class="showtimes-card-times">
                    <p>Lịch chiếu</p>
                    <div class="showtimes-card-slots">
                        ${showtimeButtons}
                    </div>
                </div>
            </div>
        </article>
    `;
}

function formatDateKey(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

function formatDisplayDate(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }
    return date.toLocaleDateString("vi-VN");
}

function formatHumanDate(key) {
    if (!key) return "";
    const parts = key.split("-");
    if (parts.length !== 3) return key;
    return `${parts[2]}-${parts[1]}-${parts[0]}`;
}

function formatTime(date) {
    return date.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit", hour12: false });
}

function describeAgeRating(code) {
    if (!code) return null;
    switch (code.trim().toUpperCase()) {
        case "P":
            return "P - Phim được phép phổ biến đến người xem ở mọi độ tuổi.";
        case "K":
            return "K - Phim được phổ biến đến người xem dưới 13 tuổi và có người bảo hộ đi kèm.";
        case "T13":
            return "T13 - Phim được phổ biến đến người xem từ đủ 13 tuổi trở lên (13+).";
        case "T16":
            return "T16 - Phim được phổ biến đến người xem từ đủ 16 tuổi trở lên (16+).";
        case "T18":
            return "T18 - Phim được phổ biến đến người xem từ đủ 18 tuổi trở lên (18+).";
        default:
            return null;
    }
}
