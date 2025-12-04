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
            return `${Number(value || 0).toFixed(0)} \\u0111`;
        }
    };

    const parseCurrencyToNumber = (value) => {
        if (!value) return 0;
        const normalized = String(value).replace(/[^\d]/g, '');
        return Number(normalized || '0');
    };

    const PHONE_REGEX = /^0\d{9,10}$/;
    const EMAIL_REGEX = /^[\w-.]+@([\w-]+\.)+[\w-]{2,4}$/;
    const DEFAULT_BANK_LINE = 'MB Bank - 0931630902 - DAO NAM HAI';

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
        let currentDiscountPercent = 0;
        let baseTotal = 0;
        let finalTotal = 0;
        let currentPaymentMethod = 'Cash'; // Default to Cash
        
        // Check if showtimeId is provided from URL
        const urlParams = new URLSearchParams(window.location.search);
        const urlShowtimeId = urlParams.get('showtimeId');
        if (urlShowtimeId) {
            currentShowtimeId = Number(urlShowtimeId);
            initialShowtimePending = currentShowtimeId;
        }

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
        const movieSelectionCard = document.getElementById('movieSelectionCard');
        const showtimeListEl = document.getElementById('staffShowtimeList');
        const showtimeEmptyEl = document.getElementById('staffShowtimeEmpty');
        const selectedShowtimeCard = document.getElementById('selectedShowtimeCard');
        const selectedShowtimePoster = document.getElementById('selectedShowtimePoster');
        const selectedShowtimeMovie = document.getElementById('selectedShowtimeMovie');
        const selectedShowtimeTime = document.getElementById('selectedShowtimeTime');
        const selectedShowtimeAuditorium = document.getElementById('selectedShowtimeAuditorium');
        const selectedShowtimeOccupancy = document.getElementById('selectedShowtimeOccupancy');
        const seatStepNumber = document.getElementById('seatStepNumber');
        const customerStepNumber = document.getElementById('customerStepNumber');
        const seatFragmentBox = document.getElementById('staffSeatFragment');
        const seatPlaceholder = document.getElementById('seatSelectionPlaceholder');
        const summarySeatCount = document.getElementById('summarySeatCount');
        const summarySeatList = document.getElementById('summarySeatList');
        const summaryTotal = document.getElementById('summaryTotal');
        const bookingError = document.getElementById('bookingError');
        const form = document.getElementById('staffBookingForm');
        const createBtn = document.getElementById('createBookingBtn');
        const phoneInput = document.getElementById('customerPhone');
        const emailInput = document.getElementById('customerEmail');
        const discountSelect = document.getElementById('discountSelect');
        const paymentRadios = Array.from(document.querySelectorAll('input[name="paymentMethod"]'));
        const bankTransferPanel = document.getElementById('bankTransferPanel');
        const transferGenerateBtn = document.getElementById('transferGenerateBtn');
        const transferBookingLink = document.getElementById('transferBookingLink');
        const transferQrPageLink = document.getElementById('transferQrPageLink');
        const transferPanel = document.getElementById('staffPayosPanel');
        const transferPlaceholder = document.getElementById('transferPlaceholder');
        const transferHelper = document.getElementById('transferHelperText');
        const transferError = document.getElementById('transferError');
        const transferQrImage = document.getElementById('staffPayosQrImage');
        const transferOrderCode = document.getElementById('staffPayosOrderCode');
        const transferBookingCode = document.getElementById('staffPayosBookingCode');
        const transferBankInfo = document.getElementById('staffPayosBankInfo');
        const transferAmount = document.getElementById('staffPayosAmount');
        const transferDescription = document.getElementById('staffPayosDescription');
        const transferCountdown = document.getElementById('staffPayosCountdown');

        let seatIdsInput = null;
        let activeTransferBooking = null;
        let transferCountdownTimer = null;
        let transferStatusIntervalId = null;

        const hideTransferError = () => {
            if (transferError) {
                transferError.hidden = true;
                transferError.textContent = '';
            }
        };

        const showTransferError = (message) => {
            if (transferError) {
                transferError.hidden = false;
                transferError.textContent = message || 'Kh\\u00F4ng th\\u1EC3 t\\u1EA1o VietQR.';
            }
        };

        const setTransferGenerateBusy = (busy) => {
            if (!transferGenerateBtn) {
                return;
            }
            const hasBooking = Boolean(activeTransferBooking?.bookingId);
            transferGenerateBtn.disabled = busy || !hasBooking;
            transferGenerateBtn.textContent = busy ? '\\u0110ang t\\u1EA1o VietQR...' : 'T\\u1EA1o m\\u00E3 VietQR';
        };

        const resetTransferState = (clearBooking = false) => {
            if (clearBooking) {
                activeTransferBooking = null;
            }
            if (transferPanel) {
                transferPanel.hidden = true;
            }
            if (transferPlaceholder) {
                transferPlaceholder.hidden = false;
            }
            if (transferQrImage) {
                transferQrImage.removeAttribute('src');
                transferQrImage.alt = '';
            }
            if (transferOrderCode) {
                transferOrderCode.textContent = '---';
            }
            if (transferBookingCode) {
                transferBookingCode.textContent = '---';
            }
            if (transferBankInfo) {
                transferBankInfo.textContent = DEFAULT_BANK_LINE;
            }
            if (transferAmount) {
                transferAmount.textContent = formatCurrency(0);
            }
            if (transferDescription) {
                transferDescription.textContent = '---';
            }
            if (transferCountdown) {
                transferCountdown.textContent = 'QR c\\u00F2n hi\\u1EC7u l\\u1EF1c: --:--';
            }
            if (transferBookingLink) {
                transferBookingLink.hidden = true;
                transferBookingLink.removeAttribute('href');
            }
            if (transferQrPageLink) {
                transferQrPageLink.hidden = true;
                transferQrPageLink.removeAttribute('href');
            }
            hideTransferError();
            if (transferCountdownTimer) {
                clearInterval(transferCountdownTimer);
                transferCountdownTimer = null;
            }
            if (transferStatusIntervalId) {
                clearInterval(transferStatusIntervalId);
                transferStatusIntervalId = null;
            }
            setTransferGenerateBusy(false);
        };

        const prepareTransferPanelForBooking = (booking) => {
            if (!bankTransferPanel) {
                return;
            }
            activeTransferBooking = booking;
            bankTransferPanel.hidden = false;
            resetTransferState();
            if (booking?.bookingCode) {
                const detailUrl = `/staff/bookings/${booking.bookingCode}`;
                if (transferBookingLink) {
                    transferBookingLink.href = detailUrl;
                    transferBookingLink.hidden = false;
                }
                if (transferQrPageLink) {
                    transferQrPageLink.href = `${detailUrl}/qr`;
                    transferQrPageLink.hidden = false;
                }
            }
            setTransferGenerateBusy(false);
        };

        const startTransferCountdown = (expiresAtValue) => {
            if (!transferCountdown) {
                return;
            }
            if (transferCountdownTimer) {
                clearInterval(transferCountdownTimer);
                transferCountdownTimer = null;
            }
            if (!expiresAtValue) {
                transferCountdown.textContent = 'QR c\\u00F2n hi\\u1EC7u l\\u1EF1c: --:--';
                return;
            }
            const expiresAt = new Date(expiresAtValue).getTime();
            const tick = () => {
                const remaining = Math.max(0, Math.floor((expiresAt - Date.now()) / 1000));
                const minutes = String(Math.floor(remaining / 60)).padStart(2, '0');
                const seconds = String(remaining % 60).padStart(2, '0');
                transferCountdown.textContent = `QR c\\u00F2n hi\\u1EC7u l\\u1EF1c: ${minutes}:${seconds}`;
                if (remaining <= 0 && transferCountdownTimer) {
                    clearInterval(transferCountdownTimer);
                    transferCountdownTimer = null;
                }
            };
            transferCountdownTimer = window.setInterval(tick, 1000);
            tick();
        };

        const handleTransferPaid = () => {
            if (transferStatusIntervalId) {
                clearInterval(transferStatusIntervalId);
                transferStatusIntervalId = null;
            }
            showToast('Thanh to\\u00E1n th\\u00E0nh c\\u00F4ng! \\u0110ang m\\u1EDF \\u0111\\u01A1n.', 'info');
            if (activeTransferBooking?.bookingCode) {
                setTimeout(() => {
                    window.location.href = `/staff/bookings/${activeTransferBooking.bookingCode}`;
                }, 1500);
            }
        };

        const startTransferStatusWatch = () => {
            if (!activeTransferBooking?.bookingId) {
                return;
            }
            if (transferStatusIntervalId) {
                clearInterval(transferStatusIntervalId);
            }
            transferStatusIntervalId = window.setInterval(async () => {
                try {
                    const resp = await fetch(`/api/staff/bookings/${activeTransferBooking.bookingId}/status`, {
                        credentials: 'include'
                    });
                    if (!resp.ok) {
                        return;
                    }
                    const data = await resp.json().catch(() => null);
                    if (data?.status === 'Paid') {
                        handleTransferPaid();
                    }
                } catch (_) {
                    /* noop */
                }
            }, 5000);
        };

        const renderTransferCheckout = (payload) => {
            if (!payload) {
                return;
            }
            if (transferPlaceholder) {
                transferPlaceholder.hidden = true;
            }
            if (transferPanel) {
                transferPanel.hidden = false;
            }
            if (transferQrImage && payload.qrBase64) {
                transferQrImage.src = payload.qrBase64;
                transferQrImage.alt = `MA VietQR ${payload.orderCode || ''}`;
            }
            if (transferOrderCode) {
                transferOrderCode.textContent = payload.orderCode || '---';
            }
            if (transferBookingCode) {
                transferBookingCode.textContent = payload.bookingCode
                    || activeTransferBooking?.bookingCode
                    || '---';
            }
            const bankInfo = payload.bankInfo || {};
            if (transferBankInfo) {
                const bank = bankInfo.bank || 'MB Bank';
                const account = bankInfo.account || '0931630902';
                const owner = bankInfo.name || 'DAO NAM HAI';
                transferBankInfo.textContent = `${bank} - ${account} - ${owner}`;
            }
            if (transferAmount) {
                transferAmount.textContent = formatCurrency(payload.amount);
            }
            if (transferDescription) {
                transferDescription.textContent = payload.transferContent || '---';
            }
            startTransferCountdown(payload.expiresAt);
            startTransferStatusWatch();
        };

        const requestStaffPayosCheckout = async ({ emailOverride = null } = {}) => {
            if (!activeTransferBooking?.bookingId) {
                showTransferError('Ch\\u01B0a c\\u00F3 \\u0111\\u01A1n ch\\u1EDD thanh to\\u00E1n.');
                return;
            }
            const emailSource = (emailOverride ?? emailInput.value ?? '').trim() || activeTransferBooking.email;
            if (!emailSource) {
                showTransferError('Vui l\\u00F2ng nh\\u1EADp email kh\\u00E1ch h\\u00E0ng \\u0111\\u1EC3 t\\u1EA1o VietQR.');
                setFieldError('customerEmail', 'Email b\\u1EAFt bu\\u1ED9c v\\u1EDBi thanh to\\u00E1n chuy\\u1EC3n kho\\u1EA3n.');
                return;
            }
            if (!EMAIL_REGEX.test(emailSource)) {
                showTransferError('Email kh\\u00F4ng h\\u1EE3p l\\u1EC7.');
                setFieldError('customerEmail', 'Email kh\\u00F4ng h\\u1EE3p l\\u1EC7.');
                return;
            }
            hideTransferError();
            setTransferGenerateBusy(true);
            try {
                const resp = await fetch(`/api/staff/payment/bookings/${activeTransferBooking.bookingId}/payos/checkout`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify({ email: emailSource })
                });
                const data = await resp.json().catch(() => null);
                if (!resp.ok || !data?.success) {
                    const message = data?.message || 'Kh\\u00F4ng th\\u1EC3 t\\u1EA1o VietQR.';
                    throw new Error(message);
                }
                activeTransferBooking.email = emailSource;
                renderTransferCheckout(data);
            } catch (error) {
                showTransferError(error?.message || 'Kh\\u00F4ng th\\u1EC3 t\\u1EA1o VietQR.');
            } finally {
                setTransferGenerateBusy(false);
            }
        };

        const resetFormAfterBooking = () => {
            phoneInput.value = '';
            emailInput.value = '';
            if (discountSelect) {
                discountSelect.value = '0';
            }
            currentDiscountPercent = 0;
            recalcTotals();
        };
        const handleTransferBookingCreated = (booking, email) => {
            if (!booking?.bookingId) {
                if (booking?.bookingCode) {
                    window.location.href = `/staff/bookings/${booking.bookingCode}`;
                }
                return;
            }
            const normalizedEmail = (email || '').trim();
            const bookingRef = {
                bookingId: booking.bookingId,
                bookingCode: booking.bookingCode,
                email: normalizedEmail
            };
            prepareTransferPanelForBooking(bookingRef);
            showToast('\\u0110\\u01A1n ch\\u1EDD thanh to\\u00E1n \\u0111\\u00E3 t\\u1EA1o. VietQR s\\u1EBD hi\\u1EC3n th\\u1ECB b\\u00EAn ph\\u1EA3i.', 'info');
            requestStaffPayosCheckout({ emailOverride: normalizedEmail });
            clearSummary();
            resetFormAfterBooking();
            if (currentShowtimeId) {
                loadSeatFragment(currentShowtimeId);
            }
        };

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

        const escapeHtml = (text) => {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        };
        
        const updateSummaryFromSeatSelection = () => {
            const seatIds = parseSeatIds();
            summarySeatCount.textContent = seatIds.length.toString();
            summarySeatList.innerHTML = '';
            
            if (seatIds.length === 0) {
                baseTotal = 0;
                recalcTotals();
                return;
            }
            
            // Get seat data from the seat buttons in the fragment
            const seatFragment = document.getElementById('staffSeatFragment');
            if (!seatFragment) {
                baseTotal = 0;
                recalcTotals();
                return;
            }
            
            let total = 0;
            seatIds.forEach(seatId => {
                const seatBtn = seatFragment.querySelector(`[data-seat-id="${seatId}"]`);
                if (seatBtn) {
                    const label = seatBtn.getAttribute('data-seat-label') || seatBtn.textContent.trim();
                    const price = parseFloat(seatBtn.getAttribute('data-price') || '0');
                    total += price;
                    
                    const li = document.createElement('li');
                    li.className = 'd-flex justify-content-between';
                    li.innerHTML = `
                        <span>${escapeHtml(label)}</span>
                        <span>${formatCurrency(price)}</span>
                    `;
                    summarySeatList.appendChild(li);
                }
            });
            
            baseTotal = total;
            recalcTotals();
        };
        
        const recalcTotals = () => {
            // baseTotal is already calculated in updateSummaryFromSeatSelection
            finalTotal = Math.max(0, Math.round(baseTotal * (1 - currentDiscountPercent)));
            summaryTotalRawUpdate(finalTotal);
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
                backBtn.remove();
            }
            seatIdsInput = container.querySelector('#seatIdsInput');
            
            // Hook into seat selection changes - watch for seatIdsInput changes
            hookHiddenInput(seatIdsInput, () => {
                setTimeout(updateSummaryFromSeatSelection, 100);
            });
            
            // Also listen to seat button clicks
            const seatButtons = container.querySelectorAll('.seat');
            seatButtons.forEach(btn => {
                btn.addEventListener('click', () => {
                    setTimeout(updateSummaryFromSeatSelection, 100);
                });
            });
            
            updateSummaryFromSeatSelection();
        };

        const loadSeatFragment = async (showtimeId) => {
            seatFragmentBox.innerHTML = '';
            setPlaceholder('\\u0110ang t\\u1EA3i s\\u01A1 \\u0111\\u1ED3 gh\\u1EBF...');
            try {
                const url = seatFragmentTemplate.replace(':id', showtimeId);
                const response = await fetch(url, { credentials: 'include' });
                if (!response.ok) throw new Error('Kh\\u00F4ng th\\u1EC3 t\\u1EA3i s\\u01A1 \\u0111\\u1ED3 gh\\u1EBF.');
                const html = await response.text();
                seatFragmentBox.innerHTML = html;
                seatPlaceholder.hidden = true;
                const container = seatFragmentBox.querySelector('.seat-layout')?.closest('.seat-selection-container') || seatFragmentBox;
                customizeSeatLayout(container);
                window.initSeatSelection?.(container);
            } catch (error) {
                setPlaceholder(error.message || 'Kh\\u00F4ng th\\u1EC3 t\\u1EA3i s\\u01A1 \\u0111\\u1ED3 gh\\u1EBF.');
            }
        };

        const loadShowtimeDetails = async (showtimeId, options = {}) => {
            if (!showtimeEndpoint || !showtimeId) return null;
            try {
                const response = await fetch(`${showtimeEndpoint}/${showtimeId}`, { credentials: 'include' });
                if (!response.ok) throw new Error('Kh\\u00F4ng th\\u1EC3 t\\u1EA3i th\\u00F4ng tin su\\u1EA5t chi\\u1EBFu.');
                const data = await response.json();
                
                // Update selected showtime card
                if (selectedShowtimeCard && currentShowtimeId) {
                    selectedShowtimeMovie.textContent = data.movieTitle || '---';
                    selectedShowtimeTime.textContent = data.startTime ? new Date(data.startTime).toLocaleString('vi-VN', {
                        weekday: 'short',
                        day: '2-digit',
                        month: '2-digit',
                        year: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                    }) : '---';
                    selectedShowtimeAuditorium.textContent = data.auditoriumName || '---';
                    selectedShowtimeOccupancy.textContent = `${Number(data.occupancyPercent || 0).toFixed(0)}%`;
                    if (selectedShowtimePoster) {
                        const poster = data.moviePosterUrl || data.posterUrl || '';
                        if (poster) {
                            selectedShowtimePoster.src = poster;
                            selectedShowtimePoster.alt = data.movieTitle || '';
                        } else {
                            selectedShowtimePoster.src = '';
                            selectedShowtimePoster.alt = '';
                        }
                    }
                    selectedShowtimeCard.style.display = 'block';
                }
                
                return data;
            } catch (error) {
                if (selectedShowtimeCard) {
                    selectedShowtimeCard.style.display = 'none';
                }
                if (!options.silent) {
                    showToast(error.message || 'Kh\\u00F4ng th\\u1EC3 t\\u1EA3i th\\u00F4ng tin su\\u1EA5t chi\\u1EBFu.', 'error');
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
            movieEmptyEl.textContent = '\\u0110ang t\\u1EA3i danh s\\u00E1ch phim...';
            try {
                const response = await fetch(moviesEndpoint, { credentials: 'include' });
                if (!response.ok) throw new Error('Kh\\u00F4ng th\\u1EC3 t\\u1EA3i danh s\\u00E1ch phim.');
                const movies = await response.json();
                if (movies.length === 0) {
                    movieListEl.innerHTML = '';
                    movieEmptyEl.hidden = false;
                    movieEmptyEl.textContent = 'Hi\\u1EC7n ch\\u01B0a c\\u00F3 phim \\u0111ang chi\\u1EBFu.';
                    return;
                }
                renderMovieButtons(movies);
                movieEmptyEl.hidden = true;
                if (!currentMovieId && movies.length > 0) {
                    selectMovie(movies[0].id);
                }
            } catch (error) {
                movieListEl.innerHTML = '';
                movieEmptyEl.hidden = false;
                movieEmptyEl.textContent = error.message || 'Kh\\u00F4ng th\\u1EC3 t\\u1EA3i danh s\\u00E1ch phim.';
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
                    ? start.toLocaleTimeString('vi-VN', { hour: '2-digit', minute:'2-digit', hour12:false })
                    : '---';
                const dateStr = start
                    ? start.toLocaleDateString('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit' })
                    : '';
                button.innerHTML = `
                    <span class="showtime-chip__time">${timeStr}</span>
                    <span class="showtime-chip__meta">${dateStr} \- ${showtime.auditoriumName || ''}</span>
                `;
                button.addEventListener('click', () => selectShowtimeChip(showtime.showtimeId));
                showtimeListEl.appendChild(button);
            });
        };

        const loadMovieShowtimes = async (movieId) => {
            if (!showtimeEndpoint || !movieId) return [];
            showtimeListEl.innerHTML = '';
            showtimeEmptyEl.hidden = false;
            showtimeEmptyEl.textContent = '\\u0110ang t\\u1EA3i su\\u1EA5t chi\\u1EBFu...';
            try {
                const params = new URLSearchParams();
                params.set('movieId', movieId);
                params.set('onlyActive', 'true');
                const rangeStart = new Date(Date.now() - 6 * 60 * 60 * 1000);
                const rangeEnd = new Date(rangeStart.getTime() + 30 * 24 * 60 * 60 * 1000);
                params.set('start', rangeStart.toISOString());
                params.set('end', rangeEnd.toISOString());
                const response = await fetch(`${showtimeEndpoint}?${params}`, { credentials: 'include' });
                if (!response.ok) throw new Error('Kh\\u00F4ng th\\u1EC3 t\\u1EA3i su\\u1EA5t chi\\u1EBFu.');
                const data = await response.json();
                if (data.length === 0) {
                    showtimeEmptyEl.hidden = false;
                    showtimeEmptyEl.textContent = 'Ch\\u01B0a c\\u00F3 su\\u1EA5t chi\\u1EBFu ph\\u00F9 h\\u1EE3p.';
                    return [];
                }
                renderShowtimeButtons(data);
                showtimeEmptyEl.hidden = true;
                return data;
            } catch (error) {
                showtimeEmptyEl.hidden = false;
                showtimeEmptyEl.textContent = error.message || 'Kh\\u00F4ng th\\u1EC3 t\\u1EA3i su\\u1EA5t chi\\u1EBFu.';
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
            setPlaceholder('Vui l\\u00F2ng ch\\u1ECDn su\\u1EA5t chi\\u1EBFu \\u0111\\u1EC3 hi\\u1EC3n th\\u1ECB s\\u01A1 \\u0111\\u1ED3 gh\\u1EBF.');
            const showtimes = await loadMovieShowtimes(movieId);
            if (focusShowtimeId && showtimes.some((item) => item.showtimeId === focusShowtimeId)) {
                await selectShowtimeChip(focusShowtimeId);
            } else if (showtimes.length > 0) {
                await selectShowtimeChip(showtimes[0].showtimeId);
            }
        };

        const selectShowtimeChip = async (showtimeId) => {
            if (!showtimeId) return;
            if (currentShowtimeId === showtimeId) return;
            currentShowtimeId = showtimeId;
            highlightShowtime(showtimeId);
            clearSummary();
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
                    currentMovieId = summary.movieId;
                    // Hide movie selection if showtime is preselected
                    if (movieSelectionCard) {
                        movieSelectionCard.style.display = 'none';
                    }
                    if (seatStepNumber) {
                        seatStepNumber.textContent = '1';
                    }
                    if (customerStepNumber) {
                        customerStepNumber.textContent = '2';
                    }
                    await loadSeatFragment(initialShowtimePending);
                }
            } catch (error) {
                currentShowtimeId = null;
                setPlaceholder('Vui l\\u00F2ng ch\\u1ECDn su\\u1EA5t chi\\u1EBFu \\u0111\\u1EC3 hi\\u1EC3n th\\u1ECB s\\u01A1 \\u0111\\u1ED3 gh\\u1EBF.');
            } finally {
                initialShowtimePending = null;
            }
        };

        const toggleBankPanel = () => {
            if (!bankTransferPanel) return;
            const isTransfer = currentPaymentMethod === 'Transfer';
            bankTransferPanel.hidden = !isTransfer;
            if (!isTransfer) {
                resetTransferState(true);
            } else {
                setTransferGenerateBusy(false);
            }
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

        const validateCustomerInfo = () => {
            ['customerPhone', 'customerEmail'].forEach((id) => setFieldError(id, ''));
            const phone = phoneInput.value.trim();
            const email = emailInput.value.trim();
            // Phone and email are optional, but if provided, must be valid
            if (phone && !PHONE_REGEX.test(phone)) {
                setFieldError('customerPhone', 'S\\u1ED1 \\u0111i\\u1EC7n tho\\u1EA1i ph\\u1EA3i b\\u1EAFt \\u0111\\u1EA7u b\\u1EB1ng 0 v\\u00E0 c\\u00F3 10-11 s\\u1ED1.');
                return 'S\\u1ED1 \\u0111i\\u1EC7n tho\\u1EA1i kh\\u00F4ng h\\u1EE3p l\\u1EC7.';
            }
            if (email && !EMAIL_REGEX.test(email)) {
                setFieldError('customerEmail', 'Email kh\\u00F4ng h\\u1EE3p l\\u1EC7.');
                return 'Email kh\\u00F4ng h\\u1EE3p l\\u1EC7.';
            }
            if (currentPaymentMethod === 'Transfer' && !email) {
                setFieldError('customerEmail', 'Email b\\u1EAFt bu\\u1ED9c v\\u1EDBi thanh to\\u00E1n chuy\\u1EC3n kho\\u1EA3n.');
                return 'Vui l\\u00F2ng nh\\u1EADp email \\u0111\\u1EC3 t\\u1EA1o VietQR.';
            }
            return null;
        };

        const validateForm = () => {
            const customerError = validateCustomerInfo();
            if (customerError) {
                return customerError;
            }
            if (!currentShowtimeId) {
                return 'Vui l\\u00F2ng ch\\u1ECDn su\\u1EA5t chi\\u1EBFu tr\\u01B0\\u1EDBc.';
            }
            if (!parseSeatIds().length) {
                return 'Vui l\\u00F2ng ch\\u1ECDn \\u00EDt nh\\u1EA5t m\\u1ED9t gh\\u1EBF.';
            }
            if (finalTotal < 0) {
                return 'T\\u1ED5ng ti\\u1EC1n kh\\u00F4ng h\\u1EE3p l\\u1EC7.';
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
            createBtn.textContent = '\u0110ang t\u1EA1o v\u00E9...';
            try {
                const payload = {
                    showtimeId: currentShowtimeId,
                    seatIds: parseSeatIds(),
                    phone: phoneInput.value.trim(),
                    email: emailInput.value.trim(),
                    discountPercent: currentDiscountPercent,
                    discountCode: discountSelect?.selectedOptions[0]?.textContent?.trim() || '',
                    finalPrice: finalTotal,
                    paymentMethod: currentPaymentMethod || null
                };
                const response = await fetch(bookingEndpoint, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify(payload)
                });
                if (!response.ok) {
                    let message = 'Kh\\u00F4ng th\\u1EC3 t\\u1EA1o v\\u00E9 t\\u1EA1i qu\\u1EA7y.';
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
                bookingError.hidden = true;
                window.releaseCurrentSeatHold?.();
                if (currentPaymentMethod === 'Transfer') {
                    handleTransferBookingCreated(booking, payload.email);
                } else {
                    const bookingCode = booking?.bookingCode || '';
                    if (bookingCode) {
                        window.location.href = `/staff/bookings/${bookingCode}`;
                    } else {
                        window.location.href = '/staff/pending-tickets';
                    }
                    showToast('\\u0110\\u00E3 t\\u1EA1o v\\u00E9 th\\u00E0nh c\\u00F4ng.');
                }
            } catch (error) {
                bookingError.textContent = error.message || 'Kh\\u00F4ng th\\u1EC3 t\\u1EA1o v\\u00E9.';
                bookingError.hidden = false;
            } finally {
                createBtn.disabled = false;
                createBtn.textContent = 'X\u00E1c nh\u1EADn t\u1EA1o v\u00E9';
            }
        };

        const initPayments = () => {
            currentPaymentMethod = paymentRadios.find((radio) => radio.checked)?.value || null;
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
            };
            refreshInputs();
            // Watch for seat selection changes
            const observer = new MutationObserver(() => {
                refreshInputs();
                setTimeout(updateSummaryFromSeatSelection, 100);
            });
            if (seatFragmentBox) {
                observer.observe(seatFragmentBox, { childList: true, subtree: true });
            }
        };

        transferGenerateBtn?.addEventListener('click', () => requestStaffPayosCheckout());
        discountSelect?.addEventListener('change', setDiscountFromSelect);
        createBtn.addEventListener('click', createBooking);
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
















