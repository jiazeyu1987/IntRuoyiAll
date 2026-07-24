package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionPreviewRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlLogRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOverviewRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleasePackageRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleaseStatusRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRestartReqVO;

import java.util.List;

public interface RuntimeControlService {

    RuntimeControlOverviewRespVO getOverview();

    RuntimeControlOperationRespVO restart(RuntimeControlRestartReqVO reqVO, String requestedBy);

    RuntimeControlOperationRespVO executeAction(RuntimeControlActionReqVO reqVO, String requestedBy);

    RuntimeControlActionPreviewRespVO previewAction(RuntimeControlActionReqVO reqVO, String requestedBy);

    RuntimeControlLogRespVO getOperationLog(String operationId, Integer maxBytes);

    List<RuntimeControlOperationRespVO> getOperations();

    List<RuntimeControlReleasePackageRespVO> getReleasePackages();

    RuntimeControlReleaseStatusRespVO getReleaseStatus();
}
