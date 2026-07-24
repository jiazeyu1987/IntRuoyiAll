package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlCapacityStatusRespVO;

public interface RuntimeStorageGuardService {

    RuntimeControlCapacityStatusRespVO getCapacityStatus();

    RuntimeControlCapacityStatusRespVO refreshCapacityStatus();
}
