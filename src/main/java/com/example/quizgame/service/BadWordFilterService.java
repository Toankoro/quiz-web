package com.example.quizgame.service;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class BadWordFilterService {

    // Danh sách từ cấm (bạn có thể load từ file hoặc DB nếu muốn)
    private static final List<String> BAD_WORDS = Arrays.asList(
            "đm", "vl", "vcl", "cmm", "cc", "cl", "lồn", "địt", "cặc","chó"
    );

    public String filter(String input) {
        if (input == null) return null;

        String filtered = input;
        for (String badWord : BAD_WORDS) {
            // Regex để tìm đúng từ (không phải nằm trong từ khác)
            String pattern = "\\b" + Pattern.quote(badWord) + "\\b";
            filtered = filtered.replaceAll("(?i)" + pattern, "***");  // (?i) = không phân biệt hoa thường
        }

        return filtered;
    }
    public boolean containsBadWords(String input) {
        if (input == null) return false;

        // ✅ Nếu chuỗi chứa '***' thì cũng coi là vi phạm
        if (input.contains("***")) {
            return true;
        }

        for (String badWord : BAD_WORDS) {
            String pattern = "\\b" + Pattern.quote(badWord) + "\\b";
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(input).find()) {
                return true;
            }
        }

        return false;
    }
}