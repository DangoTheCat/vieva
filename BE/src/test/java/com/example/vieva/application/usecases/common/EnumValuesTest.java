package com.example.vieva.application.usecases.common;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.domain.entities.SubjectStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnumValuesTest {

    @Test
    @DisplayName("parse: returns matching enum for valid value")
    void parse_validValue_returnsEnum() {
        SubjectStatus status = EnumValues.parse(SubjectStatus.class, "ACTIVE", "Malformed request body");
        assertThat(status).isEqualTo(SubjectStatus.ACTIVE);

        SubjectStatus statusTrimmed = EnumValues.parse(SubjectStatus.class, "  INACTIVE  ", "Malformed request body");
        assertThat(statusTrimmed).isEqualTo(SubjectStatus.INACTIVE);
    }

    @Test
    @DisplayName("parse: returns null for null or blank value")
    void parse_nullOrBlank_returnsNull() {
        assertThat(EnumValues.parse(SubjectStatus.class, null, "Malformed request body")).isNull();
        assertThat(EnumValues.parse(SubjectStatus.class, "", "Malformed request body")).isNull();
        assertThat(EnumValues.parse(SubjectStatus.class, "   ", "Malformed request body")).isNull();
    }

    @Test
    @DisplayName("parse: throws ApplicationException with exact errorMessage on invalid value")
    void parse_invalidValue_throwsApplicationException() {
        assertThatThrownBy(() -> EnumValues.parse(SubjectStatus.class, "INVALID_STATUS", "Malformed request body"))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST)
                .hasMessage("Malformed request body");

        assertThatThrownBy(() -> EnumValues.parse(SubjectStatus.class, "NOT_AN_ENUM", "Invalid value for parameter 'status'"))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST)
                .hasMessage("Invalid value for parameter 'status'");
    }
}
