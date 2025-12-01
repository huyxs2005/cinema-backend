package com.cinema.hub.backend.util;

public final class SeatLayoutCalculator {

    private static final double STANDARD_RATIO = 0.2;
    private static final double VIP_RATIO = 0.7;
    private static final double COUPLE_RATIO = 0.1;

    private SeatLayoutCalculator() {
    }

    public static SeatRowDistribution calculateDistribution(int totalRows) {
        if (totalRows <= 0) {
            return new SeatRowDistribution(0, 0, 0);
        }
        if (totalRows < 3) {
            int standard = totalRows >= 1 ? 1 : 0;
            int vip = totalRows >= 2 ? 1 : 0;
            int couple = totalRows >= 3 ? 1 : 0;
            return new SeatRowDistribution(standard, vip, couple);
        }
        double[] ratios = {STANDARD_RATIO, VIP_RATIO, COUPLE_RATIO};
        int[] counts = new int[ratios.length];
        double[] remainders = new double[ratios.length];
        int assigned = 0;
        for (int i = 0; i < ratios.length; i++) {
            double quota = totalRows * ratios[i];
            counts[i] = (int) Math.floor(quota);
            remainders[i] = quota - counts[i];
            assigned += counts[i];
        }
        int remaining = totalRows - assigned;
        while (remaining > 0) {
            int bestIndex = 0;
            for (int i = 1; i < remainders.length; i++) {
                if (remainders[i] > remainders[bestIndex]) {
                    bestIndex = i;
                }
            }
            counts[bestIndex]++;
            remainders[bestIndex] = 0;
            remaining--;
        }
        ensureMinimumPerType(counts, totalRows);
        return new SeatRowDistribution(counts[0], counts[1], counts[2]);
    }

    public static SeatRowDistribution fromUserInput(int totalRows,
                                                    Integer preferredStandardRows,
                                                    Integer preferredCoupleRows) {
        if (totalRows <= 0) {
            return new SeatRowDistribution(0, 0, 0);
        }
        int standard = sanitize(totalRows, preferredStandardRows);
        int couple = sanitize(totalRows - standard, preferredCoupleRows);
        int vip = Math.max(0, totalRows - standard - couple);
        return new SeatRowDistribution(standard, vip, couple);
    }

    private static void ensureMinimumPerType(int[] counts, int totalRows) {
        if (totalRows < 3) {
            return;
        }
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] == 0) {
                int donor = findDonor(counts, i);
                if (donor != -1 && counts[donor] > 1) {
                    counts[donor]--;
                    counts[i]++;
                }
            }
        }
    }

    private static int findDonor(int[] counts, int exclude) {
        int donor = -1;
        int max = 0;
        for (int i = 0; i < counts.length; i++) {
            if (i == exclude) {
                continue;
            }
            if (counts[i] > max) {
                max = counts[i];
                donor = i;
            }
        }
        return donor;
    }

    private static int sanitize(int capacity, Integer requested) {
        int value = requested == null ? 0 : Math.max(0, requested);
        return Math.min(value, Math.max(0, capacity));
    }

    public record SeatRowDistribution(int standardRows, int vipRows, int coupleRows) {
    }
}
