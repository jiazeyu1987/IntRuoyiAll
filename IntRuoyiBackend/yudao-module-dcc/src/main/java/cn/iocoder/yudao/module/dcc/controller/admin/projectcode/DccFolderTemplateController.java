package cn.iocoder.yudao.module.dcc.controller.admin.projectcode;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccFolderTemplateService;
import jakarta.annotation.Resource;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/dcc/folder-templates")
public class DccFolderTemplateController {
    @Resource private DccFolderTemplateService service;
    @Resource private AdminUserApi userApi;

    @lombok.Data
    @lombok.EqualsAndHashCode(callSuper = true)
    public static class FolderTemplateView extends DccFolderTemplateDO {
        private String editedByName;
    }
    @GetMapping
    @PreAuthorize("@ss.hasPermission('dcc:project-code:query')")
    public CommonResult<List<FolderTemplateView>> list() {
        var templates = service.list();
        var ids = templates.stream().map(DccFolderTemplateDO::getEditedByUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        var users = ids.isEmpty() ? Map.<Long, cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO>of()
                : userApi.getUserMap(ids);
        return success(templates.stream().map(template -> {
            var view = BeanUtils.toBean(template, FolderTemplateView.class);
            var user = users.get(template.getEditedByUserId());
            if (user != null) view.setEditedByName(cn.hutool.core.util.StrUtil.isNotBlank(user.getNickname())
                    ? user.getNickname() : user.getUsername());
            return view;
        }).toList());
    }
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
