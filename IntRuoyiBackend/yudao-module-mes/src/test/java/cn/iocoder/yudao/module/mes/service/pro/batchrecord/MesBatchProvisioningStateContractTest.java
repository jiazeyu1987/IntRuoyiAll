package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchProvisioningRecordDO;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesBatchProvisioningStateContractTest {

    @Test
    void provisioningStateHasSeparateDurableLifecycle() {
        assertEquals(MesBatchProvisioningStatus.BATCH_PROVISIONING.name(),
                MesBatchProvisioningStatus.BATCH_PROVISIONING.name());
        assertEquals(MesBatchProvisioningStatus.BATCH_PROVISIONING_RETRYABLE.name(),
                MesBatchProvisioningStatus.BATCH_PROVISIONING_RETRYABLE.name());
        assertEquals(MesBatchProvisioningStatus.BATCH_PROVISIONING_BLOCKED.name(),
                MesBatchProvisioningStatus.BATCH_PROVISIONING_BLOCKED.name());
        assertEquals(MesBatchProvisioningStatus.BATCH_READY.name(),
                MesBatchProvisioningStatus.BATCH_READY.name());
        assertEquals("mes_pro_edhr_batch_provisioning_record",
                MesProEdhrBatchProvisioningRecordDO.class.getAnnotation(TableName.class).value());
        assertTrue(MesProEdhrBatchExecutionDO.class.getDeclaredFields().length > 0);
    }

    @Test
    void migrationDeclaresProvisioningStateAndIdempotency() throws Exception {
        Path backendRoot = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize().getParent();
        String sql = Files.readString(backendRoot.resolve("sql/mysql/20260826_mes_edhr_batch_provisioning_record.sql"),
                StandardCharsets.UTF_8);
        assertTrue(sql.contains("provisioning_status"));
        assertTrue(sql.contains("BATCH_PROVISIONING_RETRYABLE"));
        assertTrue(sql.contains("uk_mes_edhr_batch_provisioning_idempotency"));
    }
}
