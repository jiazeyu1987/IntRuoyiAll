package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationFollowupPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationFollowupRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationImpactTaskPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationImpactTaskRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationNotificationRetryReqVO;
import cn.iocoder.yudao.module.dcc.service.file.DccPublicationFollowupQueryService;
import cn.iocoder.yudao.module.dcc.service.file.DccPublicationNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - DCC 发布后续")
@RestController
@RequestMapping("/dcc/publication-followups")
public class DccPublicationFollowupController {
    private final DccPublicationFollowupQueryService queryService;
    private final DccPublicationNotificationService notificationService;

    public DccPublicationFollowupController(DccPublicationFollowupQueryService queryService,
                                             DccPublicationNotificationService notificationService) {
        this.queryService = queryService;
        this.notificationService = notificationService;
    }

    @GetMapping("/files/{controlledFileId}")
    @Operation(summary = "查询受控文件发布后续")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<DccPublicationFollowupRespVO> getFileFollowup(@PathVariable Long controlledFileId) {
        return success(queryService.getFileFollowup(SecurityFrameworkUtils.getLoginUserId(), controlledFileId));
    }

    @GetMapping("/management-page")
    @Operation(summary = "分页查询发布后续管理数据")
    @PreAuthorize("@ss.hasRole('doc_control') and @ss.hasPermission('dcc:controlled-file:approve') and @ss.hasPermission('dcc:controlled-file:publication-followup:manage')")
    public CommonResult<PageResult<DccPublicationFollowupRespVO>> getManagementPage(
            @Valid DccPublicationFollowupPageReqVO reqVO) {
        return success(queryService.getManagementPage(SecurityFrameworkUtils.getLoginUserId(), reqVO));
    }

    @GetMapping("/my-impact-tasks")
    @Operation(summary = "查询我的影响评估")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<PageResult<DccPublicationImpactTaskRespVO>> getMyImpactTasks(
            @Valid DccPublicationImpactTaskPageReqVO reqVO) {
        return success(queryService.getMyImpactTasks(SecurityFrameworkUtils.getLoginUserId(), reqVO));
    }

    @PostMapping("/notification-deliveries/{deliveryId}/retry")
    @Operation(summary = "重试发布通知")
    @PreAuthorize("@ss.hasRole('doc_control') and @ss.hasPermission('dcc:controlled-file:approve') and @ss.hasPermission('dcc:controlled-file:publication-followup:manage')")
    public CommonResult<Boolean> retryNotification(@PathVariable Long deliveryId,
                                                    @Valid @RequestBody DccPublicationNotificationRetryReqVO reqVO) {
        notificationService.retryDelivery(SecurityFrameworkUtils.getLoginUserId(), deliveryId,
                reqVO.getExpectedVersion(), reqVO.getReason());
        return success(true);
    }
}
