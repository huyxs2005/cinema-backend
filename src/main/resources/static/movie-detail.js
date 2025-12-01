'use strict';

document.addEventListener('DOMContentLoaded', () => {
    const toast = document.getElementById('movieLinkToast');
    let toastTimeoutId;

    const showMissingToast = (message, type = 'error') => {
        if (!toast) {
            alert(message);
            return;
        }
        toast.textContent = message;
        toast.classList.remove('show', 'error', 'warning');
        toast.classList.add(type === 'warning' ? 'warning' : 'error');
        requestAnimationFrame(() => toast.classList.add('show'));
        clearTimeout(toastTimeoutId);
        toastTimeoutId = setTimeout(() => toast.classList.remove('show'), 3200);
    };

    document.querySelectorAll('[data-missing-message]').forEach((element) => {
        element.addEventListener('click', function handleMissingLink() {
            const message = this.dataset.missingMessage || 'Liên kết không tồn tại';
            const type = (this.dataset.toastType || 'error').trim().toLowerCase();
            showMissingToast(message, type);
        });
    });

    const showtimeSection = document.querySelector('.movie-showtimes-section');
    if (!showtimeSection) {
        return;
    }

    const movieId = showtimeSection.dataset.movieId;
    const dateStrip = document.getElementById('showtimeDateStrip');
    const slotGrid = document.getElementById('showtimeSlotGrid');
    const seatWrapper = document.getElementById('seatSelectionWrapper');
    const seatMessage = document.getElementById('seatSelectionMessage');
    const seatContainer = document.getElementById('seatSelectionContainer');
    const loginTrigger = document.getElementById('openLoginModal') || document.querySelector('[data-open-modal="login"]');
    const SEAT_INTENT_STORAGE_KEY = 'cinemaSeatIntent';
    const today = new Date();
    const endDate = new Date();
    endDate.setDate(today.getDate() + 6);
    const urlParams = new URLSearchParams(window.location.search);
    const queryShowtimeParam = urlParams.get('showtimeId') || urlParams.get('showtime');
    const pendingSeatIntent = consumeSeatIntentForMovie(movieId);
    let pendingAutoShowtimeId = queryShowtimeParam || pendingSeatIntent?.showtimeId || null;
    if (pendingAutoShowtimeId) {
        pendingAutoShowtimeId = String(pendingAutoShowtimeId);
    }

    const formatDateParam = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    const formatTimeLabel = (isoDate) => {
        const time = new Date(isoDate);
        return time.toLocaleTimeString('vi-VN', {hour: '2-digit', minute: '2-digit', hour12: false});
    };

    const weekdayLabel = (date) => date.toLocaleDateString('vi-VN', {weekday: 'long'});
    const monthLabel = (date) => `Th. ${String(date.getMonth() + 1).padStart(2, '0')}`;

    const buildRange = () => {
        const days = [];
        for (let i = 0; i < 7; i += 1) {
            const day = new Date(today);
            day.setDate(today.getDate() + i);
            days.push(day);
        }
        return days;
    };

    const range = buildRange();
    let groupedShowtimes = {};
    let activeDateKey = formatDateParam(today);
    let shouldScrollAfterLoad = Boolean(pendingAutoShowtimeId);

    const resetSeatSelection = () => {
        if (seatContainer) {
            seatContainer.hidden = true;
            seatContainer.innerHTML = '';
        }
        if (seatMessage) {
            seatMessage.hidden = true;
            seatMessage.textContent = '';
        }
        if (seatWrapper) {
            seatWrapper.hidden = true;
        }
    };

    const loadSeatLayout = async (slot, timeLabel) => {
        if (!seatContainer || !seatWrapper) {
            return;
        }
        seatWrapper.hidden = false;
        if (seatMessage) {
            seatMessage.hidden = false;
            seatMessage.textContent = `Đang tải sơ đồ ghế cho suất ${timeLabel} - ${slot.auditoriumName || 'phòng chiếu'}...`;
        }
        seatContainer.hidden = true;
        seatContainer.innerHTML = '';
        if (shouldScrollAfterLoad && seatWrapper) {
            seatWrapper.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
        if (window.releaseCurrentSeatHold) {
            try {
                await window.releaseCurrentSeatHold();
            } catch (error) {
                // ignore
            }
        }
        try {
            const response = await fetch(`/movies/seat-fragment/${slot.id}`);
            if (response.status === 401 || response.status === 403) {
                if (seatMessage) {
                    seatMessage.hidden = false;
                    seatMessage.textContent = 'Bạn cần đăng nhập để đặt và giữ ghế.';
                }
                seatContainer.hidden = true;
                if (seatWrapper) {
                    seatWrapper.hidden = false;
                }
                if (window.setLoginPromptMessage) {
                    window.setLoginPromptMessage('Bạn cần phải đăng nhập để có thể đặt vé');
                    window.loginPromptLocked = true;
                }
                if (loginTrigger) {
                    loginTrigger.click();
                }
                return;
            }
            if (!response.ok) {
                throw new Error('seat_fragment_error');
            }
            const html = await response.text();
            seatContainer.innerHTML = html;
            seatContainer.hidden = false;
            if (seatMessage) {
                seatMessage.hidden = true;
            }
            if (window.setLoginPromptMessage) {
                window.setLoginPromptMessage();
            }
            if (window.initSeatSelection) {
                window.initSeatSelection(seatContainer);
            }
            if (shouldScrollAfterLoad && seatWrapper) {
                seatWrapper.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
            shouldScrollAfterLoad = false;
        } catch (error) {
            if (seatMessage) {
                seatMessage.hidden = false;
                seatMessage.textContent = 'Không thể tải sơ đồ ghế. Vui lòng thử lại sau.';
            }
            seatContainer.hidden = true;
            shouldScrollAfterLoad = false;
        }
    };

    const renderSlots = (dateKey) => {
        slotGrid.innerHTML = '';
        const slots = groupedShowtimes[dateKey] || [];
        if (slots.length === 0) {
            slotGrid.innerHTML = '<div class="showtime-empty-state">Không có suất chiếu cho ngày này.</div>';
            resetSeatSelection();
            return;
        }
        slots.forEach((slot) => {
            const btn = document.createElement('button');
            btn.className = 'showtime-slot';
            btn.dataset.showtimeId = String(slot.id);
            const timeLabel = formatTimeLabel(slot.startTime);
            btn.innerHTML = `<strong>${timeLabel}</strong><small>${slot.auditoriumName || 'Phòng chiếu'}</small>`;
            btn.addEventListener('click', () => {
                document.querySelectorAll('.showtime-slot.active').forEach((el) => el.classList.remove('active'));
                btn.classList.add('active');
                loadSeatLayout(slot, timeLabel);
            });
            slotGrid.appendChild(btn);
        });
        if (pendingAutoShowtimeId) {
            const autoButton = slotGrid.querySelector(`.showtime-slot[data-showtime-id="${pendingAutoShowtimeId}"]`);
            if (autoButton) {
                pendingAutoShowtimeId = null;
                autoButton.click();
            }
        }
    };

    const setActiveDate = (dateKey, options = {}) => {
        const shouldResetSeats = options.resetSeats !== false;
        if (shouldResetSeats) {
            resetSeatSelection();
            document.querySelectorAll('.showtime-slot.active').forEach((el) => el.classList.remove('active'));
        }
        activeDateKey = dateKey;
        dateStrip.querySelectorAll('.showtime-date-card').forEach((card) => {
            if (card.dataset.dateKey === dateKey) {
                card.classList.add('active');
            } else {
                card.classList.remove('active');
            }
        });
        renderSlots(dateKey);
    };

    const renderDateStrip = () => {
        dateStrip.innerHTML = '';
        range.forEach((date) => {
            const key = formatDateParam(date);
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'showtime-date-card';
            button.dataset.dateKey = key;
            button.innerHTML = `
                <span class="month-label">${monthLabel(date)}</span>
                <span class="date-number">${String(date.getDate()).padStart(2, '0')}</span>
                <span class="weekday">${weekdayLabel(date)}</span>
            `;
            button.addEventListener('click', () => setActiveDate(key));
            dateStrip.appendChild(button);
        });
    };

    const findDateKeyForShowtime = (targetId) => {
        if (!targetId) {
            return null;
        }
        const lookup = String(targetId);
        return Object.keys(groupedShowtimes).find((key) =>
            (groupedShowtimes[key] || []).some((slot) => String(slot.id) === lookup)
        ) || null;
    };

    const applyShowtimes = (items) => {
        groupedShowtimes = items.reduce((acc, item) => {
            if (!item.startTime) {
                return acc;
            }
            const dateKey = item.startTime.split('T')[0];
            acc[dateKey] = acc[dateKey] || [];
            acc[dateKey].push(item);
            return acc;
        }, {});
        Object.keys(groupedShowtimes).forEach((key) => {
            groupedShowtimes[key].sort((a, b) => new Date(a.startTime) - new Date(b.startTime));
        });
        if (pendingAutoShowtimeId) {
            const autoDateKey = findDateKeyForShowtime(pendingAutoShowtimeId);
            if (autoDateKey) {
                activeDateKey = autoDateKey;
            } else {
                pendingAutoShowtimeId = null;
                shouldScrollAfterLoad = false;
            }
        }
        renderDateStrip();
        setActiveDate(activeDateKey, { resetSeats: !pendingAutoShowtimeId });
    };

    const fetchShowtimes = () => {
        const fromParam = formatDateParam(today);
        const toParam = formatDateParam(endDate);
        fetch(`/api/showtimes/movie/${movieId}?from=${fromParam}&to=${toParam}`)
            .then((response) => {
                if (!response.ok) {
                    throw new Error('failed_showtimes');
                }
                return response.json();
            })
            .then(applyShowtimes)
            .catch(() => {
                dateStrip.innerHTML = '<div class="showtime-empty-state">Không thể tải lịch chiếu.</div>';
                slotGrid.innerHTML = '';
            });
    };

    renderDateStrip();
    renderSlots(activeDateKey);
    fetchShowtimes();

    function consumeSeatIntentForMovie(expectedMovieId) {
        try {
            const storage = window.sessionStorage;
            if (!storage) {
                return null;
            }
            const raw = storage.getItem(SEAT_INTENT_STORAGE_KEY);
            if (!raw) {
                return null;
            }
            storage.removeItem(SEAT_INTENT_STORAGE_KEY);
            const payload = JSON.parse(raw);
            if (payload?.expiresAt && Date.now() > payload.expiresAt) {
                return null;
            }
            if (payload?.movieId && expectedMovieId && String(payload.movieId) !== String(expectedMovieId)) {
                return null;
            }
            return payload;
        } catch (error) {
            try {
                window.sessionStorage?.removeItem(SEAT_INTENT_STORAGE_KEY);
            } catch (err) {
                // ignore
            }
            return null;
        }
    }
});
