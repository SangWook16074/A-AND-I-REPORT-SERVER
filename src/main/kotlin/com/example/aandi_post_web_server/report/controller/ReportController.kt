package com.example.aandi_post_web_server.report.controller

import com.example.aandi_post_web_server.report.dtos.ReportDetailDTO
import com.example.aandi_post_web_server.report.dtos.ReportRequestDTO
import com.example.aandi_post_web_server.report.dtos.ReportSummaryDTO
import com.example.aandi_post_web_server.report.entity.Report
import com.example.aandi_post_web_server.report.service.ReportService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Tag(name = "REPORT API", description = "레포트 공지 게시글 API")
@RestController
@RequestMapping("/v1/reports")
class ReportController(
    private val reportService: ReportService,
) {
    @Operation(summary = "공지 생성", description = "공지를 생성하는 API입니다.")
    @PostMapping
    private suspend fun createReport(@Validated @RequestBody reportRequestDTO: ReportRequestDTO): Mono<Report> =
        reportService.createReport(reportRequestDTO)

    @Operation(summary = "진행중인 리포트 전체 조회", description = "진행중인 리포트의 요약을 가져오는 API입니다.")
    @GetMapping
    private suspend fun getAllSummaryReport(): Flux<ReportSummaryDTO> =
        reportService.getAllOngoingReportSummaries()

    @Operation(summary = "ID를 통한 상세 조회", description = "ID를 통해 리포트를 상세조회하는 API입니다.")
    @GetMapping("/{id}")
    suspend fun getReportDetail(@PathVariable id: String): ResponseEntity<ReportDetailDTO> {
        val report = reportService.getReportDetailById(id).awaitFirst()
        return ResponseEntity.ok(report)
    }

    @Operation(summary = "리포트 수정", description = "리포트 ID를 통해 해당 리포트를 수정합니다.")
    @PutMapping("/{id}")
    private suspend fun updateReport(
        @Parameter(description = "레포트 ID") @PathVariable id: String,
        @RequestBody @Valid reportRequestDTO: ReportRequestDTO,
    ): Mono<Report> = reportService.updateReport(id, reportRequestDTO)

    @Operation(summary = "리포트 삭제", description = "리포트 ID를 통해 해당 레포트를 삭제합니다.")
    @DeleteMapping("/{id}")
    private suspend fun deleteReport(
        @Parameter(description = "레포트 ID") @PathVariable id: String,
    ): Mono<String> = reportService.deleteReport(id).map { "리포트 삭제 완료" }

    @Operation(summary = "모든 리포트 전체 조회", description = "리포트가 잘 생성되었는지 확인용 API 입니다.")
    @GetMapping("/allReport")
    private suspend fun getAllReport(): Flux<Report> = reportService.getAllReport()
}
