package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchRecordFormPermissionRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrProcessFormPermissionRuleRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrProcessFormPermissionRuleSaveReqVO;

public interface MesProEdhrProcessFormPermissionRuleService {

    MesProEdhrProcessFormPermissionRuleRespVO getRule(Long routeProcessId, String batchRecordReportId);

    MesProEdhrProcessFormPermissionRuleRespVO getRuleByReport(String batchRecordReportId);

    MesProEdhrProcessFormPermissionRuleRespVO saveRule(MesProEdhrProcessFormPermissionRuleSaveReqVO reqVO);

    MesProEdhrProcessFormPermissionRuleRespVO saveRuleByReport(MesProEdhrBatchRecordFormPermissionRuleSaveReqVO reqVO);
}
