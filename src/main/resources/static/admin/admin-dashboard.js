(function () {
    const API = {
        summary: "/api/admin/dashboard/summary",
        revenue: "/api/admin/dashboard/revenue-range",
        status: "/api/admin/dashboard/order-status-chart",
        table: "/api/admin/dashboard/order-table",
        export: "/api/admin/dashboard/export.xlsx"
    };

    const SELECTORS = {
        rangeButtons: "#dashboardRangeButtons",
        customSection: "#customRangeSection",
        rangeStart: "#dashboardRangeStart",
        rangeEnd: "#dashboardRangeEnd",
        kpiRevenueValue: "#kpiRevenueValue",
        kpiRevenueRangeLabel: "#kpiRevenueRangeLabel",
        totalOrders: "#kpiTotalOrders",
        completedOrders: "#kpiCompletedOrders",
        paymentRate: "#kpiPaymentRate",
        failedOrders: "#kpiFailedOrders",
        seatsSold: "#kpiSeatsSold",
        updatedAt: "#dashboardSummaryUpdatedAt",
        statusLegend: "#orderStatusLegend",
        statusCanvas: "#orderStatusChart",
        revenueCanvas: "#revenueLineChart",
        revenueRangeLabel: "#revenueChartRange",
        filters: {
            search: "#filterSearch",
            status: "#filterPaymentStatus",
            createdStart: "#filterCreatedStart",
            createdEnd: "#filterCreatedEnd"
        },
        tableBody: "#transactionTableBody",
        paginationPrev: "#paginationPrev",
        paginationNext: "#paginationNext",
        paginationInfo: "#paginationInfo",
        recordStats: "#tableRecordStats",
        toast: "#dashboardToast"
    };

    const state = {
        rangeKey: "today",
        range: computeRange("today"),
        filters: {
            search: "",
            status: "",
            createdStart: "",
            createdEnd: ""
        },
        page: 0,
        size: 10,
        totalPages: 0,
        charts: {
            status: null,
            revenue: null
        },
        loading: false
    };

    document.addEventListener("DOMContentLoaded", initDashboard);

    function initDashboard() {
        const rangeContainer = document.querySelector(SELECTORS.rangeButtons);
        if (!rangeContainer) {
            return;
        }
        bindRangeButtons(rangeContainer);
        bindCustomRangeControls();
        bindFilters();
        bindPagination();
        bindExport();
        loadAll();
    }

    function bindRangeButtons(container) {
        container.querySelectorAll(".range-btn").forEach((button) => {
            button.addEventListener("click", () => {
                const range = button.dataset.range;
                if (range === "custom") {
                    toggleCustomSection(true);
                    return;
                }
                toggleCustomSection(false);
                setActiveRangeButton(range);
                state.rangeKey = range;
                state.range = computeRange(range);
                loadAll();
            });
        });
    }

    function bindCustomRangeControls() {
        const applyBtn = document.getElementById("applyCustomRangeBtn");
        const cancelBtn = document.getElementById("cancelCustomRangeBtn");
        applyBtn?.addEventListener("click", () => {
            const startInput = document.querySelector(SELECTORS.rangeStart);
            const endInput = document.querySelector(SELECTORS.rangeEnd);
            if (!startInput.value || !endInput.value) {
                showToast("Vui lòng chọn đủ ngày bắt đầu và kết thúc");
                return;
            }
            if (new Date(startInput.value) > new Date(endInput.value)) {
                showToast("Ngày bắt đầu không được lớn hơn ngày kết thúc");
                return;
            }
            state.range = {
                start: startInput.value,
                end: endInput.value
            };
            state.rangeKey = "custom";
            setActiveRangeButton("custom");
            loadAll();
        });
        cancelBtn?.addEventListener("click", () => {
            const startInput = document.querySelector(SELECTORS.rangeStart);
            const endInput = document.querySelector(SELECTORS.rangeEnd);
            startInput.value = "";
            endInput.value = "";
            toggleCustomSection(false);
            state.rangeKey = "today";
            state.range = computeRange("today");
            setActiveRangeButton("today");
            loadAll();
        });
    }

    function bindFilters() {
        const form = document.getElementById("tableFilterForm");
        form?.addEventListener("submit", (event) => {
            event.preventDefault();
        });
        document.getElementById("applyFiltersBtn")?.addEventListener("click", () => {
            readFiltersFromForm();
            state.page = 0;
            loadTable();
        });
        document.getElementById("resetFiltersBtn")?.addEventListener("click", () => {
            form?.reset();
            state.filters = {
                search: "",
                status: "",
                createdStart: "",
                createdEnd: ""
            };
            state.page = 0;
            loadTable();
        });
    }

    function bindPagination() {
        document.querySelector(SELECTORS.paginationPrev)?.addEventListener("click", () => {
            if (state.page > 0) {
                state.page -= 1;
                loadTable();
            }
        });
        document.querySelector(SELECTORS.paginationNext)?.addEventListener("click", () => {
            const totalPages = state.totalPages || 0;
            if (totalPages === 0 || state.page + 1 >= totalPages) {
                return;
            }
            state.page += 1;
            loadTable();
        });
    }

    function bindExport() {
        document.getElementById("exportButton")?.addEventListener("click", () => {
            const params = buildTableParams();
            const url = `${API.export}?${params.toString()}`;
            window.open(url, "_blank");
        });
    }

    function toggleCustomSection(show) {
        const section = document.querySelector(SELECTORS.customSection);
        if (!section) {
            return;
        }
        section.hidden = !show;
    }

    function setActiveRangeButton(key) {
        document.querySelectorAll(`${SELECTORS.rangeButtons} .range-btn`).forEach((button) => {
            button.classList.toggle("active", button.dataset.range === key);
        });
    }

    function readFiltersFromForm() {
        state.filters.search = document.querySelector(SELECTORS.filters.search)?.value.trim() ?? "";
        state.filters.status = document.querySelector(SELECTORS.filters.status)?.value ?? "";
        state.filters.createdStart = document.querySelector(SELECTORS.filters.createdStart)?.value ?? "";
        state.filters.createdEnd = document.querySelector(SELECTORS.filters.createdEnd)?.value ?? "";
    }

    function loadAll() {
        if (state.loading) {
            return;
        }
        state.loading = true;
        Promise.all([loadSummary(), loadRevenue(), loadOrderStatus(), loadTable()])
            .catch((error) => {
                console.error(error);
                showToast("Không thể tải dashboard. Kiểm tra kết nối.");
            })
            .finally(() => {
                state.loading = false;
            });
    }

    async function loadSummary() {
        const params = buildRangeParams();
        const response = await fetchJson(`${API.summary}?${params.toString()}`);
        if (!response) {
            return;
        }
        updateSummary(response);
    }

    async function loadRevenue() {
        const params = buildRangeParams();
        const response = await fetchJson(`${API.revenue}?${params.toString()}`);
        if (!response) {
            return;
        }
        renderRevenueChart(response);
    }

    async function loadOrderStatus() {
        const params = buildRangeParams();
        const response = await fetchJson(`${API.status}?${params.toString()}`);
        if (!response) {
            return;
        }
        renderOrderStatus(response);
    }

    async function loadTable() {
        const params = buildTableParams();
        params.set("page", state.page);
        params.set("size", state.size);
        const response = await fetchJson(`${API.table}?${params.toString()}`);
        if (!response) {
            return;
        }
        renderTable(response);
    }

    function buildRangeParams() {
        const params = new URLSearchParams();
        if (state.range?.start) {
            params.set("start", state.range.start);
        }
        if (state.range?.end) {
            params.set("end", state.range.end);
        }
        return params;
    }

    function buildTableParams() {
        const params = new URLSearchParams();
        Object.entries(state.filters).forEach(([key, value]) => {
            if (value) {
                const mappedKey = mapFilterKey(key);
                params.set(mappedKey, value);
            }
        });
        return params;
    }

    function mapFilterKey(key) {
        switch (key) {
            case "search":
                return "query";
            case "status":
                return "status";
            case "createdStart":
                return "createdAtStart";
            case "createdEnd":
                return "createdAtEnd";
            default:
                return key;
        }
    }

    function updateSummary(data) {
        const format = (value) => formatCurrency(value ?? 0);
        setText(SELECTORS.kpiRevenueValue, format(data.selectedRevenue));
        const rangeLabel = buildRangeLabel(data.selectedRangeStart, data.selectedRangeEnd);
        setText(SELECTORS.kpiRevenueRangeLabel, rangeLabel);
        setText(SELECTORS.totalOrders, formatNumber(data.totalOrders));
        setText(SELECTORS.completedOrders, formatNumber(data.completedOrders));
        setText(SELECTORS.paymentRate, `${formatDecimal(data.paymentSuccessRate)}% tỷ lệ thanh toán`);
        setText(SELECTORS.failedOrders, formatNumber(data.failedOrders));
        setText(SELECTORS.seatsSold, formatNumber(data.seatsSold));
        if (data.generatedAt) {
            const localeTime = new Date(data.generatedAt).toLocaleString("vi-VN");
            setText(SELECTORS.updatedAt, `Cập nhật: ${localeTime}`);
        }
    }

    function renderOrderStatus(rows) {
        const canvas = document.querySelector(SELECTORS.statusCanvas);
        if (!canvas) {
            return;
        }
        const labels = rows.map((item) => item.status);
        const values = rows.map((item) => item.count);
        const colors = ["#16a34a", "#f97316", "#dc2626"];
        if (state.charts.status) {
            state.charts.status.destroy();
        }
        state.charts.status = new Chart(canvas, {
            type: "doughnut",
            data: {
                labels,
                datasets: [{
                    data: values,
                    backgroundColor: colors.slice(0, values.length),
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                cutout: "70%"
            }
        });
        renderStatusLegend(rows, colors);
    }

    function renderStatusLegend(rows, colors) {
        const legend = document.querySelector(SELECTORS.statusLegend);
        if (!legend) {
            return;
        }
        legend.innerHTML = "";
        rows.forEach((row, index) => {
            const item = document.createElement("div");
            item.className = "legend-item";
            item.innerHTML = `
                <span class="legend-dot" style="background:${colors[index % colors.length]}"></span>
                <span>${row.status} (${row.count})</span>
            `;
            legend.appendChild(item);
        });
    }

    function renderRevenueChart(rows) {
        const canvas = document.querySelector(SELECTORS.revenueCanvas);
        if (!canvas) {
            return;
        }
        const series = shouldGroupRevenueByMonth()
            ? buildMonthlyRevenueSeries(rows)
            : rows.map((item) => ({
                    label: item.date,
                    date: item.date,
                    revenue: Number(item.revenue ?? 0)
                }));
        const labels = series.map((item) => item.label || item.date);
        const values = series.map((item) => Number(item.revenue ?? 0));
        if (state.charts.revenue) {
            state.charts.revenue.destroy();
        }
        state.charts.revenue = new Chart(canvas, {
            type: "line",
            data: {
                labels,
                datasets: [{
                    label: "Doanh thu",
                    data: values,
                    fill: true,
                    borderColor: "#ff512f",
                    backgroundColor: "rgba(255,81,47,0.15)",
                    tension: 0.35
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    x: { ticks: { color: "#b8b9c5" } },
                    y: {
                        ticks: {
                            color: "#b8b9c5",
                            callback: (value) => formatCurrency(value, false)
                        }
                    }
                }
            }
        });
        if (labels.length) {
            setText(SELECTORS.revenueRangeLabel, `${labels[0]} → ${labels[labels.length - 1]}`);
        }
    }

    function renderTable(pageDto) {
        const body = document.querySelector(SELECTORS.tableBody);
        if (!body) {
            return;
        }
        const rows = pageDto.content ?? [];
        body.innerHTML = "";
        if (!rows.length) {
            body.innerHTML = `<tr><td colspan="9" class="text-center text-muted py-5">Không có dữ liệu phù hợp bộ lọc.</td></tr>`;
        } else {
            rows.forEach((row) => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td>${row.bookingCode ?? "-"}</td>
                    <td>${row.accountNumber ?? "-"}</td>
                    <td>${formatDateTime(row.createdAt)}</td>
                    <td>${row.detail ?? ""}</td>
                    <td>${formatCreator(row)}</td>
                    <td>${row.customerEmail ?? "-"}</td>
                    <td>${row.customerPhone ?? "-"}</td>
                    <td>${formatCurrency(row.finalAmount)}</td>
                    <td><span class="badge bg-secondary">${row.paymentStatus ?? "-"}</span></td>
                `;
                body.appendChild(tr);
            });
        }
        updatePaginationInfo(pageDto);
    }

    function updatePaginationInfo(pageDto) {
        const totalElements = pageDto.totalElements ?? 0;
        const totalPages = pageDto.totalPages ?? 0;
        const page = pageDto.page ?? 0;
        const size = pageDto.size ?? state.size;
        const start = totalElements === 0 ? 0 : (page * size) + 1;
        const end = Math.min((page + 1) * size, totalElements);
        setText(SELECTORS.paginationInfo, `${page + 1} / ${Math.max(totalPages, 1)}`);
        setText(SELECTORS.recordStats, `Hiển thị ${start}-${end} / ${totalElements}`);
        state.totalPages = totalPages;
        const prevBtn = document.querySelector(SELECTORS.paginationPrev);
        const nextBtn = document.querySelector(SELECTORS.paginationNext);
        prevBtn?.classList.toggle("disabled", page <= 0);
        nextBtn?.classList.toggle("disabled", page + 1 >= totalPages);
        if (page + 1 >= totalPages && state.page >= totalPages && totalPages > 0) {
            state.page = totalPages - 1;
        }
    }

    function computeRange(rangeKey) {
        const today = new Date();
        const start = new Date(today);
        const end = new Date(today);
        switch (rangeKey) {
            case "yesterday":
                start.setDate(today.getDate() - 1);
                end.setDate(today.getDate() - 1);
                break;
            case "this_week": {
                const day = today.getDay() || 7;
                start.setDate(today.getDate() - day + 1);
                end.setDate(start.getDate() + 6);
                break;
            }
            case "this_month":
                start.setDate(1);
                end.setMonth(start.getMonth() + 1, 0);
                break;
            case "last_month":
                start.setMonth(start.getMonth() - 1, 1);
                end.setMonth(start.getMonth() + 1, 0);
                break;
            case "this_year":
                start.setMonth(0, 1);
                end.setMonth(11, 31);
                break;
            case "last_year":
                start.setFullYear(start.getFullYear() - 1, 0, 1);
                end.setFullYear(start.getFullYear(), 11, 31);
                break;
            default:
                break;
        }
        return {
            start: formatDate(start),
            end: formatDate(end)
        };
    }

    async function fetchJson(url) {
        const response = await fetch(url, {
            headers: { "Accept": "application/json" }
        });
        if (!response.ok) {
            throw new Error(`Request failed ${response.status}`);
        }
        return response.json();
    }

    function formatCurrency(value, withSymbol = true) {
        const amount = Number(value ?? 0);
        const formatted = amount.toLocaleString("vi-VN");
        return withSymbol ? `${formatted} ₫` : formatted;
    }

    function formatDecimal(value) {
        if (value === null || value === undefined) {
            return "0.00";
        }
        return Number(value).toFixed(2);
    }

    function formatNumber(value) {
        return Number(value ?? 0).toLocaleString("vi-VN");
    }

    function formatDate(date) {
        if (!(date instanceof Date)) {
            return date ?? "";
        }
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        return `${year}-${month}-${day}`;
    }

    function formatDateTime(value) {
        if (!value) {
            return "-";
        }
        return new Date(value).toLocaleString("vi-VN");
    }

    function shouldGroupRevenueByMonth() {
        return state.rangeKey === "this_year" || state.rangeKey === "last_year";
    }

    function buildMonthlyRevenueSeries(rows) {
        const year = state.range?.start ? new Date(state.range.start).getFullYear() : null;
        const months = Array.from({ length: 12 }, (_, index) => ({
            label: `Tháng ${index + 1}`,
            date: year ? `${year}-${String(index + 1).padStart(2, "0")}` : `Tháng ${index + 1}`,
            revenue: 0
        }));
        rows.forEach((item) => {
            if (!item?.date) {
                return;
            }
            const date = new Date(item.date);
            if (Number.isNaN(date.getTime())) {
                return;
            }
            const monthIndex = date.getMonth();
            const revenue = Number(item.revenue ?? 0);
            months[monthIndex].revenue += revenue;
        });
        return months;
    }

    function formatCreator(row) {
        const userId = row.userId != null ? `User #${row.userId}` : null;
        const staffId = row.createdByStaffId != null ? `Staff #${row.createdByStaffId}` : null;
        if (userId && staffId) {
            return `${userId} / ${staffId}`;
        }
        return userId || staffId || "-";
    }

    function setText(selector, text) {
        const el = document.querySelector(selector);
        if (el) {
            el.textContent = text ?? "";
        }
    }

    function buildRangeLabel(start, end) {
        if (!start || !end) {
            return "Không xác định";
        }
        const from = new Date(start).toLocaleDateString("vi-VN");
        const to = new Date(end).toLocaleDateString("vi-VN");
        return `${from} → ${to}`;
    }

    function showToast(message) {
        const toast = document.querySelector(SELECTORS.toast);
        if (!toast) {
            alert(message);
            return;
        }
        toast.textContent = message;
        toast.hidden = false;
        setTimeout(() => {
            toast.hidden = true;
        }, 4000);
    }
})();
