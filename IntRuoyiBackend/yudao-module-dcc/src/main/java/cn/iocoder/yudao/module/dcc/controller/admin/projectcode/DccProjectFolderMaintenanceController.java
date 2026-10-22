package cn.iocoder.yudao.module.dcc.controller.admin.projectcode;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderDO;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFolderMaintenanceService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/dcc/project-codes")
public class DccProjectFolderMaintenanceController {
    @Resource private DccProjectFolderMaintenanceService service;
    @PutMapping("/{projectId}/folders")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:update')")
    public CommonResult<DccProjectFolderDO> save(@PathVariable Long projectId,@RequestBody DccProjectFolderMaintenanceService.Save input) {
        return success(service.save(getLoginUserId(),projectId,input));
    }
    public record Delete(boolean confirmed,String changeReason) {}
    @DeleteMapping("/{projectId}/folders/{folderId}")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:update')")
    public CommonResult<Boolean> delete(@PathVariable Long projectId,@PathVariable Long folderId,@RequestBody Delete input) {
        service.delete(getLoginUserId(),projectId,folderId,input!=null && input.confirmed(),input==null?null:input.changeReason());
        return success(true);
    }
}
