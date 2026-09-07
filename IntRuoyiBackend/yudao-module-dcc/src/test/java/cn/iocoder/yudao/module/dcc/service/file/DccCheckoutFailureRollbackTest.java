package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DccCheckoutFailureRollbackTest {

    @Test
    void checkinFailurePathCleansIsolatedCopyAndThrows() throws Exception {
        String service = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao", "module",
                        "dcc", "service", "file", "DccControlledFileQueryServiceImpl.java"),
                StandardCharsets.UTF_8);
        assertTrue(service.contains("cleanupPreparedSourceIfNeeded(preparedSource)"));
        assertTrue(service.contains("CONTROLLED_FILE_CHECKIN_NO_CHANGE"));
        assertTrue(service.contains("CONTROLLED_FILE_CHECKIN_NOT_OWNER"));
        assertTrue(service.contains("@Transactional(rollbackFor = Exception.class)"));
    }
}
