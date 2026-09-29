package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationBatchOptionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationBatchOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationPageReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeviationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - eDHR偏差管理")
@RestController
@RequestMapping("/mes/pro/edhr-deviation")
@Validated
public class MesProEdhrDeviationController {

    @Resource
    private MesProEdhrDeviationService deviationService;

    @GetMapping("/batch-options")
    @Operation(summary = "分页查询可关联的正式批记录")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-deviation:create')")
    public CommonResult<PageResult<MesProEdhrDeviationBatchOptionRespVO>> getBatchOptions(
            @Valid MesProEdhrDeviationBatchOptionPageReqVO reqVO) {
        return success(deviationService.getBatchOptions(reqVO));
    }

    @GetMapping("/batch-options-by-active-order")
    @Operation(summary = "按活跃订单解析正式批记录偏差来源")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-deviation:query')")
    public CommonResult<java.util.List<MesProEdhrDeviationBatchOptionRespVO>> getBatchOptionsByActiveOrder(
            @jakarta.validation.constraints.NotNull Long activeOrderId) {
        return success(deviationService.getBatchOptionsByActiveOrder(activeOrderId));
    }

    @PostMapping("/create")
    @Operation(summary = "发起一条关联正式批记录的偏差")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-deviation:create')")
    public CommonResult<MesProEdhrDeviationRespVO> create(
            @Valid @RequestBody MesProEdhrDeviationCreateReqVO reqVO) {
        return success(deviationService.create(SecurityFrameworkUtils.getLoginUserId(), reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询偏差")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-deviation:query')")
    public CommonResult<PageResult<MesProEdhrDeviationRespVO>> getPage(
            @Valid MesProEdhrDeviationPageReqVO reqVO) {
        return success(deviationService.getPage(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获取偏差详情")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-deviation:query')")
    public CommonResult<MesProEdhrDeviationRespVO> get(@jakarta.validation.constraints.NotNull Long id) {
        return success(deviationService.get(id));
    }
}
