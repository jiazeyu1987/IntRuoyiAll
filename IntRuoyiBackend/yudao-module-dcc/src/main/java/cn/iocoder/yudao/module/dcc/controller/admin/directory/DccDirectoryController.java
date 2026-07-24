package cn.iocoder.yudao.module.dcc.controller.admin.directory;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectoryAccessRuleRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectoryAccessRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectoryAccessRuleDirectoryRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectoryActiveNasTransferRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectoryDeleteSubtreeReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectoryDeleteSubtreeRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectoryImportRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectoryRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectorySaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccDirectoryAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAdminService;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessRuleDirectorySummary;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryNasTransferGuardService;
import cn.iocoder.yudao.module.dcc.service.directory.DccVisibleDirectoryNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - DCC 目录")
@RestController
@RequestMapping("/dcc/directories")
@Validated
public class DccDirectoryController {

    @Resource
    private DccDirectoryAdminService directoryAdminService;
    @Resource
    private DccDirectoryNasTransferGuardService nasTransferGuardService;

    @GetMapping("/tree")
    @Operation(summary = "获取 DCC 目录树")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<List<DccDirectoryRespVO>> getDirectoryTree() {
        List<DccFileDirectoryDO> directories = directoryAdminService.getDirectoryTree(getLoginUserId());
        List<DccDirectoryRespVO> directoryRespList = convertList(directories,
                item -> BeanUtils.toBean(item, DccDirectoryRespVO.class));
        return success(buildTree(directoryRespList));
    }

    @GetMapping("/children")
    @Operation(summary = "获取 DCC 目录直接子级")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<List<DccDirectoryRespVO>> getDirectoryChildren(
            @RequestParam(value = "parentId", required = false) Long parentId) {
        return success(convertList(directoryAdminService.listVisibleChildDirectories(getLoginUserId(), parentId),
                this::toDirectoryRespVO));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索 DCC 可见目录")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<List<DccDirectoryRespVO>> searchDirectories(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
        return success(convertList(directoryAdminService.searchVisibleDirectories(getLoginUserId(), keyword.trim(), limit),
                this::toDirectoryRespVO));
    }

    @PostMapping("/import-intauth")
    @Operation(summary = "从 IntAuth 一次性导入 DCC 目录树")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:directory:manage')")
    public CommonResult<DccDirectoryImportRespVO> importDirectoriesFromIntAuth() {
        var result = directoryAdminService.importDirectoriesFromIntAuth();
        DccDirectoryImportRespVO respVO = new DccDirectoryImportRespVO();
        respVO.setImportedCount(result.getImportedCount());
        respVO.setRootCount(result.getRootCount());
        return success(respVO);
    }

    @PostMapping
    @Operation(summary = "创建 DCC 目录")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:directory:manage')")
    public CommonResult<Long> createDirectory(@Valid @RequestBody DccDirectorySaveReqVO reqVO) {
        return success(directoryAdminService.createDirectory(reqVO));
    }

    @PutMapping("/{id:\\d+}")
    @Operation(summary = "更新 DCC 目录")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:directory:manage')")
    public CommonResult<Boolean> updateDirectory(@PathVariable("id") Long id,
                                                 @Valid @RequestBody DccDirectorySaveReqVO reqVO) {
        reqVO.setId(id);
        directoryAdminService.updateDirectory(reqVO);
        return success(true);
    }

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "获取 DCC 目录详情")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<DccDirectoryRespVO> getDirectory(@PathVariable("id") Long id) {
        return success(BeanUtils.toBean(directoryAdminService.getDirectory(getLoginUserId(), id), DccDirectoryRespVO.class));
    }

    @PostMapping("/{id:\\d+}/delete-subtree")
    @Operation(summary = "删除 DCC 目录子树")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:directory:manage')")
    public CommonResult<DccDirectoryDeleteSubtreeRespVO> deleteSubtree(@PathVariable("id") Long id,
                                                                       @Valid @RequestBody DccDirectoryDeleteSubtreeReqVO reqVO) {
        return success(DccDirectoryDeleteSubtreeRespVO.of(
                directoryAdminService.deleteDirectorySubtree(id, reqVO.getConfirmText())));
    }

    @GetMapping("/{id:\\d+}/active-nas-transfer")
    @Operation(summary = "获取目录删除前 active 后台收集任务")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:directory:manage')")
    public CommonResult<DccDirectoryActiveNasTransferRespVO> getActiveNasTransfer(@PathVariable("id") Long id) {
        return success(nasTransferGuardService.getActiveTransfer(id));
    }

    @PostMapping("/{id:\\d+}/active-nas-transfer/stop")
    @Operation(summary = "停止目录删除前 active 后台收集任务")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:directory:manage')")
    public CommonResult<DccDirectoryActiveNasTransferRespVO> stopActiveNasTransfer(@PathVariable("id") Long id) {
        return success(nasTransferGuardService.stopActiveTransfer(id));
    }

    @GetMapping("/access-rule-directories")
    @Operation(summary = "获取已绑定访问规则的目录列表")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:access-rule:manage')")
    public CommonResult<List<DccDirectoryAccessRuleDirectoryRespVO>> listAccessRuleDirectories() {
        List<DccDirectoryAccessRuleDirectorySummary> directories = directoryAdminService.listAccessRuleDirectories();
        return success(convertList(directories, item -> {
            DccDirectoryAccessRuleDirectoryRespVO respVO = new DccDirectoryAccessRuleDirectoryRespVO();
            respVO.setId(item.id());
            respVO.setName(item.name());
            respVO.setDirectoryPath(item.directoryPath());
            return respVO;
        }));
    }

    @GetMapping("/{id:\\d+}/access-rules")
    @Operation(summary = "获取目录访问规则")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:access-rule:manage')")
    public CommonResult<List<DccDirectoryAccessRuleRespVO>> getAccessRules(@PathVariable("id") Long id) {
        List<DccDirectoryAccessRuleDO> rules = directoryAdminService.getAccessRules(id);
        return success(convertList(rules, item -> normalizeMergedReadPermission(
                BeanUtils.toBean(item, DccDirectoryAccessRuleRespVO.class))));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id:\\d+}/access-rules")
    @Operation(summary = "删除目录整组访问规则")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:access-rule:manage')")
    public CommonResult<Boolean> deleteAccessRules(@PathVariable("id") Long id) {
        directoryAdminService.deleteAccessRules(id);
        return success(true);
    }

    @PutMapping("/{id:\\d+}/access-rules")
    @Operation(summary = "替换目录访问规则")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:access-rule:manage')")
    public CommonResult<Boolean> replaceAccessRules(@PathVariable("id") Long id,
                                                    @Valid @RequestBody List<DccDirectoryAccessRuleSaveReqVO> reqVOList) {
        directoryAdminService.replaceAccessRules(id, reqVOList);
        return success(true);
    }

    private List<DccDirectoryRespVO> buildTree(List<DccDirectoryRespVO> directories) {
        Map<Long, DccDirectoryRespVO> directoryMap = new LinkedHashMap<>();
        directories.forEach(item -> directoryMap.put(item.getId(), item));
        List<DccDirectoryRespVO> roots = new ArrayList<>();
        for (DccDirectoryRespVO directory : directories) {
            if (directory.getParentId() == null) {
                roots.add(directory);
                continue;
            }
            DccDirectoryRespVO parent = directoryMap.get(directory.getParentId());
            if (parent == null) {
                roots.add(directory);
                continue;
            }
            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }
            parent.getChildren().add(directory);
        }
        return roots;
    }

    private DccDirectoryRespVO toDirectoryRespVO(DccVisibleDirectoryNode node) {
        DccDirectoryRespVO respVO = BeanUtils.toBean(node.directory(), DccDirectoryRespVO.class);
        respVO.setHasChildren(node.hasChildren());
        respVO.setDirectoryPath(node.directoryPath());
        return respVO;
    }

    private DccDirectoryAccessRuleRespVO normalizeMergedReadPermission(DccDirectoryAccessRuleRespVO respVO) {
        boolean mergedReadAllowed = Boolean.TRUE.equals(respVO.getCanQuery()) || Boolean.TRUE.equals(respVO.getCanPreview());
        respVO.setCanQuery(mergedReadAllowed);
        respVO.setCanPreview(mergedReadAllowed);
        return respVO;
    }
}
