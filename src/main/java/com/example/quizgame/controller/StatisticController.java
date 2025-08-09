package com.example.quizgame.controller;

import com.example.quizgame.service.StatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticController {

    private final StatisticService statisticService;

    @GetMapping("/daily")
    public ResponseEntity<List<Map<String, Object>>> getDailyStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate) {

        return ResponseEntity.ok(statisticService.getDailyStats(startDate));
    }
    @GetMapping("/top-quiz-creators")
    public ResponseEntity<List<Map<String, Object>>> getTopQuizCreators() {
        return ResponseEntity.ok(statisticService.getTop5UsersByQuizCount());
    }
}
