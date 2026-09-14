package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDefectReasonDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDefectReasonMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_DEFECT_REASON_REQUIRED;

@Service
@Validated
public class MesDefectReasonCatalogServiceImpl implements MesDefectReasonCatalogService {

    private final MesTeamLeaderScopeService scopeService;
    private final MesProcessPoolDefectReasonMapper reasonMapper;
    private final MesProcessPoolTeamMaintenanceAuditMapper auditMapper;

    public MesDefectReasonCatalogServiceImpl(MesTeamLeaderScopeService scopeService,
                                             MesProcessPoolDefectReasonMapper reasonMapper,
                                             MesProcessPoolTeamMaintenanceAuditMapper auditMapper) {
        this.scopeService = scopeService;
        this.reasonMapper = reasonMapper;
        this.auditMapper = auditMapper;
    }

    @Override
    public Long createReason(MesDefectReasonSaveReqBO reqBO) {
        validateReq(reqBO);
        if (reqBO.getProcessId() != null) {
            scopeService.assertCanMaintainProcess(reqBO.getLeaderUserId(), reqBO.getProcessId());
        }
        MesProcessPoolDefectReasonDO reason = MesProcessPoolDefectReasonDO.builder()
                .leaderUserId(reqBO.getLeaderUserId())
                .routeProcessId(reqBO.getRouteProcessId())
                .processId(reqBO.getProcessId())
                .reasonType(reqBO.getReasonType())
                .reasonCode(reqBO.getReasonCode())
                .reasonName(reqBO.getReasonName())
                .enabled(Boolean.TRUE)
                .build();
        reasonMapper.insert(reason);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "CREATE_DEFECT_REASON",
                "DEFECT_REASON", reason.getId(), null, reason.toString());
        return reason.getId();
    }

    private static void validateReq(MesDefectReasonSaveReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || isBlank(reqBO.getReasonType())
                || isBlank(reqBO.getReasonCode()) || isBlank(reqBO.getReasonName())) {
            throw exception(PRO_PROCESS_POOL_DEFECT_REASON_REQUIRED, "reasonType/reasonCode/reasonName");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
