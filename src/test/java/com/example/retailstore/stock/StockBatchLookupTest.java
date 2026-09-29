package com.example.retailstore.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.example.retailstore.shared.events.SpringEventPublisher;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;

class StockBatchLookupTest {
    private final StockRepository repository = mock(StockRepository.class);
    private final StockService service = new StockService(
            repository, mock(StockHistoryRepository.class), Clock.systemUTC(), mock(SpringEventPublisher.class));

    @Test
    void batchesRequestedIdsAndMapsResultsIndependentOfDatabaseOrder() {
        Stock first = mock(Stock.class);
        Stock second = mock(Stock.class);
        when(first.getProductId()).thenReturn(1L);
        when(second.getProductId()).thenReturn(2L);
        var ids = List.of(1L, 2L, 1L);
        when(repository.findAllByProductIdIn(ids)).thenReturn(List.of(second, first));

        assertThat(service.findByProductIds(ids))
                .containsEntry(1L, first)
                .containsEntry(2L, second)
                .hasSize(2);
        verify(repository).findAllByProductIdIn(ids);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void preservesMissingRecordValidation() {
        when(repository.findAllByProductIdIn(List.of(99L))).thenReturn(List.of());
        assertThatThrownBy(() -> service.findByProductIds(List.of(99L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Stock not found for product: 99");
    }

    @Test
    void emptyRequestDoesNotQueryRepository() {
        assertThat(service.findByProductIds(List.of())).isEmpty();
        verifyNoInteractions(repository);
    }
}
