package com.example.vieva.domain.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "test@example.com",
            "USER.NAME@DOMAIN.CO.UK",
            "first+last@sub.domain.org",
            "123@numbers.com"
    })
    @DisplayName("Email: valid formats are accepted and normalized to lower case")
    void validEmails_Success(String input) {
        Email email = new Email(input);
        assertThat(email.getValue()).isEqualTo(input.trim().toLowerCase(java.util.Locale.ROOT));
        assertThat(email.toString()).isEqualTo(input.trim().toLowerCase(java.util.Locale.ROOT));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "plainaddress",
            "@missingusername.com",
            "username@.com",
            "username@com",
            "username@domain..com",
            "user name@domain.com"
    })
    @DisplayName("Email: invalid formats throw IllegalArgumentException")
    void invalidEmails_ThrowException(String input) {
        assertThatThrownBy(() -> new Email(input))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Email: null input throws IllegalArgumentException")
    void nullEmail_ThrowException() {
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Email: input exceeding 255 chars throws IllegalArgumentException")
    void tooLongEmail_ThrowException() {
        String longEmail = "a".repeat(250) + "@domain.com";
        assertThatThrownBy(() -> new Email(longEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum length");
    }

    @Test
    @DisplayName("Email: equals and hashCode work correctly")
    void equalsAndHashCode() {
        Email e1 = new Email("Test@Example.com");
        Email e2 = new Email("test@example.com");
        Email e3 = new Email("other@example.com");

        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
        assertThat(e1).isNotEqualTo(e3);
    }
}
