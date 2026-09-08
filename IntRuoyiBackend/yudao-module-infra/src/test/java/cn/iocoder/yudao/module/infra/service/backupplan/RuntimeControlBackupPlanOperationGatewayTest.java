package cn.iocoder.yudao.module.infra.service.backupplan;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionPreviewRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlLogRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOverviewRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleasePackageRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleaseStatusRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRestartReqVO;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RuntimeControlBackupPlanOperationGatewayTest {

    @Test
    void backupNowShouldPassConfiguredTargetWithoutHardcodedProdConfirmation() {
        RuntimeControlBackupPlanOperationGateway gateway = new RuntimeControlBackupPlanOperationGateway();
        FakeRuntimeControlService runtimeControlService = new FakeRuntimeControlService();
        ReflectionTestUtils.setField(gateway, "runtimeControlService", runtimeControlService);

        RuntimeControlOperationRespVO result = gateway.backupNow(7L, "FULL", "test");

        assertEquals("backup-now", result.getAction());
        assertEquals("7", runtimeControlService.requestedBy);
        assertEquals("backup-now", runtimeControlService.reqVO.getAction());
        assertEquals("FULL", runtimeControlService.reqVO.getBackupKind());
        assertEquals("test", runtimeControlService.reqVO.getTargetEnvironment());
        assertNull(runtimeControlService.reqVO.getProdConfirmText());
    }

    private static class FakeRuntimeControlService implements RuntimeControlService {

        private RuntimeControlActionReqVO reqVO;
        private String requestedBy;

        @Override
        public RuntimeControlOverviewRespVO getOverview() {
            throw new UnsupportedOperationException();
        }

        @Override
        public RuntimeControlOperationRespVO restart(RuntimeControlRestartReqVO reqVO, String requestedBy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RuntimeControlOperationRespVO executeAction(RuntimeControlActionReqVO reqVO, String requestedBy) {
            this.reqVO = reqVO;
            this.requestedBy = requestedBy;
            RuntimeControlOperationRespVO operation = new RuntimeControlOperationRespVO();
            operation.setAction(reqVO.getAction());
            return operation;
        }

        @Override
        public RuntimeControlActionPreviewRespVO previewAction(RuntimeControlActionReqVO reqVO, String requestedBy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RuntimeControlLogRespVO getOperationLog(String operationId, Integer maxBytes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RuntimeControlOperationRespVO> getOperations() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RuntimeControlReleasePackageRespVO> getReleasePackages() {
            throw new UnsupportedOperationException();
        }

        @Override
        public RuntimeControlReleaseStatusRespVO getReleaseStatus() {
            throw new UnsupportedOperationException();
        }
    }
}
