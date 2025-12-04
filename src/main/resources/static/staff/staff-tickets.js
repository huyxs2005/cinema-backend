(function () {
    const ready = (cb) => {
        if (document.readyState !== 'loading') {
            cb();
        } else {
            document.addEventListener('DOMContentLoaded', cb);
        }
    };

    const showToast = (message, tone = 'info') => {
        const toast = document.getElementById('staffTicketsToast');
        if (!toast) return;
        toast.textContent = message;
        toast.classList.remove('error', 'warning', 'show');
        if (tone === 'error') {
            toast.classList.add('error');
        } else if (tone === 'warning') {
            toast.classList.add('warning');
        }
        requestAnimationFrame(() => toast.classList.add('show'));
        setTimeout(() => toast.classList.remove('show'), 3200);
    };

    const confirmAction = async (url, bookingCode = null) => {
        try {
            const response = await fetch(url, { 
                method: 'POST', 
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            if (!response.ok) {
                const body = await response.json().catch(() => ({}));
                const errorMessage = body?.message || body?.error || 'Yêu cầu thất bại';
                throw new Error(errorMessage);
            }
            
            const result = await response.json();
            
            // If verify action, show ticket info with bookingCode from result or parameter
            if (url.includes('/verify')) {
                const codeToUse = result?.bookingCode || bookingCode;
                if (codeToUse) {
                    showToast('Đã xác thực thành công!', 'info');
                    // Small delay to ensure toast is visible
                    setTimeout(async () => {
                        await showTicketInfo(codeToUse);
                    }, 500);
                } else {
                    console.warn('No booking code available, reloading page');
                    window.location.reload();
                }
            } else {
                // For cancel action, reload page
                showToast('Đã hủy booking thành công', 'info');
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            }
        } catch (error) {
            console.error('Action error:', error);
            showToast(error.message || 'Không thể thực hiện thao tác', 'error');
            throw error; // Re-throw to allow .finally() in caller
        }
    };
    
    const showTicketInfo = async (bookingCode) => {
        try {
            // Fetch ticket information
            const response = await fetch(`/api/staff/check-ticket/${bookingCode}`, { credentials: 'include' });
            if (!response.ok) {
                throw new Error('Không thể tải thông tin vé');
            }
            const ticketInfo = await response.json();
            
            // Generate QR code URL
            const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(bookingCode)}`;
            
            // Update modal content
            document.getElementById('ticketQrCode').src = qrUrl;
            document.getElementById('ticketBookingCode').textContent = ticketInfo.bookingCode || '---';
            document.getElementById('ticketModalBookingCode').textContent = ticketInfo.bookingCode || '---';
            document.getElementById('ticketModalMovie').textContent = ticketInfo.movieTitle || '---';
            document.getElementById('ticketModalAuditorium').textContent = ticketInfo.auditorium || '---';
            document.getElementById('ticketModalShowtime').textContent = ticketInfo.showtimeLabel || '---';
            document.getElementById('ticketModalSeats').textContent = ticketInfo.seats ? ticketInfo.seats.join(', ') : '---';
            document.getElementById('ticketModalCustomer').textContent = ticketInfo.customerName || '---';
            document.getElementById('ticketModalEmail').textContent = ticketInfo.customerEmail || '---';
            
            // Payment status
            const paymentStatus = ticketInfo.paid ? 'Đã thanh toán' : 'Chưa thanh toán';
            document.getElementById('ticketModalPaymentStatus').innerHTML = ticketInfo.paid 
                ? `<span class="badge bg-success">${paymentStatus}</span>` 
                : `<span class="badge bg-warning">${paymentStatus}</span>`;
            
            // Check-in status
            let checkInStatus = 'Chưa check-in';
            if (ticketInfo.fullyCheckedIn) {
                checkInStatus = 'Đã check-in đầy đủ';
            } else if (ticketInfo.checkedInCount > 0) {
                checkInStatus = `Đã check-in một phần (${ticketInfo.checkedInCount}/${ticketInfo.totalSeats})`;
            }
            document.getElementById('ticketModalCheckInStatus').innerHTML = ticketInfo.fullyCheckedIn
                ? `<span class="badge bg-success">${checkInStatus}</span>`
                : ticketInfo.checkedInCount > 0
                ? `<span class="badge bg-warning">${checkInStatus}</span>`
                : `<span class="badge bg-secondary">${checkInStatus}</span>`;
            
            // Show print button if ticket is paid
            const printBtn = document.getElementById('printTicketBtn');
            if (printBtn) {
                printBtn.style.display = ticketInfo.paid ? 'inline-block' : 'none';
                printBtn.onclick = () => {
                    window.open(`/movies/tickets/${bookingCode}`, '_blank');
                };
            }
            
            // Show modal using Bootstrap
            const modalElement = document.getElementById('ticketInfoModal');
            if (modalElement) {
                const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
                modal.show();
            } else {
                console.error('Modal element not found');
                showToast('Không thể hiển thị modal', 'error');
            }
            
        } catch (error) {
            showToast(error.message || 'Không thể tải thông tin vé', 'error');
            // Still reload page on error
            setTimeout(() => window.location.reload(), 1500);
        }
    };

    const downloadPdf = async (bookingCode) => {
        if (!bookingCode) return;
        try {
            const response = await fetch(`/api/staff/bookings/${bookingCode}`, { credentials: 'include' });
            if (!response.ok) {
                const body = await response.json().catch(() => ({}));
                throw new Error(body?.message || 'Không thể tải vé PDF');
            }
            const booking = await response.json();
            if (!booking?.ticketPdfBase64) {
                throw new Error('Vé chưa sẵn sàng để in');
            }
            const link = document.createElement('a');
            link.href = `data:application/pdf;base64,${booking.ticketPdfBase64}`;
            link.download = `ticket-${booking.bookingCode || 'cinema'}.pdf`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        } catch (error) {
            showToast(error.message || 'Không thể in vé', 'error');
        }
    };

    ready(() => {
        const pendingRoot = document.getElementById('pendingTicketsRoot');
        if (pendingRoot) {
            const baseEndpoint = pendingRoot.dataset.baseEndpoint || '/api/staff/bookings';
            
            // Use event delegation for dynamically loaded content
            pendingRoot.addEventListener('click', (event) => {
                const actionBtn = event.target.closest('[data-action]');
                if (!actionBtn) {
                    return;
                }
                
                event.preventDefault();
                event.stopPropagation();
                
                const bookingId = actionBtn.dataset.bookingId;
                if (!bookingId) {
                    showToast('Không tìm thấy mã booking', 'error');
                    return;
                }
                
                const action = actionBtn.dataset.action;
                if (action === 'verify') {
                    // Disable button during processing
                    const originalDisabled = actionBtn.disabled;
                    const originalText = actionBtn.textContent;
                    actionBtn.disabled = true;
                    actionBtn.textContent = 'Đang xử lý...';
                    
                    // Get booking code from the card
                    const ticketCard = actionBtn.closest('.ticket-card');
                    const bookingCodeEl = ticketCard?.querySelector('.ticket-code');
                    const bookingCode = bookingCodeEl?.textContent?.trim();
                    
                    if (!bookingCode) {
                        showToast('Không tìm thấy mã booking code', 'error');
                        actionBtn.disabled = originalDisabled;
                        actionBtn.textContent = originalText;
                        return;
                    }
                    
                    confirmAction(`${baseEndpoint}/${bookingId}/verify`, bookingCode)
                        .catch(() => {
                            // Error already shown in confirmAction
                        })
                        .finally(() => {
                            // Don't re-enable if verify was successful (modal will be shown)
                            // Only re-enable on error
                            if (!actionBtn.disabled) {
                                // Button was already re-enabled or modal is showing
                            } else {
                                // Check if modal is showing
                                const modal = document.getElementById('ticketInfoModal');
                                if (!modal || !modal.classList.contains('show')) {
                                    actionBtn.disabled = false;
                                    actionBtn.textContent = originalText;
                                }
                            }
                        });
                } else if (action === 'cancel') {
                    if (confirm('Bạn có chắc chắn muốn hủy booking này?')) {
                        const originalDisabled = actionBtn.disabled;
                        const originalText = actionBtn.textContent;
                        actionBtn.disabled = true;
                        actionBtn.textContent = 'Đang hủy...';
                        confirmAction(`${baseEndpoint}/${bookingId}/cancel`)
                            .catch(() => {
                                // Error already shown
                            })
                            .finally(() => {
                                actionBtn.disabled = originalDisabled;
                                actionBtn.textContent = originalText;
                            });
                    }
                }
            });
        }

        const soldRoot = document.getElementById('soldTicketsRoot');
        if (soldRoot) {
            soldRoot.addEventListener('click', (event) => {
                const actionBtn = event.target.closest('[data-action="print"]');
                if (!actionBtn) {
                    return;
                }
                const bookingCode = actionBtn.dataset.bookingCode;
                downloadPdf(bookingCode);
            });
        }
    });
})();

