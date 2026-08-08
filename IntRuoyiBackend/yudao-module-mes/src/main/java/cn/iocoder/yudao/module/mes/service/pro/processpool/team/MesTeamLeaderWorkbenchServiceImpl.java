package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderSubmissionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineEventRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED;

@Service
@Validated
public class MesTeamLeaderWorkbenchServiceImpl implements MesTeamLeaderWorkbenchService {

    private final MesTeamLeaderScopeService scopeService;
    private final ProcessPoolTimelineService timelineService;

    public MesTeamLeaderWorkbenchServiceImpl(MesTeamLeaderScopeService scopeService,
                                             ProcessPoolTimelineService timelineService) {
        this.scopeService = scopeService;
        this.timelineService = timelineService;
    }

    @Override
    public PageResult<ProcessPoolTimelineEventRespVO> getSubmissionPage(Long leaderUserId, String leaderType,
                                                                        MesTeamLeaderSubmissionPageReqVO reqVO) {
        validateLeaderContext(leaderUserId, leaderType);
        if (reqVO == null) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED, "submissionPage");
        }
        Set<Long> responsibleEmployeeIds = scopeService.listResponsibleEmployeeIds(leaderUserId, leaderType);
        if (reqVO.getEmployeeUserId() != null) {
            scopeService.assertCanAccessEmployee(leaderUserId, leaderType, reqVO.getEmployeeUserId());
        }
        if (responsibleEmployeeIds.isEmpty()) {
            return PageResult.empty();
        }
        reqVO.setEmployeeUserIds(responsibleEmployeeIds);
        if (MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC.equals(leaderType)) {
            reqVO.setEventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION);
        }
        return timelineService.getTimelinePage(reqVO);
    }

    @Override
    public ProcessPoolTimelineDetailRespVO getSubmissionDetail(Long leaderUserId, String leaderType, Long eventId) {
        validateLeaderContext(leaderUserId, leaderType);
        ProcessPoolTimelineDetailRespVO detail = timelineService.getTimelineDetail(eventId);
        scopeService.assertCanAccessEmployee(leaderUserId, leaderType, detail.getActualEmployeeUserId());
        return detail;
    }

    private static void validateLeaderContext(Long leaderUserId, String leaderType) {
        if (leaderUserId == null || isBlank(leaderType)) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED, "leaderUserId/leaderType");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
