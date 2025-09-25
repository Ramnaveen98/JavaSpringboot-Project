package com.autobridge_api.slots;

import com.autobridge_api.slots.dto.SlotViewDtos.SlotViewDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/slots")
public class SlotSearchController {

    private final SlotRepository repo;

    public SlotSearchController(SlotRepository repo) {
        this.repo = repo;
    }


    @GetMapping("/search")
    public List<SlotViewDto> search(
            @RequestParam SlotType type,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long agentId,
            @RequestParam(defaultValue = "America/Detroit") String tz
    ) {
        Instant now = Instant.now();
        Instant fromTs = (from != null) ? from : now;
        Instant toTs = (to != null) ? to : now.plus(Duration.ofDays(7));

        List<Slot> slots = (agentId == null)
                ? repo.findByTypeAndStatusAndStartAtBetween(type, SlotStatus.AVAILABLE, fromTs, toTs)
                : repo.findByTypeAndStatusAndStartAtBetweenAndAgentId(type, SlotStatus.AVAILABLE, fromTs, toTs, agentId);

        ZoneId zone = ZoneId.of(tz);
        DateTimeFormatter localFmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zone);
        DateTimeFormatter utcFmt   = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

        return slots.stream().map(s -> new SlotViewDto(
                s.getId(),
                s.getType(),
                utcFmt.format(s.getStartAt()),
                utcFmt.format(s.getEndAt()),
                localFmt.format(s.getStartAt()),
                localFmt.format(s.getEndAt()),
                zone.getId(),
                s.getStatus(),
                s.getCapacity(),
                s.getAgentId(),
                s.getNotes()
        )).toList();
    }
}
