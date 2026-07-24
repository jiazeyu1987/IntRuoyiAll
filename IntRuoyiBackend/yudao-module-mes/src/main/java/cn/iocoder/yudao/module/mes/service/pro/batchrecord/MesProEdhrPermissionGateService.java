package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

public interface MesProEdhrPermissionGateService {

    void requireAbility(MesProEdhrPermissionGateCommand command);
}
