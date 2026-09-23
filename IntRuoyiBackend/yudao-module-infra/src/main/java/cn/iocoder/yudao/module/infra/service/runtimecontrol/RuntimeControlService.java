package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionPreviewRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlLogRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOverviewRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleasePackageRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleaseStatusRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRestartReqVO;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.ReleaseWorkflowRecord;

import java.util.List;
import java.util.Optional;

public interface RuntimeControlService {

    RuntimeControlOverviewRespVO getOverview();

    RuntimeControlOperationRespVO restart(RuntimeControlRestartReqVO reqVO, String requestedBy);

    RuntimeControlOperationRespVO executeAction(RuntimeControlActionReqVO reqVO, String requestedBy);

    /** Internal workflow dispatcher; no controller exposes this entry point directly. */
    RuntimeControlOperationRespVO dispatchWorkflowAction(RuntimeControlActionReqVO reqVO, String requestedBy);

    Optional<RuntimeControlBackupPublicationReceipt> inspectBackupReceipt(ReleaseWorkflowRecord workflow);

    RuntimeControlBackupPublicationReceipt acknowledgeBackupReceipt(ReleaseWorkflowRecord workflow,
            RuntimeControlBackupPublicationReceipt receipt, String decisionDigest);

    void completeBackupConfirmation(ReleaseWorkflowRecord workflow, RuntimeControlBackupPublicationReceipt confirmedReceipt);

    void validateBackupPublishPrerequisites();

    boolean isOperationExecutorAlive(String operationId);

    boolean cancelOperation(String operationId);

    void rejectLegacyProductionAction(RuntimeControlActionReqVO reqVO);

    RuntimeControlActionPreviewRespVO previewAction(RuntimeControlActionReqVO reqVO, String requestedBy);

    RuntimeControlLogRespVO getOperationLog(String operationId, Integer maxBytes);

    List<RuntimeControlOperationRespVO> getOperations();

    List<RuntimeControlReleasePackageRespVO> getReleasePackages();

    Optional<RuntimeControlReleasePackageRespVO> getReleasePackage(String releaseTag);

    RuntimeControlReleaseStatusRespVO getReleaseStatus();
}
