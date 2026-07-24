package cn.iocoder.yudao.module.system.service.configpackage;

import cn.iocoder.yudao.module.system.controller.admin.configpackage.vo.SystemConfigPackageImportRespVO;
import cn.iocoder.yudao.module.system.controller.admin.configpackage.vo.SystemConfigPackagePrecheckRespVO;

import java.util.Collection;

public interface SystemConfigPackageService {

    byte[] exportPackage();

    SystemConfigPackagePrecheckRespVO precheck(byte[] content, Collection<String> availableComponents);

    SystemConfigPackageImportRespVO importPackage(byte[] content, Boolean confirmed,
                                                  String targetSnapshotSha256,
                                                  Collection<String> availableComponents);
}
