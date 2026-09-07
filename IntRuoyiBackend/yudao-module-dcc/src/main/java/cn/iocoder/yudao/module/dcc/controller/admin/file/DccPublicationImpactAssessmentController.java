package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationImpactCreateRevisionReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationImpactDecisionReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationImpactLinkRevisionReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationImpactReasonReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationImpactReassignReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationImpactVersionReqVO;
import cn.iocoder.yudao.module.dcc.service.file.DccRelatedFileImpactAssessmentService;
import cn.iocoder.yudao.module.dcc.service.file.DccImpactRevisionCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - DCC 发布关联文件影响评估")
@RestController
@RequestMapping("/dcc/publication-impact-tasks")
public class DccPublicationImpactAssessmentController {

    @Resource
    private DccRelatedFileImpactAssessmentService service;
    @Resource
    private DccImpactRevisionCommandService revisionCommandService;

    @PostMapping("/{id}/start")
    @Operation(summary = "开始处理关联文件影响评估")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> start(@PathVariable("id") Long id,
                                       @Valid @RequestBody DccPublicationImpactVersionReqVO reqVO) {
        service.startTask(SecurityFrameworkUtils.getLoginUserId(), id, reqVO.getExpectedVersion());
        return success(true);
    }

    @PostMapping("/{id}/decision")
    @Operation(summary = "提交关联文件影响评估结论")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> decide(@PathVariable("id") Long id,
                                        @Valid @RequestBody DccPublicationImpactDecisionReqVO reqVO) {
        service.submitDecision(SecurityFrameworkUtils.getLoginUserId(), id, reqVO.getExpectedVersion(),
                reqVO.getDecision(), reqVO.getReason());
        return success(true);
    }

    @PostMapping("/{id}/reassign")
    @Operation(summary = "文控转派关联文件影响评估")
    @PreAuthorize("@ss.hasRole('doc_control') and @ss.hasPermission('dcc:controlled-file:approve')")
    public CommonResult<Boolean> reassign(@PathVariable("id") Long id,
                                          @Valid @RequestBody DccPublicationImpactReassignReqVO reqVO) {
        service.reassignTask(SecurityFrameworkUtils.getLoginUserId(), id, reqVO.getExpectedVersion(),
                reqVO.getNewAssigneeUserId(), reqVO.getReason());
        return success(true);
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "文控重新打开关联文件影响评估")
    @PreAuthorize("@ss.hasRole('doc_control') and @ss.hasPermission('dcc:controlled-file:approve')")
    public CommonResult<Boolean> reopen(@PathVariable("id") Long id,
                                        @Valid @RequestBody DccPublicationImpactReasonReqVO reqVO) {
        service.reopenTask(SecurityFrameworkUtils.getLoginUserId(), id, reqVO.getExpectedVersion(),
                reqVO.getReason());
        return success(true);
    }

    @PostMapping("/{id}/link-revision")
    @Operation(summary = "关联现有大版本")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> linkRevision(@PathVariable("id") Long id,
                                              @Valid @RequestBody DccPublicationImpactLinkRevisionReqVO reqVO) {
        service.linkExistingMajorRevision(SecurityFrameworkUtils.getLoginUserId(), id, reqVO.getExpectedVersion(),
                reqVO.getRevisionControlledFileId(), reqVO.getReason());
        return success(true);
    }

    @PostMapping("/{id}/create-revision")
    @Operation(summary = "通过现有受控文件流程创建并关联大版本")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit')")
    public CommonResult<Long> createRevision(@PathVariable("id") Long id,
                                             @Valid @RequestBody DccPublicationImpactCreateRevisionReqVO reqVO) {
        return success(revisionCommandService.createAndLinkMajorRevision(
                SecurityFrameworkUtils.getLoginUserId(), id, reqVO.getExpectedVersion(),
                reqVO.getSourceControlledFileId(), reqVO.getReason()));
    }

}
