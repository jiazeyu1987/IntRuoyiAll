package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentBatchConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentItemRespVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.pqc.MesPqcItemEquipmentConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Arrays;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - QA 检验设备配置")
@RestController
@RequestMapping("/mes/pqc/item-equipment")
@Validated
public class MesPqcItemEquipmentConfigController {

    private final MesPqcItemEquipmentConfigService itemEquipmentConfigService;

    public MesPqcItemEquipmentConfigController(MesPqcItemEquipmentConfigService itemEquipmentConfigService) {
        this.itemEquipmentConfigService = itemEquipmentConfigService;
    }

    @GetMapping("/items")
    @Operation(summary = "读取当前租户当前 QA 项目可配置的检验项目")
    @Parameter(name = "dccProjectCodeId", description = "当前 DCC 项目代码 ID", required = true)
    @PreAuthorize("@ss.hasPermission('mes:qc-template:query')")
    public CommonResult<List<MesPqcItemEquipmentItemRespVO>> getConfigurableItems(
            @RequestParam("dccProjectCodeId") Long dccProjectCodeId) {
        return success(itemEquipmentConfigService.listConfigurableItems(dccProjectCodeId));
    }

    @GetMapping("/config")
    @Operation(summary = "读取当前租户检验项目设备配置")
    @Parameter(name = "itemCode", description = "检验项目编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:qc-template:query')")
    public CommonResult<MesPqcItemEquipmentConfigRespVO> getConfig(@RequestParam("itemCode") String itemCode) {
        return success(itemEquipmentConfigService.getItemConfig(itemCode));
    }

    @GetMapping("/config/batch")
    @Operation(summary = "批量读取当前 QA 项目同名检验项目设备配置")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:query')")
    public CommonResult<MesPqcItemEquipmentConfigRespVO> getBatchConfig(
            @RequestParam("dccProjectCodeId") Long dccProjectCodeId,
            @RequestParam("itemCodes") String itemCodes) {
        return success(itemEquipmentConfigService.getItemConfig(dccProjectCodeId,
                Arrays.stream(itemCodes.split(",")).map(String::trim).filter(code -> !code.isEmpty()).toList()));
    }

    @PostMapping("/config")
    @Operation(summary = "保存当前租户检验项目设备配置")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:update')")
    public CommonResult<MesPqcItemEquipmentConfigRespVO> saveConfig(
            @Valid @RequestBody MesPqcItemEquipmentConfigSaveReqVO reqVO) {
        return success(itemEquipmentConfigService.replaceItemConfig(reqVO));
    }

    @PostMapping("/config/batch")
    @Operation(summary = "批量保存当前 QA 项目同名检验项目设备配置")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:update')")
    public CommonResult<MesPqcItemEquipmentConfigRespVO> saveBatchConfig(
            @Valid @RequestBody MesPqcItemEquipmentBatchConfigSaveReqVO reqVO) {
        return success(itemEquipmentConfigService.replaceItemConfigs(reqVO));
    }
}
