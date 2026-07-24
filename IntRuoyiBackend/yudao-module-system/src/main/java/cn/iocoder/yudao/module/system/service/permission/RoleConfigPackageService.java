package cn.iocoder.yudao.module.system.service.permission;

public interface RoleConfigPackageService {

    byte[] exportPackage();

    void importPackage(byte[] content);

}
