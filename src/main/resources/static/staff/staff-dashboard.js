(function() {
    'use strict';

    let currentPage = 'dashboard';
    let movies = [];
    let filteredMovies = [];
    let pendingBookings = [];
    let todayBookings = [];
    let selectedDate = null;
    let selectedDateShowtimes = [];

    // Initialize
    document.addEventListener('DOMContentLoaded', function() {
        setupSidebar();
        setupFilters();
        setupWeekCalendar();
        loadDashboard();
        loadMoviesByStatus();
    });

    function setupSidebar() {
        const menuLinks = document.querySelectorAll('.staff-sidebar-menu a[data-page]');
        menuLinks.forEach(link => {
            link.addEventListener('click', function(e) {
                e.preventDefault();
                const page = this.getAttribute('data-page');
                switchPage(page);
            });
        });
    }

    function switchPage(page) {
        currentPage = page;
        
        // Update active menu item
        document.querySelectorAll('.staff-sidebar-menu a').forEach(a => a.classList.remove('active'));
        document.querySelector(`.staff-sidebar-menu a[data-page="${page}"]`)?.classList.add('active');
        
        // Show/hide pages
        document.querySelectorAll('.staff-page').forEach(p => p.classList.remove('active'));
        const pageElement = document.getElementById(page + 'Page');
        if (pageElement) {
            pageElement.classList.add('active');
        }

        // Load data for the page
        if (page === 'dashboard') {
            loadDashboard();
        } else if (page === 'pending') {
            loadPendingBookings();
        } else if (page === 'today') {
            loadTodayBookings();
        }
    }

    function setupFilters() {
        // Dashboard filters
        document.getElementById('applyFilters')?.addEventListener('click', applyMovieFilters);
        document.getElementById('clearFilters')?.addEventListener('click', clearMovieFilters);
        document.getElementById('movieSearch')?.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') applyMovieFilters();
        });

        // Pending filters
        document.getElementById('applyPendingFilters')?.addEventListener('click', applyPendingFilters);
        document.getElementById('clearPendingFilters')?.addEventListener('click', clearPendingFilters);
        document.getElementById('pendingSearch')?.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') applyPendingFilters();
        });

        // Today filters
        document.getElementById('applyTodayFilters')?.addEventListener('click', applyTodayFilters);
        document.getElementById('clearTodayFilters')?.addEventListener('click', clearTodayFilters);
        document.getElementById('todaySearch')?.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') applyTodayFilters();
        });

        // Status tabs
        document.querySelectorAll('.status-tab').forEach(tab => {
            tab.addEventListener('click', function() {
                const status = this.getAttribute('data-status');
                switchStatusTab(status);
            });
        });
    }

    function switchStatusTab(status) {
        // Update active tab
        document.querySelectorAll('.status-tab').forEach(tab => {
            tab.classList.remove('active');
            if (tab.getAttribute('data-status') === status) {
                tab.classList.add('active');
            }
        });

        // Update active panel
        document.querySelectorAll('.status-panel').forEach(panel => {
            panel.classList.remove('active');
        });
        const panelId = status === 'comingSoon' ? 'comingSoonPanel' :
                       status === 'nowShowing' ? 'nowShowingPanel' : 'stoppedPanel';
        const panel = document.getElementById(panelId);
        if (panel) {
            panel.classList.add('active');
        }
    }

    function setupWeekCalendar() {
        const calendar = document.getElementById('weekCalendar');
        if (!calendar) return;

        // Only show today and the next 6 days
        const baseDate = new Date();
        baseDate.setHours(0, 0, 0, 0);

        const dayNames = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
        const dayNamesFull = ['Ch? nh?t', 'Th? 2', 'Th? 3', 'Th? 4', 'Th? 5', 'Th? 6', 'Th? 7'];
        const monthNames = ['Th?ng 1', 'Th?ng 2', 'Th?ng 3', 'Th?ng 4', 'Th?ng 5', 'Th?ng 6',
                           'Th?ng 7', 'Th?ng 8', 'Th?ng 9', 'Th?ng 10', 'Th?ng 11', 'Th?ng 12'];

        calendar.innerHTML = '';

        for (let i = 0; i < 7; i++) {
            const date = new Date(baseDate);
            date.setDate(baseDate.getDate() + i);

            const dayOfWeek = date.getDay();
            const dayName = dayNamesFull[dayOfWeek];
            const dayNumber = date.getDate();
            const month = monthNames[date.getMonth()];
            const dateKey = formatDateKey(date);

            const isToday = isSameDay(date, baseDate);

            const dayElement = document.createElement('div');
            dayElement.className = `week-day ${isToday ? 'active' : ''}`;
            dayElement.dataset.date = dateKey;
            dayElement.innerHTML = `
                <div class="week-day-name">${dayName}</div>
                <div class="week-day-number">${dayNumber}</div>
                <div class="week-day-month">${month}</div>
                ${isToday ? '<div class="week-day-today-badge">H?m nay</div>' : ''}
            `;

            dayElement.addEventListener('click', function() {
                selectDate(dateKey, date);
            });

            calendar.appendChild(dayElement);
        }

        // Don't auto-select - let user choose or show movies list by default
    }

    function formatDateKey(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    function isSameDay(date1, date2) {
        return date1.getFullYear() === date2.getFullYear() &&
               date1.getMonth() === date2.getMonth() &&
               date1.getDate() === date2.getDate();
    }

    async function selectDate(dateKey, date) {
        selectedDate = dateKey;
        
        // Update active day
        document.querySelectorAll('.week-day').forEach(day => {
            day.classList.remove('active');
            if (day.dataset.date === dateKey) {
                day.classList.add('active');
            }
        });

        // Hide status sections, show selected date section
        const selectedDateEl = document.getElementById('selectedDateMovies');
        const selectedDateTitleEl = document.getElementById('selectedDateTitle');
        const selectedDateMoviesListEl = document.getElementById('selectedDateMoviesList');
        const selectedDateEmptyEl = document.getElementById('selectedDateEmpty');
        const moviesByStatusEl = document.querySelector('.movies-by-status-section');

        if (selectedDateEl) selectedDateEl.style.display = 'block';
        if (moviesByStatusEl) moviesByStatusEl.style.display = 'none';

        // Update title
        const dayNames = ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];
        const dayOfWeek = date.getDay();
        const dayName = dayNames[dayOfWeek === 0 ? 6 : dayOfWeek - 1];
        const dateStr = new Intl.DateTimeFormat('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        }).format(date);
        
        if (selectedDateTitleEl) {
            selectedDateTitleEl.textContent = `${dayName}, ${dateStr}`;
        }

        // Show loading state
        if (selectedDateMoviesListEl) {
            selectedDateMoviesListEl.innerHTML = '<div class="staff-empty-hint">Đang tải lịch chiếu...</div>';
        }
        if (selectedDateEmptyEl) {
            selectedDateEmptyEl.style.display = 'none';
        }

        // Load showtimes for selected date
        await loadShowtimesForDate(dateKey);
    }

    async function loadShowtimesForDate(dateKey) {
        try {
            const response = await fetch(`/api/showtimes/day?date=${dateKey}`);
            if (!response.ok) throw new Error('Failed to load showtimes');
            const showtimes = await response.json();
            selectedDateShowtimes = showtimes;
            await renderSelectedDateMovies(showtimes);
        } catch (error) {
            console.error('Error loading showtimes:', error);
            showError('Không thể tải lịch chiếu');
            const selectedDateMoviesListEl = document.getElementById('selectedDateMoviesList');
            const selectedDateEmptyEl = document.getElementById('selectedDateEmpty');
            if (selectedDateMoviesListEl) selectedDateMoviesListEl.innerHTML = '';
            if (selectedDateEmptyEl) selectedDateEmptyEl.style.display = 'block';
        }
    }

    async function renderSelectedDateMovies(showtimes) {
        const container = document.getElementById('selectedDateMoviesList');
        const empty = document.getElementById('selectedDateEmpty');

        if (!container) return;

        if (showtimes.length === 0) {
            container.innerHTML = '';
            empty.style.display = 'block';
            return;
        }

        empty.style.display = 'none';

        // Group showtimes by movie
        const grouped = showtimes.reduce((acc, showtime) => {
            const movieId = showtime.movieId;
            if (!acc[movieId]) {
                acc[movieId] = {
                    movieId: showtime.movieId,
                    movieTitle: showtime.movieTitle,
                    showtimes: []
                };
            }
            acc[movieId].showtimes.push(showtime);
            return acc;
        }, {});

        // Load movie details for each movie
        const movieDetailsPromises = Object.keys(grouped).map(async (movieId) => {
            try {
                const response = await fetch(`/api/movies/${movieId}`);
                if (response.ok) {
                    const movie = await response.json();
                    return { movieId: parseInt(movieId), movie };
                }
            } catch (error) {
                console.error(`Error loading movie ${movieId}:`, error);
            }
            return { movieId: parseInt(movieId), movie: null };
        });

        const movieDetails = await Promise.all(movieDetailsPromises);
        const movieMap = movieDetails.reduce((acc, { movieId, movie }) => {
            acc[movieId] = movie;
            return acc;
        }, {});

        container.innerHTML = Object.values(grouped).map(group => {
            const movie = movieMap[group.movieId] || {};
            const showtimes = group.showtimes.sort((a, b) => new Date(a.startTime) - new Date(b.startTime));

            return `
                <div class="showtime-group">
                    <div class="showtime-group-header">
                        <img src="${movie.posterUrl || '/img/placeholder.jpg'}" 
                             alt="${escapeHtml(group.movieTitle)}" 
                             class="showtime-group-poster"
                             onerror="this.src='/img/placeholder.jpg'">
                        <div class="showtime-group-info">
                            <h4>${escapeHtml(group.movieTitle)}</h4>
                            <div class="showtime-group-meta">
                                ${movie.ageRating || 'P'} | ${movie.durationMinutes || 0} phút
                            </div>
                        </div>
                    </div>
                    <div class="showtime-slots">
                        ${showtimes.map(st => `
                            <div class="showtime-slot" onclick="window.location.href='/staff/counter-booking?showtimeId=${st.showtimeId ?? st.id}'">
                                <div class="showtime-slot-time">${formatTime(st.startTime)}</div>
                                <div class="showtime-slot-auditorium">${escapeHtml(st.auditoriumName || 'N/A')}</div>
                            </div>
                        `).join('')}
                    </div>
                </div>
            `;
        }).join('');
    }

    async function loadDashboard() {
        try {
            const response = await fetch('/api/movies/all');
            if (!response.ok) throw new Error('Failed to load movies');
            movies = await response.json();
            filteredMovies = movies;
            
            // Show movies by status section by default if no date selected
            if (!selectedDate) {
                const selectedDateEl = document.getElementById('selectedDateMovies');
                const moviesByStatusEl = document.querySelector('.movies-by-status-section');
                
                if (selectedDateEl) selectedDateEl.style.display = 'none';
                if (moviesByStatusEl) moviesByStatusEl.style.display = 'block';
            }
        } catch (error) {
            console.error('Error loading movies:', error);
            showError('Không thể tải danh sách phim');
        }
    }

    async function loadMoviesByStatus() {
        try {
            // Load all movies first to categorize by showtimes
            const allResponse = await fetch('/api/movies/all');
            if (!allResponse.ok) return;

            const allMovies = await allResponse.json();
            const now = new Date();
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            const tomorrow = new Date(today);
            tomorrow.setDate(tomorrow.getDate() + 1);

            // Categorize movies based on today's showtimes
            const categorized = await Promise.all(
                allMovies.map(async (movie) => {
                    try {
                        const response = await fetch(`/api/showtimes/movie/${movie.id}`);
                        if (!response.ok) {
                            return { movie, category: 'none' };
                        }

                        const allShowtimes = await response.json();
                        
                        // Get showtimes for today only
                        const todayShowtimes = allShowtimes.filter(st => {
                            const showtimeDate = new Date(st.startTime);
                            return showtimeDate >= today && showtimeDate < tomorrow;
                        });

                        if (todayShowtimes.length === 0) {
                            return { movie, category: 'none' };
                        }

                        // Check if any showtime is currently playing (startTime <= now <= endTime)
                        const currentlyPlaying = todayShowtimes.some(st => {
                            const start = new Date(st.startTime);
                            const end = new Date(st.endTime);
                            return start <= now && now <= end;
                        });

                        if (currentlyPlaying) {
                            // Movie is currently playing - show all today's showtimes
                            return { movie, category: 'nowShowing', showtimes: todayShowtimes };
                        }

                        // Check if all showtimes in today have ended (all endTime < now)
                        const allEnded = todayShowtimes.every(st => {
                            const end = new Date(st.endTime);
                            return end < now;
                        });

                        if (allEnded) {
                            // All showtimes in today have ended
                            return { movie, category: 'stopped', showtimes: [] };
                        }

                        // Check if all showtimes are in future (all startTime > now) - coming soon
                        const futureShowtimes = todayShowtimes.filter(st => {
                            const start = new Date(st.startTime);
                            return start > now;
                        });

                        if (futureShowtimes.length > 0) {
                            // Has future showtimes in today and not currently playing = coming soon
                            return { movie, category: 'comingSoon', showtimes: futureShowtimes };
                        }

                        // Should not reach here, but if it does, consider stopped
                        return { movie, category: 'stopped', showtimes: [] };
                    } catch (error) {
                        console.error(`Error categorizing movie ${movie.id}:`, error);
                        return { movie, category: 'none' };
                    }
                })
            );

            // Separate into categories
            const comingSoon = categorized
                .filter(item => item.category === 'comingSoon')
                .map(item => ({ movie: item.movie, showtimes: item.showtimes || [] }));
            
            const nowShowing = categorized
                .filter(item => item.category === 'nowShowing')
                .map(item => ({ movie: item.movie, showtimes: item.showtimes || [] }));
            
            const stopped = categorized
                .filter(item => item.category === 'stopped')
                .map(item => ({ movie: item.movie, showtimes: [] }));

            // Render each category
            await renderStatusMoviesWithShowtimes('comingSoon', comingSoon);
            await renderStatusMoviesWithShowtimes('nowShowing', nowShowing);
            await renderStatusMoviesWithShowtimes('stopped', stopped);
        } catch (error) {
            console.error('Error loading movies by status:', error);
        }
    }

    async function renderStatusMoviesWithShowtimes(status, moviesWithShowtimes) {
        const containerId = status === 'comingSoon' ? 'comingSoonMovies' :
                           status === 'nowShowing' ? 'nowShowingMovies' : 'stoppedMovies';
        const emptyId = status === 'comingSoon' ? 'comingSoonEmpty' :
                       status === 'nowShowing' ? 'nowShowingEmpty' : 'stoppedEmpty';
        
        const container = document.getElementById(containerId);
        const empty = document.getElementById(emptyId);

        if (!container) return;

        if (moviesWithShowtimes.length === 0) {
            container.innerHTML = '';
            if (empty) empty.style.display = 'block';
            return;
        }

        if (empty) empty.style.display = 'none';

        container.innerHTML = moviesWithShowtimes.map(({ movie, showtimes }) => {
            // Sort showtimes by start time
            const sortedShowtimes = (showtimes || []).sort((a, b) => 
                new Date(a.startTime) - new Date(b.startTime)
            ).slice(0, 6); // Limit to 6 showtimes

            // For stopped movies, don't show showtimes section at all
            if (status === 'stopped') {
                return `
                    <div class="status-movie-item">
                        <img src="${movie.posterUrl || '/img/placeholder.jpg'}" 
                             alt="${escapeHtml(movie.title)}" 
                             class="status-movie-poster"
                             onerror="this.src='/img/placeholder.jpg'">
                        <div class="status-movie-details">
                            <h3 class="status-movie-title">${escapeHtml(movie.title)}</h3>
                            <div class="status-movie-info">
                                <div class="status-movie-info-item">
                                    <i class="bi bi-clock"></i>
                                    <span>${movie.durationMinutes || 0} phút</span>
                                </div>
                                <div class="status-movie-info-item">
                                    <i class="bi bi-person-badge"></i>
                                    <span>${movie.ageRating || 'P'}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
            }

            // For coming soon and now showing, show showtimes if available
            return `
                <div class="status-movie-item">
                    <img src="${movie.posterUrl || '/img/placeholder.jpg'}" 
                         alt="${escapeHtml(movie.title)}" 
                         class="status-movie-poster"
                         onerror="this.src='/img/placeholder.jpg'">
                    <div class="status-movie-details">
                        <h3 class="status-movie-title">${escapeHtml(movie.title)}</h3>
                        <div class="status-movie-info">
                            <div class="status-movie-info-item">
                                <i class="bi bi-clock"></i>
                                <span>${movie.durationMinutes || 0} phút</span>
                            </div>
                            <div class="status-movie-info-item">
                                <i class="bi bi-person-badge"></i>
                                <span>${movie.ageRating || 'P'}</span>
                            </div>
                        </div>
                        ${sortedShowtimes.length > 0 ? `
                            <div class="status-movie-showtimes">
                                <div class="status-movie-showtimes-label">Suất chiếu:</div>
                                <div class="status-showtimes-grid">
                                    ${sortedShowtimes.map(st => `
                                        <a href="/staff/counter-booking?showtimeId=${st.showtimeId ?? st.id}" class="status-showtime-item">
                                            <div class="status-showtime-time">${formatTime(st.startTime)}</div>
                                            <div class="status-showtime-auditorium">${escapeHtml(st.auditoriumName || 'N/A')}</div>
                                        </a>
                                    `).join('')}
                                </div>
                            </div>
                        ` : ''}
                    </div>
                </div>
            `;
        }).join('');
    }

    function applyMovieFilters() {
        // Filters are now handled by tabs, so we just reload movies by status
        selectedDate = null;
        const selectedDateEl = document.getElementById('selectedDateMovies');
        const moviesByStatusEl = document.querySelector('.movies-by-status-section');
        
        if (selectedDateEl) selectedDateEl.style.display = 'none';
        if (moviesByStatusEl) moviesByStatusEl.style.display = 'block';
        
        // Update calendar active state
        document.querySelectorAll('.week-day').forEach(day => {
            day.classList.remove('active');
        });
        
        // Reload movies by status
        loadMoviesByStatus();
    }

    function clearMovieFilters() {
        document.getElementById('movieSearch').value = '';
        document.getElementById('movieStatusFilter').value = 'all';
        filteredMovies = movies;
        
        // Clear date selection and show movies by status
        selectedDate = null;
        const selectedDateEl = document.getElementById('selectedDateMovies');
        const moviesByStatusEl = document.querySelector('.movies-by-status-section');
        
        if (selectedDateEl) selectedDateEl.style.display = 'none';
        if (moviesByStatusEl) moviesByStatusEl.style.display = 'block';
        
        // Update calendar active state
        document.querySelectorAll('.week-day').forEach(day => {
            day.classList.remove('active');
        });
        
        // Reload movies by status
        loadMoviesByStatus();
    }

    function getMovieStatus(movie) {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const releaseDate = movie.releaseDate ? new Date(movie.releaseDate) : null;
        const endDate = movie.endDate ? new Date(movie.endDate) : null;

        if (endDate && endDate < today) {
            return 'Stopped';
        }
        if (releaseDate && releaseDate > today) {
            return 'ComingSoon';
        }
        return 'NowShowing';
    }


    async function loadPendingBookings() {
        try {
            const response = await fetch('/api/staff/bookings?pendingOnly=true');
            if (!response.ok) throw new Error('Failed to load pending bookings');
            pendingBookings = await response.json();
            renderPendingBookings();
        } catch (error) {
            console.error('Error loading pending bookings:', error);
            showError('Không thể tải danh sách booking đang chờ');
        }
    }

    function applyPendingFilters() {
        const keyword = document.getElementById('pendingSearch')?.value.trim().toLowerCase() || '';
        renderPendingBookings(keyword);
    }

    function clearPendingFilters() {
        document.getElementById('pendingSearch').value = '';
        renderPendingBookings();
    }

    function renderPendingBookings(keyword = '') {
        const tbody = document.getElementById('pendingBookingsTable');
        const empty = document.getElementById('pendingEmpty');

        if (!tbody) return;

        let filtered = pendingBookings;
        if (keyword) {
            filtered = pendingBookings.filter(booking => {
                const searchText = `${booking.bookingCode} ${booking.movieTitle} ${booking.auditoriumName}`.toLowerCase();
                return searchText.includes(keyword);
            });
        }

        if (filtered.length === 0) {
            tbody.innerHTML = '';
            empty.style.display = 'block';
            return;
        }

        empty.style.display = 'none';
        tbody.innerHTML = filtered.map(booking => {
            const seatCount = booking.seats?.length || 0;
            const paymentMethod = booking.paymentMethod || '-';
            const paymentStatus = booking.paymentStatus === 'Paid' ? 'Paid' : 'Unpaid';
            const createdAt = formatDateTime(booking.createdAt);
            const showtimeTime = formatDateTime(booking.showtimeStart);

            return `
                <tr>
                    <td><strong>${escapeHtml(booking.bookingCode)}</strong><br><small>${createdAt}</small></td>
                    <td>
                        <strong>${escapeHtml(booking.movieTitle)}</strong><br>
                        <small>${showtimeTime} - ${escapeHtml(booking.auditoriumName)}</small>
                    </td>
                    <td>${seatCount}</td>
                    <td>
                        <span class="badge ${paymentStatus === 'Paid' ? 'badge-success' : 'badge-warning'}">${paymentStatus}</span><br>
                        <small>${escapeHtml(paymentMethod)}</small>
                    </td>
                    <td>${formatTimeOnly(booking.createdAt)}</td>
                    <td>
                        <div class="booking-actions">
                            <button class="btn-sm btn-primary-sm" onclick="viewBookingDetail('${booking.bookingCode}')">Chi tiết</button>
                            ${paymentMethod === 'VietQR' || paymentMethod === 'VIETQR' ? `
                                <button class="btn-sm btn-secondary-sm" onclick="verifyPayment(${booking.bookingId}, 'VietQR')">VietQR</button>
                            ` : ''}
                            <button class="btn-sm btn-secondary-sm" onclick="verifyPayment(${booking.bookingId}, 'Cash')">Xác nhận tiền mặt</button>
                            <button class="btn-sm btn-danger-sm" onclick="cancelBooking(${booking.bookingId})">Hủy</button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    }

    async function loadTodayBookings() {
        try {
            const today = new Date().toISOString();
            const response = await fetch(`/api/staff/bookings?date=${today}`);
            if (!response.ok) throw new Error('Failed to load today bookings');
            todayBookings = await response.json();
            renderTodayBookings();
        } catch (error) {
            console.error('Error loading today bookings:', error);
            showError('Không thể tải danh sách booking hôm nay');
        }
    }

    function applyTodayFilters() {
        const keyword = document.getElementById('todaySearch')?.value.trim().toLowerCase() || '';
        renderTodayBookings(keyword);
    }

    function clearTodayFilters() {
        document.getElementById('todaySearch').value = '';
        renderTodayBookings();
    }

    function renderTodayBookings(keyword = '') {
        const tbody = document.getElementById('todayBookingsTable');
        const empty = document.getElementById('todayEmpty');

        if (!tbody) return;

        let filtered = todayBookings;
        if (keyword) {
            filtered = todayBookings.filter(booking => {
                const searchText = `${booking.bookingCode} ${booking.movieTitle} ${booking.auditoriumName}`.toLowerCase();
                return searchText.includes(keyword);
            });
        }

        if (filtered.length === 0) {
            tbody.innerHTML = '';
            empty.style.display = 'block';
            return;
        }

        empty.style.display = 'none';
        tbody.innerHTML = filtered.map(booking => {
            const paymentMethod = booking.paymentMethod || '-';
            const paymentStatus = booking.paymentStatus === 'Paid' ? 'Paid' : 'Unpaid';
            const bookingStatus = booking.bookingStatus || 'Pending';
            const staffName = booking.createdByStaffName || '-';
            const showtimeTime = formatDateTime(booking.showtimeStart);

            return `
                <tr>
                    <td><strong>${escapeHtml(booking.bookingCode)}</strong></td>
                    <td>${escapeHtml(booking.movieTitle)}</td>
                    <td>${showtimeTime}<br><small>${escapeHtml(booking.auditoriumName)}</small></td>
                    <td>
                        <span class="badge ${paymentStatus === 'Paid' ? 'badge-success' : 'badge-warning'}">${paymentStatus}</span><br>
                        <small>${escapeHtml(paymentMethod)}</small>
                    </td>
                    <td><span class="badge badge-${getBookingStatusClass(bookingStatus)}">${getBookingStatusLabel(bookingStatus)}</span></td>
                    <td>${escapeHtml(staffName)}</td>
                    <td>
                        <button class="btn-sm btn-primary-sm" onclick="viewBookingDetail('${booking.bookingCode}')">Chi tiết</button>
                    </td>
                </tr>
            `;
        }).join('');
    }

    // Helper functions
    function formatDateTime(dateStr) {
        if (!dateStr) return '-';
        const date = new Date(dateStr);
        if (isNaN(date.getTime())) return dateStr;
        return new Intl.DateTimeFormat('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        }).format(date);
    }

    function formatTime(dateStr) {
        if (!dateStr) return '-';
        const date = new Date(dateStr);
        if (isNaN(date.getTime())) return dateStr;
        return new Intl.DateTimeFormat('vi-VN', {
            hour: '2-digit',
            minute: '2-digit'
        }).format(date);
    }

    function formatTimeOnly(dateStr) {
        if (!dateStr) return '-';
        const date = new Date(dateStr);
        if (isNaN(date.getTime())) return dateStr;
        return new Intl.DateTimeFormat('vi-VN', {
            hour: '2-digit',
            minute: '2-digit'
        }).format(date);
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function getBookingStatusClass(status) {
        const map = {
            'Pending': 'warning',
            'PendingVerification': 'warning',
            'Confirmed': 'success',
            'Cancelled': 'danger',
            'Refunded': 'danger'
        };
        return map[status] || 'warning';
    }

    function getBookingStatusLabel(status) {
        const map = {
            'Pending': 'Pending',
            'PendingVerification': 'Đang xác minh',
            'Confirmed': 'Đã xác nhận',
            'Cancelled': 'Đã hủy',
            'Refunded': 'Đã hoàn tiền'
        };
        return map[status] || status;
    }

    function showError(message) {}

    // Global functions for buttons
    window.viewBookingDetail = function(bookingCode) {
        window.location.href = `/staff/pending-tickets?focus=${bookingCode}`;
    };

    window.verifyPayment = async function(bookingId, method) {
        try {
            const response = await fetch(`/api/staff/bookings/${bookingId}/payment-method`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ paymentMethod: method })
            });
            if (!response.ok) throw new Error('Failed to update payment method');
            
            const result = await response.json();
            if (result.paymentStatus === 'Unpaid') {
                // Verify the booking
                const verifyResponse = await fetch(`/api/staff/bookings/${bookingId}/verify`, {
                    method: 'POST'
                });
                if (!verifyResponse.ok) throw new Error('Failed to verify booking');
            }
            
            // Reload the page
            if (currentPage === 'pending') {
                loadPendingBookings();
            } else {
                loadTodayBookings();
            }
        } catch (error) {
            console.error('Error verifying payment:', error);
            alert('Không thể xác nhận thanh toán: ' + error.message);
        }
    };

    window.cancelBooking = async function(bookingId) {
        if (!confirm('Bạn có chắc chắn muốn hủy booking này?')) return;
        
        try {
            const response = await fetch(`/api/staff/bookings/${bookingId}/cancel`, {
                method: 'POST'
            });
            if (!response.ok) throw new Error('Failed to cancel booking');
            
            if (currentPage === 'pending') {
                loadPendingBookings();
            } else {
                loadTodayBookings();
            }
        } catch (error) {
            console.error('Error cancelling booking:', error);
            alert('Không thể hủy booking: ' + error.message);
        }
    };

})();

