package com.example.retailstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.example.retailstore.shared.events.SpringEventPublisher;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductBatchLookupTest {
    private final ProductRepository repository = mock(ProductRepository.class);
    private final ProductService service = new ProductService(repository, mock(SpringEventPublisher.class));

    @Test
    void batchesRequestedIdsAndMapsResultsIndependentOfDatabaseOrder() {
        Product first = mock(Product.class);
        Product second = mock(Product.class);
        when(first.getId()).thenReturn(1L);
        when(second.getId()).thenReturn(2L);
        var ids = List.of(1L, 2L, 1L);
        when(repository.findAllById(ids)).thenReturn(List.of(second, first));

        assertThat(service.findByIds(ids))
                .containsEntry(1L, first)
                .containsEntry(2L, second)
                .hasSize(2);
        verify(repository).findAllById(ids);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void preservesMissingRecordValidation() {
        when(repository.findAllById(List.of(99L))).thenReturn(List.of());
        assertThatThrownBy(() -> service.findByIds(List.of(99L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Product not found: 99");
    }

    @Test
    void emptyRequestDoesNotQueryRepository() {
        assertThat(service.findByIds(List.of())).isEmpty();
        verifyNoInteractions(repository);
    }
}
