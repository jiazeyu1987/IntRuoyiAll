package cn.iocoder.yudao.module.erp.controller.admin.config;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeActiveConnectionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeActiveConnectionSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeExternalWritePermissionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeExternalWritePermissionSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 金蝶配置")
@RestController
@RequestMapping("/erp/kingdee-config")
@Validated
public class ErpKingdeeConfigController {

    @Resource
    private ErpKingdeeConfigService kingdeeConfigService;

    @GetMapping("/get")
    @Operation(summary = "获取 ERP 金蝶配置")
    @PreAuthorize("@ss.hasPermission('erp:kingdee-config:query')")
    public CommonResult<ErpKingdeeConfigRespVO> getConfig() {
        return success(kingdeeConfigService.getConfig());
    }

    @PutMapping("/save")
    @Operation(summary = "保存 ERP 金蝶配置")
    @PreAuthorize("@ss.hasPermission('erp:kingdee-config:save')")
    public CommonResult<Boolean> saveConfig(@Valid @RequestBody ErpKingdeeConfigSaveReqVO saveReqVO) {
        kingdeeConfigService.saveConfig(saveReqVO);
        return success(true);
    }

    @GetMapping("/active-connection")
    @Operation(summary = "获取 ERP 金蝶当前连接")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:golden-finger')")
    public CommonResult<ErpKingdeeActiveConnectionRespVO> getActiveConnection() {
        return success(kingdeeConfigService.getActiveConnection());
    }

    @PutMapping("/active-connection")
    @Operation(summary = "保存 ERP 金蝶当前连接")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:golden-finger')")
    public CommonResult<ErpKingdeeActiveConnectionRespVO> updateActiveConnection(
            @Valid @RequestBody ErpKingdeeActiveConnectionSaveReqVO saveReqVO) {
        return success(kingdeeConfigService.updateActiveConnection(saveReqVO));
    }

    @GetMapping("/external-write-permission")
    @Operation(summary = "获取 ERP 写权限开关")
    @PreAuthorize("@ss.hasRole('super_admin')")
    public CommonResult<ErpKingdeeExternalWritePermissionRespVO> getExternalWritePermission() {
        ErpKingdeeExternalWritePermissionRespVO respVO = new ErpKingdeeExternalWritePermissionRespVO();
        respVO.setEnabled(kingdeeConfigService.isExternalWriteEnabled());
        return success(respVO);
    }

    @PutMapping("/external-write-permission")
    @Operation(summary = "保存 ERP 写权限开关")
    @PreAuthorize("@ss.hasRole('super_admin')")
    public CommonResult<Boolean> updateExternalWritePermission(
            @Valid @RequestBody ErpKingdeeExternalWritePermissionSaveReqVO saveReqVO) {
        kingdeeConfigService.updateExternalWriteEnabled(saveReqVO.getEnabled());
        return success(true);
    }

}
