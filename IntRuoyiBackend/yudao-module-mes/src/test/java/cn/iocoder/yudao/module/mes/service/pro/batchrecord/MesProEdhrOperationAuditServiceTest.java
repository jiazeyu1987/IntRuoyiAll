package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOperationAuditPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOperationAuditRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrOperationAuditEventMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditErrorCodeConstants.PRO_EDHR_OPERATION_AUDIT_CONTEXT_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(MesProEdhrOperationAuditServiceImpl.class)
class MesProEdhrOperationAuditServiceTest extends BaseDbUnitTest {

    @Resource
    private MesProEdhrOperationAuditService auditService;
    @Resource
    private MesProEdhrOperationAuditEventMapper auditEventMapper;

    @Test
    void recordSuccessAndFailureEvents_linkSameObjectAuditHashChain() {
        MesProEdhrOperationAuditRespVO first = auditService.record(newCommand("OPEN", "SUCCESS")
                .setRequestId("req-open-1")
                .setActionName("打开 eDHR 批次")
                .setMetadataJson("{\"tab\":\"edhr\"}"));
        MesProEdhrOperationAuditRespVO second = auditService.record(newCommand("SIGN", "FAILED")
                .setRequestId("req-sign-1")
                .setActionName("签名")
                .setFailureCode("SIGN_PASSWORD_INVALID")
                .setFailureMessage("电子签名密码错误"));

        assertNotNull(first.getId());
        assertNotNull(first.getOccurredAt());
        assertEquals("BATCH_EXECUTION", first.getObjectType());
        assertEquals("1001", first.getObjectId());
        assertEquals("OPEN", first.getOperationType());
        assertEquals("ALLOW", first.getPermissionDecision());
        assertEquals("SUCCESS", first.getResultStatus());
        assertNotNull(first.getAuditHash());
        assertEquals(64, first.getAuditHash().length());
        assertNotEquals(first.getAuditHash(), first.getPreviousAuditHash());

        assertEquals(first.getAuditHash(), second.getPreviousAuditHash());
        assertEquals("FAILED", second.getResultStatus());
        assertEquals("SIGN_PASSWORD_INVALID", second.getFailureCode());

        List<MesProEdhrOperationAuditEventDO> stored =
                auditEventMapper.selectListByObject("BATCH_EXECUTION", "1001");
        assertEquals(2, stored.size());
        assertEquals(second.getId(), stored.get(0).getId());
        assertEquals(first.getId(), stored.get(1).getId());
    }

    @Test
    void getPage_filtersByBusinessObjectAndResult() {
        auditService.record(newCommand("OPEN", "SUCCESS")
                .setRequestId("req-open-2")
                .setBatchExecutionId(1001L)
                .setOccurredAt(LocalDateTime.of(2026, 6, 15, 16, 0)));
        auditService.record(newCommand("DOWNLOAD", "SUCCESS")
                .setRequestId("req-download-2")
                .setObjectId("1002")
                .setBatchExecutionId(1002L)
                .setOccurredAt(LocalDateTime.of(2026, 6, 15, 16, 5)));
        auditService.record(newCommand("SIGN", "FAILED")
                .setRequestId("req-sign-2")
                .setBatchExecutionId(1001L)
                .setOccurredAt(LocalDateTime.of(2026, 6, 15, 16, 10)));

        PageResult<MesProEdhrOperationAuditRespVO> page = auditService.getPage(
                new MesProEdhrOperationAuditPageReqVO()
                        .setBatchExecutionId(1001L)
                        .setResultStatus("SUCCESS")
                        .setOccurredAt(new LocalDateTime[]{
                                LocalDateTime.of(2026, 6, 15, 15, 59),
                                LocalDateTime.of(2026, 6, 15, 16, 1)}));

        assertEquals(1L, page.getTotal());
        assertEquals("OPEN", page.getList().get(0).getOperationType());
        assertEquals("req-open-2", page.getList().get(0).getRequestId());
    }

    @Test
    void record_missingAuditContextFailsFast() {
        assertServiceException(() -> auditService.record(new MesProEdhrOperationAuditCommand()
                        .setRequestId("req-missing-context")
                        .setObjectId("1001")
                        .setOperationType("OPEN")
                        .setActorUserId(188L)
                        .setPermissionDecision("ALLOW")
                        .setResultStatus("SUCCESS")),
                PRO_EDHR_OPERATION_AUDIT_CONTEXT_MISSING);
    }

    @Test
    void record_usesIndependentWritableTransactionForReadOnlyQueryCallers() throws Exception {
        Method method = MesProEdhrOperationAuditServiceImpl.class.getDeclaredMethod("record",
                MesProEdhrOperationAuditCommand.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }

    private MesProEdhrOperationAuditCommand newCommand(String operationType, String resultStatus) {
        return new MesProEdhrOperationAuditCommand()
                .setRequestId("req-" + operationType)
                .setObjectType("BATCH_EXECUTION")
                .setObjectId("1001")
                .setBatchExecutionId(1001L)
                .setOperationType(operationType)
                .setActorUserId(188L)
                .setActorUsername("aoteman")
                .setPermissionCode("mes:pro-edhr-batch-execution:" + operationType.toLowerCase())
                .setPermissionDecision("ALLOW")
                .setResultStatus(resultStatus);
    }
}
