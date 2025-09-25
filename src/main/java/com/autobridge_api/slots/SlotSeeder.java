package com.autobridge_api.slots;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

@Slf4j
@Configuration
public class SlotSeeder {

    @Bean
    CommandLineRunner seedSlots(SlotRepository repo) {
        return args -> {
            Instant now = Instant.now();
            Instant horizon = now.plus(Duration.ofDays(7));

            for (SlotType type : EnumSet.of(SlotType.TEST_DRIVE, SlotType.DELIVERY, SlotType.SERVICE)) {
                List<Slot> future = repo.findByTypeAndStatusAndStartAtBetween(
                        type, SlotStatus.AVAILABLE, now, horizon);

                if (future.isEmpty()) {
                    log.info("Seeding future {} slots…", type);
                    seedForType(repo, type, now);
                } else {
                    log.info("Found {} future {} slots. Skipping seed.", future.size(), type);
                }
            }
        };
    }

    private void seedForType(SlotRepository repo, SlotType type, Instant base) {
        // three 1-hour slots starting ~2h from now
        Instant s1 = base.plus(Duration.ofHours(2));
        Instant s2 = base.plus(Duration.ofHours(5));
        Instant s3 = base.plus(Duration.ofDays(1)).plus(Duration.ofHours(2));

        repo.save(Slot.builder().type(type).startAt(s1).endAt(s1.plus(Duration.ofHours(1)))
                .status(SlotStatus.AVAILABLE).capacity(1).notes(type + " A").build());

        repo.save(Slot.builder().type(type).startAt(s2).endAt(s2.plus(Duration.ofHours(1)))
                .status(SlotStatus.AVAILABLE).capacity(1).notes(type + " B").build());

        repo.save(Slot.builder().type(type).startAt(s3).endAt(s3.plus(Duration.ofHours(1)))
                .status(SlotStatus.AVAILABLE).capacity(1).notes(type + " C").build());

        log.info("Seeded 3 {} slots.", type);
    }
}
