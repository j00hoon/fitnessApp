package com.fitness.aiservice.service;


import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAiService {
    private final GeminiService geminiService;

    public Recommendation generateRecommendation (Activity activity) {
        try {
            String prompt = createPromptForActivity(activity);
            String aiResponse = geminiService.getAnswer(prompt);

            log.info("Response from AI: {}", aiResponse);
            return processAiResponse(activity, aiResponse);
        } catch (Exception e) {
            log.error("Failed to generate AI recommendation", e);
            return createDefaultRecommendation(activity);
        }
    }

    private Recommendation processAiResponse(Activity activity, String aiResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(aiResponse);

            JsonNode textNode = rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            String jsonContent = textNode.asString()
                    .replaceAll("```json\\n", "")
                    .replaceAll("\\n```", "")
                    .trim();

//            log.info("Parsed response from AI : {}", jsonContent);

            JsonNode analysisJson = mapper.readTree(jsonContent);
            String analysis = analysisJson.path("analysis").asString().trim();

            List<String> improvements = extractImprovements(analysisJson.path("improvements"));
            List<String> suggestions = extractSuggestions(analysisJson.path("suggestions"));
            List<String> safety = extractSafety(analysisJson.path("safety"));

            return Recommendation.builder()
                    .activityId(activity.getId())
                    .userId(activity.getUserId())
                    .activityType(activity.getType())
                    .recommendation(analysis)
                    .improvements(improvements)
                    .suggestions(suggestions)
                    .safety(safety)
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return createDefaultRecommendation(activity);
        }
    }

    private Recommendation createDefaultRecommendation(Activity activity) {
        return Recommendation.builder()
                .activityId(activity.getId())
                .userId(activity.getUserId())
                .activityType(activity.getType())
                .recommendation("Unable to generate detailed analysis")
                .improvements(Collections.singletonList("Continue with your current routine"))
                .suggestions(Collections.singletonList("Consider consulting a fitness professional"))
                .safety(Arrays.asList(
                        "Always warm up before exercise",
                        "Stay hydrated",
                        "Listen to your body"
                ))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private List<String> extractSafety(JsonNode safetyNode) {
        List<String> safety = new ArrayList<>();

        if (safetyNode.isArray()) {
            safetyNode.forEach(safetyTip ->
                    safety.add(safetyTip.asString())
            );
        }

        if (safety.isEmpty()) {
            safety.add("No specific safety tips provided");
        }

        return safety;
    }

    private List<String> extractSuggestions(JsonNode suggestionsNode) {
        List<String> suggestions = new ArrayList<>();

        if (suggestionsNode.isArray()) {
            suggestionsNode.forEach(suggestion -> {
                String workout = suggestion.path("workout").asString();
                String description = suggestion.path("description").asString();

                if (!workout.isBlank() && !description.isBlank()) {
                    suggestions.add(workout + ": " + description);
                }
            });
        }

        if (suggestions.isEmpty()) {
            suggestions.add("No specific workout suggestions provided");
        }

        return suggestions;
    }

    private List<String> extractImprovements(JsonNode improvementsNode) {
        List<String> improvements = new ArrayList<>();

        if (improvementsNode.isArray()) {
            improvementsNode.forEach(improvement ->
                    improvements.add(improvement.asString())
            );
        }

        if (improvements.isEmpty()) {
            improvements.add("No specific improvements provided");
        }

        return improvements;
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
