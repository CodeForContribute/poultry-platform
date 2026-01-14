package com.poultry.analytics.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateRangeRequest {

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    public Instant getStartInstant() {
        return startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public Instant getEndInstant() {
        return endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public static DateRangeRequest today() {
        LocalDate today = LocalDate.now();
        return DateRangeRequest.builder().startDate(today).endDate(today).build();
    }

    public static DateRangeRequest lastNDays(int days) {
        LocalDate today = LocalDate.now();
        return DateRangeRequest.builder().startDate(today.minusDays(days)).endDate(today).build();
    }

  public static DateRangeRequest thisMonth()
  {
    LocalDate today = LocalDate.now();
    LocalDate firstDayOfMonth = today.withDayOfMonth(1);
    return DateRangeRequest.builder().startDate(firstDayOfMonth).endDate(today).build();
  }
}
