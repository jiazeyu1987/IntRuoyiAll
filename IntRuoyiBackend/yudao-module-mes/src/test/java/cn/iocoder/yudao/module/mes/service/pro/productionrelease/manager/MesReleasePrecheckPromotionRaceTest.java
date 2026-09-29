package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleasePrecheckReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseCheckItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesOrderReleaseCompletenessCheck;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesOrderReleaseCompletenessService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFourMaterialGateResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFourMaterialGateService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImpl;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MesReleasePrecheckPromotionRaceTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void stalePrecheckCannotOverwriteFormalApproval(boolean byTransactionId) {
        var service = new MesProEdhrReleaseServiceImpl();
        var transactions = mock(MesProEdhrReleaseTransactionMapper.class);
        var batches = mock(MesProEdhrBatchExecutionMapper.class);
        ReflectionTestUtils.setField(service, "releaseTransactionMapper", transactions);
        ReflectionTestUtils.setField(service, "batchExecutionMapper", batches);
        ReflectionTestUtils.setField(service, "batchExecutionOriginMapper", mock(MesProEdhrBatchExecutionOriginMapper.class));
        var tasks = mock(MesProEdhrBatchExecutionTaskMapper.class);
        when(tasks.selectListByBatchExecutionId(901L)).thenReturn(java.util.List.of());
        ReflectionTestUtils.setField(service, "batchExecutionTaskMapper", tasks);
        var checkItems = mock(MesProEdhrReleaseCheckItemMapper.class);
        when(checkItems.selectList(any())).thenReturn(java.util.List.of());
        ReflectionTestUtils.setField(service, "releaseCheckItemMapper", checkItems);
        var completeness = mock(MesOrderReleaseCompletenessService.class);
        var check = new MesOrderReleaseCompletenessCheck("CHECK", "check", "category", "PASS", "INFO",
                "MES", "BATCH", "901", "BE-901", "", "");
        when(completeness.evaluateInspectionResult(any())).thenReturn(check);
        when(completeness.evaluateDeviationClosed(any())).thenReturn(check);
        when(completeness.evaluateReworkClosed(any())).thenReturn(check);
        when(completeness.evaluateScrapRecorded(any())).thenReturn(check);
        when(completeness.evaluateInventoryConsistency(any())).thenReturn(check);
        ReflectionTestUtils.setField(service, "releaseCompletenessService", completeness);
        var materialGate = mock(MesProEdhrFourMaterialGateService.class);
        when(materialGate.evaluate(901L)).thenReturn(new MesProEdhrFourMaterialGateResult(
                MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_READY, true, "manifest", java.util.List.of()));
        ReflectionTestUtils.setField(service, "fourMaterialGateService", materialGate);
        ReflectionTestUtils.setField(service, "releaseTransactionEventMapper", mock(MesProEdhrReleaseTransactionEventMapper.class));
        ReflectionTestUtils.setField(service, "operationAuditService", mock(MesProEdhrOperationAuditService.class));
        var applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        when(applications.selectListByReleaseTransactionId(990L)).thenReturn(java.util.List.of());
        ReflectionTestUtils.setField(service, "releaseApplicationMapper", applications);
        var audit = mock(GxpAuditService.class);
        ReflectionTestUtils.setField(service, "gxpAuditService", audit);
        ReflectionTestUtils.setField(service, "nonconformanceReviewService", mock(MesProEdhrNonconformanceReviewService.class));
        var old = new MesProEdhrReleaseTransactionDO().setId(990L).setBatchExecutionId(901L)
                .setReleaseStatus("PRECHECK_PASSED").setDhrStatus("PASS")
                .setInspectionStatus("PASS").setDeviationStatus("PASS").setReworkStatus("PASS")
                .setScrapStatus("PASS").setInventoryStatus("PASS")
                .setRequiredCheckCount(10).setFailedCheckCount(0).setBlockingCheckCount(0).setVersion(1);
        when(transactions.selectById(990L)).thenReturn(old);
        when(transactions.selectCurrentByBatchExecutionId(901L)).thenReturn(old);
        when(batches.selectById(901L)).thenReturn(new MesProEdhrBatchExecutionDO()
                .setId(901L).setBatchExecutionCode("BE-901"));
        when(transactions.selectByIdForUpdate(990L)).thenReturn(
                new MesProEdhrReleaseTransactionDO().setId(990L).setBatchExecutionId(901L)
                        .setReleaseStatus("PENDING_APPROVAL").setVersion(2));
        var request = new MesProEdhrReleasePrecheckReqVO();
        request.setBatchExecutionId(901L);
        if (byTransactionId) request.setReleaseTransactionId(990L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("race-test");
            assertThrows(ServiceException.class, () -> service.precheck(request));
        }
        verify(transactions).selectByIdForUpdate(990L);
        verify(transactions, never()).updateById(any(MesProEdhrReleaseTransactionDO.class));
    }
}
