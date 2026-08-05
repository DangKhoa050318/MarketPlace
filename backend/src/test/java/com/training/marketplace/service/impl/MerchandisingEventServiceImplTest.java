package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.RecordMerchandisingEventRequest;
import com.training.marketplace.dto.response.MerchandisingSummaryResponse;
import com.training.marketplace.entity.MerchandisingEvent;
import com.training.marketplace.enums.MerchandisingEventType;
import com.training.marketplace.enums.MerchandisingTargetType;
import com.training.marketplace.repository.MerchandisingEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Unit tests for merchandising event dedup (T-404), CTR math (T-404) and last-click attribution (B-408). */
@ExtendWith(MockitoExtension.class)
class MerchandisingEventServiceImplTest {

    @Mock
    private MerchandisingEventRepository eventRepository;

    @InjectMocks
    private MerchandisingEventServiceImpl service;

    private RecordMerchandisingEventRequest impressionOn(UUID eventId, long targetId) {
        return new RecordMerchandisingEventRequest(
                eventId, MerchandisingEventType.IMPRESSION, MerchandisingTargetType.BANNER, targetId, "sess-1");
    }

    @Test
    void record_isNoOp_whenEventIdAlreadySeen() {
        UUID id = UUID.randomUUID();
        given(eventRepository.existsByEventId(id)).willReturn(true);

        service.record(impressionOn(id, 7L), 3L);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void record_persistsNewEvent_withCallerFields() {
        UUID id = UUID.randomUUID();
        given(eventRepository.existsByEventId(id)).willReturn(false);

        service.record(impressionOn(id, 7L), 3L);

        ArgumentCaptor<MerchandisingEvent> captor = ArgumentCaptor.forClass(MerchandisingEvent.class);
        verify(eventRepository).save(captor.capture());
        MerchandisingEvent saved = captor.getValue();
        assertThat(saved.getEventId()).isEqualTo(id);
        assertThat(saved.getEventType()).isEqualTo(MerchandisingEventType.IMPRESSION);
        assertThat(saved.getTargetType()).isEqualTo(MerchandisingTargetType.BANNER);
        assertThat(saved.getTargetId()).isEqualTo(7L);
        assertThat(saved.getUserId()).isEqualTo(3L);
        assertThat(saved.getSessionId()).isEqualTo("sess-1");
    }

    @Test
    void record_swallowsConcurrentDuplicateInsert() {
        UUID id = UUID.randomUUID();
        given(eventRepository.existsByEventId(id)).willReturn(false);
        given(eventRepository.save(any())).willThrow(new DataIntegrityViolationException("dup event_id"));

        assertThatCode(() -> service.record(impressionOn(id, 7L), null)).doesNotThrowAnyException();
        verify(eventRepository).save(any());
    }

    @Test
    void summary_computesCtrAsClicksOverImpressions() {
        MerchandisingTargetType target = MerchandisingTargetType.CAMPAIGN;
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        given(eventRepository.countInRange(target, 5L, MerchandisingEventType.IMPRESSION, from, to)).willReturn(200L);
        given(eventRepository.countInRange(target, 5L, MerchandisingEventType.CLICK, from, to)).willReturn(50L);
        given(eventRepository.countAttributedOrders(target, 5L, from, to)).willReturn(4L);

        MerchandisingSummaryResponse summary = service.summary(target, 5L, from, to);

        assertThat(summary.impressions()).isEqualTo(200L);
        assertThat(summary.clicks()).isEqualTo(50L);
        assertThat(summary.ctr()).isEqualTo(0.25);
        assertThat(summary.attributedOrders()).isEqualTo(4L);
    }

    @Test
    void summary_ctrIsZero_whenNoImpressions() {
        MerchandisingTargetType target = MerchandisingTargetType.BANNER;
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        given(eventRepository.countInRange(target, 1L, MerchandisingEventType.IMPRESSION, from, to)).willReturn(0L);
        given(eventRepository.countInRange(target, 1L, MerchandisingEventType.CLICK, from, to)).willReturn(3L);
        given(eventRepository.countAttributedOrders(target, 1L, from, to)).willReturn(0L);

        MerchandisingSummaryResponse summary = service.summary(target, 1L, from, to);

        assertThat(summary.ctr()).isEqualTo(0.0);
    }

    @Test
    void attributeOrder_stampsOrderOnMostRecentClick() {
        LocalDateTime orderTime = LocalDateTime.now();
        MerchandisingEvent lastClick = MerchandisingEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(MerchandisingEventType.CLICK)
                .targetType(MerchandisingTargetType.COLLECTION)
                .targetId(9L)
                .userId(2L)
                .build();
        given(eventRepository.findRecentUnattributedClicksByUser(eq(2L), any(), eq(orderTime)))
                .willReturn(List.of(lastClick));

        service.attributeOrder(100L, 2L, orderTime);

        assertThat(lastClick.getOrderId()).isEqualTo(100L);
        verify(eventRepository).save(lastClick);
    }

    @Test
    void attributeOrder_isNoOp_whenUserAnonymous() {
        service.attributeOrder(100L, null, LocalDateTime.now());
        verify(eventRepository, never()).save(any());
    }
}
