package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertCreateReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOwnerMatrixSaveReqVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeOpsAlertServiceImplTest {

    @TempDir
    private Path tempDir;

    private RuntimeOpsAlertServiceImpl alertService;
    private RuntimeOpsResponsibilityServiceImpl responsibilityService;
    private RuntimeOpsCapturingSiteMessageSender siteMessageSender;

    @BeforeEach
    void setUp() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        RuntimeOpsOwnerMatrixStore ownerMatrixStore = new RuntimeOpsOwnerMatrixStore(properties);
        RuntimeOpsAlertStore alertStore = new RuntimeOpsAlertStore(properties);
        responsibilityService = new RuntimeOpsResponsibilityServiceImpl(ownerMatrixStore);
        siteMessageSender = new RuntimeOpsCapturingSiteMessageSender();
        alertService = new RuntimeOpsAlertServiceImpl(alertStore, responsibilityService, siteMessageSender);
    }

    @Test
    void createAlertShouldSendSiteMessageToConfiguredRequiredOwnerAndRecordSent() {
        createOwner("prod", "backup-failed", "ops-owner", 1001L);

        RuntimeControlAlertRespVO alert = alertService.createAlert(alert("prod", "backup-failed",
                "RUNTIME_OPS_ALERT", "备份失败", "prod backup failed"));

        assertNotNull(alert.getId());
        assertEquals(RuntimeControlSiteMessageStatus.SENT, alert.getSiteMessageStatus());
        assertEquals(9001L, alert.getNotifyMessageId());
        assertEquals(1001L, siteMessageSender.lastUserId);
        assertEquals("RUNTIME_OPS_ALERT", siteMessageSender.lastTemplateCode);
        assertEquals("备份失败", siteMessageSender.lastParams.get("title"));
        assertEquals(1, alertService.getAlertsPage(new RuntimeControlAlertPageReqVO()).getTotal());
    }

    @Test
    void createAlertShouldSendLocalCapacityWarningToDefaultOpsOwner() {
        RuntimeControlAlertRespVO alert = alertService.createAlert(alert("local", "storage-capacity-warning",
                "RUNTIME_OPS_ALERT", "容量预警", "local log capacity is high"));

        assertNotNull(alert.getId());
        assertEquals(RuntimeControlSiteMessageStatus.SENT, alert.getSiteMessageStatus());
        assertEquals(9001L, alert.getNotifyMessageId());
        assertEquals(1L, siteMessageSender.lastUserId);
        assertEquals("RUNTIME_OPS_ALERT", siteMessageSender.lastTemplateCode);
        assertEquals("容量预警", siteMessageSender.lastParams.get("title"));
    }

    @Test
    void createAlertShouldRecordBlockedWhenTemplateIsMissingAndNeverSendSiteMessage() {
        createOwner("prod", "disk-capacity-warning", "ops-owner", 1002L);

        RuntimeControlAlertRespVO alert = alertService.createAlert(alert("prod", "disk-capacity-warning",
                null, "磁盘容量预警", "disk is high"));

        assertEquals(RuntimeControlSiteMessageStatus.BLOCKED, alert.getSiteMessageStatus());
        assertTrue(alert.getSiteMessageFailureReason().contains("模板"));
        assertEquals(0, siteMessageSender.callCount.get());
    }

    @Test
    void createAlertShouldRecordFailedAndPropagateWhenSiteMessageApiFails() {
        createOwner("prod", "probe-failed", "ops-owner", 1003L);
        siteMessageSender.failure = new IllegalStateException("notify template disabled");

        ServiceException exception = assertThrows(ServiceException.class, () -> alertService.createAlert(alert("prod",
                "probe-failed", "RUNTIME_OPS_ALERT", "探针失败", "probe failed")));

        assertTrue(exception.getMessage().contains("站内信"));
        PageResult<RuntimeControlAlertRespVO> page = alertService.getAlertsPage(new RuntimeControlAlertPageReqVO());
        assertEquals(1, page.getTotal());
        assertEquals(RuntimeControlSiteMessageStatus.FAILED, page.getList().get(0).getSiteMessageStatus());
        assertTrue(page.getList().get(0).getSiteMessageFailureReason().contains("notify template disabled"));
    }

    @Test
    void createAlertShouldRecordBlockedWhenRequiredOwnerIsMissing() {
        RuntimeControlAlertRespVO alert = alertService.createAlert(alert("prod", "backup-drill-failed",
                "RUNTIME_OPS_ALERT", "演练失败", "drill failed"));

        assertEquals(RuntimeControlSiteMessageStatus.BLOCKED, alert.getSiteMessageStatus());
        assertTrue(alert.getSiteMessageFailureReason().contains("责任人"));
        assertEquals(0, siteMessageSender.callCount.get());
    }

    @Test
    void acknowledgeShouldFailFastWhenOperatorIsBlank() {
        createOwner("prod", "backup-failed", "ops-owner", 1001L);
        RuntimeControlAlertRespVO alert = alertService.createAlert(alert("prod", "backup-failed",
                "RUNTIME_OPS_ALERT", "备份失败", "prod backup failed"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> alertService.acknowledge(alert.getId(), " "));

        assertTrue(exception.getMessage().contains("acknowledgedBy"));
    }

    private RuntimeControlAlertCreateReqVO alert(String environment, String action, String templateCode,
                                                String title, String content) {
        RuntimeControlAlertCreateReqVO reqVO = new RuntimeControlAlertCreateReqVO();
        reqVO.setEnvironment(environment);
        reqVO.setAction(action);
        reqVO.setSeverity("WARN");
        reqVO.setTitle(title);
        reqVO.setContent(content);
        reqVO.setNotifyTemplateCode(templateCode);
        reqVO.setTemplateParams(Map.of("title", title, "content", content));
        return reqVO;
    }

    private void createOwner(String environment, String action, String role, Long userId) {
        RuntimeControlOwnerMatrixSaveReqVO reqVO = new RuntimeControlOwnerMatrixSaveReqVO();
        reqVO.setEnvironment(environment);
        reqVO.setAction(action);
        reqVO.setRole(role);
        reqVO.setRequired(true);
        reqVO.setOwnerUserId(userId);
        reqVO.setOwnerName("owner-" + userId);
        responsibilityService.createOwner(reqVO);
    }

    private static final class RuntimeOpsCapturingSiteMessageSender implements RuntimeOpsSiteMessageSender {

        private final AtomicInteger callCount = new AtomicInteger();
        private Long lastUserId;
        private String lastTemplateCode;
        private Map<String, Object> lastParams;
        private RuntimeException failure;

        @Override
        public Long sendSingleMessageToAdmin(Long userId, String templateCode, Map<String, Object> templateParams) {
            callCount.incrementAndGet();
            lastUserId = userId;
            lastTemplateCode = templateCode;
            lastParams = templateParams;
            if (failure != null) {
                throw failure;
            }
            return 9001L;
        }
    }
}
