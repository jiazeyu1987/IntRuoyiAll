package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRequestReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionArchiveDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionArchiveMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@Import(MesProEdhrBatchVoidEffectServiceImpl.class)
class MesProEdhrBatchVoidEffectServiceImplTest extends BaseDbUnitTest {

    private static final Long ACTOR_ID = 101L;
    private static final String HASH_64 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Resource
    private MesProEdhrBatchVoidEffectService batchVoidEffectService;
    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrBatchExecutionArchiveMapper batchArchiveMapper;

    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private MesProEdhrGoldenFingerPermissionService goldenFingerPermissionService;
    @MockitoBean
    private MesProEdhrWorkTaskService workTaskService;

    @Test
    void executeDirectPlatformVoidBatchExecution_cancelsActiveWorkTasks() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatchExecution();
        insertSealedBatchArchive(batch.getId());

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            batchVoidEffectService.executeDirectPlatformVoidBatchExecution(new EdhrRecordChangeRequestReqVO()
                    .setBatchExecutionId(batch.getId())
                    .setReasonCategory("ORDER_CANCELLED")
                    .setReasonText("金手指直通作废后工作台任务必须同步关闭。")
                    .setPassword("request-pass")
                    .setComment("direct void"), ACTOR_ID);
        }

        verify(workTaskService).cancelActiveTasksByBatch(batch.getId(),
                "批次已作废：金手指直通作废后工作台任务必须同步关闭。");
    }

    private static MockedStatic<SecurityFrameworkUtils> mockLoginUser() {
        MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(ACTOR_ID);
        return security;
    }

    private MesProEdhrBatchExecutionDO insertClosedBatchExecution() {
        MesProEdhrBatchExecutionDO batch = MesProEdhrBatchExecutionDO.builder()
                .batchExecutionCode("BATCH-VOID-EFFECT-" + System.nanoTime())
                .workOrderId(30L)
                .workOrderCode("MO-VOID-EFFECT")
                .batchCode("BATCH-VOID-EFFECT")
                .routeId(40L)
                .routeCode("ROUTE-VOID-EFFECT")
                .status(30)
                .taskTotal(2)
                .taskApprovedCount(2)
                .blockedCount(0)
                .aggregateHash(HASH_64)
                .closedBy(ACTOR_ID)
                .closedAt(LocalDateTime.now().minusHours(1))
                .build();
        batchExecutionMapper.insert(batch);
        return batch;
    }

    private MesProEdhrBatchExecutionArchiveDO insertSealedBatchArchive(Long batchExecutionId) {
        MesProEdhrBatchExecutionArchiveDO archive = MesProEdhrBatchExecutionArchiveDO.builder()
                .batchExecutionId(batchExecutionId)
                .archiveVersion(1)
                .artifactType("FINAL_PDF")
                .archiveStatus("SEALED")
                .fileName("edhr-batch.pdf")
                .contentType("application/pdf")
                .fileSize(100L)
                .filePath("mes/edhr/batch-void-effect.pdf")
                .contentHash(HASH_64)
                .sourceManifestJson("{\"batchExecutionId\":" + batchExecutionId + "}")
                .generatedBy(ACTOR_ID)
                .generatedAt(LocalDateTime.now().minusMinutes(30))
                .sealedSignatureId(8802L)
                .archiveValidFlag(Boolean.TRUE)
                .archiveValidStatus("VALID")
                .build();
        batchArchiveMapper.insert(archive);
        return archive;
    }
}
