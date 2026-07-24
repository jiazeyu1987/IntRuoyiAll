package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchRouteConfigImportRespVO;

public interface MesProSchedulerWorkbenchRouteConfigPackageService {

    byte[] exportPackage();

    MesProSchedulerWorkbenchRouteConfigImportRespVO importPackage(byte[] content);

}
