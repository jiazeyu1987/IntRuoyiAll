package cn.iocoder.yudao.module.dcc.registrationcertificate;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccRegistrationCertificateChangeSqlContractTest {

    @Test
    void changeInsertSqlDoesNotWriteGeneratedSelectedItemCount() throws Exception {
        String source = Files.readString(resolveChangeServiceSource());
        Pattern changeInsert = Pattern.compile(
                "INSERT\\s+INTO\\s+dcc_registration_certificate_change\\s*\\((.*?)\\)\\s*VALUES",
                Pattern.DOTALL);
        Matcher matcher = changeInsert.matcher(source);
        int insertCount = 0;
        while (matcher.find()) {
            insertCount++;
            String columns = matcher.group(1);
            assertFalse(columns.contains("selected_item_count"),
                    "dcc_registration_certificate_change.selected_item_count is a MySQL generated column and must not be written by INSERT SQL");
        }
        assertTrue(insertCount >= 2, "change service must keep explicit INSERT SQL contracts for applied and pending changes");
    }

    @Test
    void changeAddsSnapshotsWithoutMutatingFormalVersionOrMasterFacts() throws Exception {
        String source=Files.readString(resolveChangeServiceSource());
        assertFalse(Pattern.compile("UPDATE\\s+dcc_registration_certificate_version\\s+SET\\s+(certificate_no|classification|approval_date|effective_date|expiry_date)",Pattern.CASE_INSENSITIVE).matcher(source).find());
        assertFalse(source.contains("SET first_obtained_date="));
        assertTrue(source.contains("normalizedChangeFacts"));
        assertTrue(source.contains("JSON_EXTRACT(s.facts_json, '$.certificateNo')"));
    }

    private static Path resolveChangeServiceSource() {
        List<Path> candidates = List.of(
                Path.of("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/change/DccRegistrationCertificateChangeService.java"),
                Path.of("src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/change/DccRegistrationCertificateChangeService.java"),
                Path.of("IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/change/DccRegistrationCertificateChangeService.java"));
        return candidates.stream()
                .map(Path::toAbsolutePath)
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("DccRegistrationCertificateChangeService.java not found"));
    }
}
