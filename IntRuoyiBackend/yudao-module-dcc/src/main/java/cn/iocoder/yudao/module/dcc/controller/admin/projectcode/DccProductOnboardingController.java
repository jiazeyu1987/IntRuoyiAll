package cn.iocoder.yudao.module.dcc.controller.admin.projectcode;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.onboarding.DccProductOnboardingCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.onboarding.DccProductOnboardingRespVO;
import cn.iocoder.yudao.module.dcc.service.projectcode.onboarding.DccProductOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - DCC 产品建档申请")
@RestController
@RequestMapping("/dcc/product-onboarding-requests")
@Validated
public class DccProductOnboardingController {

    @Resource
    private DccProductOnboardingService onboardingService;

    @PostMapping("/create")
    @Operation(summary = "创建产品建档申请")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:create')")
    public CommonResult<Long> createRequest(@Valid @RequestBody DccProductOnboardingCreateReqVO reqVO) {
        return success(onboardingService.createRequest(getLoginUserId(), reqVO));
    }

    @PostMapping("/{id:\\d+}/approve")
    @Operation(summary = "审批通过产品建档申请并生成 DCC 项目代码")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:update')")
    public CommonResult<DccProductOnboardingRespVO> approveRequest(@PathVariable("id") Long id) {
        return success(BeanUtils.toBean(onboardingService.approveRequest(getLoginUserId(), id),
                DccProductOnboardingRespVO.class));
    }
}
