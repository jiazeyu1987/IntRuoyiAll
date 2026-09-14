package cn.iocoder.yudao.module.dcc.controller.admin.projectcode;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentAuditPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentAuditRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentCandidatePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentCandidateRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentRevokeReqVO;
import cn.iocoder.yudao.module.dcc.service.projectcode.assignment.DccProjectCodeAssignmentService;
import cn.iocoder.yudao.module.dcc.service.projectcode.assignmentaudit.DccProjectCodeMetadataChangeAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - DCC 项目代码修正分配")
@RestController
@RequestMapping("/dcc")
@Validated
public class DccProjectCodeAssignmentController {

    @Resource
    private DccProjectCodeAssignmentService assignmentService;
    @Resource
    private DccProjectCodeMetadataChangeAuditService metadataChangeAuditService;

    @PostMapping("/project-codes/{projectCodeId:\\d+}/assignments")
    @Operation(summary = "创建 DCC 项目代码修正分配")
    @PreAuthorize("@ss.hasRole('doc_control') or @ss.hasPermission('dcc:project-code-assignment:assign')")
    public CommonResult<DccProjectCodeAssignmentRespVO> createAssignment(
            @PathVariable("projectCodeId") Long projectCodeId,
            @Valid @RequestBody DccProjectCodeAssignmentCreateReqVO reqVO) {
        return success(assignmentService.createAssignment(getLoginUserId(), projectCodeId, reqVO));
    }

    @GetMapping("/project-codes/{projectCodeId:\\d+}/assignments/page")
    @Operation(summary = "分页查询 DCC 项目代码修正分配")
    @PreAuthorize("@ss.hasRole('doc_control') or @ss.hasPermission('dcc:project-code-assignment:query')")
    public CommonResult<PageResult<DccProjectCodeAssignmentRespVO>> getProjectCodeAssignmentPage(
            @PathVariable("projectCodeId") Long projectCodeId,
            @Valid DccProjectCodeAssignmentPageReqVO reqVO) {
        return success(assignmentService.getProjectCodeAssignmentPage(projectCodeId, reqVO));
    }

    @GetMapping("/project-code-assignments/my/page")
    @Operation(summary = "分页查询我的 DCC 项目代码修正分配")
    @PreAuthorize("@ss.hasPermission('dcc:project-code-assignment:execute')")
    public CommonResult<PageResult<DccProjectCodeAssignmentRespVO>> getMyAssignmentPage(
            @Valid DccProjectCodeAssignmentPageReqVO reqVO) {
        return success(assignmentService.getMyAssignmentPage(getLoginUserId(), reqVO));
    }

    @GetMapping("/project-codes/{projectCodeId:\\d+}/assignment-candidates/page")
    @Operation(summary = "全局搜索 DCC 项目代码修正候选文件")
    @PreAuthorize("@ss.hasRole('doc_control') or @ss.hasPermission('dcc:project-code-assignment:assign')")
    public CommonResult<PageResult<DccProjectCodeAssignmentCandidateRespVO>> getAssignmentCandidatePage(
            @PathVariable("projectCodeId") Long projectCodeId,
            @Valid DccProjectCodeAssignmentCandidatePageReqVO reqVO) {
        return success(assignmentService.getAssignmentCandidatePage(getLoginUserId(), projectCodeId, reqVO));
    }

    @GetMapping("/project-code-assignments/{assignmentId:\\d+}/files/page")
    @Operation(summary = "分页查询 DCC 项目代码修正分配文件")
    @PreAuthorize("@ss.hasRole('doc_control') or @ss.hasPermission('dcc:project-code-assignment:execute')")
    public CommonResult<PageResult<DccProjectCodeAssignmentFileRespVO>> getAssignmentFilePage(
            @PathVariable("assignmentId") Long assignmentId,
            @Valid DccProjectCodeAssignmentFilePageReqVO reqVO) {
        return success(assignmentService.getAssignmentFilePage(getLoginUserId(), assignmentId, reqVO));
    }

    @PutMapping("/project-code-assignments/{assignmentId:\\d+}/revoke")
    @Operation(summary = "撤回 DCC 项目代码修正分配")
    @PreAuthorize("@ss.hasRole('doc_control') or @ss.hasPermission('dcc:project-code-assignment:revoke')")
    public CommonResult<Boolean> revokeAssignment(@PathVariable("assignmentId") Long assignmentId,
                                                  @Valid @RequestBody DccProjectCodeAssignmentRevokeReqVO reqVO) {
        assignmentService.revokeAssignment(getLoginUserId(), assignmentId, reqVO);
        return success(true);
    }

    @GetMapping("/project-code-assignment-audits/page")
    @Operation(summary = "分页查询 DCC 项目代码修正字段追溯")
    @PreAuthorize("@ss.hasRole('doc_control') or @ss.hasPermission('dcc:project-code-assignment:audit:query')")
    public CommonResult<PageResult<DccProjectCodeAssignmentAuditRespVO>> getAssignmentAuditPage(
            @Valid DccProjectCodeAssignmentAuditPageReqVO reqVO) {
        return success(metadataChangeAuditService.getAuditPage(reqVO));
    }

    @GetMapping("/project-code-assignment-audits/{changeId:\\d+}/items")
    @Operation(summary = "查询 DCC 项目代码修正单次保存字段追溯明细")
    @PreAuthorize("@ss.hasRole('doc_control') or @ss.hasPermission('dcc:project-code-assignment:audit:query')")
    public CommonResult<List<DccProjectCodeAssignmentAuditRespVO>> getAssignmentAuditItems(
            @PathVariable("changeId") Long changeId) {
        return success(metadataChangeAuditService.getAuditChangeItems(changeId));
    }

}
