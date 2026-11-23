package cn.iocoder.yudao.module.bpm.controller.admin.businessapproval;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.service.RoleApprovalConfigPackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 权限角色与审批策略配置包")
@RestController
@RequestMapping("/system/role/config-package")
public class RoleApprovalConfigPackageController {
    @Resource private RoleApprovalConfigPackageService transferService;

    @GetMapping("/export")
    @Operation(summary = "导出全部权限角色授权、审批策略及依赖流程")
    @ApiAccessLog(operateType = EXPORT)
    @PreAuthorize("@ss.hasPermission('system:role:export')"
            + " && @ss.hasPermission('bpm:business-approval-policy:query') && @ss.hasPermission('bpm:model:query')")
    public void exportConfigPackage(HttpServletResponse response) throws IOException {
        byte[] content;
        try {
            content = transferService.exportPackage();
        } catch (BusinessApprovalException exception) {
            throw new cn.iocoder.yudao.framework.common.exception.ServiceException(400, exception.getMessage());
        }
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8("权限角色与审批策略配置包.json"));
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(content);
    }

    @PostMapping("/import")
    @Operation(summary = "整包导入权限角色授权、审批策略及依赖流程")
    @PreAuthorize("@ss.hasPermission('system:role:create') && @ss.hasPermission('system:role:update')"
            + " && @ss.hasPermission('system:permission:assign-role-menu')"
            + " && @ss.hasPermission('system:permission:assign-role-data-scope')"
            + " && @ss.hasPermission('bpm:business-approval-policy:create')"
            + " && @ss.hasPermission('bpm:business-approval-policy:publish')"
            + " && @ss.hasPermission('bpm:business-approval-policy:disable')"
            + " && @ss.hasPermission('bpm:model:create') && @ss.hasPermission('bpm:model:deploy')"
            + " && @ss.hasPermission('bpm:category:create')")
    public CommonResult<Integer> importConfigPackage(@RequestParam("file") MultipartFile file) throws IOException {
        try {
            return success(transferService.importPackage(file.getBytes()));
        } catch (BusinessApprovalException exception) {
            return CommonResult.error(400, exception.getMessage());
        }
    }
}
