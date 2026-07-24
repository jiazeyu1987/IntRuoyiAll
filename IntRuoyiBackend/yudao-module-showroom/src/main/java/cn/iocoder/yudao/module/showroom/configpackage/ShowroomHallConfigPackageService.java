package cn.iocoder.yudao.module.showroom.configpackage;

import cn.iocoder.yudao.module.showroom.controller.admin.vo.hall.ShowroomHallConfigPackageImportRespVO;

public interface ShowroomHallConfigPackageService {

    byte[] exportPackage();

    ShowroomHallConfigPackageImportRespVO importPackage(byte[] content);

}
