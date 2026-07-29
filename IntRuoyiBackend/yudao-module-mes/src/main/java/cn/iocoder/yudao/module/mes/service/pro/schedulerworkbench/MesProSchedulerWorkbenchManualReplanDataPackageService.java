package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchManualReplanDataImportRespVO;

public interface MesProSchedulerWorkbenchManualReplanDataPackageService {

    byte[] exportPackage();

    MesProSchedulerWorkbenchManualReplanDataImportRespVO importPackage(byte[] content);
}
