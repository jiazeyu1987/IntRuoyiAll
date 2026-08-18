package cn.iocoder.yudao.module.mes.service.pro.route;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProRouteVersionSnapshotWriteContractTest {

    @Test
    void everyRouteVersionSnapshotWriter_shouldPopulateHashAndFormatTogether() throws Exception {
        Path javaRoot = findJavaRoot();
        Path routeRoot = javaRoot.resolve(Path.of("cn", "iocoder", "yudao", "module", "mes",
                "service", "pro", "route"));
        assertUsesIdentityWriter(routeRoot.resolve("MesProRouteCandidateConfigServiceImpl.java"));
        assertUsesIdentityWriter(routeRoot.resolve("MesProRouteServiceImpl.java"));
        assertUsesIdentityWriter(routeRoot.resolve("MesProRouteVersionWorkflowServiceImpl.java"));
        assertUsesIdentityWriter(javaRoot.resolve(Path.of("cn", "iocoder", "yudao", "module", "mes",
                "service", "pro", "batchrecordreport", "MesProBatchRecordRouteGenerationServiceImpl.java")));
    }

    private void assertUsesIdentityWriter(Path sourceFile) throws Exception {
        String source = Files.readString(sourceFile, StandardCharsets.UTF_8);
        assertTrue(source.contains("MesProRouteVersionSnapshotIdentityWriter.apply("),
                () -> sourceFile.getFileName() + " must write snapshot JSON/hash/format as one identity");
    }

    private Path findJavaRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path relative = Path.of("yudao-module-mes", "src", "main", "java");
        while (current != null) {
            Path candidate = current.resolve(relative);
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new AssertionError("unable to locate MES main Java source root");
    }
}
