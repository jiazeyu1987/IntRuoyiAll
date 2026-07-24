package cn.iocoder.yudao.module.infra.controller.admin.externalwritepermission;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.infra.controller.admin.externalwritepermission.vo.ExternalWritePermissionRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.externalwritepermission.vo.ExternalWritePermissionSaveReqVO;
import cn.iocoder.yudao.module.infra.service.externalwritepermission.ExternalWritePermissionService;
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

@Tag(name = "管理后台 - 外部系统写权限")
@RestController
@RequestMapping("/infra/external-write-permission")
@Validated
public class ExternalWritePermissionController {

    @Resource
    private ExternalWritePermissionService externalWritePermissionService;

    @GetMapping("/erp")
    @Operation(summary = "获取 ERP 写权限开关")
    @PreAuthorize("@ss.hasRole('super_admin')")
    public CommonResult<ExternalWritePermissionRespVO> getErpExternalWritePermission() {
        ExternalWritePermissionRespVO respVO = new ExternalWritePermissionRespVO();
        respVO.setEnabled(externalWritePermissionService.isErpExternalWriteEnabled());
        return success(respVO);
    }

    @PutMapping("/erp")
    @Operation(summary = "保存 ERP 写权限开关")
    @PreAuthorize("@ss.hasRole('super_admin')")
    public CommonResult<Boolean> updateErpExternalWritePermission(
            @Valid @RequestBody ExternalWritePermissionSaveReqVO saveReqVO) {
        externalWritePermissionService.updateErpExternalWriteEnabled(saveReqVO.getEnabled());
        return success(true);
    }

}
