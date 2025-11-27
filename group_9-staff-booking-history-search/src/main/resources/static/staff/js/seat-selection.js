document.addEventListener('DOMContentLoaded', () => {
    const seatGrid = document.getElementById('seatGrid');
    if (!seatGrid) {
        return;
    }
    const seatApi = seatGrid.dataset.seatApi;
    const showtimeId = seatGrid.dataset.showtimeId;
    const holdUrl = `/staff/api/showtimes/${showtimeId}/holds`;
    const releaseUrl = `${holdUrl}/release`;
    const selectedSeatInput = document.getElementById('selectedSeatsInput');
    const selectedSeatList = document.getElementById('selectedSeatList');
    const totalElement = document.getElementById('orderTotal');
    const comboInputs = Array.from(document.querySelectorAll('input[name^="combo_"]'));

    let seatsById = new Map();
    let couplePairs = new Map();
    let selectedSeats = new Map();

    init();

    function init() {
        refreshSeatMap();
        comboInputs.forEach(input => input.addEventListener('input', () => {
            if (parseInt(input.value, 10) < 0) {
                input.value = '0';
            }
            updateTotals();
        }));
        window.addEventListener('beforeunload', releaseAllSelectedSeats);
    }

    async function refreshSeatMap() {
        try {
            const resp = await fetch(seatApi, {cache: 'no-store'});
            if (!resp.ok) {
                throw new Error('Failed to load seat map');
            }
            const data = await resp.json();
            renderSeatGrid(data);
        } catch (err) {
            console.error(err);
            seatGrid.innerHTML = '<div class="empty-state">Không tải được sơ đồ ghế.</div>';
        }
    }

    function renderSeatGrid(data) {
        seatGrid.innerHTML = '';
        seatsById = new Map();
        couplePairs = new Map();
        const newSelection = new Map();

        const rows = new Map();
        (data.seats || []).forEach(seat => {
            seatsById.set(seat.showtimeSeatId, seat);
            if (seat.couple && seat.couplePairId) {
                if (!couplePairs.has(seat.couplePairId)) {
                    couplePairs.set(seat.couplePairId, []);
                }
                couplePairs.get(seat.couplePairId).push(seat);
            }
            if (!rows.has(seat.rowLabel)) {
                rows.set(seat.rowLabel, []);
            }
            rows.get(seat.rowLabel).push(seat);
            if (seat.heldByCurrentUser) {
                newSelection.set(seat.showtimeSeatId, seat);
            }
        });
        selectedSeats = newSelection;

        Array.from(rows.entries())
            .sort((a, b) => a[0].localeCompare(b[0]))
            .forEach(([rowLabel, seats]) => {
                const rowElement = document.createElement('div');
                rowElement.className = 'seat-row';
                const label = document.createElement('span');
                label.textContent = rowLabel;
                label.style.minWidth = '26px';
                rowElement.appendChild(label);
                seats.sort((a, b) => a.seatNumber - b.seatNumber)
                    .forEach(seat => rowElement.appendChild(createSeatElement(seat)));
                seatGrid.appendChild(rowElement);
            });
        renderSelectedSeatList();
        updateTotals();
    }

    function createSeatElement(seat) {
        const seatDiv = document.createElement('div');
        seatDiv.className = `seat ${seat.status.toLowerCase()}`;
        if (seat.couple) {
            seatDiv.classList.add('couple');
        }
        if (seat.heldByCurrentUser) {
            seatDiv.classList.add('selected');
        }
        seatDiv.textContent = seat.seatNumber;
        seatDiv.dataset.id = seat.showtimeSeatId;
        seatDiv.dataset.couple = seat.couple ? 'true' : 'false';
        seatDiv.dataset.pair = seat.couplePairId || '';
        const canInteract = seat.status === 'AVAILABLE' || seat.heldByCurrentUser;
        if (canInteract) {
            seatDiv.addEventListener('click', () => handleSeatToggle(seat.showtimeSeatId));
        }
        return seatDiv;
    }

    function renderSelectedSeatList() {
        if (!selectedSeatList) {
            return;
        }
        selectedSeatList.innerHTML = '';
        if (selectedSeats.size === 0) {
            selectedSeatList.innerHTML = '<span class="text-muted">Chưa chọn ghế</span>';
            return;
        }
        Array.from(selectedSeats.values())
            .sort((a, b) => a.rowLabel.localeCompare(b.rowLabel) || a.seatNumber - b.seatNumber)
            .forEach(seat => {
                const pill = document.createElement('span');
                pill.className = 'selected-seat-pill';
                pill.textContent = `${seat.rowLabel}${seat.seatNumber}`;
                selectedSeatList.appendChild(pill);
            });
    }

    async function handleSeatToggle(seatId) {
        const seat = seatsById.get(seatId);
        if (!seat) {
            return;
        }
        const idsToToggle = getSeatIdsForInteraction(seat);
        const currentlySelected = idsToToggle.every(id => selectedSeats.has(id));
        try {
            if (currentlySelected) {
                await releaseSeats(idsToToggle);
            } else {
                await holdSeats(idsToToggle);
            }
        } catch (err) {
            console.error(err);
            alert('Không thể cập nhật trạng thái ghế, vui lòng thử lại.');
        } finally {
            refreshSeatMap();
        }
    }

    function getSeatIdsForInteraction(seat) {
        if (seat.couple && seat.couplePairId && couplePairs.has(seat.couplePairId)) {
            return couplePairs.get(seat.couplePairId).map(item => item.showtimeSeatId);
        }
        return [seat.showtimeSeatId];
    }

    async function holdSeats(seatIds) {
        const payload = JSON.stringify({seatIds});
        const resp = await fetch(holdUrl, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: payload
        });
        if (!resp.ok) {
            throw new Error('Hold failed');
        }
    }

    async function releaseSeats(seatIds) {
        const payload = JSON.stringify({seatIds});
        const resp = await fetch(releaseUrl, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: payload
        });
        if (!resp.ok) {
            throw new Error('Release failed');
        }
    }

    function releaseAllSelectedSeats() {
        if (selectedSeats.size === 0) {
            return;
        }
        const ids = Array.from(selectedSeats.keys());
        const payload = JSON.stringify({seatIds: ids});
        if (navigator.sendBeacon) {
            const blob = new Blob([payload], {type: 'application/json'});
            navigator.sendBeacon(releaseUrl, blob);
        } else {
            fetch(releaseUrl, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: payload,
                keepalive: true
            }).catch(() => {});
        }
    }

    function updateTotals() {
        if (selectedSeatInput) {
            selectedSeatInput.value = Array.from(selectedSeats.keys()).join(',');
        }
        const seatTotal = Array.from(selectedSeats.values())
            .reduce((sum, seat) => sum + parseFloat(seat.price || 0), 0);
        const comboTotal = comboInputs.reduce((sum, input) => {
            const qty = parseInt(input.value, 10) || 0;
            const price = parseFloat(input.dataset.price || '0');
            const computed = qty * price;
            return sum + (isNaN(computed) ? 0 : computed);
        }, 0);
        const total = seatTotal + comboTotal;
        if (totalElement) {
            totalElement.textContent = formatCurrency(total);
        }
    }

    function formatCurrency(value) {
        return new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'}).format(value || 0);
    }
});
