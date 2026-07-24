package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resourceadjustment.MesProScheduleResourceAdjustmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resourceadjustment.MesProScheduleResourceAdjustmentSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProScheduleResourceAdjustmentDO;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProScheduleResourceAdjustmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 排产日资源调整")
@RestController
@RequestMapping("/mes/pro/route-resource-adjustment")
@Validated
public class MesProScheduleResourceAdjustmentController {

    @Resource
    private MesProScheduleResourceAdjustmentService adjustmentService;

    @GetMapping("/list")
    @Operation(summary = "获得工艺路线日资源调整列表")
    @Parameter(name = "routeId", description = "工艺路线编号", required = true)
    @Parameter(name = "calendarDate", description = "生效日期", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-route:query', 'mes:pro-schedule-order:query')")
    public CommonResult<List<MesProScheduleResourceAdjustmentRespVO>> getAdjustmentList(
            @RequestParam("routeId") Long routeId,
            @RequestParam("calendarDate") LocalDate calendarDate) {
        List<MesProScheduleResourceAdjustmentDO> list = adjustmentService.getAdjustmentList(routeId, calendarDate);
        return success(BeanUtils.toBean(list, MesProScheduleResourceAdjustmentRespVO.class));
    }

    @PostMapping("/save")
    @Operation(summary = "保存工艺路线日资源调整")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-route:update', 'mes:pro-schedule-order:create')")
    public CommonResult<Boolean> saveAdjustment(@Valid @RequestBody MesProScheduleResourceAdjustmentSaveReqVO saveReqVO) {
        adjustmentService.saveAdjustment(saveReqVO);
        return success(true);
    }

}
