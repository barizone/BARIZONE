package it.barizone.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Job in background (invio push, pulizia sessioni). Disattivabili con barizone.jobs.enabled=false. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "barizone.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
