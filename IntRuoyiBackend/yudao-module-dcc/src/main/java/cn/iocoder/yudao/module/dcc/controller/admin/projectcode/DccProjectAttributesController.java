package cn.iocoder.yudao.module.dcc.controller.admin.projectcode;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccFolderTemplateService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderDO;
import java.util.List;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.*;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Arrays;
import java.util.Objects;

@RestController
@RequestMapping("/dcc/project-codes")
public class DccProjectAttributesController {
    @Resource private DccProjectAttributesService service;
    @Resource private DccProjectLeaderService leaderService;
    @Resource private DccProjectCodeMapper projectMapper;
    @Resource private DccFolderTemplateService folderService;
    @Resource private DccProjectAccessService accessService;
    @Resource private DccProjectConfigurationAuditService auditService;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFilePlacementService placementService;

    @GetMapping("/{projectId}/folders/{folderId}/file-placements")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:project-code:query','dcc:controlled-file:query')")
    public CommonResult<List<cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFilePlacementDO>> placements(
            @PathVariable Long projectId, @PathVariable Long folderId) {
        return success(placementService.list(getLoginUserId(), projectId, folderId));
    }

    @GetMapping("/{projectId}/folders")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:project-code:query','dcc:controlled-file:query')")
    public CommonResult<List<DccProjectFolderDO>> folders(@PathVariable Long projectId) {
        return success(folderService.readProjectFolders(getLoginUserId(), projectId));
    }

    public record Configuration(Long projectLeaderUserId, DccProjectAttributes defaultAttributes,
                                String changeReason, String expectedConfigurationToken) {}
    public record ConfigurationView(@JsonFormat(shape = JsonFormat.Shape.STRING) Long id,
                                    @JsonFormat(shape = JsonFormat.Shape.STRING) Long projectLeaderUserId,
                                    String defaultAttributesJson, String configurationToken) {}

    @GetMapping("/{projectId}/attributes/configuration")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:project-code:query','dcc:controlled-file:query')")
    public CommonResult<ConfigurationView> configuration(@PathVariable Long projectId) {
        accessService.assertProjectViewerOrAbove(getLoginUserId(), projectId);
        var project = projectMapper.selectById(projectId);
        requireConfigurationProject(project);
        // Raw stored values remain readable for explicit repair; proposed new values are validated on save.
        return success(new ConfigurationView(project.getId(), project.getProjectLeaderUserId(),
                project.getDefaultAttributesJson(), configurationToken(project)));
    }

    private String configurationToken(DccProjectCodeDO project) {
        return DigestUtil.sha256Hex(JsonUtils.toJsonString(Arrays.asList(project.getTenantId(), project.getId(),
                project.getProjectLeaderUserId(), project.getProjectLeader(), project.getDefaultAttributesJson())));
    }

    private void requireConfigurationProject(DccProjectCodeDO project) {
        if (project == null || !Objects.equals(project.getTenantId(),
                cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())
                || !"ENABLE".equals(project.getStatus())) throw fail(DEFAULTS_MISSING);
    }
    @GetMapping("/{projectId}/attributes/defaults")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:project-code:query','dcc:controlled-file:submit','dcc:controlled-file:query')")
    public CommonResult<DccProjectAttributes> defaults(@PathVariable Long projectId, @RequestParam String applicationType) {
        return success(service.initialize(getLoginUserId(), projectId, applicationType));
    }
    @PutMapping("/{projectId}/attributes/configuration")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:update')")
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<Boolean> configure(@PathVariable Long projectId, @RequestBody Configuration input) {
        accessService.assertProjectViewerOrAbove(getLoginUserId(), projectId);
        accessService.assertProjectEditorOrOwner(getLoginUserId(), projectId);
        if (input == null) throw fail(INVALID);
        auditService.validateReason(input.changeReason());
        var user = leaderService.requireEnabledAccount(input.projectLeaderUserId());
        String json = service.encode(input.defaultAttributes());
        var project = projectMapper.selectByIdForUpdate(projectId);
        requireConfigurationProject(project);
        if (input.expectedConfigurationToken() == null || !input.expectedConfigurationToken().matches("[a-f0-9]{64}")
                || !configurationToken(project).equals(input.expectedConfigurationToken())) throw fail(CONFIGURATION_CHANGED);
        String before = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(project);
        int written = projectMapper.update(null, new LambdaUpdateWrapper<DccProjectCodeDO>()
                .eq(DccProjectCodeDO::getId, projectId)
                .set(DccProjectCodeDO::getProjectLeaderUserId, input.projectLeaderUserId())
                .set(DccProjectCodeDO::getProjectLeader, user.getNickname())
                .set(DccProjectCodeDO::getDefaultAttributesJson, json));
        if (written != 1) throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                WRITE_INCOMPLETE, "项目默认属性与负责人");
        String after = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(projectMapper.selectById(projectId));
        auditService.append("dcc.project-attributes.configure", projectId, java.util.UUID.randomUUID().toString(), input.changeReason(), before, after);
        return success(true);
    }
}
