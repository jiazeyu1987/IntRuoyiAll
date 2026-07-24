package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchSmokeTestStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchSmokeTestStatusRespVO;

public interface MesProSchedulerWorkbenchSmokeTestService {

    MesProSchedulerWorkbenchSmokeTestStatusRespVO getStatus();

    MesProSchedulerWorkbenchSmokeTestStatusRespVO start(MesProSchedulerWorkbenchSmokeTestStartReqVO reqVO);

    MesProSchedulerWorkbenchSmokeTestStatusRespVO stop();

}
