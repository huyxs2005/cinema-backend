(function () {
    const ready = (cb) => {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', cb);
        } else {
            cb();
        }
    };

    const waitForHtml5Qrcode = (max = 50) => new Promise((resolve, reject) => {
        let count = 0;
        const timer = setInterval(() => {
            if (window.Html5Qrcode) {
                clearInterval(timer);
                resolve(true);
            }
            if (count++ > max) {
                clearInterval(timer);
                reject(new Error('Thư viện html5-qrcode chưa sẵn sàng – kiểm tra <script>.'));
            }
        }, 100);
    });

    const formatDateTime = (value) => {
        if (!value) {
            return '';
        }
        try {
            return new Intl.DateTimeFormat('vi-VN', {
                day: '2-digit',
                month: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            }).format(new Date(value));
        } catch (error) {
            return value;
        }
    };

    const formatShowtime = (ticket) => {
        if (ticket.showtimeLabel) {
            return ticket.showtimeLabel;
        }
        if (ticket.showtimeStart && ticket.showtimeEnd) {
            return `${formatDateTime(ticket.showtimeStart)} - ${formatDateTime(ticket.showtimeEnd)}`;
        }
        return ticket.showtimeStart ? formatDateTime(ticket.showtimeStart) : '---';
    };

    const formatSeats = (seats) => {
        if (!Array.isArray(seats) || seats.length === 0) {
            return '---';
        }
        return seats.join(', ');
    };

    const extractErrorMessage = async (response) => {
        try {
            const data = await response.json();
            if (data && data.message) {
                return data.message;
            }
        } catch (error) {
            // ignore
        }
        const text = await response.text();
        if (text) {
            return text;
        }
        return 'Đã có lỗi xảy ra.';
    };

    ready(() => {
        const root = document.getElementById('staffScanRoot');
        if (!root) {
            return;
        }

        const lookupEndpoint = root.dataset.lookupEndpoint || '';
        const checkinEndpoint = root.dataset.checkinEndpoint || '';

        const dom = {
            manualInput: document.getElementById('manualInput'),
            manualBtn: document.getElementById('manualScanBtn'),
            checkinBtn: document.getElementById('checkinBtn'),
            toast: document.getElementById('staffScanToast'),
            scanError: document.getElementById('scanError'),
            bookingCode: document.getElementById('ticketBookingCode'),
            customer: document.getElementById('ticketCustomer'),
            movie: document.getElementById('ticketMovie'),
            showtime: document.getElementById('ticketShowtime'),
            seat: document.getElementById('ticketSeat'),
            paymentBadge: document.getElementById('ticketPaymentStatus'),
            checkinBadge: document.getElementById('ticketCheckinStatus'),
            cameraStatus: document.getElementById('cameraStatusText'),
            cameraSubText: document.getElementById('cameraStatusSubtext'),
            qrStatus: document.getElementById('qr-status'),
            cameraFallback: document.getElementById('cameraFallback'),
            cameraErrorLabel: document.getElementById('cameraErrorLabel'),
            cameraRetryBtn: document.getElementById('cameraRetryBtn'),
            resumeBtn: document.getElementById('resumeScanBtn'),
            scanStatusPill: document.getElementById('scanStatusPill'),
            scanStatusText: document.getElementById('scanStatusText'),
            scanStateMeta: document.getElementById('scanStateMeta'),
            modal: document.getElementById('ticketModal'),
            modalMovie: document.getElementById('modalTicketMovie'),
            modalShowtime: document.getElementById('modalTicketShowtime'),
            modalSeat: document.getElementById('modalTicketSeat'),
            modalStatus: document.getElementById('modalTicketStatus'),
            modalClose: document.getElementById('closeTicketModal'),
            modalCheckin: document.getElementById('modalCheckinBtn')
        };

        const state = {
            scanner: null,
            scanning: false,
            decodeLock: false
        };

        const showToast = (message, tone = 'info') => {
            if (!dom.toast) {
                return;
            }
            dom.toast.textContent = message;
            dom.toast.classList.remove('error', 'warning', 'show');
            if (tone === 'error') {
                dom.toast.classList.add('error');
            } else if (tone === 'warning') {
                dom.toast.classList.add('warning');
            }
            requestAnimationFrame(() => dom.toast.classList.add('show'));
            setTimeout(() => dom.toast.classList.remove('show'), 3200);
        };

        const setCameraStatus = (title, subtitle = '') => {
            if (dom.cameraStatus) {
                dom.cameraStatus.textContent = title;
            }
            if (dom.cameraSubText) {
                dom.cameraSubText.textContent = subtitle;
            }
            if (dom.qrStatus) {
                dom.qrStatus.textContent = subtitle ? `${title} - ${subtitle}` : title;
            }
        };

        const showCameraFallback = (message) => {
            if (dom.cameraFallback) {
                dom.cameraFallback.hidden = false;
            }
            if (dom.cameraErrorLabel) {
                dom.cameraErrorLabel.textContent = message;
            }
        };

        const hideCameraFallback = () => {
            if (dom.cameraFallback) {
                dom.cameraFallback.hidden = true;
            }
            if (dom.cameraErrorLabel) {
                dom.cameraErrorLabel.textContent = '';
            }
        };

        const setScanState = (tone, title, meta = '') => {
            if (!dom.scanStatusPill) {
                return;
            }
            dom.scanStatusPill.classList.remove('neutral', 'success', 'warning', 'danger');
            dom.scanStatusPill.classList.add(tone);
            if (dom.scanStatusText) {
                dom.scanStatusText.textContent = title;
            }
            if (dom.scanStateMeta) {
                dom.scanStateMeta.textContent = meta;
            }
        };

        const resetTicketView = () => {
            dom.bookingCode.textContent = '---';
            dom.customer.textContent = '---';
            dom.movie.textContent = '---';
            dom.showtime.textContent = '---';
            dom.seat.textContent = '---';
            dom.paymentBadge.textContent = 'Chưa xác định';
            dom.paymentBadge.className = 'badge';
            dom.checkinBadge.textContent = 'Chưa check-in';
            dom.checkinBadge.className = 'badge';
            if (dom.checkinBtn) {
                dom.checkinBtn.disabled = true;
                dom.checkinBtn.dataset.bookingCode = '';
            }
            if (dom.modalCheckin) {
                dom.modalCheckin.disabled = true;
            }
            setScanState('neutral', 'Chưa quét', 'Chưa có dữ liệu');
            state.decodeLock = false;
        };

        const showError = (message) => {
            setCameraStatus('Không thể khởi chạy camera', message);
            showCameraFallback(message);
            showToast(message, 'error');
        };

        const applyBadge = (element, text, tone) => {
            element.textContent = text;
            element.className = tone ? `badge ${tone}` : 'badge';
        };

        const updateTicketView = (ticket) => {
            dom.bookingCode.textContent = ticket.bookingCode || '---';
            dom.customer.textContent = ticket.customerName || ticket.customerEmail || '---';
            dom.movie.textContent = ticket.movieTitle || '---';
            dom.showtime.textContent = formatShowtime(ticket);
            dom.seat.textContent = formatSeats(ticket.seats);

            const paid = Boolean(ticket.paid || (ticket.paymentStatus && ticket.paymentStatus.toUpperCase() === 'PAID'));
            applyBadge(dom.paymentBadge, paid ? 'Đã thanh toán' : 'Chưa thanh toán', paid ? 'badge-success' : 'badge-warning');

            let checkLabel = 'Chưa check-in';
            let checkTone = 'badge-warning';
            if (ticket.checkInStatus === 'CHECKED_IN') {
                checkLabel = 'Đã check-in';
                checkTone = 'badge-success';
            } else if (ticket.checkInStatus === 'PARTIAL') {
                checkLabel = `Đã vào ${ticket.checkedInCount}/${ticket.totalSeats}`;
            }
            applyBadge(dom.checkinBadge, checkLabel, checkTone);

            const seatsMeta = ticket.totalSeats
                ? `${ticket.checkedInCount}/${ticket.totalSeats} ghế đã check-in`
                : 'Chưa có dữ liệu ghế';

            if (ticket.showtimeExpired) {
                setScanState('danger', 'Vé hết hiệu lực', `${seatsMeta} - Không thể check-in`);
            } else if (!paid) {
                setScanState('warning', 'Vé chưa thanh toán', `${seatsMeta} - Cần thanh toán trước`);
            } else if (ticket.fullyCheckedIn) {
                setScanState('success', 'Vé hợp lệ', `${seatsMeta} - Đã hoàn tất check-in`);
            } else {
                setScanState('success', 'Vé hợp lệ', `${seatsMeta} - Sẵn sàng check-in`);
            }

            if (dom.checkinBtn) {
                dom.checkinBtn.disabled = !ticket.checkinAllowed;
                dom.checkinBtn.dataset.bookingCode = ticket.bookingCode || '';
            }
            if (dom.modalCheckin) {
                dom.modalCheckin.disabled = !ticket.checkinAllowed;
            }
            dom.modalMovie.textContent = ticket.movieTitle || '---';
            dom.modalShowtime.textContent = formatShowtime(ticket);
            dom.modalSeat.textContent = formatSeats(ticket.seats);
            dom.modalStatus.textContent = paid ? 'HỢP LỆ' : 'CHƯA THANH TOÁN';
            dom.modalStatus.className = `status-pill ${paid ? 'success' : 'warning'}`;
        };

        const handleTicketSuccess = (ticket, options = {}) => {
            const { showPopup = true, toastMessage = 'Đã tải thông tin vé.' } = options;
            if (dom.scanError) {
                dom.scanError.hidden = true;
            }
            state.currentTicket = ticket;
            updateTicketView(ticket);
            if (showPopup && dom.modal) {
                dom.modal.classList.add('show');
            }
            if (toastMessage) {
                showToast(toastMessage);
            }
        };

        const handleTicketFailure = (message) => {
            resetTicketView();
            if (dom.scanError) {
                dom.scanError.textContent = message;
                dom.scanError.hidden = false;
            }
            setScanState('danger', 'Vé không hợp lệ', message);
        };

        const buildLookupUrl = (value) => {
            const url = new URL(lookupEndpoint, window.location.origin);
            url.searchParams.set('value', value.trim());
            return url;
        };

        const fetchTicket = async (value, options = {}) => {
            if (!lookupEndpoint) {
                handleTicketFailure('Không tìm thấy endpoint tra cứu vé.');
                return;
            }
            const trimmed = value.trim();
            if (!trimmed) {
                handleTicketFailure('Giá trị tra cứu không hợp lệ.');
                return;
            }
            try {
                const response = await fetch(buildLookupUrl(trimmed), { credentials: 'include' });
                if (!response.ok) {
                    throw new Error(await extractErrorMessage(response));
                }
                const ticket = await response.json();
                handleTicketSuccess(ticket, options);
            } catch (error) {
                handleTicketFailure(error.message || 'Không thể tải thông tin vé.');
            }
        };

        const parseBookingCode = (raw) => {
            if (!raw) {
                return null;
            }
            let value = raw.trim();
            if (!value) {
                return null;
            }
            if (value.toUpperCase().startsWith('TICKET:')) {
                value = value.slice(7);
            }
            if (value.toUpperCase().startsWith('BOOKING:')) {
                value = value.slice(8);
            }
            return value.trim();
        };

        const stopScanner = async () => {
            if (state.scanner && state.scanning) {
                try {
                    await state.scanner.stop();
                } catch (error) {
                    // ignore
                }
            }
            state.scanning = false;
        };

        const onScanSuccess = async (decodedText) => {
            if (!decodedText || state.decodeLock) {
                return;
            }
            const bookingCode = parseBookingCode(decodedText);
            if (!bookingCode) {
                return;
            }
            if (dom.manualInput) {
                dom.manualInput.value = bookingCode;
            }
            state.decodeLock = true;
            await stopScanner();
            if (dom.resumeBtn) {
                dom.resumeBtn.disabled = false;
                dom.resumeBtn.textContent = 'Quét lại';
            }
            fetchTicket(bookingCode, { showPopup: true });
        };

        const onScanFailure = () => {
            // ignore per-frame failures
        };

        const startCamera = async () => {
            const previewEl = document.getElementById('qr-camera-preview');
            if (!previewEl) {
                showError('Không tìm thấy phần tử id="qr-camera-preview".');
                return;
            }
            setCameraStatus('Đang bật camera', 'Đang yêu cầu quyền truy cập...');
            hideCameraFallback();
            if (dom.resumeBtn) {
                dom.resumeBtn.disabled = true;
                dom.resumeBtn.textContent = 'Đang quét';
            }
            try {
                await waitForHtml5Qrcode();
                if (state.scanner) {
                    await stopScanner();
                }
                state.scanner = new Html5Qrcode('qr-camera-preview');
                state.decodeLock = false;
                await state.scanner.start(
                    { facingMode: 'environment' },
                    { fps: 10, qrbox: 250 },
                    onScanSuccess,
                    onScanFailure
                );
                state.scanning = true;
                setCameraStatus('Camera sẵn sàng', 'Đưa mã QR vào khung để quét.');
            } catch (error) {
                showError(`Không thể khởi chạy camera: ${error.message || error}`);
            }
        };

        const handleManualLookup = async () => {
            if (!dom.manualInput || !dom.manualBtn) {
                return;
            }
            const inputValue = dom.manualInput.value.trim();
            if (!inputValue) {
                if (dom.scanError) {
                    dom.scanError.textContent = 'Vui lòng nhập BookingCode hoặc BookingId.';
                    dom.scanError.hidden = false;
                }
                dom.manualInput.focus();
                return;
            }
            dom.manualBtn.disabled = true;
            dom.manualBtn.textContent = 'Đang tra cứu...';
            try {
                await fetchTicket(inputValue, {
                    showPopup: false,
                    toastMessage: 'Đã tải thông tin vé thủ công.'
                });
            } finally {
                dom.manualBtn.disabled = false;
                dom.manualBtn.textContent = 'Tra cứu';
            }
        };

        const checkInTicket = async () => {
            if (!state.scanner) {
                // still allow check-in if data available
            }
            if (!root || !state.currentTicket || !state.currentTicket.bookingCode) {
                showToast('Vui lòng quét vé trước.', 'warning');
                return;
            }
            if (!state.currentTicket.checkinAllowed) {
                showToast('Vé chưa đủ điều kiện check-in.', 'warning');
                return;
            }
            if (!checkinEndpoint) {
                showToast('Không tìm thấy endpoint check-in.', 'error');
                return;
            }
            const bookingCode = state.currentTicket.bookingCode;
            if (dom.checkinBtn) {
                dom.checkinBtn.disabled = true;
                dom.checkinBtn.textContent = 'Đang check-in...';
            }
            if (dom.modalCheckin) {
                dom.modalCheckin.disabled = true;
            }
            try {
                const url = `${checkinEndpoint.replace(/\/$/, '')}/${encodeURIComponent(bookingCode)}`;
                const response = await fetch(url, { method: 'POST', credentials: 'include' });
                if (!response.ok) {
                    throw new Error(await extractErrorMessage(response));
                }
                const ticket = await response.json();
                handleTicketSuccess(ticket, { showPopup: false, toastMessage: 'Đã check-in thành công.' });
                if (dom.modal) {
                    dom.modal.classList.remove('show');
                }
            } catch (error) {
                showToast(error.message || 'Không thể check-in.', 'error');
            } finally {
                if (dom.checkinBtn) {
                    dom.checkinBtn.textContent = 'Check-in ngay';
                    dom.checkinBtn.disabled = !state.currentTicket || !state.currentTicket.checkinAllowed;
                }
                if (dom.modalCheckin) {
                    dom.modalCheckin.disabled = !state.currentTicket || !state.currentTicket.checkinAllowed;
                }
            }
        };

        resetTicketView();
        startCamera();

        if (dom.manualBtn) {
            dom.manualBtn.addEventListener('click', handleManualLookup);
        }
        if (dom.manualInput) {
            dom.manualInput.addEventListener('keydown', (event) => {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    handleManualLookup();
                }
            });
        }
        if (dom.checkinBtn) {
            dom.checkinBtn.addEventListener('click', checkInTicket);
        }
        if (dom.modalCheckin) {
            dom.modalCheckin.addEventListener('click', checkInTicket);
        }
        if (dom.cameraRetryBtn) {
            dom.cameraRetryBtn.addEventListener('click', startCamera);
        }
        if (dom.resumeBtn) {
            dom.resumeBtn.addEventListener('click', startCamera);
        }
        if (dom.modalClose) {
            dom.modalClose.addEventListener('click', () => dom.modal.classList.remove('show'));
        }
        if (dom.modal) {
            dom.modal.addEventListener('click', (event) => {
                if (event.target === dom.modal) {
                    dom.modal.classList.remove('show');
                }
            });
        }
        document.addEventListener('visibilitychange', async () => {
            if (document.hidden) {
                await stopScanner();
            }
        });
        window.addEventListener('beforeunload', stopScanner);
    });
})();
