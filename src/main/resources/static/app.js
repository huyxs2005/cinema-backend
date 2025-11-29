const MOVIE_API = {
    nowShowing: "/api/movies/now-showing",
    comingSoon: "/api/movies/coming-soon"
};
const PLACEHOLDER_POSTER = "https://via.placeholder.com/320x480.png?text=Cinema+HUB";

document.addEventListener("DOMContentLoaded", () => {
    initMovieSection("movieGrid", "statusMessage", MOVIE_API.nowShowing);
    initMovieSection("upcomingMovieGrid", "upcomingStatusMessage", MOVIE_API.comingSoon);
});

function initMovieSection(gridId, statusId, endpoint) {
    const grid = document.getElementById(gridId);
    const status = document.getElementById(statusId);
    if (!grid || !endpoint) {
        return;
    }
    fetchMovies(grid, status, endpoint);
}

async function fetchMovies(grid, statusEl, endpoint) {
    if (statusEl) {
        statusEl.textContent = "Đang tải...";
        statusEl.classList.remove("hidden");
    }

    try {
        const response = await fetch(endpoint);
        if (!response.ok) {
            throw new Error(`Failed to load movies: ${response.status}`);
        }
        const movies = await response.json();
        renderMovies(grid, movies);
        if (statusEl) {
            statusEl.textContent = "";
            statusEl.classList.add("hidden");
        }
    } catch (error) {
        console.error(error);
        if (statusEl) {
            statusEl.textContent = "Không thể tải danh sách phim.";
            statusEl.classList.remove("hidden");
        }
    }
}

function renderMovies(grid, movies) {
    grid.innerHTML = "";
    if (!movies || movies.length === 0) {
        const empty = document.createElement("div");
        empty.className = "status-message";
        empty.textContent = "Hiện chưa có phim nào.";
        grid.appendChild(empty);
        return;
    }

    movies.forEach((movie) => {
        const card = document.createElement("article");
        card.className = "movie-card";

        const poster = document.createElement("img");
        poster.src = movie.posterUrl || PLACEHOLDER_POSTER;
        poster.alt = `${movie.title || "Movie"} poster`;
        card.appendChild(poster);

        const content = document.createElement("div");
        content.className = "movie-content";

        const metaRow = document.createElement("div");
        metaRow.className = "movie-meta-row";

        const genreLine = document.createElement("span");
        genreLine.className = "movie-genre";
        genreLine.textContent = formatGenres(movie.genres);

        const release = document.createElement("span");
        release.className = "movie-meta";
        release.textContent = formatDate(movie.releaseDate);

        metaRow.appendChild(genreLine);
        metaRow.appendChild(release);
        content.appendChild(metaRow);

        const title = document.createElement("h3");
        title.className = "movie-title";
        title.textContent = movie.title || "Untitled";
        content.appendChild(title);

        card.appendChild(content);
        card.addEventListener("click", () => handleViewDetails(movie));
        grid.appendChild(card);
    });
}

function formatGenres(genres) {
    if (!genres || !genres.length) {
        return "Đang cập nhật thể loại";
    }
    return genres.join(", ");
}

function formatDate(dateStr) {
    if (!dateStr) {
        return "Đang cập nhật";
    }
    const date = new Date(dateStr);
    if (Number.isNaN(date.getTime())) {
        return dateStr;
    }
    return date.toLocaleDateString("vi-VN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit"
    });
}

function handleViewDetails(movie) {
    if (!movie || !movie.id) {
        return;
    }
    window.location.href = `/movies/${movie.id}`;
}
