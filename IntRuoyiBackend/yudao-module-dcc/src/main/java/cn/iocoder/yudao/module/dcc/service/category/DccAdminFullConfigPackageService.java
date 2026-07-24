package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccAdminFullConfigPackageImportRespVO;

public interface DccAdminFullConfigPackageService {

    byte[] exportPackage();

    DccAdminFullConfigPackageImportRespVO importPackage(byte[] content);
}
