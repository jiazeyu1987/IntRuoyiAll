package cn.iocoder.yudao.module.dcc.service.audit;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAccessLogDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.IllegalTransactionStateException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(DccControlledFileAccessAuditService.class)
class DccControlledFileAccessAuditServiceTest extends BaseDbUnitTest {

    @Resource
    private DccControlledFileAccessAuditService accessAuditService;
    @Resource
    private DccControlledFileAccessLogMapper accessLogMapper;
    @Resource
    private PlatformTransactionManager transactionManager;

    @Test
    void lifecycleSuccessRequiresBusinessTransactionAndCorrectResult() {
        var success=new DccLifecycleLogCreateCommand(900L,"A/1",99L,"CHECKOUT","SUCCESS",null,"edit");
        assertThrows(IllegalTransactionStateException.class,()->accessAuditService.recordLifecycleLog(success));
        assertThrows(IllegalArgumentException.class,()->new TransactionTemplate(transactionManager).execute(s->
                accessAuditService.recordLifecycleLog(new DccLifecycleLogCreateCommand(900L,"A/1",99L,"CHECKOUT","FAILED","DENIED","edit"))));
        assertThrows(IllegalArgumentException.class,()->accessAuditService.recordLifecycleFailureLog(success));
        assertEquals(0L,accessLogMapper.selectCount());
    }

    @Test
    void independentFailureSurvivesOuterRollbackButSuccessDoesNot() {
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactionManager).execute(s->{
            accessAuditService.recordLifecycleLog(new DccLifecycleLogCreateCommand(901L,"A/1-1",99L,"CHECKIN","SUCCESS",null,"new body"));
            accessAuditService.recordLifecycleFailureLog(new DccLifecycleLogCreateCommand(900L,"A/1",99L,"CHECKIN","FAILED","CHECKIN_FAILED","source action failed"));
            throw new IllegalStateException("late outer failure");
        }));
        var saved=accessLogMapper.selectList();assertEquals(1,saved.size());assertEquals("FAILED",saved.get(0).getResult());
        assertEquals(900L,saved.get(0).getControlledFileId());assertEquals("A/1",saved.get(0).getFileVersionNo());assertEquals(99L,saved.get(0).getUserId());
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void recordDirectLinkDeniedLog_createsTraceableAnonymousAuditRecordWithoutWatermarkEvent() {
        accessAuditService.recordDirectLinkDeniedLog(new DccDirectLinkDeniedLogCreateCommand(1L, 900L, 700L,
                "PUBLISHED", "DIRECT_LINK", "INFRA_DIRECT_LINK", "DENIED", "DCC_DIRECT_LINK_BLOCKED",
                "DCC controlled file direct link is blocked: infraFileId=700, artifactRole=PUBLISHED",
                "10.0.0.7", "REQ-DIRECT-001", "Playwright-E2E"));

        DccControlledFileAccessLogDO accessLog = accessLogMapper.selectOne(
                new LambdaQueryWrapper<DccControlledFileAccessLogDO>()
                        .eq(DccControlledFileAccessLogDO::getControlledFileId, 900L)
                        .eq(DccControlledFileAccessLogDO::getActionType, "DIRECT_LINK"));
        assertEquals(1L, accessLog.getTenantId());
        assertEquals(0L, accessLog.getUserId());
        assertNull(accessLog.getAccessEventId());
        assertNull(accessLog.getWatermarkTraceCode());
        assertEquals("INFRA_DIRECT_LINK", accessLog.getPurpose());
        assertEquals("DENIED", accessLog.getResult());
        assertEquals("DCC_DIRECT_LINK_BLOCKED", accessLog.getFailureCode());
        assertEquals("10.0.0.7", accessLog.getSourceIp());
        assertEquals("REQ-DIRECT-001", accessLog.getRequestId());
        assertEquals("Playwright-E2E", accessLog.getUserAgent());
    }

    @Test
    void recordDirectLinkDeniedLog_usesControlledFileTenantWhenRequestHasNoUserTenant() {
        TenantContextHolder.setTenantId(0L);

        accessAuditService.recordDirectLinkDeniedLog(new DccDirectLinkDeniedLogCreateCommand(122L, 901L, 700L,
                "PUBLISHED", "DIRECT_LINK", "INFRA_DIRECT_LINK", "DENIED", "DCC_DIRECT_LINK_BLOCKED",
                "DCC controlled file direct link is blocked: infraFileId=700, artifactRole=PUBLISHED",
                "10.0.0.7", "REQ-DIRECT-001", "Playwright-E2E"));

        DccControlledFileAccessLogDO accessLog = TenantUtils.executeIgnore(() -> accessLogMapper.selectOne(
                new LambdaQueryWrapper<DccControlledFileAccessLogDO>()
                        .eq(DccControlledFileAccessLogDO::getControlledFileId, 901L)
                        .eq(DccControlledFileAccessLogDO::getActionType, "DIRECT_LINK")));
        assertNotNull(accessLog);
        assertEquals(122L, accessLog.getTenantId());
    }

    @Test
    void recordBoundaryLog_createsUploadFailureAuditWithoutControlledFileCapability() {
        TenantContextHolder.setTenantId(122L);

        accessAuditService.recordBoundaryLog(new DccAccessBoundaryLogCreateCommand(113L, "UPLOAD",
                "SOURCE", "DENIED", "DCC_UPLOAD_SIZE_EXCEEDED",
                "DCC upload size exceeds policy limit", "10.0.0.9", "REQ-UPLOAD-001",
                "Playwright-E2E"));

        DccControlledFileAccessLogDO accessLog = accessLogMapper.selectOne(
                new LambdaQueryWrapper<DccControlledFileAccessLogDO>()
                        .eq(DccControlledFileAccessLogDO::getRequestId, "REQ-UPLOAD-001")
                        .eq(DccControlledFileAccessLogDO::getActionType, "UPLOAD"));
        assertNotNull(accessLog);
        assertEquals(122L, accessLog.getTenantId());
        assertNull(accessLog.getControlledFileId());
        assertNull(accessLog.getAccessEventId());
        assertNull(accessLog.getWatermarkTraceCode());
        assertEquals(113L, accessLog.getUserId());
        assertEquals("SOURCE", accessLog.getPurpose());
        assertEquals("DENIED", accessLog.getResult());
        assertEquals("DCC_UPLOAD_SIZE_EXCEEDED", accessLog.getFailureCode());
        assertEquals("10.0.0.9", accessLog.getSourceIp());
        assertEquals("REQ-UPLOAD-001", accessLog.getRequestId());
        assertEquals("Playwright-E2E", accessLog.getUserAgent());
    }
}
