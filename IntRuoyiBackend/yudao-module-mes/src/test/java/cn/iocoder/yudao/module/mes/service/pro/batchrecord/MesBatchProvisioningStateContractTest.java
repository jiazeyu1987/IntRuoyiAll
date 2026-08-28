package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchProvisioningRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchProvisioningRecordMapper;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void legacyBatchWithoutProvisioningRecordMustNotBeReused() throws Exception {
        MesProEdhrBatchExecutionServiceImpl service = new MesProEdhrBatchExecutionServiceImpl();
        MesProEdhrBatchProvisioningRecordMapper provisioningRecordMapper =
                mock(MesProEdhrBatchProvisioningRecordMapper.class);
        when(provisioningRecordMapper.selectByBatchExecutionId(any(), any())).thenReturn(null);
        ReflectionTestUtils.setField(service, "provisioningRecordMapper", provisioningRecordMapper);
        Method method = MesProEdhrBatchExecutionServiceImpl.class.getDeclaredMethod(
                "requireExistingProvisioning", MesProEdhrBatchExecutionDO.class,
                MesBatchExecutionProvisionCommand.class);
        method.setAccessible(true);
        MesProEdhrBatchExecutionDO legacyBatch = new MesProEdhrBatchExecutionDO()
                .setId(88L)
                .setProvisioningStatus(null);
        MesBatchExecutionProvisionCommand command = new MesBatchExecutionProvisionCommand()
                .setEntryType("ACTIVE_ORDER_COMPLETION")
                .setSourceCredentialId("1001")
                .setSourceSnapshotHash("source-hash")
                .setSourceBundleHash("bundle-hash")
                .setSourceVersion("1")
                .setIdempotencyKey("complete-1001");

        TenantContextHolder.setTenantId(1L);
        try {
            Exception ex = assertThrows(Exception.class, () -> method.invoke(service, legacyBatch, command));
            assertTrue(ex.getCause() instanceof IllegalStateException);
            assertEquals("BATCH_PROVISIONING_RECORD_REQUIRED", ex.getCause().getMessage());
        } finally {
            TenantContextHolder.clear();
        }
    }
}
