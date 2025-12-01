(function () {
    const ready = (cb) => {
        if (document.readyState !== 'loading') {
            cb();
        } else {
            document.addEventListener('DOMContentLoaded', cb);
        }
    };

    const formatDateTime = (value, options = {}) => {
        if (!value) {
            return '';
        }
        try {
            const date = new Date(value);
            return new Intl.DateTimeFormat('vi-VN', {
                weekday: 'short',
                day: '2-digit',
                month: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                ...options
            }).format(date);
        } catch (error) {
            return value;
        }
    };

    const formatCurrency = (value) => {
        try {
            return new Intl.NumberFormat('vi-VN').format(value || 0);
        } catch (error) {
            return value || 0;
        }
    };

    ready(() => {
        const root = document.getElementById('staffShowtimesRoot');
        if (!root) {
            return;
        }
        const endpoint = root.dataset.showtimesEndpoint;
        const bookingBase = root.dataset.bookingBase || '/staff/counter-booking';
        const defaultDate = root.dataset.defaultDate || '';

        const filterForm = document.getElementById('staffShowtimesFilter');
        const movieSelect = document.getElementById('filterMovie');
        const auditoriumSelect = document.getElementById('filterAuditorium');
        const dateInput = document.getElementById('filterDate');
        const startTimeInput = document.getElementById('filterStartTime');
        const endTimeInput = document.getElementById('filterEndTime');
        const statusSelect = document.getElementById('filterStatus');
        const resetBtn = document.getElementById('resetFilterBtn');

        const tableBody = document.getElementById('staffShowtimesBody');
        const emptyState = document.getElementById('staffShowtimesEmpty');
        const showtimeCountEl = document.getElementById('showtimeCount');
        const soldSeatCountEl = document.getElementById('soldSeatCount');
        const avgOccupancyEl = document.getElementById('avgOccupancy');

        const state = {
            showtimes: []
        };

        const setLoadingRow = (message) => {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center py-4 text-muted">${message}</td>
                </tr>
            `;
        };

        const setEmptyMessage = (message, hidden = false) => {
            if (!emptyState) {
                return;
            }
            emptyState.textContent = message;
            emptyState.hidden = hidden;
        };

        const buildRangeParams = (params) => {
            const dateValue = dateInput.value || defaultDate;
            if (!dateValue) {
                return;
            }
            const startTimeValue = startTimeInput.value || '00:00';
            const endTimeValue = endTimeInput.value || '';

            const startDateTime = `${dateValue}T${startTimeValue}:00`;
            params.append('start', startDateTime);

            if (endTimeValue) {
                params.append('end', `${dateValue}T${endTimeValue}:00`);
            } else {
                const date = new Date(`${dateValue}T00:00:00`);
                date.setDate(date.getDate() + 1);
                const nextDay = date.toISOString().slice(0, 10);
                params.append('end', `${nextDay}T00:00:00`);
            }
        };

        const buildQueryParams = () => {
            const params = new URLSearchParams();
            buildRangeParams(params);
            return params;
        };

        const getShowtimeStatus = (showtime) => {
            const now = Date.now();
            const start = showtime.startTime ? new Date(showtime.startTime).getTime() : 0;
            const end = showtime.endTime ? new Date(showtime.endTime).getTime() : 0;
            if (end && end < now) {
                return 'FINISHED';
            }
            if (start && start <= now && end && end >= now) {
                return 'RUNNING';
            }
            return 'UPCOMING';
        };

        const occupyBadgeClass = (value) => {
            const percent = Number(value || 0);
            if (percent >= 85) {
                return 'bg-danger';
            }
            if (percent >= 60) {
                return 'bg-warning text-dark';
            }
            return 'bg-success';
        };

        const populateFilterOptions = () => {
            const movies = new Map();
            const auditoriums = new Map();

            state.showtimes.forEach((showtime) => {
                if (showtime.movieTitle && showtime.movieTitle.trim()) {
                    movies.set(showtime.movieTitle, showtime.movieTitle);
                }
                if (showtime.auditoriumName && showtime.auditoriumName.trim()) {
                    auditoriums.set(showtime.auditoriumName, showtime.auditoriumName);
                }
            });

            const currentMovie = movieSelect.value;
            movieSelect.innerHTML = `<option value="">Tất cả phim</option>`;
            movies.forEach((label) => {
                const option = document.createElement('option');
                option.value = label;
                option.textContent = label;
                movieSelect.appendChild(option);
            });
            movieSelect.value = currentMovie;

            const currentAuditorium = auditoriumSelect.value;
            auditoriumSelect.innerHTML = `<option value="">Tất cả phòng</option>`;
            auditoriums.forEach((label) => {
                const option = document.createElement('option');
                option.value = label;
                option.textContent = label;
                auditoriumSelect.appendChild(option);
            });
            auditoriumSelect.value = currentAuditorium;
        };

        const filterShowtimes = () => {
            let filtered = [...state.showtimes];

            if (movieSelect.value) {
                filtered = filtered.filter((item) => item.movieTitle === movieSelect.value);
            }
            if (auditoriumSelect.value) {
                filtered = filtered.filter((item) => item.auditoriumName === auditoriumSelect.value);
            }
            if (statusSelect.value) {
                filtered = filtered.filter((item) => getShowtimeStatus(item) === statusSelect.value);
            }

            const startTimeValue = startTimeInput.value;
            const endTimeValue = endTimeInput.value;
            if (startTimeValue) {
                filtered = filtered.filter((item) => {
                    const start = item.startTime ? new Date(item.startTime) : null;
                    if (!start) {
                        return false;
                    }
                    const comparable = start.toISOString().slice(11, 16);
                    return comparable >= startTimeValue;
                });
            }
            if (endTimeValue) {
                filtered = filtered.filter((item) => {
                    const start = item.startTime ? new Date(item.startTime) : null;
                    if (!start) {
                        return false;
                    }
                    const comparable = start.toISOString().slice(11, 16);
                    return comparable <= endTimeValue;
                });
            }

            return filtered;
        };

        const updateStats = (items) => {
            const totalShowtimes = items.length;
            const soldSeats = items.reduce((sum, item) => sum + Number(item.soldSeats || 0), 0);
            const averageOccupancy = items.length
                ? (items.reduce((sum, item) => sum + Number(item.occupancyPercent || 0), 0) / items.length)
                : 0;

            showtimeCountEl.textContent = totalShowtimes;
            soldSeatCountEl.textContent = formatCurrency(soldSeats);
            avgOccupancyEl.textContent = `${averageOccupancy.toFixed(0)}%`;
        };

        const renderTable = () => {
            const showtimes = filterShowtimes();
            updateStats(showtimes);
            if (!showtimes.length) {
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="7" class="text-center py-4 text-muted">Không tìm thấy suất chiếu.</td>
                    </tr>
                `;
                setEmptyMessage('Không tìm thấy suất chiếu phù hợp.', false);
                return;
            }
            setEmptyMessage('Không tìm thấy suất chiếu phù hợp.', true);
            const rows = showtimes.map((item) => {
                const occupancy = Number(item.occupancyPercent || 0).toFixed(0);
                const badgeClass = occupyBadgeClass(occupancy);
                const status = getShowtimeStatus(item);
                const statusLabel = status === 'RUNNING'
                    ? 'Đang chiếu'
                    : status === 'FINISHED'
                        ? 'Đã kết thúc'
                        : 'Chưa chiếu';
                const timeLabel = `${formatDateTime(item.startTime)} - ${formatDateTime(item.endTime, {
                    weekday: undefined
                })}`;
                const bookingUrl = `${bookingBase}?showtimeId=${item.showtimeId}`;
                return `
                    <tr>
                        <td>
                            <div class="fw-semibold">${item.movieTitle || '---'}</div>
                            <small class="text-muted">${statusLabel}</small>
                        </td>
                        <td>${item.auditoriumName || '---'}</td>
                        <td>${timeLabel}</td>
                        <td class="text-center">${item.totalSeats || item.sellableSeats || 0}</td>
                        <td class="text-center">${item.soldSeats || 0}</td>
                        <td class="text-center">
                            <span class="badge ${badgeClass}">${occupancy}%</span>
                        </td>
                        <td class="text-end">
                            <a class="btn btn-sm btn-outline" href="${bookingUrl}">Đặt vé</a>
                        </td>
                    </tr>
                `;
            });
            tableBody.innerHTML = rows.join('');
        };

        const loadShowtimes = async () => {
            if (!endpoint) {
                return;
            }
            setLoadingRow('Đang tải dữ liệu suất chiếu...');
            setEmptyMessage('Không tìm thấy suất chiếu phù hợp.', true);
            try {
                const params = buildQueryParams();
                const url = new URL(endpoint, window.location.origin);
                if ([...params.keys()].length) {
                    url.search = params.toString();
                }
                const response = await fetch(url.toString(), { credentials: 'include' });
                if (!response.ok) {
                    throw new Error('Không thể tải danh sách suất chiếu.');
                }
                const payload = await response.json();
                state.showtimes = Array.isArray(payload) ? payload : [];
                populateFilterOptions();
                renderTable();
            } catch (error) {
                setEmptyMessage(error.message || 'Không thể tải dữ liệu.', false);
                setLoadingRow('Đã xảy ra lỗi khi tải dữ liệu.');
            }
        };

        filterForm.addEventListener('submit', (event) => {
            event.preventDefault();
            loadShowtimes();
        });

        statusSelect.addEventListener('change', renderTable);
        movieSelect.addEventListener('change', renderTable);
        auditoriumSelect.addEventListener('change', renderTable);
        startTimeInput.addEventListener('change', renderTable);
        endTimeInput.addEventListener('change', renderTable);

        resetBtn.addEventListener('click', () => {
            movieSelect.value = '';
            auditoriumSelect.value = '';
            statusSelect.value = '';
            startTimeInput.value = '';
            endTimeInput.value = '';
            dateInput.value = defaultDate || '';
            loadShowtimes();
        });

        if (defaultDate && dateInput) {
            dateInput.value = defaultDate;
        }
        loadShowtimes();
    });
})();
