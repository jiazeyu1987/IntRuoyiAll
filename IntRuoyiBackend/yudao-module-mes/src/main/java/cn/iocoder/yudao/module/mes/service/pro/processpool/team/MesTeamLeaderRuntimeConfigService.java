package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;

import java.util.List;

public interface MesTeamLeaderRuntimeConfigService {

    List<MesTeamFormalUserCandidateBO> searchFormalUserCandidates(Long leaderUserId, String keyword);

    List<MesProcessPoolTeamEmployeeProfileDO> listEmployeeProfiles(Long leaderUserId, Boolean enabled);

    Long createTemporaryEmployee(MesTeamTemporaryEmployeeCreateReqBO reqBO);

    Long linkFormalEmployee(MesTeamFormalEmployeeLinkReqBO reqBO);

    void renameEmployee(MesTeamEmployeeDisplayNameUpdateReqBO reqBO);

    void updateEmployeeEnabled(MesTeamEmployeeStatusUpdateReqBO reqBO);

    void resetTemporaryEmployeeSignaturePassword(MesTeamTempSignaturePasswordResetReqBO reqBO);

    List<MesProcessPoolTeamMaintenanceAuditDO> listEmployeeAuditRecords(Long leaderUserId, Long employeeProfileId);

    Long createEmployee(MesTeamEmployeeProfileSaveReqBO reqBO);

    Long createDevice(MesTeamDeviceSaveReqBO reqBO);

    void updateDeviceStatus(MesTeamDeviceStatusUpdateReqBO reqBO);

    Long bindDeviceToProcess(MesTeamProcessDeviceBindingSaveReqBO reqBO);

    Long saveDeviceParameterRule(MesTeamDeviceParameterRuleSaveReqBO reqBO);

    Long saveProcessDefectReason(MesTeamProcessDefectReasonSaveReqBO reqBO);
}
