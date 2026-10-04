package cn.iocoder.yudao.module.dcc.service.file;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class DccActivationJobRegistrationContractTest {
    @Test void preparedRegistrationRequiresApprovedCadenceAndUsesPausedUniqueHandlerWithoutFixedId() throws Exception {
        String sql=Files.readString(Path.of("../sql/mysql/20261003_dcc_controlled_file_activation_job_registration.sql"));
        assertTrue(sql.contains("dependsOn=20260930_dcc_a_lifecycle"));assertTrue(sql.contains("IF @dcc_activation_cron IS NULL"));assertTrue(sql.contains("SIGNAL SQLSTATE '45000'"));
        assertTrue(sql.contains("WHERE handler_name='dccControlledFileActivationJob'"));assertTrue(sql.contains("IF job_count>1"));assertTrue(sql.contains("IF job_count=0"));
        assertTrue(sql.contains("2,'dccControlledFileActivationJob',NULL,@dcc_activation_cron"));
        assertFalse(sql.matches("(?is).*INSERT INTO infra_job\\s*\\([^)]*\\bid\\b.*"));assertFalse(sql.matches("(?is).*UPDATE\\s+infra_job.*"));
        assertFalse(sql.matches("(?is).*SET\\s+@dcc_activation_cron\\s*=.*"));
        String source=Files.readString(Path.of("src/main/java/cn/iocoder/yudao/module/dcc/service/file/listener/DccControlledFileActivationJob.java"));
        assertTrue(source.contains("@Component(\"dccControlledFileActivationJob\")"));assertTrue(source.contains("@TenantJob"));
        assertTrue(source.contains("lifecycleService.activateDue(file.getId())"));
    }
}
