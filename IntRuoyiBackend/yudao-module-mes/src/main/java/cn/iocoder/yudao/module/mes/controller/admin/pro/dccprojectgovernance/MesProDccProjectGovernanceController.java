package cn.iocoder.yudao.module.mes.controller.admin.pro.dccprojectgovernance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.dccprojectgovernance.vo.MesProDccProjectGovernanceStatusRespVO;
import cn.iocoder.yudao.module.mes.service.pro.dccprojectgovernance.MesProDccProjectGovernanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES DCC 项目治理状态")
@RestController
@Validated
public class MesProDccProjectGovernanceController {

    @Resource
    private MesProDccProjectGovernanceService dccProjectGovernanceService;

    @GetMapping("/mes/pro/dcc-project-governance/status")
    @Operation(summary = "按 DCC 项目名称查询 MES 工艺路线与批记录治理状态")
    public CommonResult<List<MesProDccProjectGovernanceStatusRespVO>> getStatus(
            @NotEmpty(message = "DCC 项目名称不能为空") @RequestParam("projectNames") List<String> projectNames,
            @RequestParam(value = "routeStatusRequired", defaultValue = "true") Boolean routeStatusRequired,
            @RequestParam(value = "mainBatchRecordStatusRequired", defaultValue = "true") Boolean mainBatchRecordStatusRequired,
            @RequestParam(value = "formSlotStatusRequired", defaultValue = "true") Boolean formSlotStatusRequired) {
        return success(BeanUtils.toBean(dccProjectGovernanceService.getStatus(projectNames,
                        Boolean.TRUE.equals(routeStatusRequired),
                        Boolean.TRUE.equals(mainBatchRecordStatusRequired),
                        Boolean.TRUE.equals(formSlotStatusRequired)),
                MesProDccProjectGovernanceStatusRespVO.class));
    }
}
