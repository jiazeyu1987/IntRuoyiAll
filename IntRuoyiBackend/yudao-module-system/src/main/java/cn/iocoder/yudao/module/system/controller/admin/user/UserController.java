package cn.iocoder.yudao.module.system.controller.admin.user;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.*;
import cn.iocoder.yudao.module.system.convert.user.UserConvert;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.enums.common.SexEnum;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.service.user.UserDingTalkImportExcelParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Collections;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - 用户")
@RestController
@RequestMapping("/system/user")
@Validated
public class UserController {

    @Resource
    private AdminUserService userService;
    @Resource
    private DeptService deptService;
    @Resource
    private cn.iocoder.yudao.module.system.service.user.UserDisplayService userDisplayService;
    @Resource
    private cn.iocoder.yudao.module.system.service.dept.DeptSupervisorService supervisorService;

    @Resource
    private UserDingTalkImportExcelParser userDingTalkImportExcelParser;
    @Resource private cn.iocoder.yudao.module.system.service.user.AdminUserDirectSupervisorService directSupervisorService;

    @GetMapping("/direct-supervisor")
    @Operation(summary="查询用户直属主管关系")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<UserDirectSupervisorRespVO> getDirectSupervisor(@RequestParam("userId") Long userId) {
        return success(directSupervisorService.getRelation(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId(),userId));
    }

    @PutMapping("/direct-supervisor")
    @Operation(summary="独立设置或清空用户直属主管")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<UserDirectSupervisorRespVO> saveDirectSupervisor(@Valid @RequestBody UserDirectSupervisorSaveReqVO request) {
        return success(directSupervisorService.saveRelation(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId(),request));
    }

    @PostMapping("/create")
    @Operation(summary = "新增用户")
    @PreAuthorize("@ss.hasPermission('system:user:create')")
    public CommonResult<Long> createUser(@Valid @RequestBody UserSaveReqVO reqVO) {
        Long id = userService.createUser(reqVO);
        return success(id);
    }

    @PutMapping("update")
    @Operation(summary = "修改用户")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> updateUser(@Valid @RequestBody UserSaveReqVO reqVO) {
        userService.updateUser(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除用户")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> deleteUser(@RequestParam("id") Long id) {
        userService.deleteUser(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @Operation(summary = "批量删除用户")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> deleteUserList(@RequestParam("ids") List<Long> ids) {
        userService.deleteUserList(ids);
        return success(true);
    }

    @PutMapping("/update-password")
    @ApiAccessLog(requestEnable = false, sanitizeKeys = {"password"})
    @Operation(summary = "重置用户密码")
    @PreAuthorize("@ss.hasPermission('system:user:update-password')")
    public CommonResult<Boolean> updateUserPassword(@Valid @RequestBody UserUpdatePasswordReqVO reqVO) {
        userService.updateUserPassword(reqVO.getId(), reqVO.getPassword());
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改用户状态")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> updateUserStatus(@Valid @RequestBody UserUpdateStatusReqVO reqVO) {
        userService.updateUserStatus(reqVO.getId(), reqVO.getStatus());
        return success(true);
    }

    @PutMapping("/lifecycle-deactivation")
    @Operation(summary = "登记离职/转岗账号联动停用")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> recordUserLifecycleDeactivation(
            @Valid @RequestBody UserLifecycleDeactivateReqVO reqVO) {
        userService.recordUserLifecycleDeactivation(reqVO);
        return success(true);
    }

    @PutMapping("/lock")
    @Operation(summary = "锁定用户30分钟")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> lockUser(@RequestParam("id") Long id,
            @RequestParam("reason") @NotBlank(message = "锁定原因不能为空") String reason) {
        userService.lockUser(id, reason);
        return success(true);
    }

    @PutMapping("/unlock")
    @Operation(summary = "解锁用户")
    @Parameters({
            @Parameter(name = "id", description = "编号", required = true, example = "1024"),
            @Parameter(name = "reason", description = "解锁原因", required = true, example = "误输密码后经线下核验确认本人")
    })
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> unlockUser(@RequestParam("id") Long id,
                                            @RequestParam("reason") @NotBlank(message = "解锁原因不能为空") String reason) {
        userService.resetUserLoginFailure(id, reason);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得用户分页列表")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<PageResult<UserRespVO>> getUserPage(@Valid UserPageReqVO pageReqVO) {
        // 获得用户分页列表
        PageResult<AdminUserDO> pageResult = userService.getUserPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(new PageResult<>(pageResult.getTotal()));
        }
        // 拼接数据
        return success(new PageResult<>(userDisplayService.describe(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/selection-page")
    @Operation(summary = "获得人员选择候选分页（精简身份信息）")
    public CommonResult<PageResult<UserSelectionRespVO>> getSelectionPage(@Valid UserPageReqVO reqVO) {
        PageResult<AdminUserDO> page = userService.getUserPage(reqVO);
        return success(new PageResult<>(toSelectionUsers(page.getList()), page.getTotal()));
    }

    @GetMapping("/selection-list")
    @Operation(summary = "按编号回显人员精简身份信息")
    public CommonResult<List<UserSelectionRespVO>> getSelectionList(
            @RequestParam("ids") @jakarta.validation.constraints.Size(min = 1, max = 200) List<Long> ids,
            @Valid UserPageReqVO reqVO) {
        reqVO.setIds(ids);
        reqVO.setPageNo(1);
        reqVO.setPageSize(200);
        return success(toSelectionUsers(userService.getUserPage(reqVO).getList()));
    }

    private List<UserSelectionRespVO> toSelectionUsers(List<AdminUserDO> users) {
        Map<Long, DeptDO> depts = deptService.getDeptMap(convertSet(users, AdminUserDO::getDeptId));
        return users.stream().map(user -> {
            UserSelectionRespVO row = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(user, UserSelectionRespVO.class);
            DeptDO dept = depts.get(user.getDeptId());
            if (dept != null) row.setDeptName(dept.getName());
            return row;
        }).collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/department-tree")
    @Operation(summary = "用户查询所需的组织树及主管状态")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<List<cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO>> getDepartmentTree() {
        List<DeptDO> depts = deptService.getSimpleDeptList();
        var supervisors = supervisorService.describe(depts);
        var result = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(depts,
                cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO.class);
        result.forEach(dept -> dept.setSupervisor(supervisors.get(dept.getId())));
        return success(result);
    }

    @GetMapping("/supervisor-impact")
    @Operation(summary = "预览账号停用或删除将解除的主管配置")
    @PreAuthorize("@ss.hasAnyPermissions('system:user:update', 'system:user:delete')")
    public CommonResult<List<cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO>> getSupervisorImpact(
            @RequestParam("ids") @jakarta.validation.constraints.Size(min = 1, max = 200) List<Long> ids) {
        for (Long id : ids) {
            if (id == null || userService.getUser(id) == null) {
                throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                        cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_NOT_EXISTS);
            }
        }
        return success(cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(
                supervisorService.getAssignments(ids),
                cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得用户详情列表")
    @Parameter(name = "ids", description = "编号列表", required = true, example = "[1024]")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<List<UserRespVO>> getUserList(@RequestParam("ids") List<Long> ids) {
        List<AdminUserDO> list = userService.getUserList(ids);
        if (CollUtil.isEmpty(list)) {
            return success(Collections.emptyList());
        }
        // 拼接数据
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(convertSet(list, AdminUserDO::getDeptId));
        return success(UserConvert.INSTANCE.convertList(list, deptMap));
    }

    @GetMapping({"/list-all-simple", "/simple-list"})
    @Operation(summary = "获取用户精简信息列表", description = "只包含被开启的用户，主要用于前端的下拉选项")
    public CommonResult<List<UserSimpleRespVO>> getSimpleUserList() {
        List<AdminUserDO> list = userService.getUserListByStatus(CommonStatusEnum.ENABLE.getStatus());
        // 拼接数据
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(list, AdminUserDO::getDeptId));
        return success(UserConvert.INSTANCE.convertSimpleList(list, deptMap));
    }

    @GetMapping("/get")
    @Operation(summary = "获得用户详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<UserRespVO> getUser(@RequestParam("id") Long id) {
        AdminUserDO user = userService.getUser(id);
        if (user == null) {
            return success(null);
        }
        // 拼接数据
        DeptDO dept = deptService.getDept(user.getDeptId());
        return success(UserConvert.INSTANCE.convert(user, dept));
    }

    @GetMapping("/get-for-update")
    @Operation(summary = "获得用户编辑详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<UserEditRespVO> getUserForUpdate(@RequestParam("id") Long id) {
        return success(userService.getUserForUpdate(id));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出用户")
    @PreAuthorize("@ss.hasPermission('system:user:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportUserList(@Validated UserPageReqVO exportReqVO,
                                HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AdminUserDO> list = userService.getUserPage(exportReqVO).getList();
        // 输出 Excel
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(list, AdminUserDO::getDeptId));
        ExcelUtils.write(response, "用户数据.xls", "数据", UserRespVO.class,
                UserConvert.INSTANCE.convertList(list, deptMap));
    }

    @GetMapping("/export-generic-account-excel")
    @Operation(summary = "导出通用账户不合规清单")
    @PreAuthorize("@ss.hasPermission('system:user:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportGenericAccountUserList(HttpServletResponse response) throws IOException {
        List<AdminUserDO> list = userService.getGenericAccountUserList();
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(list, AdminUserDO::getDeptId));
        ExcelUtils.write(response, "通用账户不合规清单.xls", "数据", UserRespVO.class,
                UserConvert.INSTANCE.convertList(list, deptMap));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得导入用户模板")
    public void importTemplate(HttpServletResponse response) throws IOException {
        // 手动创建导出 demo
        List<UserImportExcelVO> list = Arrays.asList(
                UserImportExcelVO.builder().username("yunai").deptId(1L).email("yunai@iocoder.cn").mobile("15601691300")
                        .nickname("瑛泰").status(CommonStatusEnum.ENABLE.getStatus()).sex(SexEnum.MALE.getSex()).build(),
                UserImportExcelVO.builder().username("yuanma").deptId(2L).email("yuanma@iocoder.cn").mobile("15601701300")
                        .nickname("源码").status(CommonStatusEnum.DISABLE.getStatus()).sex(SexEnum.FEMALE.getSex()).build()
        );
        // 输出
        ExcelUtils.write(response, "用户导入模板.xls", "用户列表", UserImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入用户")
    @Parameters({
            @Parameter(name = "file", description = "Excel 文件", required = true),
            @Parameter(name = "updateSupport", description = "是否支持更新，默认为 false", example = "true")
    })
    @PreAuthorize("@ss.hasPermission('system:user:import')")
    public CommonResult<UserImportRespVO> importExcel(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport) throws Exception {
        List<UserImportExcelVO> list = ExcelUtils.read(file, UserImportExcelVO.class);
        return success(userService.importUserList(list, updateSupport));
    }

    @PostMapping("/import-dingtalk")
    @Operation(summary = "导入钉钉组织用户")
    @Parameter(name = "file", description = "Excel 文件", required = true)
    @PreAuthorize("@ss.hasPermission('system:user:import')")
    public CommonResult<UserDingTalkImportRespVO> importDingTalkExcel(@RequestParam("file") MultipartFile file) throws Exception {
        List<UserDingTalkImportExcelVO> list = userDingTalkImportExcelParser.parse(file.getInputStream());
        return success(userService.importDingTalkUserList(list));
    }

}
