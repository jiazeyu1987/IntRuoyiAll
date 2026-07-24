package cn.iocoder.yudao.module.dcc.service.position;

public interface DccApprovalPositionConfigPackageService {

    byte[] exportPackage();

    void importPackage(byte[] content);

}
