package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesDeviationTaskNavigationRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesDeviationTaskNavigationService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@Validated
@RequestMapping("/mes/pro/edhr-deviation-review")
public class MesDeviationTaskNavigationController {
    @Resource private MesDeviationTaskNavigationService navigationService;

    @GetMapping("/task-navigation")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<MesDeviationTaskNavigationRespVO> get(@RequestParam @NotNull @Positive Long workTaskId) {
        return success(navigationService.get(SecurityFrameworkUtils.getLoginUserId(), workTaskId));
    }
}
