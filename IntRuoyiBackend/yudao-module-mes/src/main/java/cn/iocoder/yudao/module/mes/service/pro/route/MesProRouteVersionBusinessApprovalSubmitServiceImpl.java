package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalOrchestrator;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE;

@Service
@Validated
public class MesProRouteVersionBusinessApprovalSubmitServiceImpl
        implements MesProRouteVersionBusinessApprovalSubmitService {

    private static final String DATA_DOMAIN = "MES";
    private static final String SYSTEM_CODE = "MES";
    private static final String OBJECT_TYPE = "ROUTE_VERSION";
    private static final String ACTION_CODE = "PUBLISH";

    @Resource
    private MesProRouteVersionWorkflowService workflowService;
    @Resource
    private BusinessApprovalOrchestrator businessApprovalOrchestrator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProRouteVersionDO submitAndPublishCandidate(Long routeVersionId) {
        MesProRouteVersionDO candidate = workflowService.getVersion(routeVersionId);
        if (isAlreadyActive(candidate)) {
            return candidate;
        }
        requireDraftCandidate(candidate);
        businessApprovalOrchestrator.submit(buildPublishContext(candidate, requireApplicantUserId()));
        return workflowService.getVersion(routeVersionId);
    }

    private BusinessApprovalContext buildPublishContext(MesProRouteVersionDO candidate, Long applicantUserId) {
        return BusinessApprovalContext.builder()
                .tenantId(TenantContextHolder.getRequiredTenantId())
                .dataDomain(DATA_DOMAIN)
                .systemCode(SYSTEM_CODE)
                .objectType(OBJECT_TYPE)
                .objectId(String.valueOf(candidate.getId()))
                .objectVersion(candidate.getVersionNo())
                .actionCode(ACTION_CODE)
                .objectState(candidate.getLifecycleStatus())
                .applicantUserId(applicantUserId)
                .reason("publish route version")
                .variables(buildPublishVariables(candidate))
                .build();
    }

    private Map<String, Object> buildPublishVariables(MesProRouteVersionDO candidate) {
        JSONObject snapshot = parseRouteSnapshot(candidate);
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("routeId", requireLong(candidate.getRouteId(),
                "route version publish routeId is required"));
        variables.put("routeVersionId", requireLong(candidate.getId(),
                "route version publish routeVersionId is required"));
        variables.put("routeVersionNo", requireText(candidate.getVersionNo(),
                "route version publish versionNo is required"));
        variables.put("routeCode", requireText(snapshot.getString("routeCode"),
                "route version publish routeCode is required in snapshot"));
        variables.put("routeName", requireText(snapshot.getString("routeName"),
                "route version publish routeName is required in snapshot"));
        return variables;
    }

    private JSONObject parseRouteSnapshot(MesProRouteVersionDO candidate) {
        String routeSnapshotJson = candidate.getRouteSnapshotJson();
        if (routeSnapshotJson == null || routeSnapshotJson.isBlank()) {
            throw new IllegalStateException("route version publish snapshot is required: routeVersionId="
                    + candidate.getId());
        }
        try {
            JSONObject snapshot = JSON.parseObject(routeSnapshotJson);
            if (snapshot == null) {
                throw new IllegalStateException("route version publish snapshot is empty: routeVersionId="
                        + candidate.getId());
            }
            return snapshot;
        } catch (JSONException ex) {
            throw new IllegalStateException("route version publish snapshot is invalid: routeVersionId="
                    + candidate.getId(), ex);
        }
    }

    private void requireDraftCandidate(MesProRouteVersionDO candidate) {
        if (!MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(candidate.getLifecycleStatus())) {
            throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                    candidate.getId(), candidate.getLifecycleStatus());
        }
    }

    private boolean isAlreadyActive(MesProRouteVersionDO candidate) {
        return Boolean.TRUE.equals(candidate.getActive())
                && MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE.equals(candidate.getLifecycleStatus());
    }

    private Long requireApplicantUserId() {
        Long applicantUserId = SecurityFrameworkUtils.getLoginUserId();
        if (applicantUserId == null) {
            throw new IllegalStateException("route version publish applicant is required");
        }
        return applicantUserId;
    }

    private Long requireLong(Long value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }

}
