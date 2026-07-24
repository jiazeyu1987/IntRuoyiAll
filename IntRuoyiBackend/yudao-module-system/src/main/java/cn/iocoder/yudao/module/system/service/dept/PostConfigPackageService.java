package cn.iocoder.yudao.module.system.service.dept;

public interface PostConfigPackageService {

    byte[] exportPackage();

    void importPackage(byte[] content);

}
