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
    const selectionPlaceholder = document.getElementById('showtimeSelectionPlaceholder');
    const today = new Date();
    const endDate = new Date();
    endDate.setDate(today.getDate() + 6);

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

    const renderSlots = (dateKey) => {
        slotGrid.innerHTML = '';
        const slots = groupedShowtimes[dateKey] || [];
        if (slots.length === 0) {
            slotGrid.innerHTML = '<div class="showtime-empty-state">Không có suất chiếu cho ngày này.</div>';
            selectionPlaceholder.classList.remove('active');
            selectionPlaceholder.textContent = 'Chọn một suất chiếu để xem chi tiết ghế (tính năng sẽ được cập nhật).';
            return;
        }
        slots.forEach((slot) => {
            const btn = document.createElement('button');
            btn.className = 'showtime-slot';
            const timeLabel = formatTimeLabel(slot.startTime);
            btn.innerHTML = `<strong>${timeLabel}</strong><small>${slot.auditoriumName || 'Phòng chiếu'}</small>`;
            btn.addEventListener('click', () => {
                document.querySelectorAll('.showtime-slot.active').forEach((el) => el.classList.remove('active'));
                btn.classList.add('active');
                selectionPlaceholder.classList.add('active');
                selectionPlaceholder.textContent = `Bạn đã chọn suất ${timeLabel} - ${slot.auditoriumName || 'phòng chiếu'} (tính năng chọn ghế sẽ được cập nhật).`;
            });
            slotGrid.appendChild(btn);
        });
    };

    const setActiveDate = (dateKey) => {
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
        renderDateStrip();
        setActiveDate(activeDateKey);
    };

    const fetchShowtimes = () => {
        const fromParam = formatDateParam(today);
        const toParam = formatDateParam(endDate);
        fetch(`/api/showtimes/movie/${movieId}?from=${fromParam}&to=${toParam}`)
            .then((response) => {
                if (!response.ok) {
                    throw new Error('Failed to load showtimes');
                }
                return response.json();
            })
            .then(applyShowtimes)
            .catch(() => {
                dateStrip.innerHTML = '<div class="showtime-empty-state">Không thể tải lịch chiếu.</div>';
                slotGrid.innerHTML = '';
            });
    };

    // Initial render and async fetch of showtime slots.
    renderDateStrip();
    renderSlots(activeDateKey);
    fetchShowtimes();
});
