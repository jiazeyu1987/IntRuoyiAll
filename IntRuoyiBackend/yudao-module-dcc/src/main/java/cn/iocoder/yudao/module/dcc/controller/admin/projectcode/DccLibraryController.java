package cn.iocoder.yudao.module.dcc.controller.admin.projectcode;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/** Shared libraries use system role permissions and remain separate from project templates. */
@RestController
@RequestMapping("/dcc/project-codes/library")
@Validated
public class DccLibraryController {
    @Resource private DccProjectCodeMapper projects;
    @Resource private DccProjectAccessService access;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFolderMaintenanceService folders;
    public record Library(Long id, String projectName, String projectCode, String status, String libraryScope, boolean canEdit) {}

    private cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO require(String scope) {
        if (!"QMS".equals(scope) && !"GENERAL".equals(scope)) throw new ServiceException(400,"目录类型必须为QMS或GENERAL");
        var rows=projects.selectList(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO>()
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO::getLibraryScope,scope)
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO::getTenantId,cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO::getStatus,"ENABLE"));
        if(rows.size()!=1) throw new ServiceException(400,"独立文件夹尚未初始化或配置不唯一，请执行目录迁移");
        return rows.get(0);
    }
    @GetMapping
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:query','dcc:controlled-file:import')")
    public CommonResult<Library> get(@RequestParam String scope) {
        var p=require(scope);var actor=getLoginUserId();access.assertProjectViewerOrAbove(actor,p.getId());
        return success(new Library(p.getId(),p.getProjectName(),p.getProjectCode(),p.getStatus(),p.getLibraryScope(),access.hasProjectEditorOrOwner(actor,p.getId())));
    }
    @PutMapping("/folders")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderDO> saveFolder(
            @RequestParam String scope,
            @RequestBody cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFolderMaintenanceService.Save input) {
        return success(folders.save(getLoginUserId(), require(scope).getId(), input));
    }
    @DeleteMapping("/folders/{folderId}")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<Boolean> deleteFolder(@RequestParam String scope, @PathVariable Long folderId,
            @RequestBody DccProjectFolderMaintenanceController.Delete input) {
        folders.delete(getLoginUserId(), require(scope).getId(), folderId,
                input != null && input.confirmed(), input == null ? null : input.changeReason());
        return success(true);
    }
}
