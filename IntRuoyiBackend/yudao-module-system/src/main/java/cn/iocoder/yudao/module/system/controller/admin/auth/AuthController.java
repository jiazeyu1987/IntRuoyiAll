package cn.iocoder.yudao.module.system.controller.admin.auth;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.framework.security.config.SecurityProperties;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
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
import cn.iocoder.yudao.module.system.service.invoicevoucherprintassistant.InvoiceVoucherPrintAssistantService;
import cn.iocoder.yudao.module.system.service.invoicevoucherprintassistant.InvoiceVoucherPrintKingdeeConfigProvider;
import cn.iocoder.yudao.module.system.service.invoicevoucherprintassistant.InvoiceVoucherPrintKingdeeConfigProvider.KingdeeConfigSnapshot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.system.dal.redis.RedisKeyConstants.INVOICE_VOUCHER_PRINT_TICKET;

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
    private static final String FINANCE_INVOICE_VOUCHER_PRINT_ROLE_CODE = "finance_invoice_voucher_print";
    private static final String INVOICE_VOUCHER_PRINT_MENU_PATH = "invoice-voucher-print";
    private static final String INVOICE_VOUCHER_PRINT_MENU_COMPONENT = "erp/finance/invoice-voucher-print/index";
    private static final String INVOICE_VOUCHER_PRINT_MENU_COMPONENT_NAME = "ErpInvoiceVoucherPrint";
    private static final String INVOICE_VOUCHER_PRINT_PERMISSION = "erp:invoice-voucher-print:query";
    private static final long INVOICE_VOUCHER_PRINT_TICKET_TTL_SECONDS = 120L;
    private static final String INVOICE_VOUCHER_PRINT_TICKET_SEPARATOR = "|";
    private static final String KINGDEE_CONFIG_PREFIX = "发票凭证打印助手 ERP 配置缺失：";

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
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private InvoiceVoucherPrintAssistantService invoiceVoucherPrintAssistantService;
    @Resource
    private InvoiceVoucherPrintKingdeeConfigProvider kingdeeConfigProvider;

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
        List<RoleDO> roles = getEnabledRoleList(loginUserId);

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
        if (roles.stream().noneMatch(role -> FINANCE_INVOICE_VOUCHER_PRINT_ROLE_CODE.equals(role.getCode()))) {
            permissionMenuList = filterInvoiceVoucherPrintMenu(permissionMenuList);
            menuList = filterInvoiceVoucherPrintMenu(menuList);
        }

        // 2. 拼接结果返回
        AuthPermissionInfoRespVO respVO = AuthConvert.INSTANCE.convert(user, roles, menuList);
        respVO.setPermissions(convertSet(permissionMenuList, MenuDO::getPermission));
        return success(respVO);
    }

    @PostMapping("/invoice-voucher-print-ticket")
    @Operation(summary = "创建发票凭证打印助手访问票据")
    @DataPermission(enable = false)
    public CommonResult<AuthInvoiceVoucherPrintTicketRespVO> createInvoiceVoucherPrintTicket() {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null || userService.getUser(loginUserId) == null) {
            throw exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "账号未登录");
        }
        List<RoleDO> roles = getEnabledRoleList(loginUserId);
        if (!hasInvoiceVoucherPrintRole(roles)) {
            throw exception0(GlobalErrorCodeConstants.FORBIDDEN.getCode(), "没有发票凭证打印权限");
        }

        String ticket = UUID.randomUUID().toString();
        LocalDateTime expiresTime = LocalDateTime.now().plusSeconds(INVOICE_VOUCHER_PRINT_TICKET_TTL_SECONDS);
        stringRedisTemplate.opsForValue().set(formatInvoiceVoucherPrintTicketKey(ticket),
                buildInvoiceVoucherPrintTicketPayload(loginUserId, expiresTime),
                INVOICE_VOUCHER_PRINT_TICKET_TTL_SECONDS, TimeUnit.SECONDS);
        return success(AuthInvoiceVoucherPrintTicketRespVO.builder()
                .ticket(ticket)
                .expiresTime(expiresTime)
                .build());
    }

    @GetMapping("/invoice-voucher-print-assistant/status")
    @Operation(summary = "获得发票凭证打印助手运行状态")
    @PreAuthorize("@ss.hasPermission('" + INVOICE_VOUCHER_PRINT_PERMISSION + "')")
    public CommonResult<AuthInvoiceVoucherPrintAssistantStatusRespVO> getInvoiceVoucherPrintAssistantStatus() {
        return success(invoiceVoucherPrintAssistantService.getStatus());
    }

    @PostMapping("/invoice-voucher-print-assistant/start")
    @Operation(summary = "启动发票凭证打印助手")
    @PreAuthorize("@ss.hasPermission('" + INVOICE_VOUCHER_PRINT_PERMISSION + "')")
    public CommonResult<AuthInvoiceVoucherPrintAssistantStatusRespVO> startInvoiceVoucherPrintAssistant() {
        return success(invoiceVoucherPrintAssistantService.start());
    }

    @PostMapping("/invoice-voucher-print-ticket/validate")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "校验发票凭证打印助手访问票据")
    public CommonResult<AuthInvoiceVoucherPrintTicketValidateRespVO> validateInvoiceVoucherPrintTicket(
            @RequestBody @Valid AuthInvoiceVoucherPrintTicketValidateReqVO reqVO) {
        String ticket = reqVO.getTicket().trim();
        String redisKey = formatInvoiceVoucherPrintTicketKey(ticket);
        String payload = stringRedisTemplate.opsForValue().get(redisKey);
        if (StrUtil.isBlank(payload)) {
            return success(invalidInvoiceVoucherPrintTicket("missing"));
        }

        AuthInvoiceVoucherPrintTicketValidateRespVO respVO = parseInvoiceVoucherPrintTicket(payload);
        stringRedisTemplate.delete(redisKey);
        if (!Boolean.TRUE.equals(respVO.getValid())) {
            return success(respVO);
        }
        if (respVO.getExpiresTime().isBefore(LocalDateTime.now())) {
            return success(invalidInvoiceVoucherPrintTicket("expired"));
        }
        if (!INVOICE_VOUCHER_PRINT_PERMISSION.equals(respVO.getPermission())) {
            return success(invalidInvoiceVoucherPrintTicket("permission_mismatch"));
        }
        respVO.setKingdeeConfig(buildInvoiceVoucherPrintKingdeeConfig());
        return success(respVO);
    }

    private AuthInvoiceVoucherPrintTicketValidateRespVO.KingdeeConfig buildInvoiceVoucherPrintKingdeeConfig() {
        KingdeeConfigSnapshot snapshot = kingdeeConfigProvider.getCurrentConfigSnapshot();
        if (snapshot == null) {
            throw exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode(),
                    KINGDEE_CONFIG_PREFIX + "kingdeeConfig");
        }
        return AuthInvoiceVoucherPrintTicketValidateRespVO.KingdeeConfig.builder()
                .baseUrl(snapshot.getBaseUrl())
                .acctId(snapshot.getAcctId())
                .username(snapshot.getUsername())
                .password(snapshot.getPassword())
                .appId(snapshot.getAppId())
                .signedData(snapshot.getSignedData())
                .timestamp(snapshot.getTimestamp())
                .lcid(snapshot.getLcid())
                .build();
    }

    private List<MenuDO> getEnabledMenuList(Set<Long> menuIds) {
        return menuService.getMenuList(menuIds).stream()
                .filter(menu -> CommonStatusEnum.ENABLE.getStatus().equals(menu.getStatus()))
                .collect(Collectors.toList());
    }

    private List<RoleDO> getEnabledRoleList(Long loginUserId) {
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(loginUserId);
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        return roleService.getRoleList(roleIds).stream()
                .filter(role -> CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()))
                .collect(Collectors.toList());
    }

    private static boolean hasInvoiceVoucherPrintRole(List<RoleDO> roles) {
        return roles.stream().anyMatch(role -> FINANCE_INVOICE_VOUCHER_PRINT_ROLE_CODE.equals(role.getCode()));
    }

    private static String formatInvoiceVoucherPrintTicketKey(String ticket) {
        return String.format(INVOICE_VOUCHER_PRINT_TICKET, ticket);
    }

    private static String buildInvoiceVoucherPrintTicketPayload(Long userId, LocalDateTime expiresTime) {
        return userId + INVOICE_VOUCHER_PRINT_TICKET_SEPARATOR
                + INVOICE_VOUCHER_PRINT_PERMISSION + INVOICE_VOUCHER_PRINT_TICKET_SEPARATOR
                + expiresTime;
    }

    private static AuthInvoiceVoucherPrintTicketValidateRespVO parseInvoiceVoucherPrintTicket(String payload) {
        String[] parts = payload.split("\\|", -1);
        if (parts.length != 3) {
            return invalidInvoiceVoucherPrintTicket("malformed");
        }
        try {
            return AuthInvoiceVoucherPrintTicketValidateRespVO.builder()
                    .valid(true)
                    .userId(Long.valueOf(parts[0]))
                    .permission(parts[1])
                    .expiresTime(LocalDateTime.parse(parts[2]))
                    .build();
        } catch (RuntimeException ex) {
            return invalidInvoiceVoucherPrintTicket("malformed");
        }
    }

    private static AuthInvoiceVoucherPrintTicketValidateRespVO invalidInvoiceVoucherPrintTicket(String reason) {
        return AuthInvoiceVoucherPrintTicketValidateRespVO.builder()
                .valid(false)
                .reason(reason)
                .build();
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

    private static List<MenuDO> filterInvoiceVoucherPrintMenu(List<MenuDO> menuList) {
        return menuList.stream()
                .filter(menu -> !isInvoiceVoucherPrintMenu(menu))
                .collect(Collectors.toList());
    }

    private static boolean isInvoiceVoucherPrintMenu(MenuDO menu) {
        if (menu == null) {
            return false;
        }
        return INVOICE_VOUCHER_PRINT_PERMISSION.equals(menu.getPermission())
                || INVOICE_VOUCHER_PRINT_MENU_PATH.equals(normalizeMenuPath(menu.getPath()))
                || INVOICE_VOUCHER_PRINT_MENU_COMPONENT.equals(normalizeMenuPath(menu.getComponent()))
                || INVOICE_VOUCHER_PRINT_MENU_COMPONENT_NAME.equals(menu.getComponentName());
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
