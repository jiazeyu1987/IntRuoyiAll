package cn.iocoder.yudao.module.system.controller.admin.permission;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.rolecategory.RoleCategoryRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.rolecategory.RoleCategorySaveReqVO;
import cn.iocoder.yudao.module.system.service.permission.RoleCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 角色分类")
@RestController
@RequestMapping("/system/role-category")
@Validated
public class RoleCategoryController {

    @Resource
    private RoleCategoryService roleCategoryService;

    @PostMapping("/create")
    @Operation(summary = "创建角色分类")
    @PreAuthorize("@ss.hasPermission('system:role-category:create')")
    public CommonResult<Long> createRoleCategory(@Valid @RequestBody RoleCategorySaveReqVO createReqVO) {
        return success(roleCategoryService.createRoleCategory(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改角色分类")
    @PreAuthorize("@ss.hasPermission('system:role-category:update')")
    public CommonResult<Boolean> updateRoleCategory(@Valid @RequestBody RoleCategorySaveReqVO updateReqVO) {
        roleCategoryService.updateRoleCategory(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除角色分类")
    @Parameter(name = "id", description = "分类编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('system:role-category:delete')")
    public CommonResult<Boolean> deleteRoleCategory(@RequestParam("id") Long id) {
        roleCategoryService.deleteRoleCategory(id);
        return success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "获得角色分类列表")
    @PreAuthorize("@ss.hasPermission('system:role-category:query')")
    public CommonResult<List<RoleCategoryRespVO>> getRoleCategoryList() {
        return success(BeanUtils.toBean(roleCategoryService.getRoleCategoryList(), RoleCategoryRespVO.class));
    }

    @GetMapping("/enabled-list")
    @Operation(summary = "获得启用角色分类列表")
    @PreAuthorize("@ss.hasPermission('system:role-category:query')")
    public CommonResult<List<RoleCategoryRespVO>> getEnabledRoleCategoryList() {
        return success(BeanUtils.toBean(roleCategoryService.getEnabledRoleCategoryList(), RoleCategoryRespVO.class));
    }

}
