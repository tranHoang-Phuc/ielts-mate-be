package com.fptu.sep490.commonlibrary.utils;

import com.fptu.sep490.commonlibrary.enumeration.IeltsScale;

public final class IeltsBandConverter {
    private static final double[][] LISTENING = {
            {39,40,9.0},{37,38,8.5},{35,36,8.0},{32,34,7.5},{30,31,7.0},
            {26,29,6.5},{23,25,6.0},{18,22,5.5},{16,17,5.0},{13,15,4.5},
            {11,12,4.0},{ 8,10,3.5},{ 6, 7,3.0},{ 4, 5,2.5},{ 2, 3,2.0},
            { 1, 1,1.5},{ 0, 0,1.0}
    };

    private static final double[][] READING_AC = {
            {39,40,9.0},{37,38,8.5},{35,36,8.0},{33,34,7.5},{30,32,7.0},
            {27,29,6.5},{23,26,6.0},{19,22,5.5},{15,18,5.0},{13,14,4.5},
            {10,12,4.0},{ 8, 9,3.5},{ 6, 7,3.0},{ 4, 5,2.5},{ 3, 3,2.0},
            { 2, 2,1.5},{ 1, 1,1.0},{ 0, 0,0.0}
    };

    private static final double[][] READING_GT = {
            {40,40,9.0},{39,39,8.5},{37,38,8.0},{36,36,7.5},{34,35,7.0},
            {32,33,6.5},{30,31,6.0},{27,29,5.5},{23,26,5.0},{19,22,4.5},
            {15,18,4.0},{12,14,3.5},{ 9,11,3.0},{ 6, 8,2.5},{ 4, 5,2.0},
            { 3, 3,1.5},{ 2, 2,1.0},{ 0, 1,0.0}
    };

    public static double convertScoreToBand(int totalScore, int numberOfExamsInFrame, IeltsScale scale) {
        if (numberOfExamsInFrame <= 0 || totalScore < 0) return 0.0;

        double avg = (double) totalScore / numberOfExamsInFrame;
        int correctPerTest = (int) Math.round(avg);
        if (correctPerTest < 0) correctPerTest = 0;
        if (correctPerTest > 40) correctPerTest = 40;

        double[][] table = switch (scale) {
            case LISTENING -> LISTENING;
            case READING_AC -> READING_AC;
            case READING_GT -> READING_GT;
        };

        for (double[] row : table) {
            int min = (int) row[0];
            int max = (int) row[1];
            double band = row[2];
            if (correctPerTest >= min && correctPerTest <= max) {
                return band;
            }
        }
        return 0.0;
    }

    public static double convertToOverallBand(Double readingBand, Double listeningBand) {
        double r = readingBand != null ? readingBand : 0.0;
        double l = listeningBand != null ? listeningBand : 0.0;

        double overall = (r + l) / 2.0;

        overall = Math.round(overall * 2) / 2.0;

        if (overall < 0.0) overall = 0.0;
        if (overall > 9.0) overall = 9.0;

        return overall;
    }

}
