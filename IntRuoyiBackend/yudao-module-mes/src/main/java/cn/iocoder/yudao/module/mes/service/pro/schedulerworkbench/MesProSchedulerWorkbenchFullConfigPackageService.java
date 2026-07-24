package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchFullConfigImportRespVO;

public interface MesProSchedulerWorkbenchFullConfigPackageService {

    byte[] exportPackage();

    MesProSchedulerWorkbenchFullConfigImportRespVO importPackage(byte[] content);
}
