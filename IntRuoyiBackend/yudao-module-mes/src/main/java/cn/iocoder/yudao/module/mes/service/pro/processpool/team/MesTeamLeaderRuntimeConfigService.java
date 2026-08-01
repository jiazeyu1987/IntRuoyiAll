package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

public interface MesTeamLeaderRuntimeConfigService {

    Long createEmployee(MesTeamEmployeeProfileSaveReqBO reqBO);

    Long bindEmployeeToProcess(MesTeamProcessEmployeeBindingSaveReqBO reqBO);

    Long createDevice(MesTeamDeviceSaveReqBO reqBO);

    void updateDeviceStatus(MesTeamDeviceStatusUpdateReqBO reqBO);

    Long bindDeviceToProcess(MesTeamProcessDeviceBindingSaveReqBO reqBO);

    Long saveDeviceParameterRule(MesTeamDeviceParameterRuleSaveReqBO reqBO);

    Long saveProcessDefectReason(MesTeamProcessDefectReasonSaveReqBO reqBO);
}
