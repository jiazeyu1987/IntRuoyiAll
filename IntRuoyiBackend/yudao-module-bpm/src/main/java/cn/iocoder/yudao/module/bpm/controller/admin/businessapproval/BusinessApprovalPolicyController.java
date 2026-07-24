package cn.iocoder.yudao.module.bpm.controller.admin.businessapproval;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalPolicyAdministrationService;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicyPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicyRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicySaveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicySwitchModeReqVO;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 平台业务审批策略")
@RestController
@RequestMapping("/business-approval/policies")
@Validated
public class BusinessApprovalPolicyController {

    @Resource
    private BusinessApprovalPolicyAdministrationService policyService;

    @GetMapping("")
    @Operation(summary = "查询平台业务审批策略")
    @PreAuthorize("@ss.hasPermission('bpm:business-approval-policy:query')")
    public CommonResult<PageResult<BusinessApprovalPolicyRespVO>> getPolicyPage(
            @Valid BusinessApprovalPolicyPageReqVO reqVO) {
        return success(policyService.getPolicyPage(reqVO));
    }

    @PostMapping("")
    @Operation(summary = "保存平台业务审批策略")
    @PreAuthorize("@ss.hasPermission('bpm:business-approval-policy:create')")
    public CommonResult<BusinessApprovalPolicyRespVO> savePolicy(
            @Valid @RequestBody BusinessApprovalPolicySaveReqVO reqVO) {
        return success(policyService.savePolicy(reqVO));
    }

    @PostMapping("/{policyId}/publish")
    @Operation(summary = "发布平台业务审批策略")
    @PreAuthorize("@ss.hasPermission('bpm:business-approval-policy:publish')")
    public CommonResult<Boolean> publishPolicy(@PathVariable("policyId") Long policyId) {
        policyService.publishPolicy(policyId);
        return success(true);
    }

    @PostMapping("/{policyId}/disable")
    @Operation(summary = "停用平台业务审批策略")
    @PreAuthorize("@ss.hasPermission('bpm:business-approval-policy:disable')")
    public CommonResult<Boolean> disablePolicy(@PathVariable("policyId") Long policyId) {
        policyService.disablePolicy(policyId);
        return success(true);
    }

    @PostMapping("/{policyId}/switch-mode")
    @Operation(summary = "一键切换平台业务审批策略模式")
    @PreAuthorize("@ss.hasPermission('bpm:business-approval-policy:publish')")
    public CommonResult<BusinessApprovalPolicyRespVO> switchPolicyMode(@PathVariable("policyId") Long policyId,
            @Valid @RequestBody BusinessApprovalPolicySwitchModeReqVO reqVO) {
        return success(policyService.switchPolicyMode(getLoginUserId(), policyId, reqVO));
    }

}
