package com.training.marketplace.repository;

import com.training.marketplace.service.ChatProductCandidate;
import com.training.marketplace.service.ChatSearchCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatProductQueryRepositoryTest {

    @Mock private JdbcTemplate jdbcTemplate;

    @Test
    void separatesDynamicClausesFromSurroundingSqlKeywords() {
        when(jdbcTemplate.query(
                anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<ChatProductCandidate>>any(),
                any(Object[].class)))
                .thenReturn(List.of());
        ChatProductQueryRepository repository = new ChatProductQueryRepository(jdbcTemplate);

        repository.search(new ChatSearchCriteria(
                "laptop", null, null, null, new BigDecimal("20000000"), null,
                Set.of(), Set.of(), false, false, 6));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
                sqlCaptor.capture(),
                org.mockito.ArgumentMatchers.<RowMapper<ChatProductCandidate>>any(),
                any(Object[].class));
        assertThat(sqlCaptor.getValue())
                .containsPattern("WHERE\\s+p\\.active = TRUE")
                .containsPattern("v\\.price <= \\?\\s+GROUP BY")
                .doesNotContain("WHEREp.active")
                .doesNotContain("?GROUP BY");
    }
}
