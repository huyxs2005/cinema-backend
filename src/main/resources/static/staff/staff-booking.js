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
        if (!value) return 0;
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
        const moviesEndpoint = root.dataset.moviesEndpoint;
        const seatFragmentTemplate = root.dataset.seatFragmentTemplate || '/movies/seat-fragment/:id';
        let currentMovieId = null;
        let currentShowtimeId = Number(root.dataset.initialShowtime || '0') || null;
        let initialShowtimePending = currentShowtimeId;
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

        const movieListEl = document.getElementById('staffMovieList');
        const movieEmptyEl = document.getElementById('staffMovieEmpty');
        const showtimeListEl = document.getElementById('staffShowtimeList');
        const showtimeEmptyEl = document.getElementById('staffShowtimeEmpty');
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

        let seatIdsInput = null;
        let holdTokenInput = null;
        let selectedListEl = null;
        let selectedTotalEl = null;

        const setPlaceholder = (message) => {
            if (!seatPlaceholder) return;
            seatPlaceholder.hidden = false;
            seatPlaceholder.textContent = message;
        };

        const clearSummary = () => {
            summarySeatCount.textContent = '0';
            summarySeatList.innerHTML = '';
            summaryTotal.dataset.rawValue = '0';
            summaryTotal.textContent = formatCurrency(0);
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

        const summaryTotalRawUpdate = (value) => {
            summaryTotal.dataset.rawValue = String(value);
            summaryTotal.textContent = formatCurrency(value);
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
            checkoutBtn?.classList.add('d-none');
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
            setPlaceholder('Đang tải sơ đồ ghế...');
            try {
                const url = seatFragmentTemplate.replace(':id', showtimeId);
                const response = await fetch(url, { credentials: 'include' });
                if (!response.ok) throw new Error('Không thể tải sơ đồ ghế.');
                const html = await response.text();
                seatFragmentBox.innerHTML = html;
                seatPlaceholder.hidden = true;
                const container = seatFragmentBox.querySelector('.seat-layout')?.closest('.seat-selection-container') || seatFragmentBox;
                customizeSeatLayout(container);
                window.initSeatSelection?.(container);
            } catch (error) {
                setPlaceholder(error.message || 'Không thể tải sơ đồ ghế.');
            }
        };

        const loadShowtimeDetails = async (showtimeId, options = {}) => {
            if (!showtimeEndpoint || !showtimeId) return null;
            try {
                const response = await fetch(`${showtimeEndpoint}/${showtimeId}`, { credentials: 'include' });
                if (!response.ok) throw new Error('Không thể tải thông tin suất chiếu.');
                const data = await response.json();
                detailMovie.textContent = data.movieTitle || '---';
                detailAuditorium.textContent = data.auditoriumName || '---';
                detailTime.textContent = data.startTime ? new Date(data.startTime).toLocaleString('vi-VN') : '---';
                detailOccupancy.textContent = `${Number(data.occupancyPercent || 0).toFixed(0)}%`;
                detailCard.hidden = false;
                return data;
            } catch (error) {
                detailCard.hidden = true;
                if (!options.silent) {
                    showToast(error.message || 'Không thể tải thông tin suất chiếu.', 'error');
                }
                throw error;
            }
        };

        const highlightMovie = (movieId) => {
            if (!movieListEl) return;
            Array.from(movieListEl.querySelectorAll('.movie-pill')).forEach((btn) => {
                btn.classList.toggle('active', Number(btn.dataset.movieId) === movieId);
            });
        };

        const highlightShowtime = (showtimeId) => {
            if (!showtimeListEl) return;
            Array.from(showtimeListEl.querySelectorAll('.showtime-chip')).forEach((btn) => {
                btn.classList.toggle('active', Number(btn.dataset.showtimeId) === showtimeId);
            });
        };

        const renderMovieButtons = (movies) => {
            if (!movieListEl) return;
            movieListEl.innerHTML = '';
            movies.forEach((movie) => {
                const button = document.createElement('button');
                button.type = 'button';
                button.className = 'movie-pill';
                button.dataset.movieId = String(movie.id);
                button.innerHTML = `
                    ${movie.posterUrl ? `<img src="${movie.posterUrl}" alt="${movie.title}" loading="lazy">` : ''}
                    <div>
                        <span class="movie-pill__title">${movie.title}</span>
                        ${movie.originalTitle ? `<span class="movie-pill__meta">${movie.originalTitle}</span>` : ''}
                    </div>
                `;
                button.addEventListener('click', () => selectMovie(movie.id));
                movieListEl.appendChild(button);
            });
        };

        const fetchMovies = async () => {
            if (!moviesEndpoint || !movieListEl) return;
            movieEmptyEl.hidden = false;
            movieEmptyEl.textContent = 'Đang tải danh sách phim...';
            try {
                const response = await fetch(moviesEndpoint, { credentials: 'include' });
                if (!response.ok) throw new Error('Không thể tải danh sách phim.');
                const movies = await response.json();
                if (movies.length === 0) {
                    movieListEl.innerHTML = '';
                    movieEmptyEl.hidden = false;
                    movieEmptyEl.textContent = 'Hiện chưa có phim đang chiếu.';
                    return;
                }
                renderMovieButtons(movies);
                movieEmptyEl.hidden = true;
            } catch (error) {
                movieListEl.innerHTML = '';
                movieEmptyEl.hidden = false;
                movieEmptyEl.textContent = error.message || 'Không thể tải danh sách phim.';
            }
        };

        const renderShowtimeButtons = (showtimes) => {
            if (!showtimeListEl) return;
            showtimeListEl.innerHTML = '';
            showtimes.forEach((showtime) => {
                const button = document.createElement('button');
                button.type = 'button';
                button.className = 'showtime-chip';
                button.dataset.showtimeId = String(showtime.showtimeId);
                const start = showtime.startTime ? new Date(showtime.startTime) : null;
                const timeStr = start
                    ? start.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
                    : '---';
                const dateStr = start
                    ? start.toLocaleDateString('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit' })
                    : '';
                button.innerHTML = `
                    <span class="showtime-chip__time">${timeStr}</span>
                    <span class="showtime-chip__meta">${dateStr} · ${showtime.auditoriumName || ''}</span>
                `;
                button.addEventListener('click', () => selectShowtimeChip(showtime.showtimeId));
                showtimeListEl.appendChild(button);
            });
        };

        const loadMovieShowtimes = async (movieId) => {
            if (!showtimeEndpoint || !movieId) return [];
            showtimeListEl.innerHTML = '';
            showtimeEmptyEl.hidden = false;
            showtimeEmptyEl.textContent = 'Đang tải suất chiếu...';
            try {
                const params = new URLSearchParams();
                params.set('movieId', movieId);
                params.set('onlyActive', 'true');
                params.set('start', new Date().toISOString());
                const response = await fetch(`${showtimeEndpoint}?${params}`, { credentials: 'include' });
                if (!response.ok) throw new Error('Không thể tải suất chiếu.');
                const data = await response.json();
                if (data.length === 0) {
                    showtimeEmptyEl.hidden = false;
                    showtimeEmptyEl.textContent = 'Chưa có suất chiếu phù hợp.';
                    return [];
                }
                renderShowtimeButtons(data);
                showtimeEmptyEl.hidden = true;
                return data;
            } catch (error) {
                showtimeEmptyEl.hidden = false;
                showtimeEmptyEl.textContent = error.message || 'Không thể tải suất chiếu.';
                return [];
            }
        };

        const selectMovie = async (movieId, focusShowtimeId = null) => {
            if (!movieId) return;
            if (currentMovieId === movieId && !focusShowtimeId) return;
            currentMovieId = movieId;
            if (!focusShowtimeId) {
                currentShowtimeId = null;
            }
            highlightMovie(movieId);
            highlightShowtime(null);
            clearSummary();
            window.releaseCurrentSeatHold?.();
            detailCard.hidden = true;
            setPlaceholder('Chọn suất chiếu để hiển thị sơ đồ ghế.');
            const showtimes = await loadMovieShowtimes(movieId);
            if (focusShowtimeId && showtimes.some((item) => item.showtimeId === focusShowtimeId)) {
                selectShowtimeChip(focusShowtimeId);
            }
        };

        const selectShowtimeChip = async (showtimeId) => {
            if (!showtimeId) return;
            if (currentShowtimeId === showtimeId) return;
            currentShowtimeId = showtimeId;
            highlightShowtime(showtimeId);
            clearSummary();
            window.releaseCurrentSeatHold?.();
            try {
                await loadShowtimeDetails(showtimeId);
            } catch (error) {
                // handled inside
            }
            await loadSeatFragment(showtimeId);
        };

        const preselectInitialShowtime = async () => {
            if (!initialShowtimePending) {
                return;
            }
            try {
                const summary = await loadShowtimeDetails(initialShowtimePending, { silent: true });
                if (summary?.movieId) {
                    await selectMovie(summary.movieId, initialShowtimePending);
                }
            } catch (error) {
                currentShowtimeId = null;
                setPlaceholder('Chọn suất chiếu để hiển thị sơ đồ ghế.');
            } finally {
                initialShowtimePending = null;
            }
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
                setFieldError('customerFullName', 'Họ tên không được chứa ký tự đặc biệt.');
                return 'Họ tên không hợp lệ.';
            }
            if (!phone) {
                setFieldError('customerPhone', 'Vui lòng nhập số điện thoại.');
                return 'Vui lòng nhập số điện thoại.';
            }
            if (!PHONE_REGEX.test(phone)) {
                setFieldError('customerPhone', 'Số điện thoại phải bắt đầu bằng 0 và có 10-11 số.');
                return 'Số điện thoại không hợp lệ.';
            }
            if (email && !EMAIL_REGEX.test(email)) {
                setFieldError('customerEmail', 'Email không hợp lệ.');
                return 'Email không hợp lệ.';
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
                bookingStatusLabel.textContent = `Thanh toán: ${booking.paymentStatus || '---'} · Trạng thái: ${booking.bookingStatus || '---'}`;
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

        const setupSeatFragmentListeners = () => {
            const refreshInputs = () => {
                seatIdsInput = document.getElementById('seatIdsInput');
                holdTokenInput = document.getElementById('holdTokenInput');
                selectedListEl = document.getElementById('selectedSeatsList');
                selectedTotalEl = document.getElementById('selectedTotal');
            };
            refreshInputs();
        };

        discountSelect?.addEventListener('change', setDiscountFromSelect);
        createBtn.addEventListener('click', createBooking);
        printBtn.addEventListener('click', downloadTicketPdf);
        form?.addEventListener('submit', (event) => event.preventDefault());

        fetchMovies().then(() => preselectInitialShowtime());
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
