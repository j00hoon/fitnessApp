package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityMessageListener {

    private final ActivityAiService activityAiService;
    private final RecommendationRepository recommendationRepository;

    @RabbitListener(queues = "activity.queue")
    public void processActivity(Activity activity) {
        log.info("Received Activity : {}", activity.getId());
//        log.info("Generated Recommendation : {}", activityAiService.generateRecommendation(activity));
        Recommendation recommendation = activityAiService.generateRecommendation(activity);
        log.info("Generated Recommendation : {}", recommendation);
        Recommendation saved = recommendationRepository.save(recommendation);

        log.info(
                "Saved Recommendation: id={}, activityId={}",
                saved.getId(),
                saved.getActivityId());
    }
}
