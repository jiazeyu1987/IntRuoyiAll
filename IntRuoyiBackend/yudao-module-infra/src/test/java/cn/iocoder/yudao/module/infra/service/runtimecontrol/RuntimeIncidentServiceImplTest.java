package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentCloseReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentCreateReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOwnerMatrixSaveReqVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeIncidentServiceImplTest {

    @TempDir
    private Path tempDir;

    private RuntimeControlProperties properties;
    private RuntimeIncidentServiceImpl incidentService;
    private RuntimeOpsResponsibilityServiceImpl responsibilityService;

    @BeforeEach
    void setUp() {
        properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        responsibilityService = new RuntimeOpsResponsibilityServiceImpl(new RuntimeOpsOwnerMatrixStore(properties));
        incidentService = new RuntimeIncidentServiceImpl(new RuntimeIncidentStore(properties), responsibilityService);
    }

    @Test
    void createIncidentAndRecordActionShouldPreserveOperatorVerificationEvidenceAndStoreUnderRuntimeOps() {
        RuntimeControlIncidentRespVO incident = incidentService.createIncident(createReq("DIRECT", null), "1001");

        RuntimeControlIncidentActionReqVO actionReqVO = new RuntimeControlIncidentActionReqVO();
        actionReqVO.setAction("确认磁盘日志目录增长");
        actionReqVO.setVerificationResult("日志增长来自批处理错误重试");
        actionReqVO.setEvidence("operationId=op-1001, log=app.log");
        RuntimeControlIncidentRespVO updated = incidentService.recordAction(incident.getId(), actionReqVO, "1002");

        assertEquals("OPEN", updated.getStatus());
        assertEquals(1, updated.getActions().size());
        assertEquals("1002", updated.getActions().get(0).getOperator());
        assertEquals("日志增长来自批处理错误重试", updated.getActions().get(0).getVerificationResult());
        assertFalse(Files.exists(tempDir.resolve("incidents.json")));
        assertTrue(Files.isRegularFile(tempDir.resolve("runtime-ops").resolve("incidents.json")));
    }

    @Test
    void createIncidentShouldFailFastWhenCreatorIsBlank() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> incidentService.createIncident(createReq("DIRECT", null), " "));

        assertTrue(exception.getMessage().contains("createdBy"));
    }

    @Test
    void closeIncidentShouldFailWhenResponsibilityGateIsMissing() {
        RuntimeControlIncidentRespVO incident = incidentService.createIncident(
                createReq("ALERT", "1", "unconfigured-action"), "1001");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> incidentService.closeIncident(incident.getId(), closeReq(), "1003"));

        assertTrue(exception.getMessage().contains("责任人"));
    }

    @Test
    void closeIncidentShouldFailWhenCloseEvidenceIsIncomplete() {
        responsibilityService.createOwner(owner());
        RuntimeControlIncidentRespVO incident = incidentService.createIncident(createReq("HIGH_RISK_OPERATION", "op-1001"), "1001");
        RuntimeControlIncidentCloseReqVO closeReqVO = closeReq();
        closeReqVO.setPostmortemStatus(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> incidentService.closeIncident(incident.getId(), closeReqVO, "1003"));

        assertTrue(exception.getMessage().contains("复盘"));
    }

    @Test
    void closeIncidentShouldRequireAllGatesAndRecordCloser() {
        responsibilityService.createOwner(owner());
        RuntimeControlIncidentRespVO incident = incidentService.createIncident(createReq("DIRECT", null), "1001");

        RuntimeControlIncidentRespVO closed = incidentService.closeIncident(incident.getId(), closeReq(), "1003");

        assertEquals("CLOSED", closed.getStatus());
        assertEquals("1003", closed.getClosedBy());
        assertEquals("PASSED", closed.getVerificationResult());
        assertEquals("DONE", closed.getPostmortemStatus());
        PageResult<RuntimeControlIncidentRespVO> page =
                incidentService.getIncidentsPage(new RuntimeControlIncidentPageReqVO());
        assertEquals(1, page.getTotal());
    }

    private RuntimeControlIncidentCreateReqVO createReq(String sourceType, String sourceId) {
        return createReq(sourceType, sourceId, "storage-capacity-warning");
    }

    private RuntimeControlIncidentCreateReqVO createReq(String sourceType, String sourceId, String action) {
        RuntimeControlIncidentCreateReqVO reqVO = new RuntimeControlIncidentCreateReqVO();
        reqVO.setEnvironment("prod");
        reqVO.setAction(action);
        reqVO.setSeverity("WARN");
        reqVO.setTitle("日志磁盘容量异常");
        reqVO.setDescription("日志目录超过阈值");
        reqVO.setSourceType(sourceType);
        reqVO.setSourceId(sourceId);
        return reqVO;
    }

    private RuntimeControlIncidentCloseReqVO closeReq() {
        RuntimeControlIncidentCloseReqVO reqVO = new RuntimeControlIncidentCloseReqVO();
        reqVO.setOwnerGateResult("PASSED");
        reqVO.setVerificationResult("PASSED");
        reqVO.setRemainingRisk("已确认无剩余高风险");
        reqVO.setPostmortemStatus("DONE");
        reqVO.setCloseReason("容量恢复且复盘完成");
        return reqVO;
    }

    private RuntimeControlOwnerMatrixSaveReqVO owner() {
        RuntimeControlOwnerMatrixSaveReqVO reqVO = new RuntimeControlOwnerMatrixSaveReqVO();
        reqVO.setEnvironment("prod");
        reqVO.setAction("storage-capacity-warning");
        reqVO.setRole("incident-owner");
        reqVO.setRequired(true);
        reqVO.setOwnerUserId(1001L);
        reqVO.setOwnerName("owner-1001");
        return reqVO;
    }
}
