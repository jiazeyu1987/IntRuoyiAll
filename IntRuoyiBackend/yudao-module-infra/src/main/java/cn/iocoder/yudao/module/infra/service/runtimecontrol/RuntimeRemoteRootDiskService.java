package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRemoteRootCleanupReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRemoteRootCleanupRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRemoteRootDiskStatusRespVO;

public interface RuntimeRemoteRootDiskService {

    RuntimeControlRemoteRootDiskStatusRespVO getStatus(String targetEnvironment);

    RuntimeControlRemoteRootCleanupRespVO cleanup(RuntimeControlRemoteRootCleanupReqVO reqVO, String loginUserId);
}
