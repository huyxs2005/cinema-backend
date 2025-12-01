(function () {
    const ready = (callback) => {
        if (document.readyState !== 'loading') {
            callback();
        } else {
            document.addEventListener('DOMContentLoaded', callback);
        }
    };

    const formatCurrency = (value) => {
        try {
            return new Intl.NumberFormat('vi-VN', {
                style: 'currency',
                currency: 'VND',
                maximumFractionDigits: 0
            }).format(value || 0);
        } catch (_) {
            const fallback = Number(value || 0).toFixed(0);
            return `${fallback} ₫`;
        }
    };

    const clearStoredHold = () => {
        try {
            window.sessionStorage?.removeItem('cinemaSeatHold');
        } catch (_) {
            /* noop */
        }
    };

    const releaseHold = async (showtimeId, holdToken, { keepalive = false } = {}) => {
        if (!showtimeId || !holdToken) {
            return;
        }
        const releasePath = `/api/showtimes/${showtimeId}/holds/${holdToken}`;
        const beaconPath = `${releasePath}/release`;
        const options = {
            method: 'DELETE',
            credentials: 'include',
            keepalive: Boolean(keepalive)
        };
        try {
            if (keepalive && typeof navigator?.sendBeacon === 'function') {
                const blob = new Blob([], { type: 'text/plain' });
                if (navigator.sendBeacon(beaconPath, blob)) {
                    clearStoredHold();
                    return;
                }
            }
            await fetch(releasePath, options);
            clearStoredHold();
        } catch (_) {
            /* noop */
        }
    };

    ready(() => {
        const root = document.getElementById('checkoutRoot');
        if (!root) {
            return;
        }

        const payosQrContainer = document.getElementById('payosQrContainer');
        const payosQrImage = document.getElementById('payosQrImage');
        const payosOrderCode = document.getElementById('payosOrderCode');
        const payosAmount = document.getElementById('payosAmount');
        const payosBankLine = document.getElementById('payosBankLine');
        const payosDescription = document.getElementById('payosDescription');
        const payosCountdown = document.getElementById('payosCountdown');
        const payosErrorEl = document.getElementById('payosError');
        const regenerateQrButton = document.getElementById('regenerateQrButton');
        const backButton = document.getElementById('checkoutBackButton');

        const DEFAULT_BANK_LINE = 'MB Bank - 0931630902 - DAO NAM HAI';

        const showtimeId = Number(root.dataset.showtimeId || 0);
        const userEmail = (root.dataset.userEmail || '').trim();
        let holdExpiresAt = root.dataset.expiresAt || null;
        const movieId = root.dataset.movieId || '';
        const initialBookingId = root.dataset.bookingId ? Number(root.dataset.bookingId) : null;
        const initialBookingCode = root.dataset.bookingCode || '';
        let holdToken = root.dataset.holdToken || '';
        const seatIds = (() => {
            const parseList = (list) => list
                .map((id) => parseInt(id, 10))
                .filter((id) => Number.isFinite(id));
            try {
                if (root.dataset.seatIdsJson) {
                    const parsed = JSON.parse(root.dataset.seatIdsJson);
                    if (Array.isArray(parsed)) {
                        return parseList(parsed);
                    }
                }
            } catch (_) {
                /* noop */
            }
            const fallback = (root.dataset.seatIds || '')
                .split(',')
                .filter(Boolean);
            return parseList(fallback);
        })();

        let activeBookingId = initialBookingId;
        let activeBookingCode = initialBookingCode;
        let paymentStatusIntervalId = null;
        let holdCountdownIntervalId = null;
        let qrCountdownIntervalId = null;

        const clearPayosError = () => {
            if (!payosErrorEl) {
                return;
            }
            payosErrorEl.hidden = true;
            payosErrorEl.textContent = '';
        };

        const showPayosError = (message) => {
            if (!payosErrorEl) {
                return;
            }
            payosErrorEl.hidden = false;
            payosErrorEl.textContent = message || 'Không thể tạo VietQR.';
        };

        const setRegenerateBusy = (busy) => {
            if (!regenerateQrButton) {
                return;
            }
            regenerateQrButton.disabled = busy;
            regenerateQrButton.textContent = busy ? 'Đang xử lý...' : 'Tạo QR mới';
        };

        const updateHoldCountdown = () => {
            if (!holdExpiresAt) {
                return;
            }
            const expiresAt = new Date(holdExpiresAt).getTime();
            const tick = () => {
                const remaining = Math.floor((expiresAt - Date.now()) / 1000);
                if (remaining <= 0) {
                    if (holdCountdownIntervalId) {
                        clearInterval(holdCountdownIntervalId);
                        holdCountdownIntervalId = null;
                    }
                    holdToken = '';
                    showPayosError('Phiên giữ ghế đã hết hạn. Vui lòng chọn ghế khác.');
                    setRegenerateBusy(true);
                }
            };
            holdCountdownIntervalId = window.setInterval(tick, 1000);
            tick();
        };

        const startQrCountdown = (expiresAtValue) => {
            if (!payosCountdown) {
                return;
            }
            if (qrCountdownIntervalId) {
                clearInterval(qrCountdownIntervalId);
                qrCountdownIntervalId = null;
            }
            if (!expiresAtValue) {
                payosCountdown.textContent = 'QR có hiệu lực: --:--';
                return;
            }
            const expiresAt = new Date(expiresAtValue).getTime();
            const tick = () => {
                const remaining = Math.max(0, Math.floor((expiresAt - Date.now()) / 1000));
                const minutes = String(Math.floor(remaining / 60)).padStart(2, '0');
                const seconds = String(remaining % 60).padStart(2, '0');
                payosCountdown.textContent = `QR có hiệu lực: ${minutes}:${seconds}`;
                if (remaining <= 0) {
                    clearInterval(qrCountdownIntervalId);
                    qrCountdownIntervalId = null;
                    showPayosError('QR đã hết hạn. Bấm "Tạo QR mới" để lấy mã khác.');
                }
            };
            qrCountdownIntervalId = window.setInterval(tick, 1000);
            tick();
        };

        const cancelPendingBooking = async (bookingId = activeBookingId) => {
            if (!bookingId) {
                return;
            }
            try {
                const response = await fetch(`/api/payment/status/${bookingId}`);
                if (!response.ok) {
                    return;
                }
                const data = await response.json().catch(() => null);
                if (data?.status === 'Paid') {
                    return;
                }
                await fetch(`/api/bookings/${bookingId}/cancel`, { method: 'POST' });
            } catch (_) {
                /* noop */
            }
        };

        const resetPayosState = async ({ cancelBooking = true } = {}) => {
            if (cancelBooking) {
                await cancelPendingBooking();
                activeBookingId = null;
                activeBookingCode = null;
                root.dataset.bookingId = '';
                root.dataset.bookingCode = '';
                updateUrlState({ token: holdToken || null, bookingId: null });
            }
            clearPayosError();
            if (paymentStatusIntervalId) {
                clearInterval(paymentStatusIntervalId);
                paymentStatusIntervalId = null;
            }
            if (payosQrContainer) {
                payosQrContainer.hidden = true;
            }
            if (payosQrImage) {
                payosQrImage.removeAttribute('src');
                payosQrImage.removeAttribute('alt');
            }
            if (payosOrderCode) {
                payosOrderCode.textContent = '---';
            }
            if (payosAmount) {
                payosAmount.textContent = formatCurrency(0);
            }
            if (payosBankLine) {
                payosBankLine.textContent = DEFAULT_BANK_LINE;
            }
            if (payosDescription) {
                payosDescription.textContent = '';
            }
            if (payosCountdown) {
                payosCountdown.textContent = 'QR có hiệu lực: --:--';
            }
            if (qrCountdownIntervalId) {
                clearInterval(qrCountdownIntervalId);
                qrCountdownIntervalId = null;
            }
        };

        const startPaymentStatusWatch = () => {
            if (!activeBookingId) {
                return;
            }
            if (paymentStatusIntervalId) {
                clearInterval(paymentStatusIntervalId);
            }
            paymentStatusIntervalId = window.setInterval(async () => {
                try {
                    const statusResponse = await fetch(`/api/payment/status/${activeBookingId}`);
                    if (!statusResponse.ok) {
                        return;
                    }
                    const statusData = await statusResponse.json();
                    if (statusData?.status === 'Paid') {
                        handlePaymentSuccess();
                    }
                } catch (_) {
                    /* noop */
                }
            }, 5000);
        };

        const showPayosResult = (payload) => {
            if (!payload?.success || !payload?.qrBase64) {
                setRegenerateBusy(false);
                showPayosError(payload?.message || 'Không thể tạo VietQR.');
                return;
            }
            activeBookingId = payload.bookingId ?? null;
            activeBookingCode = payload.bookingCode ?? null;
            if (activeBookingId) {
                root.dataset.bookingId = String(activeBookingId);
                root.dataset.bookingCode = activeBookingCode || '';
                holdToken = '';
                root.dataset.holdToken = '';
                updateUrlState({ token: null, bookingId: activeBookingId });
            }
            if (payosQrContainer) {
                payosQrContainer.hidden = false;
            }
            if (payosQrImage) {
                const order = payload.orderCode || '---';
                payosQrImage.src = payload.qrBase64;
                payosQrImage.alt = `Mã VietQR đơn ${order}`;
            }
            if (payosOrderCode) {
                payosOrderCode.textContent = payload.orderCode || '---';
            }
            if (payosAmount) {
                payosAmount.textContent = formatCurrency(payload.amount ?? 0);
            }
            if (payosBankLine) {
                const bankInfo = payload.bankInfo || {};
                const bank = bankInfo.bank || 'MB Bank';
                const account = bankInfo.account || '0931630902';
                const owner = bankInfo.name || 'DAO NAM HAI';
                payosBankLine.textContent = `${bank} - ${account} - ${owner}`;
            }
            if (payosDescription) {
                payosDescription.textContent = payload.transferContent || '';
            }
            startQrCountdown(payload.expiresAt);
            setRegenerateBusy(false);
            startPaymentStatusWatch();
        };

        const handlePaymentSuccess = () => {
            if (paymentStatusIntervalId) {
                clearInterval(paymentStatusIntervalId);
                paymentStatusIntervalId = null;
            }
            const toast = document.createElement('div');
            toast.className = 'payment-toast visible';
            toast.textContent = 'Thanh toán thành công! Đang chuyển đến vé của bạn...';
            document.body.appendChild(toast);
            setTimeout(() => {
                toast.classList.remove('visible');
                setTimeout(() => toast.remove(), 400);
            }, 1800);
            setTimeout(() => {
                if (activeBookingCode) {
                    window.location.href = `/movies/confirmation/${activeBookingCode}`;
                }
            }, 2000);
        };

        const buildCheckoutPayload = () => {
            if (activeBookingId) {
                return { bookingId: activeBookingId, email: userEmail };
            }
            if (!holdToken) {
                return null;
            }
            return { holdToken, email: userEmail };
        };

        const requestCheckout = async ({ retry = true } = {}) => {
            if (!holdToken && !activeBookingId) {
                const refreshed = await ensureActiveHold(true);
                if (!refreshed) {
                    showPayosError('Phiên giữ ghế đã hết hạn. Vui lòng chọn ghế khác.');
                    setRegenerateBusy(true);
                    return;
                }
                updateHoldCountdown();
            }
            if (!userEmail) {
                showPayosError('Không xác định được email. Vui lòng đăng nhập lại.');
                setRegenerateBusy(true);
                return;
            }
            const payload = buildCheckoutPayload();
            if (!payload) {
                showPayosError('Phiên giữ ghế đã hết hạn. Vui lòng chọn ghế khác.');
                setRegenerateBusy(true);
                return;
            }
            clearPayosError();
            setRegenerateBusy(true);
            try {
                const response = await fetch('/api/payment/payos/checkout', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                const data = await response.json().catch(() => null);
                if (!response.ok || !data) {
                    throw new Error(data?.message || 'Không thể tạo VietQR.');
                }
                showPayosResult(data);
            } catch (error) {
                if (retry) {
                    const refreshed = await ensureActiveHold(true);
                    if (refreshed) {
                        updateHoldCountdown();
                        setRegenerateBusy(false);
                        requestCheckout({ retry: false });
                        return;
                    }
                }
                setRegenerateBusy(false);
                showPayosError(error?.message || 'Không thể tạo VietQR.');
            }
        };

        const handleRegenerate = async () => {
            await resetPayosState({ cancelBooking: !activeBookingId });
            if (!activeBookingId) {
                const ok = await ensureActiveHold(true);
                if (!ok) {
                    return;
                }
                updateHoldCountdown();
            }
            requestCheckout();
        };

        const redirectToSeatSelection = () => {
            if (movieId) {
                const params = new URLSearchParams();
                if (showtimeId) {
                    params.set('showtimeId', showtimeId);
                }
                window.location.href = params.size > 0
                    ? `/movies/${movieId}?${params.toString()}`
                    : `/movies/${movieId}`;
            } else {
                window.location.href = '/';
            }
        };

        const handleBack = async () => {
            await resetPayosState();
            await releaseHold(showtimeId, holdToken);
            redirectToSeatSelection();
        };

        const updateUrlState = ({ token = null, bookingId = null } = {}) => {
            try {
                const url = new URL(window.location.href);
                if (token) {
                    url.searchParams.set('token', token);
                } else {
                    url.searchParams.delete('token');
                }
                if (bookingId) {
                    url.searchParams.set('bookingId', bookingId);
                } else {
                    url.searchParams.delete('bookingId');
                }
                window.history.replaceState({}, '', url.toString());
            } catch (_) {
                /* noop */
            }
        };

        const ensureActiveHold = async (force = false) => {
            if (activeBookingId) {
                return true;
            }
            if (!showtimeId || !seatIds.length) {
                return Boolean(holdToken);
            }
            if (!force && holdToken) {
                return true;
            }
            try {
                const response = await fetch(`/api/showtimes/${showtimeId}/holds`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify({
                        showtimeId,
                        seatIds,
                        previousHoldToken: holdToken || null
                    })
                });
                if (response.status === 401) {
                    showPayosError('Phiên giữ ghế đã hết hạn. Vui lòng chọn ghế khác.');
                    setRegenerateBusy(true);
                    return false;
                }
                if (!response.ok) {
                    const body = await response.json().catch(() => ({}));
                    throw new Error(body?.message || 'Không thể giữ ghế.');
                }
                const data = await response.json();
                holdToken = data.holdToken;
                holdExpiresAt = data.expiresAt;
                root.dataset.holdToken = holdToken;
                root.dataset.expiresAt = holdExpiresAt;
                updateUrlState({ token: holdToken, bookingId: null });
                return true;
            } catch (error) {
                showPayosError(error?.message || 'Không thể giữ ghế. Vui lòng chọn lại ghế.');
                setRegenerateBusy(true);
                return false;
            }
        };

        (async () => {
            if (!holdToken && !activeBookingId) {
                const ok = await ensureActiveHold(true);
                if (!ok) {
                    return;
                }
            }
            updateHoldCountdown();
            requestCheckout();
        })();

        if (regenerateQrButton) {
            regenerateQrButton.addEventListener('click', handleRegenerate);
        }
        if (backButton) {
            backButton.addEventListener('click', handleBack);
        }

        document.getElementById('logoutForm')?.addEventListener('submit', () => {
            if (activeBookingId) {
                cancelPendingBooking(activeBookingId);
            } else {
                releaseHold(showtimeId, holdToken, { keepalive: true });
            }
        });
    });
})();
