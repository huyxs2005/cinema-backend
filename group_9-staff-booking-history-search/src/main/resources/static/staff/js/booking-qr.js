document.addEventListener('DOMContentLoaded', () => {
    const container = document.querySelector('.qr-body');
    if (!container) {
        return;
    }
    const countdownEl = document.getElementById('qrCountdown');
    const expiryString = container.dataset.expiry;
    const statusApi = container.dataset.statusApi;
    const bookingCode = container.dataset.bookingCode;
    const expiry = expiryString ? new Date(expiryString) : null;

    const countdownTimer = setInterval(() => updateCountdown(), 1000);
    let pollTimer = setInterval(() => pollStatus(), 5000);

    updateCountdown();

    function updateCountdown() {
        if (!expiry || !countdownEl) {
            clearInterval(countdownTimer);
            return;
        }
        const diff = expiry.getTime() - Date.now();
        if (diff <= 0) {
            countdownEl.textContent = '00:00';
            clearInterval(countdownTimer);
            clearInterval(pollTimer);
            pollTimer = null;
            return;
        }
        const minutes = Math.floor(diff / 60000);
        const seconds = Math.floor((diff % 60000) / 1000);
        countdownEl.textContent = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    }

    function pollStatus() {
        fetch(statusApi)
            .then(resp => resp.json())
            .then(data => {
                if (!data) {
                    return;
                }
                if (data.paymentStatus && data.paymentStatus.toLowerCase() === 'paid') {
                    redirectToDetail();
                }
                if (data.bookingStatus && data.bookingStatus.toLowerCase() === 'cancelled') {
                    redirectToDetail();
                }
            })
            .catch(() => {
            });
    }

    function redirectToDetail() {
        clearInterval(pollTimer);
        window.location.href = `/staff/bookings/${bookingCode}`;
    }
});
