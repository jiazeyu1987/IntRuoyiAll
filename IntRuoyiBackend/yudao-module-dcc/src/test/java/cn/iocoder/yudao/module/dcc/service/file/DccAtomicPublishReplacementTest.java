package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DccAtomicPublishReplacementTest {

    @Test
    void publishFinalizationKeepsOldPointerUntilAtomicActivation() throws Exception {
        String finalization = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao", "module",
                        "dcc", "service", "file", "DccControlledFileFinalizationServiceImpl.java"),
                StandardCharsets.UTF_8);
        assertTrue(finalization.contains("READY_TO_PUBLISH"));
        assertTrue(finalization.contains("supersedePreviousActiveRevision"));
        assertTrue(finalization.contains("currentActiveControlledFileId(file.getId())"));
        assertTrue(finalization.contains("SUPERSEDED"));
        assertTrue(finalization.contains("markFinalizationFailed"));
    }
}
