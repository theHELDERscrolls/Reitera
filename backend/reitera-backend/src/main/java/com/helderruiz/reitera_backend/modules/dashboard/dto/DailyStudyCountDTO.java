package com.helderruiz.reitera_backend.modules.dashboard.dto;

/**
 * One entry in the study activity heatmap: a calendar date and the number of
 * distinct cards reviewed on that date by the requesting user.
 *
 * @param date  calendar date formatted as {@code YYYY-MM-DD}
 * @param count number of distinct cards reviewed
 */
public record DailyStudyCountDTO(String date, long count) {}
