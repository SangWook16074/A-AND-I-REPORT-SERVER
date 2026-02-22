package com.example.aandi_post_web_server.scripts

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.nio.file.Paths

class LegacyMigrationScriptTest : StringSpec({
    "마이그레이션 스크립트 help는 사용법을 출력하고 성공 종료한다" {
        val result = runScript("--help")
        result.exitCode shouldBe 0
        result.output.shouldContain("Usage:")
        result.output.shouldContain("migrate_legacy_reports.sh")
    }

    "마이그레이션 스크립트는 ADMIN role이 아니면 실패한다" {
        val result = runScript(
            "--backup",
            "/tmp/does-not-matter.tar.gz",
            "--api-base",
            "http://localhost:8080",
            "--course-slug",
            "back-basic",
            "--course-title",
            "BACK 기초",
            "--user-role",
            "USER",
        )

        result.exitCode shouldBe 1
        result.output.shouldContain("legacy migration requires ADMIN role")
    }
})

private data class ScriptRunResult(
    val exitCode: Int,
    val output: String,
)

private fun runScript(vararg args: String): ScriptRunResult {
    val projectRoot = File(System.getProperty("user.dir"))
    val scriptPath = Paths.get(projectRoot.absolutePath, "scripts", "migrate_legacy_reports.sh").toString()
    val command = mutableListOf("bash", scriptPath)
    command.addAll(args)

    val process = ProcessBuilder(command)
        .directory(projectRoot)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().use { it.readText() }
    val exitCode = process.waitFor()
    return ScriptRunResult(exitCode = exitCode, output = output)
}
