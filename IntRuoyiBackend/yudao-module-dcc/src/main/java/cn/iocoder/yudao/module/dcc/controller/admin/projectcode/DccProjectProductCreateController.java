package cn.iocoder.yudao.module.dcc.controller.admin.projectcode;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate.DccProjectProductApprovalActionReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate.DccProjectProductCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate.DccProjectProductCreateRespVO;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductCreateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - DCC 项目代码与产品目录联合新建申请")
@RestController
@RequestMapping("/dcc/project-product-requests")
@Validated
public class DccProjectProductCreateController {

    @Resource
    private DccProjectProductCreateService service;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectReviewerConfigurationService reviewerConfiguration;

    @GetMapping("/reviewer-config")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:query')")
    public CommonResult<cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectReviewerConfigurationService.View> getReviewerConfig() {
        return success(reviewerConfiguration.get());
    }
    @org.springframework.web.bind.annotation.PutMapping("/reviewer-config")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:update') and @ss.hasRole('doc_control')")
    public CommonResult<cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectReviewerConfigurationService.View> saveReviewerConfig(
            @Valid @RequestBody cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectReviewerConfigurationService.Save request) {
        return success(reviewerConfiguration.save(getLoginUserId(),request));
    }

    @PostMapping("/create")
    @Operation(summary = "创建 DCC 项目代码与产品目录联合新建申请")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:create')")
    public CommonResult<Long> create(@Valid @RequestBody DccProjectProductCreateReqVO reqVO) {
        return success(service.createRequest(getLoginUserId(), reqVO));
    }
    @PostMapping("/{id:\\d+}/resubmit")
    @Operation(summary = "原申请人修改驳回申请后重新提交")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:create')")
    public CommonResult<Long> resubmit(@PathVariable("id") Long id,
                                      @Valid @RequestBody DccProjectProductCreateReqVO reqVO) {
        return success(service.resubmitRejectedRequest(getLoginUserId(), id, reqVO));
    }

    @GetMapping("/pending")
    @Operation(summary = "查询 DCC 项目代码与产品目录联合新建申请")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:query')")
    public CommonResult<List<DccProjectProductCreateRespVO>> getPending() {
        return success(service.getPendingRequests().stream()
                .map(item -> BeanUtils.toBean(item, DccProjectProductCreateRespVO.class))
                .toList());
    }

    @PostMapping("/{id:\\d+}/review/approve")
    @Operation(summary = "审核通过 DCC 项目代码与产品目录联合新建申请")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:update')")
    public CommonResult<DccProjectProductCreateRespVO> reviewApprove(
            @PathVariable("id") Long id, @Valid @RequestBody DccProjectProductApprovalActionReqVO reqVO) {
        return success(toResp(service.review(getLoginUserId(), id, reqVO.getReason(), true)));
    }

    @PostMapping("/{id:\\d+}/review/reject")
    @Operation(summary = "审核驳回 DCC 项目代码与产品目录联合新建申请")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:update')")
    public CommonResult<DccProjectProductCreateRespVO> reviewReject(
            @PathVariable("id") Long id, @Valid @RequestBody DccProjectProductApprovalActionReqVO reqVO) {
        return success(toResp(service.review(getLoginUserId(), id, reqVO.getReason(), false)));
    }

    @PostMapping("/{id:\\d+}/approve/approve")
    @Operation(summary = "批准并写入 DCC 项目代码与产品目录")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:update')")
    public CommonResult<DccProjectProductCreateRespVO> approve(
            @PathVariable("id") Long id, @Valid @RequestBody DccProjectProductApprovalActionReqVO reqVO) {
        return success(toResp(service.approve(getLoginUserId(), id, reqVO.getReason(), true)));
    }

    @PostMapping("/{id:\\d+}/approve/reject")
    @Operation(summary = "批准节点驳回 DCC 项目代码与产品目录联合新建申请")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:update')")
    public CommonResult<DccProjectProductCreateRespVO> approveReject(
            @PathVariable("id") Long id, @Valid @RequestBody DccProjectProductApprovalActionReqVO reqVO) {
        return success(toResp(service.approve(getLoginUserId(), id, reqVO.getReason(), false)));
    }

    @PostMapping("/{id:\\d+}/retry-write")
    @Operation(summary = "重试写入 DCC 项目代码与产品目录")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:update')")
    public CommonResult<DccProjectProductCreateRespVO> retryWrite(@PathVariable("id") Long id,
            @Valid @RequestBody DccProjectProductApprovalActionReqVO reqVO) {
        return success(toResp(service.retryWrite(getLoginUserId(), id, reqVO.getReason())));
    }

    private DccProjectProductCreateRespVO toResp(Object item) {
        return BeanUtils.toBean(item, DccProjectProductCreateRespVO.class);
    }
}
