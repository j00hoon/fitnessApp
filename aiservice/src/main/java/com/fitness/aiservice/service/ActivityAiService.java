package com.fitness.aiservice.service;


import com.fitness.aiservice.model.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAiService {
    private final GeminiService geminiService;

    public String generateRecommendation (Activity activity) {
        String prompt = createPromptForActivity(activity);
        String aiResponse = geminiService.getAnswer(prompt);
        log.info("Response from AI : {}", aiResponse);
        return aiResponse;
    }

    private String createPromptForActivity(Activity activity) {
        return String.format("""
        Analyze this fitness activity and provide simple recommendations.

        Return ONLY valid JSON in the following format:
        {
          "analysis": "Brief workout analysis",
          "improvements": [
            "Improvement suggestion 1",
            "Improvement suggestion 2"
          ],
          "suggestions": [
            {
              "workout": "Workout name",
              "description": "Short workout description"
            }
          ],
          "safety": [
            "Safety tip"
          ]
        }

        Activity Details:
        Type: %s
        Duration: %d minutes
        Calories Burned: %d
        Additional Metrics: %s

        Guidelines:
        - Keep recommendations short and practical.
        - Base recommendations only on the provided data.
        - Do not assume missing metrics.
        - Provide 2 improvements, 2 workout suggestions, and 1-2 safety tips.
        - Return only valid JSON without markdown formatting.
        """,
                activity.getType(),
                activity.getDuration(),
                activity.getCaloriesBurned(),
                activity.getAdditionalMetrics()
        );
    }
}
