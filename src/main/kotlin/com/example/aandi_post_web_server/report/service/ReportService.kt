package com.example.aandi_post_web_server.report.service

import com.example.aandi_post_web_server.report.dtos.ReportDetailDTO
import com.example.aandi_post_web_server.report.dtos.ReportRequestDTO
import com.example.aandi_post_web_server.report.dtos.ReportSummaryDTO
import com.example.aandi_post_web_server.report.entity.Report
import com.example.aandi_post_web_server.report.repository.ReportRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.ZoneId

@Service
class ReportService(
    private val reportRepository: ReportRepository,
) {
    suspend fun createReport(reportRequestDTO: ReportRequestDTO): Mono<Report> {
        return reportRepository.save(reportRequestDTO.toEntity())
    }

    suspend fun getAllReport(): Flux<Report> = reportRepository.findAll()

    suspend fun updateReport(id: String, reportRequestDTO: ReportRequestDTO): Mono<Report> {
        return reportRepository.findById(id)
            .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "리포트를 찾을 수 없습니다: $id")))
            .flatMap { reportRepository.save(reportRequestDTO.toEntity(id)) }
    }

    suspend fun deleteReport(id: String): Mono<String> {
        return reportRepository.findById(id)
            .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "리포트를 찾을 수 없습니다: $id")))
            .flatMap { report ->
                reportRepository.delete(report).thenReturn("리포트 삭제 완료")
            }
    }

    suspend fun getAllOngoingReportSummaries(): Flux<ReportSummaryDTO> {
        val now = Instant.now().atZone(ZoneId.of("UTC")).toInstant()

        return reportRepository.findAll()
            .filter { it.startAt.isBefore(now) }
            .map { report -> report.toSummaryDto() }
    }

    suspend fun getReportDetailById(id: String): Mono<ReportDetailDTO> {
        return reportRepository.findById(id)
            .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "리포트를 찾을 수 없습니다: $id")))
            .map { report -> report.toDetailDto() }
    }

    private fun ReportRequestDTO.toEntity(id: String? = null): Report = Report(
        id = id,
        week = week,
        seq = seq,
        title = title,
        content = content,
        requirement = requirement,
        objects = objects,
        exampleIO = exampleIO,
        reportType = reportType,
        startAt = startAt.toInstant(),
        endAt = endAt.toInstant(),
        level = level,
    )

    private fun Report.toSummaryDto(): ReportSummaryDTO = ReportSummaryDTO(
        id = id ?: "",
        seq = seq,
        week = week,
        title = title,
        level = level,
        reportType = reportType,
        endAt = endAt,
    )

    private fun Report.toDetailDto(): ReportDetailDTO = ReportDetailDTO(
        id = id ?: "",
        week = week,
        title = title,
        content = content,
        requirement = requirement,
        objects = objects,
        exampleIo = exampleIO,
        reportType = reportType,
        endAt = endAt,
        level = level,
    )
}
