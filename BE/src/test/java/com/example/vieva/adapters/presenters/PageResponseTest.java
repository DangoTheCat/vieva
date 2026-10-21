package com.example.vieva.adapters.presenters;

import com.example.vieva.application.ports.input.common.PageResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {
    @Test
    void mapsContentAndRetainsPageMetadata() {
        PageResponse<String> response = PageResponse.from(PageResult.of(List.of(42), 2, 10, 25), Object::toString);
        assertThat(response.getContent()).containsExactly("42");
        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(25);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isFirst()).isFalse();
        assertThat(response.isLast()).isTrue();
        assertThat(PageResponse.from(null, Object::toString)).isNull();
    }
}
