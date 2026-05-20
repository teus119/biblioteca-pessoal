package com.biblioteca.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DtoTest {

    @Test
    void testDtoInstantiation() {
        AuthDTO authDTO = new AuthDTO();
        BookDTO bookDTO = new BookDTO();
        assertThat(authDTO).isNotNull();
        assertThat(bookDTO).isNotNull();
    }
}
