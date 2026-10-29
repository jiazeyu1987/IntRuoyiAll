package cn.iocoder.yudao.module.system.service.permission;

public interface RoleConfigPackageService {

    byte[] exportPackage(java.util.List<Long> categoryIds);

    void importPackage(byte[] content);

}
