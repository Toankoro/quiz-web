package com.example.quizgame.service;

import com.example.quizgame.reponsitory.QuizRepository;
import com.example.quizgame.reponsitory.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticService {

    private final UserRepository userRepository;
    private final QuizRepository quizRepository;

    public List<Map<String, Object>> getDailyStats(LocalDateTime startDate) {
        LocalDateTime endDate = startDate.plusDays(6).withHour(23).withMinute(59).withSecond(59);

        List<Object[]> userCounts = userRepository.countUsersByDay(startDate, endDate);
        List<Object[]> quizCounts = quizRepository.countQuizzesByDay(startDate, endDate);

        Map<LocalDate, Map<String, Object>> dailyData = new LinkedHashMap<>();

        // Khởi tạo 7 ngày liên tiếp
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.toLocalDate().plusDays(i);
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("users", 0L);
            dayData.put("quizzes", 0L);
            dailyData.put(date, dayData);
        }

        // Gán dữ liệu user
        for (Object[] row : userCounts) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            dailyData.get(date).put("users", (long) row[1]);
        }

        // Gán dữ liệu quiz
        for (Object[] row : quizCounts) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            dailyData.get(date).put("quizzes", (long) row[1]);
        }

        return new ArrayList<>(dailyData.values());
    }
    public List<Map<String, Object>> getTop5UsersByQuizCount() {
        List<Object[]> rows = quizRepository.findTop5UsersByQuizCount();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] row : rows) {
            Map<String, Object> data = new HashMap<>();
            data.put("username", row[0]);
            data.put("totalQuiz", row[1]);
            result.add(data);
        }
        return result;
    }
}
