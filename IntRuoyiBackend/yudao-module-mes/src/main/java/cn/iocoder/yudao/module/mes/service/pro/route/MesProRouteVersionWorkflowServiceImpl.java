package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionBlockerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionCreateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_ACTIVE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_APPROVAL_PROCESS_NOT_STARTED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE;

/**
 * 工艺路线版本候选工作流 Service 实现。
 */
@Service
@Validated
public class MesProRouteVersionWorkflowServiceImpl implements MesProRouteVersionWorkflowService {

    public static final String ROUTE_VERSION_APPROVAL_PROCESS_DEFINITION_KEY = "mes-route-version-approval-v1";

    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Resource
    private MesProRouteService routeService;
    @Resource
    private MesProRouteControlledContentAdapter platformAdapter;

    @Override
    public List<MesProRouteVersionDO> listByRouteId(Long routeId) {
        return routeVersionMapper.selectListByRouteId(routeId);
    }

    @Override
    public MesProRouteVersionDO getVersion(Long id) {
        MesProRouteVersionDO version = routeVersionMapper.selectById(id);
        if (version == null) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, id);
        }
        return version;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProRouteVersionDO createCandidate(MesProRouteVersionCreateReqVO reqVO) {
        MesProRouteVersionDO active = routeVersionMapper.selectActiveByRouteIdForUpdate(reqVO.getRouteId());
        if (active == null) {
            throw exception(PRO_ROUTE_VERSION_ACTIVE_NOT_EXISTS, reqVO.getRouteId());
        }
        if (reqVO.getSourceRouteVersionId() != null
                && !Objects.equals(reqVO.getSourceRouteVersionId(), active.getId())) {
            throw exception(PRO_ROUTE_VERSION_CONFLICT,
                    reqVO.getRouteId(), reqVO.getSourceRouteVersionId(), active.getId());
        }
        MesProRouteVersionDO openCandidate = routeVersionMapper.selectOpenCandidateByRouteId(reqVO.getRouteId());
        if (openCandidate != null) {
            if (MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(openCandidate.getLifecycleStatus())) {
                if (!Objects.equals(openCandidate.getSourceRouteVersionId(), active.getId())) {
                    throw exception(PRO_ROUTE_VERSION_CONFLICT,
                            reqVO.getRouteId(), openCandidate.getSourceRouteVersionId(), active.getId());
                }
                return openCandidate;
            }
            throw exception(PRO_ROUTE_VERSION_CONFLICT,
                    reqVO.getRouteId(), openCandidate.getId(), openCandidate.getLifecycleStatus());
        }
        String routeSnapshotJson = routeService.buildCurrentRouteSnapshotJson(reqVO.getRouteId(), active.getId());
        if (!MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(routeSnapshotJson)) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, active.getId());
        }
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .routeId(reqVO.getRouteId())
                .versionNo(nextVersionNo(reqVO.getRouteId()))
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .sourceRouteVersionId(active.getId())
                .routeSnapshotJson(routeSnapshotJson)
                .changeSummaryJson(buildChangeSummary(reqVO.getChangeReason()))
                .remark("工艺路线候选版本，发布后生效")
                .build();
        routeVersionMapper.insert(candidate);
        platformAdapter.recordCandidateCreated(active, candidate, SecurityFrameworkUtils.getLoginUserId(),
                reqVO.getChangeReason());
        return candidate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProRouteVersionDO submitCandidate(Long id) {
        MesProRouteVersionDO candidate = getVersion(id);
        if (Boolean.TRUE.equals(candidate.getActive())
                || !MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(candidate.getLifecycleStatus())) {
            throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                    candidate.getId(), candidate.getLifecycleStatus());
        }
        Long openCandidateCount = routeVersionMapper.countOpenCandidatesByRouteId(candidate.getRouteId());
        if (openCandidateCount != null && openCandidateCount > 1) {
            throw exception(PRO_ROUTE_VERSION_CONFLICT,
                    candidate.getRouteId(), candidate.getId(), openCandidateCount);
        }
        MesProRouteVersionBlockerRespVO blockers = getPublishBlockers(id);
        if (!Boolean.TRUE.equals(blockers.getPublishable())) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, candidate.getId());
        }
        Long submitterUserId = requireLoginUserId();
        LocalDateTime submittedTime = LocalDateTime.now();
        platformAdapter.recordSubmitted(candidate, submitterUserId, null);
        candidate.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL);
        candidate.setSubmittedBy(submitterUserId);
        candidate.setSubmittedTime(submittedTime);
        candidate.setApprovalProcessInstanceId(null);

        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(candidate.getId());
        update.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH);
        update.setSubmittedBy(submitterUserId);
        update.setSubmittedTime(submittedTime);
        update.setApprovalProcessInstanceId(null);
        routeVersionMapper.updateById(update);
        candidate.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH);
        candidate.setApprovalProcessInstanceId(null);
        platformAdapter.recordApproved(candidate, submitterUserId,
                "ROUTE_VERSION_READY_TO_PUBLISH:" + candidate.getId() + ":" + submittedTime);
        return candidate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProRouteVersionDO withdrawCandidate(Long id) {
        MesProRouteVersionDO candidate = getVersion(id);
        if (Boolean.TRUE.equals(candidate.getActive())
                || !MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL.equals(candidate.getLifecycleStatus())) {
            throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                    candidate.getId(), candidate.getLifecycleStatus());
        }
        Long operatorUserId = requireLoginUserId();
        if (StrUtil.isBlank(candidate.getApprovalProcessInstanceId())) {
            throw exception(PRO_ROUTE_VERSION_APPROVAL_PROCESS_NOT_STARTED,
                    ROUTE_VERSION_APPROVAL_PROCESS_DEFINITION_KEY);
        }
        bpmProcessInstanceApi.cancelProcessInstance(operatorUserId, candidate.getApprovalProcessInstanceId(),
                "route version approval withdraw: routeVersionId=" + candidate.getId());
        candidate.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT);
        candidate.setSubmittedBy(null);
        candidate.setSubmittedTime(null);
        candidate.setApprovalProcessInstanceId(null);
        return candidate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProRouteVersionDO reopenRejectedCandidate(Long id) {
        MesProRouteVersionDO candidate = getVersion(id);
        if (Boolean.TRUE.equals(candidate.getActive())
                || !MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED.equals(candidate.getLifecycleStatus())) {
            throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                    candidate.getId(), candidate.getLifecycleStatus());
        }
        Long openCandidateCount = routeVersionMapper.countOpenCandidatesByRouteId(candidate.getRouteId());
        if (openCandidateCount != null && openCandidateCount > 0) {
            throw exception(PRO_ROUTE_VERSION_CONFLICT,
                    candidate.getRouteId(), candidate.getId(), openCandidateCount);
        }
        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(candidate.getId());
        update.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT);
        routeVersionMapper.updateById(update);
        candidate.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT);
        return candidate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProRouteVersionDO cancelCandidate(Long id) {
        MesProRouteVersionDO candidate = getVersion(id);
        if (Boolean.TRUE.equals(candidate.getActive())
                || !(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(candidate.getLifecycleStatus())
                || MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH.equals(candidate.getLifecycleStatus())
                || MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED.equals(candidate.getLifecycleStatus()))) {
            throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                    candidate.getId(), candidate.getLifecycleStatus());
        }
        Long operatorUserId = requireLoginUserId();
        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(candidate.getId());
        update.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_CANCELLED);
        routeVersionMapper.updateById(update);
        platformAdapter.recordCancelled(candidate, operatorUserId);
        candidate.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_CANCELLED);
        return candidate;
    }

    @Override
    public MesProRouteVersionBlockerRespVO getPublishBlockers(Long id) {
        MesProRouteVersionDO candidate = getVersion(id);
        List<String> blockers = new ArrayList<>();
        if (Boolean.TRUE.equals(candidate.getActive())) {
            blockers.add("candidate is already active");
        }
        if (!(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(candidate.getLifecycleStatus())
                || MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH.equals(candidate.getLifecycleStatus()))) {
            blockers.add("candidate status is not publishable: " + candidate.getLifecycleStatus());
        }
        MesProRouteVersionDO active = routeVersionMapper.selectActiveByRouteId(candidate.getRouteId());
        if (active == null) {
            blockers.add("active route version does not exist");
        } else if (!Objects.equals(candidate.getSourceRouteVersionId(), active.getId())) {
            blockers.add("source active version drifted");
        }
        if (!MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(candidate.getRouteSnapshotJson())) {
            blockers.add("route version snapshot is incomplete");
        }
        MesProRouteVersionBlockerRespVO respVO = new MesProRouteVersionBlockerRespVO();
        respVO.setRouteVersionId(candidate.getId());
        respVO.setBlockers(blockers);
        respVO.setPublishable(blockers.isEmpty());
        return respVO;
    }

    private String nextVersionNo(Long routeId) {
        String maxVersionNo = routeVersionMapper.selectMaxVersionNoByRouteId(routeId);
        if (StrUtil.isBlank(maxVersionNo)) {
            return "V1";
        }
        String normalized = maxVersionNo.trim().toUpperCase();
        if (!normalized.matches("V\\d+")) {
            throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE, routeId, maxVersionNo);
        }
        return "V" + (Integer.parseInt(normalized.substring(1)) + 1);
    }

    private String buildChangeSummary(String changeReason) {
        JSONObject summary = new JSONObject(true);
        summary.put("changeReason", StrUtil.blankToDefault(changeReason, "工艺路线候选版本"));
        return summary.toJSONString();
    }

    private Long requireLoginUserId() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw new IllegalStateException("route version submitter is required");
        }
        return loginUserId;
    }

}
