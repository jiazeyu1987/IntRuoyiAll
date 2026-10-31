package cn.iocoder.yudao.module.mes.service.pro.route;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MesRouteSnapshotMigrationCliContractTest {
    @TempDir Path temporary;

    @Test
    void releaseEntrypointMustReturnBoundFailureReportWithoutDatabaseCredentials() throws Exception {
        Path report = temporary.resolve("report.json");
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        Path arguments = temporary.resolve("java.args");
        Files.writeString(arguments, "-cp\n\"" + classpath.replace("\\", "\\\\").replace("\"", "\\\"")
                + "\"\ncn.iocoder.yudao.module.mes.service.pro.route.MesRouteSnapshotMigrationCli\n"
                + "--mode=READINESS\n\"--report=" + report.toString().replace("\\", "/") + "\"\n"
                + "--release-tag=contract-check\n--migration-id=20261023_mes_route_snapshot_legacy_summary_enforce\n"
                + "--image-id=contract-image\n--operation-id=contract-operation\n--package-identity=contract-package\n",
                StandardCharsets.UTF_8);
        ProcessBuilder builder = new ProcessBuilder(java, "@" + arguments);
        builder.environment().remove("MES_SNAPSHOT_JDBC_URL");
        builder.environment().remove("MES_SNAPSHOT_JDBC_USER");
        builder.environment().remove("MES_SNAPSHOT_JDBC_PASSWORD");
        builder.redirectErrorStream(true).redirectOutput(temporary.resolve("process.log").toFile());
        Process process = builder.start();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) process.destroyForcibly();
        assertTrue(finished, "Offline CLI must terminate without a database connection");
        assertTrue(Files.isRegularFile(report), "Release CLI must exist and produce its bounded report");
        assertEquals(3, process.exitValue());
        JsonNode result = new ObjectMapper().readTree(Files.readString(report, StandardCharsets.UTF_8));
        assertEquals("route-snapshot-migration/v1", result.path("schemaVersion").asText());
        assertEquals("ERROR", result.path("status").asText());
        assertEquals("COMMAND_FAILED", result.path("errorCode").asText());
        assertEquals("READINESS", result.path("mode").asText());
        assertEquals("contract-check", result.path("releaseTag").asText());
        assertEquals("contract-operation", result.path("operationId").asText());
        assertEquals("contract-package", result.path("packageIdentity").asText());
        assertFalse(result.has("updatedCount"));
    }
}
