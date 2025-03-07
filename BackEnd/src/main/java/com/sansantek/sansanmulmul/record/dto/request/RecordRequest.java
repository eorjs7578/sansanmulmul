package com.sansantek.sansanmulmul.record.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Getter
@Setter
public class RecordRequest {
    @Schema(description = "그룹 고유 번호", example = "1")
    private int crewId;
    @Schema(description = "실제 등산 출발 일시", example = "YYYY-MM-DD 23:59:59")
    private LocalDateTime recordStartTime;
    @Schema(description = "실제 등산 도착 일시", example = "YYYY-MM-DD 23:59:59")
    private LocalDateTime recordEndTime;
    @Schema(description = "등산 전체 이동 거리(단위: m)", example = "0.0")
    private long recordDistance;
    @Schema(description = "등산 걸음 수(단위: 걸음)", example = "0")
    private long recordSteps;
    @Schema(description = "등산 고도(단위: m)", example = "0")
    private double recordElevation;
    @Schema(description = "등산 소요 칼로리(단위: Kcal)", example = "0")
    private int recordKcal;
}
