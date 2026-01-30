// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvoiceMapperTest {

    private InvoiceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new InvoiceMapper();
    }

    // === toLocalDate ===
    @Test
    void toLocalDate_nullInput_returnsNull() {
        // null input should yield null output
        LocalDate result = mapper.toLocalDate(null);

        assertThat(result).isNull();
    }

    @Test
    void toLocalDate_javaUtilDate_convertsCorrectly() {
        // create a date representing 2024-06-15 00:00:00 UTC
        LocalDate expected = LocalDate.of(2024, 6, 15);
        Date input = Date.from(expected.atStartOfDay(ZoneOffset.UTC).toInstant());

        LocalDate result = mapper.toLocalDate(input);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toLocalDate_javaSqlDate_convertsCorrectly() {
        // java.sql.Date requires special handling as toInstant() throws
        LocalDate expected = LocalDate.of(2024, 12, 25);
        java.sql.Date input = java.sql.Date.valueOf(expected);

        LocalDate result = mapper.toLocalDate(input);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toLocalDate_preservesDateComponents() {
        // ensure year, month, and day survive the conversion unchanged
        LocalDate expected = LocalDate.of(2023, 1, 31);
        Date input = Date.from(expected.atStartOfDay(ZoneOffset.UTC).toInstant());

        LocalDate result = mapper.toLocalDate(input);

        assertThat(result.getYear()).isEqualTo(2023);
        assertThat(result.getMonthValue()).isEqualTo(1);
        assertThat(result.getDayOfMonth()).isEqualTo(31);
    }

    @Test
    void toLocalDate_leapYearDate_convertsCorrectly() {
        // leap day should be preserved correctly
        LocalDate expected = LocalDate.of(2024, 2, 29); // 2024 is a leap year
        Date input = Date.from(expected.atStartOfDay(ZoneOffset.UTC).toInstant());

        LocalDate result = mapper.toLocalDate(input);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toLocalDate_endOfYear_convertsCorrectly() {
        // end-of-year date should round-trip accurately
        LocalDate expected = LocalDate.of(2024, 12, 31);
        Date input = Date.from(expected.atStartOfDay(ZoneOffset.UTC).toInstant());

        LocalDate result = mapper.toLocalDate(input);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toLocalDate_startOfYear_convertsCorrectly() {
        // start-of-year date should round-trip accurately
        LocalDate expected = LocalDate.of(2024, 1, 1);
        Date input = Date.from(expected.atStartOfDay(ZoneOffset.UTC).toInstant());

        LocalDate result = mapper.toLocalDate(input);

        assertThat(result).isEqualTo(expected);
    }
}
