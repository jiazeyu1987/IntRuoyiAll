package cn.iocoder.yudao.module.mes.controller.admin.qc.sampling;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.service.qc.sampling.MesQcSamplingTable;
import cn.iocoder.yudao.module.mes.service.qc.sampling.MesQcSamplingTable.InspectionLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 抽样对照表")
@RestController
@RequestMapping("/mes/qc/sampling")
@Validated
public class MesQcSamplingController {

    @GetMapping("/table")
    @Operation(summary = "获取GB/T 2828.1正常一次名义样本量对照表")
    @PreAuthorize("@ss.hasAnyPermissions('mes:qc-template:query','form:parser:query')")
    public CommonResult<List<MesQcSamplingTable.Row>> table() {
        return success(MesQcSamplingTable.rows());
    }

    @GetMapping("/lookup")
    @Operation(summary = "按总数量和检验水平查询字码及正常一次名义样本量",
            description = "批量从2开始；检验水平S1/S2/S3/S4/I/II/III；不执行AQL箭头规则，不返回Ac/Re。")
    @PreAuthorize("@ss.hasAnyPermissions('mes:qc-template:query','form:parser:query')")
    public CommonResult<MesQcSamplingTable.LookupResult> lookup(
            @Parameter(description = "总数量，不小于2的整数", required = true, example = "3000")
            @RequestParam("totalQuantity") @Min(value = 2, message = "总数量必须为不小于2的整数") long totalQuantity,
            @Parameter(description = "检验水平：S1/S2/S3/S4/I/II/III", required = true, example = "II")
            @RequestParam("inspectionLevel") InspectionLevel inspectionLevel) {
        return success(MesQcSamplingTable.lookup(totalQuantity, inspectionLevel));
    }
}
