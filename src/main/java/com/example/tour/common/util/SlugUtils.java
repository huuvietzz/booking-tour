package com.example.tour.common.util;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtils {

    private SlugUtils() {}

    /** "Tour Hạ Long" → "tour-ha-long" */
    public static String toSlug(String input) {
        if (input == null) return "";

        String s = input.trim()
                .replace('đ', 'd')      // NFD không tách được "đ" → đổi tay trước
                .replace('Đ', 'D');

        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");

        return s.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}