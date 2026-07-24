package cn.iocoder.yudao.module.system.controller.admin.permission;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.role.RolePageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.role.RoleRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleCategoryDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.permission.RoleConfigPackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static java.util.Collections.singleton;

@Tag(name = "管理后台 - 角色")
@RestController
@RequestMapping("/system/role")
@Validated
public class RoleController {

    @Resource
    private RoleService roleService;
    @Resource
    private RoleConfigPackageService roleConfigPackageService;

    @PostMapping("/create")
    @Operation(summary = "创建角色")
    @PreAuthorize("@ss.hasPermission('system:role:create')")
    public CommonResult<Long> createRole(@Valid @RequestBody RoleSaveReqVO createReqVO) {
        return success(roleService.createRole(createReqVO, null));
    }

    @PutMapping("/update")
    @Operation(summary = "修改角色")
    @PreAuthorize("@ss.hasPermission('system:role:update')")
    public CommonResult<Boolean> updateRole(@Valid @RequestBody RoleSaveReqVO updateReqVO) {
        roleService.updateRole(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除角色")
    @Parameter(name = "id", description = "角色编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:role:delete')")
    public CommonResult<Boolean> deleteRole(@RequestParam("id") Long id) {
        roleService.deleteRole(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除角色")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('system:role:delete')")
    public CommonResult<Boolean> deleteRoleList(@RequestParam("ids") List<Long> ids) {
        roleService.deleteRoleList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得角色信息")
    @PreAuthorize("@ss.hasPermission('system:role:query')")
    public CommonResult<RoleRespVO> getRole(@RequestParam("id") Long id) {
        RoleDO role = roleService.getRole(id);
        return success(buildRoleRespVO(role));
    }

    @GetMapping("/page")
    @Operation(summary = "获得角色分页")
    @PreAuthorize("@ss.hasPermission('system:role:query')")
    public CommonResult<PageResult<RoleRespVO>> getRolePage(RolePageReqVO pageReqVO) {
        PageResult<RoleDO> pageResult = roleService.getRolePage(pageReqVO);
        PageResult<RoleRespVO> result = BeanUtils.toBean(pageResult, RoleRespVO.class);
        fillCategoryInfo(result.getList(), pageResult.getList());
        fillAssignedUserCount(result.getList(), pageResult.getList());
        return success(result);
    }

    @GetMapping({"/list-all-simple", "/simple-list"})
    @Operation(summary = "获取角色精简信息列表", description = "只包含被开启的角色，主要用于前端的下拉选项")
    public CommonResult<List<RoleRespVO>> getSimpleRoleList() {
        List<RoleDO> list = roleService.getRoleListByStatus(singleton(CommonStatusEnum.ENABLE.getStatus()));
        list.sort(Comparator.comparing(RoleDO::getSort));
        List<RoleRespVO> result = BeanUtils.toBean(list, RoleRespVO.class);
        fillCategoryInfo(result, list);
        fillAssignedUserCount(result, list);
        return success(result);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出角色 Excel")
    @ApiAccessLog(operateType = EXPORT)
    @PreAuthorize("@ss.hasPermission('system:role:export')")
    public void export(HttpServletResponse response, @Validated RolePageReqVO exportReqVO) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<RoleDO> list = roleService.getRolePage(exportReqVO).getList();
        // 输出
        ExcelUtils.write(response, "角色数据.xls", "数据", RoleRespVO.class,
                buildRoleRespVOList(list));
    }

    @GetMapping("/config-package/export")
    @Operation(summary = "导出角色配置包")
    @PreAuthorize("@ss.hasPermission('system:role:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportConfigPackage(HttpServletResponse response) throws IOException {
        byte[] data = roleConfigPackageService.exportPackage();
        response.addHeader("Content-Disposition",
                "attachment;filename=" + HttpUtils.encodeUtf8("权限角色配置包.json"));
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(data);
    }

    @PostMapping("/config-package/import")
    @Operation(summary = "导入角色配置包")
    @PreAuthorize("@ss.hasPermission('system:role:create') and @ss.hasPermission('system:role:update')")
    public CommonResult<Boolean> importConfigPackage(@RequestParam("file") org.springframework.web.multipart.MultipartFile file)
            throws IOException {
        roleConfigPackageService.importPackage(file.getBytes());
        return success(true);
    }

    private RoleRespVO buildRoleRespVO(RoleDO role) {
        RoleRespVO respVO = BeanUtils.toBean(role, RoleRespVO.class);
        if (role == null) {
            return respVO;
        }
        fillCategoryInfo(List.of(respVO), List.of(role));
        fillAssignedUserCount(List.of(respVO), List.of(role));
        return respVO;
    }

    private List<RoleRespVO> buildRoleRespVOList(List<RoleDO> roles) {
        List<RoleRespVO> result = BeanUtils.toBean(roles, RoleRespVO.class);
        fillCategoryInfo(result, roles);
        fillAssignedUserCount(result, roles);
        return result;
    }

    private void fillCategoryInfo(List<RoleRespVO> result, List<RoleDO> roles) {
        Collection<Long> categoryIds = roles.stream()
                .map(RoleDO::getCategoryId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, RoleCategoryDO> categoryMap = roleService.getRoleCategoryMap(categoryIds);
        for (int i = 0; i < result.size(); i++) {
            RoleCategoryDO category = categoryMap.get(roles.get(i).getCategoryId());
            if (category == null) {
                continue;
            }
            result.get(i).setCategoryName(category.getName());
            result.get(i).setCategoryCode(category.getCode());
        }
    }

    private void fillAssignedUserCount(List<RoleRespVO> result, List<RoleDO> roles) {
        Collection<Long> roleIds = roles.stream()
                .map(RoleDO::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, Long> countMap = roleService.getAssignedUserCountMap(roleIds);
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setAssignedUserCount(countMap.getOrDefault(roles.get(i).getId(), 0L));
        }
    }

}
