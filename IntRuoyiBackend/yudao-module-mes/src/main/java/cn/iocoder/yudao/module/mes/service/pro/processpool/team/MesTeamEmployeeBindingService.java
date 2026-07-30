package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

public interface MesTeamEmployeeBindingService {

    Long addEmployeeBinding(MesTeamEmployeeBindingSaveReqBO reqBO);

    void disableEmployeeBinding(MesTeamEmployeeBindingDisableReqBO reqBO);
}
