package cn.iocoder.yudao.module.system.controller.admin.auth;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.framework.security.config.SecurityProperties;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.*;
import cn.iocoder.yudao.module.system.convert.auth.AuthConvert;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.enums.logger.LoginLogTypeEnum;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import cn.iocoder.yudao.module.system.service.auth.AdminAuthService;
import cn.iocoder.yudao.module.system.service.permission.MenuService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.social.SocialClientService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 认证")
@RestController
@RequestMapping("/system/auth")
@Validated
@Slf4j
public class AuthController {

    private static final String SRM_ROOT_PATH = "/srm";
    private static final String ADMIN_USERNAME = "admin";
    private static final String DCC_ADMIN_MENU_PATH = "controlled-file/admin";
    private static final String DCC_ADMIN_MENU_COMPONENT_NAME = "DccControlledFileAdmin";
    private static final String PUHUI_SCHEDULE_MENU_PATH = "mes/pro/puhui-schedule";
    private static final String PUHUI_SCHEDULE_MENU_COMPONENT_NAME = "MesProPuhuiSchedule";

    @Resource
    private AdminAuthService authService;
    @Resource
    private AdminUserService userService;
    @Resource
    private RoleService roleService;
    @Resource
    private MenuService menuService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private SocialClientService socialClientService;

    @Resource
    private SecurityProperties securityProperties;

    @PostMapping("/login")
    @PermitAll
    @Operation(summary = "使用账号密码登录")
    public CommonResult<AuthLoginRespVO> login(@RequestBody @Valid AuthLoginReqVO reqVO) {
        return success(authService.login(reqVO));
    }

    @PostMapping("/logout")
    @PermitAll
    @Operation(summary = "登出系统")
    public CommonResult<Boolean> logout(HttpServletRequest request) {
        String token = SecurityFrameworkUtils.obtainAuthorization(request,
                securityProperties.getTokenHeader(), securityProperties.getTokenParameter());
        if (StrUtil.isNotBlank(token)) {
            authService.logout(token, LoginLogTypeEnum.LOGOUT_SELF.getType());
        }
        return success(true);
    }

    @PostMapping("/refresh-token")
    @PermitAll
    @Operation(summary = "刷新令牌")
    @Parameter(name = "refreshToken", description = "刷新令牌", required = true)
    public CommonResult<AuthLoginRespVO> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        return success(authService.refreshToken(refreshToken));
    }

    @GetMapping("/get-permission-info")
    @Operation(summary = "获取登录用户的权限信息")
    @DataPermission(enable = false) // 忽略数据权限，避免因为过滤，导致无法查询用户。类似：https://t.zsxq.com/LHnrp
    public CommonResult<AuthPermissionInfoRespVO> getPermissionInfo() {
        // 1.1 获得用户信息
        AdminUserDO user = userService.getUser(getLoginUserId());
        if (user == null) {
            return success(null);
        }

        // 1.2 获得角色列表
        Long loginUserId = getLoginUserId();
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(loginUserId);
        List<RoleDO> roles = CollUtil.isEmpty(roleIds) ? Collections.emptyList() : roleService.getRoleList(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())); // 移除禁用的角色

        // 1.3 获得菜单列表
        Set<Long> grantedMenuIds = new LinkedHashSet<>();
        grantedMenuIds.addAll(permissionService.getRoleMenuListByRoleId(convertSet(roles, RoleDO::getId)));
        grantedMenuIds.addAll(permissionService.getDynamicMenuListByUserId(loginUserId));
        List<MenuDO> permissionMenuList = getEnabledMenuList(grantedMenuIds);
        List<MenuDO> menuList = menuService.filterDisableMenus(getEnabledMenuList(
                expandMenuIdsWithParents(permissionMenuList)));
        if (roles.stream().noneMatch(role -> RoleCodeEnum.SRM_ADMIN.getCode().equals(role.getCode()))) {
            permissionMenuList = filterSrmMenus(permissionMenuList);
            menuList = filterSrmMenus(menuList);
        }
        if (!isAdminUser(user)) {
            permissionMenuList = filterDccAdminMenu(permissionMenuList);
            menuList = filterDccAdminMenu(menuList);
        }
        if (roles.stream().noneMatch(role -> RoleCodeEnum.PUHUI_SCHEDULE_ADMIN.getCode().equals(role.getCode()))) {
            permissionMenuList = filterPuhuiScheduleMenu(permissionMenuList);
            menuList = filterPuhuiScheduleMenu(menuList);
        }

        // 2. 拼接结果返回
        AuthPermissionInfoRespVO respVO = AuthConvert.INSTANCE.convert(user, roles, menuList);
        respVO.setPermissions(convertSet(permissionMenuList, MenuDO::getPermission));
        return success(respVO);
    }

    private List<MenuDO> getEnabledMenuList(Set<Long> menuIds) {
        return menuService.getMenuList(menuIds).stream()
                .filter(menu -> CommonStatusEnum.ENABLE.getStatus().equals(menu.getStatus()))
                .collect(Collectors.toList());
    }

    private Set<Long> expandMenuIdsWithParents(List<MenuDO> menuList) {
        Set<Long> menuIds = menuList.stream()
                .map(MenuDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> visitedParentIds = new LinkedHashSet<>();
        for (MenuDO menu : menuList) {
            appendParentMenuIds(menu.getParentId(), menuIds, visitedParentIds);
        }
        return menuIds;
    }

    private void appendParentMenuIds(Long parentId, Set<Long> menuIds, Set<Long> visitedParentIds) {
        while (parentId != null && !MenuDO.ID_ROOT.equals(parentId) && visitedParentIds.add(parentId)) {
            menuIds.add(parentId);
            MenuDO parent = menuService.getMenu(parentId);
            if (parent == null) {
                log.warn("[appendParentMenuIds][parentId({}) missing, dynamic menu will remain hidden]", parentId);
                return;
            }
            parentId = parent.getParentId();
        }
    }

    private static boolean isAdminUser(AdminUserDO user) {
        return user != null && ADMIN_USERNAME.equals(user.getUsername());
    }

    private static List<MenuDO> filterDccAdminMenu(List<MenuDO> menuList) {
        return menuList.stream()
                .filter(menu -> !isDccAdminMenu(menu))
                .collect(Collectors.toList());
    }

    private static boolean isDccAdminMenu(MenuDO menu) {
        if (menu == null) {
            return false;
        }
        return DCC_ADMIN_MENU_PATH.equals(normalizeMenuPath(menu.getPath()))
                || DCC_ADMIN_MENU_COMPONENT_NAME.equals(menu.getComponentName());
    }

    private static List<MenuDO> filterPuhuiScheduleMenu(List<MenuDO> menuList) {
        return menuList.stream()
                .filter(menu -> !isPuhuiScheduleMenu(menu))
                .collect(Collectors.toList());
    }

    private static boolean isPuhuiScheduleMenu(MenuDO menu) {
        if (menu == null) {
            return false;
        }
        return PUHUI_SCHEDULE_MENU_PATH.equals(normalizeMenuPath(menu.getPath()))
                || PUHUI_SCHEDULE_MENU_COMPONENT_NAME.equals(menu.getComponentName());
    }

    private static String normalizeMenuPath(String path) {
        if (path == null) {
            return "";
        }
        return path.replace("\\", "/").replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private static List<MenuDO> filterSrmMenus(List<MenuDO> menuList) {
        return menuList.stream()
                .filter(menu -> !isSrmMenu(menu, menuList))
                .collect(Collectors.toList());
    }

    private static boolean isSrmMenu(MenuDO menu, List<MenuDO> allMenus) {
        if (menu == null) {
            return false;
        }
        if (SRM_ROOT_PATH.equals(menu.getPath())) {
            return true;
        }
        Long currentParentId = menu.getParentId();
        while (currentParentId != null && currentParentId > 0) {
            Long parentId = currentParentId;
            MenuDO parent = allMenus.stream()
                    .filter(item -> item != null && parentId.equals(item.getId()))
                    .findFirst()
                    .orElse(null);
            if (parent == null) {
                return false;
            }
            if (SRM_ROOT_PATH.equals(parent.getPath())) {
                return true;
            }
            currentParentId = parent.getParentId();
        }
        return false;
    }

    @PostMapping("/register")
    @PermitAll
    @Operation(summary = "注册用户")
    public CommonResult<AuthLoginRespVO> register(@RequestBody @Valid AuthRegisterReqVO registerReqVO) {
        return success(authService.register(registerReqVO));
    }

    // ========== 短信登录相关 ==========

    @PostMapping("/sms-login")
    @PermitAll
    @Operation(summary = "使用短信验证码登录")
    // 可按需开启限流：https://github.com/YunaiV/ruoyi-vue-pro/issues/851
    // @RateLimiter(time = 60, count = 6, keyResolver = ExpressionRateLimiterKeyResolver.class, keyArg = "#reqVO.mobile")
    public CommonResult<AuthLoginRespVO> smsLogin(@RequestBody @Valid AuthSmsLoginReqVO reqVO) {
        return success(authService.smsLogin(reqVO));
    }

    @PostMapping("/send-sms-code")
    @PermitAll
    @Operation(summary = "发送手机验证码")
    public CommonResult<Boolean> sendLoginSmsCode(@RequestBody @Valid AuthSmsSendReqVO reqVO) {
        authService.sendSmsCode(reqVO);
        return success(true);
    }

    @PostMapping("/reset-password")
    @PermitAll
    @Operation(summary = "重置密码")
    public CommonResult<Boolean> resetPassword(@RequestBody @Valid AuthResetPasswordReqVO reqVO) {
        authService.resetPassword(reqVO);
        return success(true);
    }

    // ========== 社交登录相关 ==========

    @GetMapping("/social-auth-redirect")
    @PermitAll
    @Operation(summary = "社交授权的跳转")
    @Parameters({
            @Parameter(name = "type", description = "社交类型", required = true),
            @Parameter(name = "redirectUri", description = "回调路径")
    })
    public CommonResult<String> socialLogin(@RequestParam("type") Integer type,
                                            @RequestParam("redirectUri") String redirectUri) {
        return success(socialClientService.getAuthorizeUrl(
                type, UserTypeEnum.ADMIN.getValue(), redirectUri));
    }

    @PostMapping("/social-login")
    @PermitAll
    @Operation(summary = "社交快捷登录，使用 code 授权码", description = "适合未登录的用户，但是社交账号已绑定用户")
    public CommonResult<AuthLoginRespVO> socialQuickLogin(@RequestBody @Valid AuthSocialLoginReqVO reqVO) {
        return success(authService.socialLogin(reqVO));
    }

}
