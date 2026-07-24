package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCreateSignTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRejectTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileReturnTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTransferTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccExternalFileReviewApproveTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccExternalFileReviewSubmitReqVO;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryService;
import cn.iocoder.yudao.module.dcc.service.file.DccExternalFileReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - DCC 外来文件评审")
@RestController
@RequestMapping("/dcc/external-file-reviews")
@Validated
public class DccExternalFileReviewController {

    @Resource
    private DccExternalFileReviewService externalFileReviewService;
    @Resource
    private DccControlledFileQueryService queryService;

    @PostMapping("/submit")
    @Operation(summary = "提交外来文件评审")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit')")
    public CommonResult<Long> submitExternalReview(@Valid @RequestBody DccExternalFileReviewSubmitReqVO reqVO) {
        return success(externalFileReviewService.submitExternalReview(getLoginUserId(), reqVO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取外来文件评审详情")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<DccControlledFileRespVO> getExternalReview(@PathVariable("id") Long id) {
        return success(queryService.getControlledFile(getLoginUserId(), id));
    }

    @PostMapping("/{id}/approve-task")
    @Operation(summary = "外来文件评审通过")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:review','dcc:controlled-file:approve')")
    public CommonResult<Boolean> approveTask(@PathVariable("id") Long id,
                                             @Valid @RequestBody DccExternalFileReviewApproveTaskReqVO reqVO) {
        externalFileReviewService.approveTask(getLoginUserId(), id, reqVO);
        return success(true);
    }

    @PostMapping("/{id}/reject-task")
    @Operation(summary = "外来文件评审驳回")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:review','dcc:controlled-file:approve')")
    public CommonResult<Boolean> rejectTask(@PathVariable("id") Long id,
                                            @Valid @RequestBody DccControlledFileRejectTaskReqVO reqVO) {
        externalFileReviewService.rejectTask(getLoginUserId(), id, reqVO);
        return success(true);
    }

    @PostMapping("/{id}/return-task")
    @Operation(summary = "外来文件评审回退")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:review','dcc:controlled-file:approve')")
    public CommonResult<Boolean> returnTask(@PathVariable("id") Long id,
                                            @Valid @RequestBody DccControlledFileReturnTaskReqVO reqVO) {
        externalFileReviewService.returnTask(getLoginUserId(), id, reqVO);
        return success(true);
    }

    @PostMapping("/{id}/transfer-task")
    @Operation(summary = "外来文件评审转办")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:review','dcc:controlled-file:approve')")
    public CommonResult<Boolean> transferTask(@PathVariable("id") Long id,
                                              @Valid @RequestBody DccControlledFileTransferTaskReqVO reqVO) {
        externalFileReviewService.transferTask(getLoginUserId(), id, reqVO);
        return success(true);
    }

    @PostMapping("/{id}/sign-task")
    @Operation(summary = "外来文件评审加签")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:review','dcc:controlled-file:approve')")
    public CommonResult<Boolean> createSignTask(@PathVariable("id") Long id,
                                                @Valid @RequestBody DccControlledFileCreateSignTaskReqVO reqVO) {
        externalFileReviewService.createSignTask(getLoginUserId(), id, reqVO);
        return success(true);
    }
}
