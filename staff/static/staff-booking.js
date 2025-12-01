(function () {
    const ready = (cb) => {
        if (document.readyState !== 'loading') {
            cb();
        } else {
            document.addEventListener('DOMContentLoaded', cb);
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
            return `${Number(value || 0).toFixed(0)} đ`;
        }
    };

    const parseCurrencyToNumber = (value) => {
        if (!value) {
            return 0;
        }
        const normalized = String(value).replace(/[^\d]/g, '');
        return Number(normalized || '0');
    };

    const FULL_NAME_REGEX = /^[A-Za-zÀ-ỹ\s]+$/;
    const PHONE_REGEX = /^0\d{9,10}$/;
    const EMAIL_REGEX = /^[\w-.]+@([\w-]+\.)+[\w-]{2,4}$/;

    ready(() => {
        const root = document.getElementById('staffBookingRoot');
        if (!root) {
            return;
        }

        const showtimeEndpoint = root.dataset.showtimeEndpoint;
        const bookingEndpoint = root.dataset.bookingEndpoint;
        const optionsEndpoint = root.dataset.showtimeOptions;
        const seatFragmentTemplate = root.dataset.seatFragmentTemplate || '/movies/seat-fragment/:id';
        let currentShowtimeId = Number(root.dataset.initialShowtime || '0') || null;
        let currentBooking = null;
        let currentDiscountPercent = 0;
        let baseTotal = 0;
        let finalTotal = 0;
        let currentPaymentMethod = 'CASH';

        const toastEl = document.getElementById('staffBookingToast');
        const showToast = (message, tone = 'info') => {
            if (!toastEl) return;
            toastEl.textContent = message;
            toastEl.classList.remove('error', 'warning', 'show');
            if (tone === 'error') {
                toastEl.classList.add('error');
            } else if (tone === 'warning') {
                toastEl.classList.add('warning');
            }
            requestAnimationFrame(() => toastEl.classList.add('show'));
            setTimeout(() => toastEl.classList.remove('show'), 3200);
        };

        const showtimeSelect = document.getElementById('bookingShowtimeSelect');
        const detailCard = document.getElementById('bookingShowtimeCard');
        const detailMovie = document.getElementById('detailMovie');
        const detailAuditorium = document.getElementById('detailAuditorium');
        const detailTime = document.getElementById('detailTime');
        const detailOccupancy = document.getElementById('detailOccupancy');
        const seatFragmentBox = document.getElementById('staffSeatFragment');
        const seatPlaceholder = document.getElementById('seatSelectionPlaceholder');

        const summarySeatCount = document.getElementById('summarySeatCount');
        const summarySeatList = document.getElementById('summarySeatList');
        const summaryTotal = document.getElementById('summaryTotal');
        const bookingError = document.getElementById('bookingError');
        const bookingResultBox = document.getElementById('bookingResult');
        const bookingCodeLabel = document.getElementById('bookingCodeLabel');
        const bookingStatusLabel = document.getElementById('bookingStatusLabel');

        const form = document.getElementById('staffBookingForm');
        const createBtn = document.getElementById('createBookingBtn');
        const printBtn = document.getElementById('printTicketBtn');

        const fullNameInput = document.getElementById('customerFullName');
        const phoneInput = document.getElementById('customerPhone');
        const emailInput = document.getElementById('customerEmail');
        const discountSelect = document.getElementById('discountSelect');
        const paymentRadios = Array.from(document.querySelectorAll('input[name="paymentMethod"]'));
        const bankTransferPanel = document.getElementById('bankTransferPanel');

        const summaryTotalRawUpdate = (value) => {
            if (summaryTotal) {
                summaryTotal.dataset.rawValue = String(value);
                summaryTotal.textContent = formatCurrency(value);
            }
        };

        const seatSelectionContainer = document.getElementById('staffSeatFragment');
        let seatIdsInput = null;
        let holdTokenInput = null;
        let selectedListEl = null;
        let selectedTotalEl = null;

        const clearSummary = () => {
            summarySeatCount.textContent = '0';
            summarySeatList.innerHTML = '';
            summaryTotalRawUpdate(0);
            bookingError.hidden = true;
            bookingResultBox.hidden = true;
            printBtn.disabled = true;
            currentBooking = null;
        };

        const parseSeatIds = () => {
            if (!seatIdsInput?.value) return [];
            return seatIdsInput.value
                .split(',')
                .map((id) => parseInt(id, 10))
                .filter((id) => Number.isFinite(id));
        };

        const recalcTotals = () => {
            baseTotal = parseCurrencyToNumber(selectedTotalEl?.dataset.rawValue || selectedTotalEl?.textContent || '0');
            finalTotal = Math.max(0, Math.round(baseTotal * (1 - currentDiscountPercent)));
            summaryTotalRawUpdate(finalTotal);
        };

        const updateSummaryFromSelection = () => {
            const seatIds = parseSeatIds();
            summarySeatCount.textContent = seatIds.length.toString();
            summarySeatList.innerHTML = '';
            if (selectedListEl) {
                Array.from(selectedListEl.querySelectorAll('li')).forEach((item) => {
                    const clone = document.createElement('li');
                    clone.className = 'd-flex justify-content-between';
                    const spans = item.querySelectorAll('span');
                    clone.innerHTML = `
                        <span>${spans[0] ? spans[0].textContent : item.textContent}</span>
                        <span>${spans[1] ? spans[1].textContent : ''}</span>
                    `;
                    summarySeatList.appendChild(clone);
                });
            }
            recalcTotals();
        };

        const hookHiddenInput = (input, handler) => {
            if (!input || typeof handler !== 'function') return;
            const descriptor = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(input), 'value');
            if (!descriptor) return;
            Object.defineProperty(input, 'value', {
                configurable: true,
                enumerable: false,
                get() {
                    return descriptor.get.call(this);
                },
                set(value) {
                    descriptor.set.call(this, value);
                    handler(value);
                }
            });
        };

        const customizeSeatLayout = (container) => {
            if (!container) return;
            const checkoutBtn = container.querySelector('[data-checkout-link]');
            if (checkoutBtn) {
                checkoutBtn.removeAttribute('data-checkout-link');
                checkoutBtn.classList.add('d-none');
            }
            const backBtn = container.querySelector('[data-seat-back]');
            if (backBtn) {
                backBtn.textContent = 'Bỏ chọn';
            }
            selectedListEl = container.querySelector('#selectedSeatsList');
            selectedTotalEl = container.querySelector('#selectedTotal');
            seatIdsInput = container.querySelector('#seatIdsInput');
            holdTokenInput = container.querySelector('#holdTokenInput');
            hookHiddenInput(seatIdsInput, updateSummaryFromSelection);
            hookHiddenInput(holdTokenInput, () => {});
            updateSummaryFromSelection();
        };

        const loadSeatFragment = async (showtimeId) => {
            seatFragmentBox.innerHTML = '';
            seatPlaceholder.hidden = false;
            seatPlaceholder.textContent = 'Đang tải sơ đồ ghế...';
            try {
                const url = seatFragmentTemplate.replace(':id', showtimeId);
                const response = await fetch(url, { credentials: 'include' });
                if (!response.ok) throw new Error('Không tải được sơ đồ ghế.');
                const html = await response.text();
                seatFragmentBox.innerHTML = html;
                seatPlaceholder.hidden = true;
                const container = seatFragmentBox.querySelector('.seat-layout')?.closest('.seat-selection-container') || seatFragmentBox;
                customizeSeatLayout(container);
                window.initSeatSelection?.(container);
            } catch (error) {
                seatPlaceholder.hidden = false;
                seatPlaceholder.textContent = error.message || 'Không tải được sơ đồ ghế.';
            }
        };

        const loadShowtimeDetails = async (showtimeId) => {
            if (!showtimeEndpoint) return;
            try {
                const response = await fetch(`${showtimeEndpoint}/${showtimeId}`, { credentials: 'include' });
                if (!response.ok) throw new Error('Không thể tải thông tin suất chiếu.');
                const data = await response.json();
                detailMovie.textContent = data.movieTitle || '---';
                detailAuditorium.textContent = data.auditoriumName || '---';
                detailTime.textContent = data.startTime ? new Date(data.startTime).toLocaleString('vi-VN') : '---';
                detailOccupancy.textContent = `${Number(data.occupancyPercent || 0).toFixed(0)}%`;
                detailCard.hidden = false;
            } catch (error) {
                detailCard.hidden = true;
                showToast(error.message || 'Không thể tải thông tin suất chiếu.', 'error');
            }
        };

        const loadShowtimeOptions = async () => {
            if (!optionsEndpoint || !showtimeSelect) return;
            try {
                const response = await fetch(`${optionsEndpoint}?days=7`, { credentials: 'include' });
                if (!response.ok) throw new Error('Không thể tải danh sách suất chiếu.');
                const options = await response.json();
                showtimeSelect.innerHTML = '<option value="">-- Chọn suất chiếu --</option>';
                options.forEach((option) => {
                    const opt = document.createElement('option');
                    opt.value = option.showtimeId;
                    const labelDate = option.startTime ? new Date(option.startTime).toLocaleString('vi-VN') : '';
                    opt.textContent = `${option.label} - ${labelDate}`;
                    showtimeSelect.appendChild(opt);
                });
                if (currentShowtimeId) {
                    showtimeSelect.value = currentShowtimeId;
                    showtimeSelect.dispatchEvent(new Event('change'));
                }
            } catch (error) {
                showToast(error.message || 'Không thể tải danh sách suất chiếu.', 'error');
            }
        };

        const handleShowtimeChange = async (event) => {
            const showtimeId = Number(event.target.value || '0') || null;
            currentShowtimeId = showtimeId;
            clearSummary();
            window.releaseCurrentSeatHold?.();
            if (!showtimeId) {
                seatPlaceholder.hidden = false;
                seatPlaceholder.textContent = 'Vui lòng chọn một suất chiếu để hiển thị sơ đồ ghế.';
                detailCard.hidden = true;
                return;
            }
            await loadShowtimeDetails(showtimeId);
            await loadSeatFragment(showtimeId);
        };

        const toggleBankPanel = () => {
            if (!bankTransferPanel) return;
            bankTransferPanel.hidden = currentPaymentMethod !== 'TRANSFER';
        };

        const setDiscountFromSelect = () => {
            if (!discountSelect) {
                currentDiscountPercent = 0;
                return;
            }
            const option = discountSelect.selectedOptions[0];
            currentDiscountPercent = parseFloat(option?.dataset.percent || option?.value || '0') || 0;
            recalcTotals();
        };

        const resetFieldErrors = () => {
            ['customerFullName', 'customerPhone', 'customerEmail'].forEach((id) => setFieldError(id, ''));
        };

        const validateCustomerInfo = () => {
            resetFieldErrors();
            const fullName = fullNameInput.value.trim();
            const phone = phoneInput.value.trim();
            const email = emailInput.value.trim();
            if (!fullName) {
                setFieldError('customerFullName', 'Vui lòng nhập họ tên khách.');
                return 'Vui lòng nhập họ tên khách.';
            }
            if (!FULL_NAME_REGEX.test(fullName)) {
                setFieldError('customerFullName', 'Họ tên không được chứa số.');
                return 'Họ tên không được chứa số.';
            }
            if (!phone) {
                setFieldError('customerPhone', 'Vui lòng nhập số điện thoại.');
                return 'Vui lòng nhập số điện thoại.';
            }
            if (!PHONE_REGEX.test(phone)) {
                setFieldError('customerPhone', 'Số điện thoại phải bắt đầu bằng 0 và có 10–11 số.');
                return 'Số điện thoại phải bắt đầu bằng 0 và có 10–11 số.';
            }
            if (email && !EMAIL_REGEX.test(email)) {
                setFieldError('customerEmail', 'Email không đúng định dạng.');
                return 'Email không đúng định dạng.';
            }
            return null;
        };

        const validateForm = () => {
            const customerError = validateCustomerInfo();
            if (customerError) {
                return customerError;
            }
            if (!currentShowtimeId) {
                return 'Vui lòng chọn suất chiếu trước.';
            }
            if (!parseSeatIds().length) {
                return 'Vui lòng chọn ít nhất một ghế.';
            }
            if (finalTotal <= 0) {
                return 'Tổng tiền phải lớn hơn 0.';
            }
            return null;
        };

        const createBooking = async () => {
            const validationError = validateForm();
            if (validationError) {
                bookingError.textContent = validationError;
                bookingError.hidden = false;
                return;
            }
            bookingError.hidden = true;
            createBtn.disabled = true;
            createBtn.textContent = 'Đang tạo vé...';
            try {
                const payload = {
                    showtimeId: currentShowtimeId,
                    seatIds: parseSeatIds(),
                    fullName: fullNameInput.value.trim(),
                    phone: phoneInput.value.trim(),
                    email: emailInput.value.trim(),
                    discountPercent: currentDiscountPercent,
                    discountCode: discountSelect?.selectedOptions[0]?.textContent?.trim() || '',
                    finalPrice: finalTotal,
                    paymentMethod: currentPaymentMethod
                };
                const response = await fetch(bookingEndpoint, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify(payload)
                });
                if (!response.ok) {
                    let message = 'Không thể tạo vé tại quầy.';
                    try {
                        const errorBody = await response.json();
                        if (errorBody?.message) {
                            message = errorBody.message;
                        }
                    } catch (error) {
                        const fallback = await response.text();
                        if (fallback) {
                            message = fallback;
                        }
                    }
                    throw new Error(message);
                }
                const booking = await response.json();
                currentBooking = booking;
                bookingCodeLabel.textContent = booking.bookingCode || '---';
                bookingStatusLabel.textContent = `Thanh toán: ${booking.paymentStatus || '---'} • Trạng thái: ${booking.bookingStatus || '---'}`;
                bookingResultBox.hidden = false;
                summarySeatList.innerHTML = '';
                if (Array.isArray(booking.seats)) {
                    booking.seats.forEach((seat) => {
                        const li = document.createElement('li');
                        li.className = 'd-flex justify-content-between';
                        li.innerHTML = `
                            <span>${seat.seatLabel}</span>
                            <span>${formatCurrency(seat.finalPrice)}</span>
                        `;
                        summarySeatList.appendChild(li);
                    });
                    summarySeatCount.textContent = booking.seats.length.toString();
                }
                summaryTotalRawUpdate(booking.finalAmount || booking.totalAmount || finalTotal);
                printBtn.disabled = !booking.ticketPdfBase64;
                bookingError.hidden = true;
                window.releaseCurrentSeatHold?.();
                showToast('Đã tạo vé thành công.');
            } catch (error) {
                bookingError.textContent = error.message || 'Không thể tạo vé.';
                bookingError.hidden = false;
            } finally {
                createBtn.disabled = false;
                createBtn.textContent = 'Xác nhận tạo vé';
            }
        };

        const downloadTicketPdf = () => {
            if (!currentBooking?.ticketPdfBase64) {
                showToast('Không có file vé để in.', 'warning');
                return;
            }
            const link = document.createElement('a');
            link.href = `data:application/pdf;base64,${currentBooking.ticketPdfBase64}`;
            link.download = `ticket-${currentBooking.bookingCode || 'cinema'}.pdf`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        };

        const setupSeatFragmentListeners = () => {
            const refreshInputs = () => {
                if (!seatSelectionContainer) return;
                seatIdsInput = seatSelectionContainer.querySelector('#seatIdsInput');
                holdTokenInput = seatSelectionContainer.querySelector('#holdTokenInput');
                selectedListEl = seatSelectionContainer.querySelector('#selectedSeatsList');
                selectedTotalEl = seatSelectionContainer.querySelector('#selectedTotal');
            };
            refreshInputs();
        };

        const initPayments = () => {
            currentPaymentMethod = paymentRadios.find((radio) => radio.checked)?.value || 'CASH';
            toggleBankPanel();
            paymentRadios.forEach((radio) => {
                radio.addEventListener('change', () => {
                    if (!radio.checked) return;
                    currentPaymentMethod = radio.value;
                    toggleBankPanel();
                });
            });
        };

        showtimeSelect.addEventListener('change', handleShowtimeChange);
        createBtn.addEventListener('click', createBooking);
        printBtn.addEventListener('click', downloadTicketPdf);
        discountSelect?.addEventListener('change', setDiscountFromSelect);

        loadShowtimeOptions();
        setupSeatFragmentListeners();
        initPayments();
        setDiscountFromSelect();
        recalcTotals();
    });

    function setFieldError(fieldId, message) {
        const hint = document.querySelector(`[data-error-for="${fieldId}"]`);
        if (hint) {
            hint.textContent = message || '';
        }
    }
})();
