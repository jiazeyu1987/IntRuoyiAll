package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesActiveOrderDetailReadMapperContractTest {

    @Test
    void formalActiveOrderDetailReadsProcessIdentityFromSnapshot() throws Exception {
        String xml = Files.readString(resolveBackendPath(
                "yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProcessPoolActiveOrderDetailReadMapper.xml"),
                StandardCharsets.UTF_8);

        assertTrue(xml.contains("process_snapshot.process_code_snapshot AS processCode"));
        assertTrue(xml.contains("process_snapshot.process_name_snapshot AS processName"));
        assertTrue(!xml.contains("process.code AS processCode"));
        assertTrue(!xml.contains("process.name AS processName"));
    }

    private static Path resolveBackendPath(String relative) {
        Path cwd = Paths.get("").toAbsolutePath();
        if ("yudao-module-mes".equals(cwd.getFileName().toString())) {
            return cwd.resolve(relative.substring("yudao-module-mes/".length()));
        }
        return cwd.resolve("yudao-module-mes").resolve(
                relative.substring("yudao-module-mes/".length()));
    }
}
