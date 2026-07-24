package cn.iocoder.yudao.module.showroom.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter.ShowroomVersionCenterDetailRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter.ShowroomVersionCenterHistoryRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter.ShowroomVersionCenterRepublishReqVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter.ShowroomVersionCenterRepublishRespVO;
import cn.iocoder.yudao.module.showroom.release.ShowroomVersionBundleService;
import cn.iocoder.yudao.module.showroom.release.ShowroomVersionCenterService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomAssignmentService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomWorkflowFacade;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/showroom/version-center")
@Validated
public class ShowroomVersionCenterController {

    private static final Logger log = LoggerFactory.getLogger(ShowroomVersionCenterController.class);

    private final ShowroomVersionCenterService versionCenterService;
    private final ShowroomAssignmentService assignmentService;
    private final ShowroomWorkflowFacade workflowFacade;
    private final SecurityFrameworkService securityFrameworkService;

    public ShowroomVersionCenterController(ShowroomVersionCenterService versionCenterService,
                                           ShowroomAssignmentService assignmentService,
                                           ShowroomWorkflowFacade workflowFacade,
                                           SecurityFrameworkService securityFrameworkService) {
        this.versionCenterService = versionCenterService;
        this.assignmentService = assignmentService;
        this.workflowFacade = workflowFacade;
        this.securityFrameworkService = securityFrameworkService;
    }

    @GetMapping("/history")
    public CommonResult<ShowroomVersionCenterHistoryRespVO> getHistory(@RequestParam("targetType") String targetType,
                                                                       @RequestParam("targetId") Long targetId,
                                                                       @RequestParam("siteKey") String siteKey,
                                                                       @RequestParam("stage") String stage) {
        long startedAt = System.nanoTime();
        Long operatorUserId = requireOperatorUserId();
        requireReadAccess(operatorUserId, targetType, targetId);
        try {
            ShowroomVersionCenterHistoryRespVO response = versionCenterService.getHistory(targetType, targetId,
                    siteKey, stage);
            log.info("SHOWROOM_VERSION_CENTER_HISTORY targetType={} targetId={} sourceRevisionId={} selectedRevisionId={} currentContentRevisionId={} currentPublicRevisionId={} releaseId={} operatorUserId={} durationMs={}",
                    targetType, targetId, null, null, response.currentContentRevisionId(), response.currentPublicRevisionId(),
                    response.currentReleaseId(), operatorUserId, durationMs(startedAt));
            return success(response);
        } catch (RuntimeException exception) {
            log.warn("SHOWROOM_VERSION_CENTER_BLOCKER targetType={} targetId={} sourceRevisionId={} selectedRevisionId={} currentContentRevisionId={} currentPublicRevisionId={} releaseId={} operatorUserId={} durationMs={} blockerScope={} blockerCode={}",
                    targetType, targetId, null, null, null, null, null, operatorUserId, durationMs(startedAt),
                    "SELECTED_VERSION", blockerCode(exception));
            throw exception;
        }
    }

    @GetMapping("/detail")
    public CommonResult<ShowroomVersionCenterDetailRespVO> getDetail(@RequestParam("targetType") String targetType,
                                                                     @RequestParam("targetId") Long targetId,
                                                                     @RequestParam("revisionId") Long revisionId,
                                                                     @RequestParam("siteKey") String siteKey,
                                                                     @RequestParam("stage") String stage) {
        long startedAt = System.nanoTime();
        Long operatorUserId = requireOperatorUserId();
        requireReadAccess(operatorUserId, targetType, targetId);
        try {
            ShowroomVersionCenterDetailRespVO detail = versionCenterService.getDetail(targetType, targetId,
                    revisionId, siteKey, stage);
            ShowroomVersionCenterDetailRespVO response = detail;
            if (!isShowroomPublicity(operatorUserId)) {
                response = new ShowroomVersionCenterDetailRespVO(
                        detail.targetSummary(),
                        detail.selectedVersion(),
                        detail.currentContentVersion(),
                        detail.currentPublicVersion(),
                        detail.currentRelease(),
                        detail.fieldDiffs(),
                        new ShowroomVersionCenterDetailRespVO.PermissionRespVO(
                                false,
                                "SHOWROOM_VERSION_REPUBLISH_FORBIDDEN: 当前用户无权执行版本重发"
                        ),
                        new ShowroomVersionCenterDetailRespVO.RepublishReadinessRespVO(
                                detail.republishReadiness().ready(),
                                detail.republishReadiness().blockers()
                        )
                );
            }
            log.info("SHOWROOM_VERSION_CENTER_DETAIL targetType={} targetId={} sourceRevisionId={} selectedRevisionId={} currentContentRevisionId={} currentPublicRevisionId={} releaseId={} operatorUserId={} durationMs={}",
                    targetType, targetId, null, revisionId, response.targetSummary().currentContentRevisionId(),
                    response.targetSummary().currentPublicRevisionId(),
                    response.currentRelease() == null ? null : response.currentRelease().releaseId(),
                    operatorUserId, durationMs(startedAt));
            for (ShowroomVersionCenterDetailRespVO.BlockerRespVO blocker : response.republishReadiness().blockers()) {
                log.warn("SHOWROOM_VERSION_CENTER_BLOCKER targetType={} targetId={} sourceRevisionId={} selectedRevisionId={} currentContentRevisionId={} currentPublicRevisionId={} releaseId={} operatorUserId={} durationMs={} blockerScope={} blockerCode={}",
                        targetType, targetId, null, revisionId, response.targetSummary().currentContentRevisionId(),
                        response.targetSummary().currentPublicRevisionId(),
                        response.currentRelease() == null ? null : response.currentRelease().releaseId(),
                        operatorUserId, durationMs(startedAt), blocker.scope(), blocker.blockerCode());
            }
            return success(response);
        } catch (RuntimeException exception) {
            log.warn("SHOWROOM_VERSION_CENTER_BLOCKER targetType={} targetId={} sourceRevisionId={} selectedRevisionId={} currentContentRevisionId={} currentPublicRevisionId={} releaseId={} operatorUserId={} durationMs={} blockerScope={} blockerCode={}",
                    targetType, targetId, null, revisionId, null, null, null, operatorUserId, durationMs(startedAt),
                    "SELECTED_VERSION", blockerCode(exception));
            throw exception;
        }
    }

    @PostMapping("/republish")
    public CommonResult<ShowroomVersionCenterRepublishRespVO> republish(
            @RequestBody ShowroomVersionCenterRepublishReqVO reqVO) {
        long startedAt = System.nanoTime();
        Long operatorUserId = requireOperatorUserId();
        requireRepublishAccess(operatorUserId);
        try {
            ShowroomVersionCenterRepublishRespVO response = versionCenterService.republish(reqVO, operatorUserId);
            log.info("SHOWROOM_VERSION_CENTER_REPUBLISH targetType={} targetId={} sourceRevisionId={} selectedRevisionId={} currentContentRevisionId={} currentPublicRevisionId={} releaseId={} operatorUserId={} durationMs={}",
                    reqVO.targetType(), reqVO.targetId(), reqVO.sourceRevisionId(), reqVO.sourceRevisionId(),
                    response.newRevisionId(), response.newRevisionId(), response.releaseId(), operatorUserId,
                    durationMs(startedAt));
            return success(response);
        } catch (RuntimeException exception) {
            log.warn("SHOWROOM_VERSION_CENTER_BLOCKER targetType={} targetId={} sourceRevisionId={} selectedRevisionId={} currentContentRevisionId={} currentPublicRevisionId={} releaseId={} operatorUserId={} durationMs={} blockerScope={} blockerCode={}",
                    reqVO.targetType(), reqVO.targetId(), reqVO.sourceRevisionId(), reqVO.sourceRevisionId(),
                    null, null, null, operatorUserId, durationMs(startedAt), "PUBLIC_RELEASE",
                    blockerCode(exception));
            throw exception;
        }
    }

    private void requireReadAccess(Long operatorUserId, String targetType, Long targetId) {
        String normalizedTargetType = ShowroomVersionBundleService.TARGET_COMPANY.equalsIgnoreCase(targetType)
                ? ShowroomVersionBundleService.TARGET_COMPANY
                : ShowroomVersionBundleService.TARGET_PRODUCT.equalsIgnoreCase(targetType)
                ? ShowroomVersionBundleService.TARGET_PRODUCT
                : targetType;
        if (!ShowroomVersionBundleService.TARGET_PRODUCT.equals(normalizedTargetType)) {
            return;
        }
        if (isShowroomPublicity(operatorUserId)) {
            return;
        }
        if (targetId == null || assignmentService.hasVisibleProductAccess(operatorUserId, targetId)) {
            return;
        }
        boolean pendingApprovalAccess = workflowFacade.listPendingApprovalsForReviewer(operatorUserId).stream()
                .anyMatch(request -> Objects.equals("PRODUCT", request.targetType())
                        && Objects.equals(targetId, request.targetId()));
        if (!pendingApprovalAccess) {
            throw exception0(FORBIDDEN.getCode(), "当前用户无权访问该产品版本中心");
        }
    }

    private void requireRepublishAccess(Long operatorUserId) {
        if (!isShowroomPublicity(operatorUserId)) {
            throw exception0(FORBIDDEN.getCode(), "SHOWROOM_VERSION_REPUBLISH_FORBIDDEN: 当前用户无权执行版本重发");
        }
    }

    private boolean isShowroomPublicity(Long operatorUserId) {
        return operatorUserId != null && (securityFrameworkService.hasRole(ShowroomAdminController.SHOWROOM_PUBLICITY_ROLE_CODE)
                || securityFrameworkService.hasRole(RoleCodeEnum.SUPER_ADMIN.getCode()));
    }

    private Long requireOperatorUserId() {
        Long operatorUserId = SecurityFrameworkUtils.getLoginUserId();
        if (operatorUserId == null) {
            throw exception0(FORBIDDEN.getCode(), "当前登录用户不存在，无法执行当前操作");
        }
        return operatorUserId;
    }

    private static long durationMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private static String blockerCode(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        int delimiter = message.indexOf(':');
        return delimiter > 0 ? message.substring(0, delimiter).trim() : message.trim();
    }
}
