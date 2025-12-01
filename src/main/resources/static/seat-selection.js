(function () {
    const ready = (callback) => {
        if (document.readyState !== 'loading') {
            callback();
        } else {
            document.addEventListener('DOMContentLoaded', callback);
        }
    };

    const HOLD_STORAGE_KEY = 'cinemaSeatHold';

    const persistHoldToken = (showtimeId, token) => {
        if (!showtimeId || !token) {
            return;
        }
        try {
            window.sessionStorage?.setItem(HOLD_STORAGE_KEY, JSON.stringify({
                showtimeId,
                holdToken: token
            }));
        } catch (error) {
            // ignore storage quota issues
        }
    };

    const clearStoredHold = () => {
        try {
            window.sessionStorage?.removeItem(HOLD_STORAGE_KEY);
        } catch (error) {
            // ignore
        }
    };

    const releaseStoredHold = async () => {
        let raw = null;
        try {
            raw = window.sessionStorage?.getItem(HOLD_STORAGE_KEY);
        } catch (error) {
            raw = null;
        }
        if (!raw) {
            return;
        }
        let payload = null;
        try {
            payload = JSON.parse(raw);
        } catch (error) {
            payload = null;
        }
        if (!payload?.showtimeId || !payload?.holdToken) {
            clearStoredHold();
            return;
        }
        try {
            await fetch(`/api/showtimes/${payload.showtimeId}/holds/${payload.holdToken}`, {
                method: 'DELETE',
                credentials: 'include'
            });
        } catch (error) {
            // best-effort cleanup
        }
        clearStoredHold();
    };

    const releaseUserHolds = async () => {
        try {
            await fetch('/api/showtimes/holds/release', {
                method: 'POST',
                credentials: 'include'
            });
        } catch (error) {
            /* noop */
        }
    };

    const formatCurrency = (value) => {
        try {
            return new Intl.NumberFormat('vi-VN', {
                style: 'currency',
                currency: 'VND',
                minimumFractionDigits: 0
            }).format(value || 0);
        } catch (error) {
            const fallback = Number(value || 0).toFixed(0);
            return `${fallback} ₫`;
        }
    };

    const bootstrap = (root = document) => {
        const seatLayout = root.querySelector('.seat-layout');
        if (!seatLayout) {
            return;
        }

        const seatButtons = Array.from(seatLayout.querySelectorAll('.seat[data-seat-id]'));
        if (!seatButtons.length) {
            return;
        }
        const posterUrl = seatLayout.dataset.posterUrl || '';

        const selectedList = root.querySelector('#selectedSeatsList');
        const placeholder = root.querySelector('#selectedSeatsPlaceholder');
        const totalEl = root.querySelector('#selectedTotal');
        const checkoutBtn = root.querySelector('[data-checkout-link]');
        const seatIdsInput = root.querySelector('#seatIdsInput');
        const holdTokenInput = root.querySelector('#holdTokenInput');
        const showtimeIdInput = root.querySelector('#showtimeIdInput');
        const limitMessage = root.querySelector('#selectionLimitMessage');
        const backButton = root.querySelector('[data-seat-back]');
        const seatTimerBox = root.querySelector('#seatSelectionTimer');
        const seatTimerDisplay = root.querySelector('#seatSelectionCountdown');
        const seatMessage = document.getElementById('seatSelectionMessage');
        const loginTrigger = document.querySelector('[data-open-modal="login"]');

        const currentUser = (typeof getCurrentUser === 'function' ? getCurrentUser() : window.CURRENT_USER) || null;
        const currentUserId = currentUser
            ? Number(
                currentUser.id ??
                currentUser.userId ??
                currentUser.userID ??
                currentUser.user_id ??
                currentUser.userID
            )
            : null;
        const selection = new Map();
        const maxSelection = Number(seatLayout.dataset.maxSelection || '0');
        const seatMapEndpoint = seatLayout.dataset.seatMapEndpoint || null;
        const showtimeId = Number(seatLayout.dataset.showtimeId || showtimeIdInput?.value || 0);
        const statusClasses = ['seat--available', 'seat--held', 'seat--sold', 'seat--disabled'];
        const typeClasses = ['seat--type-standard', 'seat--type-vip', 'seat--type-couple'];
        const SEAT_MAP_REFRESH_INTERVAL = 3000;

        let seatMapIntervalId = null;
        let seatTimerIntervalId = null;
        let holdToken = holdTokenInput?.value || null;
        let navigatingToCheckout = false;

        const showSeatMessage = (message, tone = 'warning') => {
            if (!seatMessage || !message) {
                return;
            }
            seatMessage.hidden = false;
            seatMessage.textContent = message;
            seatMessage.dataset.tone = tone;
        };

        const hideSeatMessage = () => {
            if (!seatMessage) {
                return;
            }
            seatMessage.hidden = true;
            seatMessage.textContent = '';
            delete seatMessage.dataset.tone;
        };

        const promptLogin = () => {
            showSeatMessage('Bạn cần đăng nhập để giữ ghế và thanh toán.', 'error');
            if (loginTrigger) {
                loginTrigger.click();
            }
        };

        const showLimitMessage = (shouldShow) => {
            if (!limitMessage) {
                return;
            }
            limitMessage.hidden = !shouldShow;
            if (shouldShow) {
                limitMessage.textContent = `Bạn chỉ có thể đặt tối đa ${maxSelection} ghế cho mỗi giao dịch.`;
            }
        };

        const clearSelectionMarkers = () => {
            seatButtons.forEach((btn) => btn.classList.remove('is-selected'));
        };

        const resetSelectionState = () => {
            selection.clear();
            clearSelectionMarkers();
            updateSelectionUI();
        };

        const normalizeSeatType = (value) => (value || 'STANDARD').toUpperCase();

        const applySeatTypeClass = (button, seatType) => {
            typeClasses.forEach((cls) => button.classList.remove(cls));
            const normalized = seatType.toLowerCase();
            button.classList.add(`seat--type-${normalized}`);
            button.dataset.seatType = seatType;
        };

        const applySeatStatus = (button, status, selectable, holdOwnerId = null) => {
            const seatId = button.dataset.seatId;
            const normalizedStatus = (status || 'DISABLED').toUpperCase();
            const isUserSelection = seatId && selection.has(seatId);
            const ownsHold = Boolean(
                holdOwnerId &&
                currentUserId != null &&
                Number(holdOwnerId) === Number(currentUserId)
            );
            const computedStatus = (isUserSelection || ownsHold) && normalizedStatus === 'HELD'
                ? 'AVAILABLE'
                : normalizedStatus;
            button.dataset.seatStatus = computedStatus;
            statusClasses.forEach((cls) => button.classList.remove(cls));
            button.classList.add(`seat--${computedStatus.toLowerCase()}`);
            const shouldShowPoster = posterUrl && (computedStatus === 'SOLD' || computedStatus === 'HELD');
            if (shouldShowPoster) {
                button.classList.add('seat--with-poster');
                button.style.backgroundImage = `url("${posterUrl}")`;
            } else if (!isUserSelection) {
                button.classList.remove('seat--with-poster');
                button.style.backgroundImage = '';
            }
            if (holdOwnerId) {
                button.dataset.holdUser = holdOwnerId;
            } else {
                delete button.dataset.holdUser;
            }
            const isSelectable = (selectable && normalizedStatus === 'AVAILABLE') || isUserSelection || ownsHold;
            button.toggleAttribute('disabled', !isSelectable);
            if (!isUserSelection && !isSelectable && seatId && selection.has(seatId)) {
                selection.delete(seatId);
            }
        };

        const refreshSeatMap = () => {
            if (!seatMapEndpoint) {
                return;
            }
            fetch(seatMapEndpoint)
                .then((response) => (response.ok ? response.json() : Promise.reject()))
                .then((seatMap) => {
                    seatMap.forEach((seat) => {
                        const button = seatButtons.find((btn) => Number(btn.dataset.seatId) === Number(seat.seatId));
                        if (!button) {
                            return;
                        }
                        const status = (seat.status || 'DISABLED').toUpperCase();
                        if (seat.holdUserId != null) {
                            button.dataset.holdUser = seat.holdUserId;
                        } else {
                            delete button.dataset.holdUser;
                        }
                        applySeatStatus(button, status, seat.selectable, seat.holdUserId ?? null);
                        applySeatTypeClass(button, normalizeSeatType(seat.seatType));
                        if (seat.coupleGroupId) {
                            button.dataset.coupleGroup = seat.coupleGroupId;
                        } else {
                            delete button.dataset.coupleGroup;
                        }
                        if (seat.price) {
                            button.dataset.price = seat.price;
                            if (selection.has(button.dataset.seatId)) {
                                selection.get(button.dataset.seatId).price = Number(seat.price) || 0;
                            }
                        }
                        if (seat.seatLabel) {
                            button.dataset.seatLabel = seat.seatLabel;
                            if (selection.has(button.dataset.seatId)) {
                                selection.get(button.dataset.seatId).label = seat.seatLabel;
                            }
                        }
                    });
                    updateSelectionUI();
                })
                .catch(() => {
                    // ignore refresh errors
                });
        };

        const startSeatMapRefresh = () => {
            if (!seatMapEndpoint || seatMapIntervalId) {
                return;
            }
            seatMapIntervalId = window.setInterval(refreshSeatMap, SEAT_MAP_REFRESH_INTERVAL);
        };

        const stopSeatMapRefresh = () => {
            if (seatMapIntervalId) {
                clearInterval(seatMapIntervalId);
                seatMapIntervalId = null;
            }
        };

        const parseSeatIds = () => {
            if (!seatIdsInput || !seatIdsInput.value) {
                return [];
            }
            return seatIdsInput.value
                    .split(',')
                    .map((id) => parseInt(id, 10))
                    .filter((id) => Number.isFinite(id));
        };

        const updateSelectionUI = () => {
            if (placeholder) {
                placeholder.hidden = selection.size > 0;
            }
            if (limitMessage && (!maxSelection || selection.size < maxSelection)) {
                showLimitMessage(false);
            }
            if (selectedList) {
                selectedList.innerHTML = '';
                selection.forEach((seat) => {
                    const li = document.createElement('li');
                    const label = document.createElement('span');
                    label.textContent = seat.label;
                    const price = document.createElement('span');
                    price.textContent = formatCurrency(seat.price);
                    li.append(label, price);
                    selectedList.appendChild(li);
                });
            }
            const total = Array.from(selection.values()).reduce((sum, seat) => sum + (seat.price || 0), 0);
            if (totalEl) {
                totalEl.textContent = formatCurrency(total);
            }
            if (seatIdsInput) {
                seatIdsInput.value = Array.from(selection.keys()).join(',');
            }
            if (checkoutBtn) {
                checkoutBtn.toggleAttribute('disabled', selection.size === 0);
            }
            scheduleSeatHoldSync();
        };

        const selectSeat = (button, options = {}) => {
            const { skipLimitCheck = false, silent = false } = options;
            const id = button.dataset.seatId;
            if (!id) {
                return;
            }
            if (!selection.has(id) && maxSelection && selection.size >= maxSelection && !skipLimitCheck) {
                showLimitMessage(true);
                return;
            }
            selection.set(id, {
                id,
                label: button.dataset.seatLabel || id,
                price: parseFloat(button.dataset.price || '0') || 0
            });
            button.classList.add('is-selected');
            if (!silent) {
                updateSelectionUI();
            }
        };

        const deselectSeat = (button, options = {}) => {
            const { silent = false } = options;
            const id = button.dataset.seatId;
            if (!id) {
                return;
            }
            selection.delete(id);
            button.classList.remove('is-selected');
            if (!silent) {
                updateSelectionUI();
            }
        };

        const toggleSeat = (button) => {
            if (selection.has(button.dataset.seatId)) {
                deselectSeat(button);
            } else {
                selectSeat(button);
            }
        };

        const toggleCoupleGroup = (groupId) => {
            if (!groupId) {
                return;
            }
            const groupedSeats = seatButtons.filter((btn) => btn.dataset.coupleGroup === groupId);
            if (!groupedSeats.length) {
                return;
            }
            const shouldSelect = groupedSeats.some((btn) => !selection.has(btn.dataset.seatId));
            if (shouldSelect) {
                const needed = groupedSeats.filter((btn) => !selection.has(btn.dataset.seatId)).length;
                const remaining = maxSelection ? Math.max(maxSelection - selection.size, 0) : needed;
                if (maxSelection && needed > remaining) {
                    showLimitMessage(true);
                    return;
                }
                groupedSeats.forEach((btn) => selectSeat(btn, { skipLimitCheck: true, silent: true }));
                updateSelectionUI();
            } else {
                groupedSeats.forEach((btn) => deselectSeat(btn, { silent: true }));
                updateSelectionUI();
            }
        };

        const handleSeatClick = (event) => {
            event.preventDefault();
            const button = event.currentTarget;
            if (button.disabled) {
                return;
            }
            const seatType = normalizeSeatType(button.dataset.seatType);
            if (seatType === 'COUPLE' && button.dataset.coupleGroup) {
                toggleCoupleGroup(button.dataset.coupleGroup);
            } else {
                toggleSeat(button);
            }
        };

        const clearSelectionTimer = () => {
            if (seatTimerIntervalId) {
                clearInterval(seatTimerIntervalId);
                seatTimerIntervalId = null;
            }
            if (seatTimerBox) {
                seatTimerBox.hidden = true;
            }
        };

        const startSelectionTimerFrom = (deadline) => {
            if (!seatTimerDisplay || !seatTimerBox || !deadline) {
                return;
            }
            const expiresAt = new Date(deadline).getTime();
            clearSelectionTimer();
            seatTimerBox.hidden = false;
            const tick = () => {
                const remaining = Math.max(0, Math.floor((expiresAt - Date.now()) / 1000));
                const minutes = String(Math.floor(remaining / 60)).padStart(2, '0');
                const seconds = String(remaining % 60).padStart(2, '0');
                seatTimerDisplay.textContent = `${minutes}:${seconds}`;
                if (remaining <= 0) {
                    clearSelectionTimer();
                    showSeatMessage('Phiên giữ ghế đã hết hạn. Vui lòng chọn lại ghế.', 'error');
                    resetSelectionState();
                    releaseHold(true);
                }
            };
            seatTimerIntervalId = window.setInterval(tick, 1000);
            tick();
        };

        const showSeatHoldConflict = () => {
            showSeatMessage('Ghế bạn chọn đang được giữ bởi khách khác trong 10 phút. Vui lòng chọn ghế khác.', 'error');
        };

        const scheduleSeatHoldSync = () => {
            if (selection.size === 0) {
                releaseHold(true);
                return;
            }
            syncSeatHold();
        };

        const syncSeatHold = async () => {
            const seatIds = parseSeatIds();
            if (!seatIds.length || !showtimeId) {
                releaseHold(true);
                return;
            }
            try {
                const response = await fetch(`/api/showtimes/${showtimeId}/holds`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ showtimeId, seatIds, previousHoldToken: holdToken || null })
                });
                if (response.status === 401) {
                    promptLogin();
                    resetSelectionState();
                    return;
                }
                if (!response.ok) {
                    const body = await response.json().catch(() => ({}));
                    throw new Error(body?.message || 'Không thể giữ ghế.');
                }
                const body = await response.json();
                holdToken = body.holdToken;
                if (holdTokenInput) {
                    holdTokenInput.value = holdToken;
                }
                persistHoldToken(showtimeId, holdToken);
                hideSeatMessage();
                startSelectionTimerFrom(body.expiresAt);
            } catch (error) {
                const message = error?.message || '';
                if (message.toLowerCase().includes('seat already held')) {
                    showSeatHoldConflict();
                } else if (message) {
                    showSeatMessage(message, 'error');
                }
                resetSelectionState();
                refreshSeatMap();
            }
        };

        const releaseHold = async (silent = false, keepalive = false) => {
            if (!holdToken || !showtimeId) {
                holdToken = null;
                if (holdTokenInput) {
                    holdTokenInput.value = '';
                }
                clearSelectionTimer();
                return;
            }
            const tokenToRelease = holdToken;
            holdToken = null;
            if (holdTokenInput) {
                holdTokenInput.value = '';
            }
            clearSelectionTimer();
            const releasePath = `/api/showtimes/${showtimeId}/holds/${tokenToRelease}`;
            const beaconPath = `${releasePath}/release`;
            const requestOptions = {
                method: 'DELETE',
                credentials: 'include',
                keepalive: Boolean(keepalive)
            };
            try {
                if (keepalive && typeof navigator !== 'undefined' && typeof navigator.sendBeacon === 'function') {
                    const blob = new Blob([], { type: 'text/plain' });
                    if (navigator.sendBeacon(beaconPath, blob)) {
                        clearStoredHold();
                        return;
                    }
                }
                await fetch(releasePath, requestOptions);
                clearStoredHold();
            } catch (error) {
                if (!silent) {
                    console.error('Unable to release hold', error);
                }
            }
        };

        const handleCheckoutClick = async () => {
            if (!selection.size || !showtimeId) {
                return;
            }
            if (!currentUser) {
                promptLogin();
                return;
            }
            if (checkoutBtn) {
                checkoutBtn.disabled = true;
                checkoutBtn.textContent = 'Đang chuyển trang...';
            }
            try {
                if (!holdToken) {
                    await syncSeatHold();
                }
                if (!holdToken) {
                    throw new Error('Phiên giữ ghế đã hết hạn. Vui lòng chọn lại ghế.');
                }
                persistHoldToken(showtimeId, holdToken);
                navigatingToCheckout = true;
                const params = new URLSearchParams({ token: holdToken });
                window.location.href = `/checkout/${showtimeId}?${params.toString()}`;
            } catch (error) {
                navigatingToCheckout = false;
                showSeatMessage(error?.message || 'Không thể chuyển tới trang thanh toán.', 'error');
                refreshSeatMap();
            } finally {
                if (checkoutBtn) {
                    checkoutBtn.disabled = selection.size === 0;
                    checkoutBtn.textContent = 'Thanh toán';
                }
            }
        };

        const handleBackClick = async () => {
            resetSelectionState();
            await releaseHold(true);
            const container = seatLayout.closest('.seat-selection-container');
            if (container) {
                container.hidden = true;
            }
            const wrapper = document.getElementById('seatSelectionWrapper');
            if (wrapper) {
                wrapper.hidden = true;
            }
        };

        const releaseOnUnload = () => {
            if (navigatingToCheckout) {
                return;
            }
            releaseHold(true, true);
        };

        const hydrateFromMarkup = () => {
            seatButtons.forEach((button) => {
                if (button.classList.contains('is-selected')) {
                    selectSeat(button, { skipLimitCheck: true, silent: true });
                }
                const initialStatus = button.dataset.seatStatus || null;
                const initialSelectable = !button.hasAttribute('disabled');
                const holdOwner = button.dataset.holdUser || null;
                if (initialStatus) {
                    applySeatStatus(button, initialStatus, initialSelectable, holdOwner);
                }
                button.addEventListener('click', handleSeatClick);
            });
            updateSelectionUI();
            refreshSeatMap();
            startSeatMapRefresh();
        };

        hydrateFromMarkup();

        if (checkoutBtn) {
            checkoutBtn.addEventListener('click', handleCheckoutClick);
        }

        if (backButton) {
            backButton.addEventListener('click', handleBackClick);
        }

        window.addEventListener('pagehide', releaseOnUnload);
        window.addEventListener('beforeunload', releaseOnUnload);
        document.getElementById('logoutForm')?.addEventListener('submit', () => {
            releaseHold(true, true);
        });

        window.releaseCurrentSeatHold = () => releaseHold(true);
    };

    const initializeSeatSelection = () => {
        ready(() => bootstrap(document));
        window.initSeatSelection = (container) => bootstrap(container || document);
    };

    (async () => {
        try {
            await releaseStoredHold();
            await releaseUserHolds();
        } catch (error) {
            // ignore
        }
        initializeSeatSelection();
    })();
})();
