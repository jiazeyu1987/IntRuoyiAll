package cn.iocoder.yudao.module.dcc.controller.admin.projectcode;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccFolderTemplateService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/dcc/folder-templates")
public class DccFolderTemplateController {
    @Resource private DccFolderTemplateService service;
    @GetMapping
    @PreAuthorize("@ss.hasPermission('dcc:project-code:query')")
    public CommonResult<List<DccFolderTemplateDO>> list() { return success(service.list()); }
    @PutMapping
    @PreAuthorize("@ss.hasPermission('dcc:project-code:update')")
    public CommonResult<Long> save(@RequestBody DccFolderTemplateService.Save input) {
        return success(service.save(getLoginUserId(), input));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:update')")
    public CommonResult<Boolean> delete(@PathVariable Long id, @RequestParam String reason) {
        service.delete(getLoginUserId(), id, reason); return success(true);
    }
}
