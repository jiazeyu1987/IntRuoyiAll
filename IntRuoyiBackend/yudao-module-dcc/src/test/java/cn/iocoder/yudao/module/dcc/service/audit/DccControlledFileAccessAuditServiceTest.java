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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import(DccControlledFileAccessAuditService.class)
class DccControlledFileAccessAuditServiceTest extends BaseDbUnitTest {

    @Resource
    private DccControlledFileAccessAuditService accessAuditService;
    @Resource
    private DccControlledFileAccessLogMapper accessLogMapper;

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
