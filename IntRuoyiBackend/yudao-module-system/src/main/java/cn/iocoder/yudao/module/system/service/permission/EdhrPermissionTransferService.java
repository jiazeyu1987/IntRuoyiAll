package cn.iocoder.yudao.module.system.service.permission;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.rolecategory.RoleCategorySaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.*;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.*;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.enums.permission.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import jakarta.annotation.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.system.service.permission.EdhrPermissionWorkbook.*;

/** Tenant-local commands only. The uploaded workbook never selects a tenant or database ID. */
@Service
public class EdhrPermissionTransferService {
    private static final String CATEGORY = "EDHR_PERMISSION_TRANSFER";
    private static final String CATEGORY_NAME = "eDHR权限迁移";
    private static final Set<String> BUSINESS_ROLES = Set.of("MES_PQC_RELEASE_OWNER", "MES_MANAGEMENT_REPRESENTATIVE",
            "pqc_permission", "pqc_leader_permission");
    private final Map<String, CatalogEntry> catalog;
    // Preview receipts expire after 15 minutes and after a service restart; no client-authored plan is accepted.
    private final byte[] receiptSecret = new byte[32];
    @Resource private RoleMapper roleMapper;
    @Resource private MenuMapper menuMapper;
    @Resource private AdminUserMapper userMapper;
    @Resource private PermissionService permissionService;
    @Resource private RoleService roleService;
    @Resource private RoleCategoryService categoryService;
    @Resource private TenantService tenantService;
    @Resource private ApprovalAuthorityApi approvalAuthority;
    @Resource private GxpAuditService gxpAuditService;

    public record CatalogEntry(String permission, String purpose, String sources) { }
    private record MenuIdentity(String permission, String path) { }
    public record MenuDetail(Long id, String name, String permission, String purpose, boolean added) { }
    public record RolePlan(String sourceCode, String code, String name, String dataScope, Long roleId,
                           Set<Long> existingMenuIds, Set<Long> menuIds, List<MenuDetail> menus,
                           List<String> existingMembers) { }
    public record AccountPlan(String sourceUsername, String sourceName, String targetUsername, String targetName,
                              Long userId, Set<Long> existingRoleIds, List<String> existingRoles,
                              Set<String> roleCodes, List<String> addedRoles) { }
    public record AccountMapping(String sourceUsername, String sourceName, String targetUsername) { }
    public record Preview(List<String> errors, List<RolePlan> roles, List<AccountPlan> accounts,
                          List<AccountMapping> mappings, Long categoryId, boolean createCategory, String token) { }
    public record ImportResult(int createdRoles, int changedRoles, int changedAccounts, boolean unchanged) { }

    public EdhrPermissionTransferService() throws IOException {
        new SecureRandom().nextBytes(receiptSecret);
        try (var input = new ClassPathResource("edhr-permission-transfer-catalog.json").getInputStream()) {
            var entries = JsonUtils.parseArray(new String(input.readAllBytes(), StandardCharsets.UTF_8), CatalogEntry.class);
            var map = new TreeMap<String, CatalogEntry>();
            for (var entry : entries) {
                if (!entry.permission().startsWith("mes:") || entry.purpose().isBlank()
                        || map.put(entry.permission(), entry) != null) throw new IllegalStateException("Invalid eDHR catalog");
            }
            if (map.isEmpty()) throw new IllegalStateException("Empty eDHR catalog");
            catalog = Collections.unmodifiableMap(map);
        }
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public byte[] exportWorkbook() throws IOException {
        requireTenant(SOURCE);
        Map<Long, MenuDO> menus = menuMapper.selectList().stream().collect(Collectors.toMap(MenuDO::getId, Function.identity()));
        List<List<String>> roleRows = new ArrayList<>();
        List<List<String>> accountRows = new ArrayList<>();
        List<AdminUserDO> users = userMapper.selectList().stream().sorted(Comparator.comparing(AdminUserDO::getUsername)).toList();
        for (RoleDO role : roleMapper.selectList().stream().sorted(Comparator.comparing(RoleDO::getCode)).toList()) {
            if (!Objects.equals(role.getType(), RoleTypeEnum.CUSTOM.getType()) || isAdmin(role.getCode())
                    || !enabled(role.getStatus())) continue;
            List<MenuDO> granted = permissionService.getRoleMenuListByRoleId(role.getId()).stream().map(menus::get)
                    .filter(Objects::nonNull).filter(m -> m.getPermission() != null && catalog.containsKey(m.getPermission()))
                    .sorted(Comparator.comparing(MenuDO::getPermission).thenComparing(MenuDO::getId)).toList();
            if (granted.isEmpty() && !BUSINESS_ROLES.contains(role.getCode())) continue;
            String scope = scopeName(role.getDataScope());
            String code = targetCode(role.getCode());
            String name = BUSINESS_ROLES.contains(code) ? role.getName() : "eDHR-" + role.getName();
            if (name.length() > 30) name = name.substring(0, 30);
            if (granted.isEmpty()) roleRows.add(List.of(role.getCode(), code, name, scope, "", "", "精确业务身份角色；不隐含菜单权限"));
            for (MenuDO menu : granted) roleRows.add(List.of(role.getCode(), code, name, scope,
                    menu.getPermission(), menuPath(menu, menus), catalog.get(menu.getPermission()).purpose()));
            Set<Long> members = permissionService.getUserRoleIdListByRoleId(Set.of(role.getId()));
            for (AdminUserDO user : users) if (members.contains(user.getId())) {
                accountRows.add(List.of(user.getUsername(), Objects.toString(user.getNickname(), ""),
                        enabled(user.getStatus()) ? "启用" : "停用", user.getUsername(), role.getCode(), code));
            }
        }
        return write(accountRows, roleRows, catalog.values().stream()
                .map(c -> List.of(c.permission(), c.purpose(), c.sources())).toList(),
                ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toString());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Preview preview(byte[] bytes, String mappings) throws IOException {
        requireTenant(TARGET);
        Content content = mappedContent(bytes, mappings);
        lockAuthority();
        Preview plan = buildPlan(content);
        String token = plan.errors().isEmpty() ? receipt(bytes, plan, Instant.now().plusSeconds(900).getEpochSecond()) : "";
        return new Preview(plan.errors(), plan.roles(), plan.accounts(), plan.mappings(), plan.categoryId(), plan.createCategory(), token);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public ImportResult confirm(byte[] bytes, String mappings, String token, String reason) throws IOException {
        requireTenant(TARGET);
        if (reason == null || reason.trim().length() < 5 || reason.length() > 500) throw invalid("请填写5至500字的授权变更原因");
        Content content = mappedContent(bytes, mappings);
        lockAuthority();
        Preview plan = buildPlan(content);
        if (!plan.errors().isEmpty()) throw invalid(String.join("；", plan.errors()));
        verifyReceipt(bytes, plan, token);
        int created = 0, changed = 0, accounts = 0;
        String commandId = "edhr-transfer:" + UUID.randomUUID();
        String auditReason = "eDHR Excel权限迁移：" + reason.trim();
        Long categoryId = plan.categoryId();
        if (plan.createCategory()) {
            var category = new RoleCategorySaveReqVO();
            category.setCode(CATEGORY); category.setName(CATEGORY_NAME); category.setStatus(0); category.setSort(100);
            category.setRemark("Excel迁移创建的eDHR专用业务角色；不承载管理员身份");
            categoryId = categoryService.createRoleCategory(category);
        }
        Map<String, Long> ids = new TreeMap<>();
        for (RolePlan role : plan.roles()) {
            Long id = role.roleId();
            if (id == null) {
                var req = new RoleSaveReqVO();
                req.setCode(role.code()); req.setName(role.name()); req.setStatus(0); req.setSort(100);
                req.setCategoryId(categoryId); req.setRemark("来自芋道源码的eDHR显式授权，来源角色：" + role.sourceCode());
                id = roleService.createRole(req, RoleTypeEnum.CUSTOM.getType());
                created++;
                // RoleService creates ALL; explicitly apply a narrower source scope through the audited service.
                if ("仅本人".equals(role.dataScope())) permissionService.assignRoleDataScope(id, DataScopeEnum.SELF.getScope(),
                        Set.of(), auditReason, commandId + ":scope:" + id);
            }
            if (!role.existingMenuIds().equals(role.menuIds())) {
                permissionService.assignRoleMenu(id, role.menuIds(), auditReason, commandId + ":menu:" + id);
                changed++;
            }
            ids.put(role.code(), id);
        }
        for (AccountPlan account : plan.accounts()) {
            Set<Long> target = new TreeSet<>(account.existingRoleIds());
            account.roleCodes().forEach(code -> target.add(ids.get(code)));
            if (!target.equals(account.existingRoleIds())) {
                permissionService.assignUserRole(account.userId(), target, auditReason, commandId + ":user:" + account.userId());
                accounts++;
            }
        }
        return new ImportResult(created, changed, accounts, created == 0 && changed == 0 && accounts == 0);
    }

    private void lockAuthority() {
        // Match PermissionCommandProtocol's lock order before taking user/role subject locks.
        gxpAuditService.acquireLedgerLock();
        approvalAuthority.lockTenantApprovalAuthority(TenantContextHolder.getRequiredTenantId());
    }

    private Preview buildPlan(Content content) {
        List<String> errors = new ArrayList<>();
        Map<Long, MenuDO> menus = menuMapper.selectList().stream().collect(Collectors.toMap(MenuDO::getId, Function.identity()));
        AtomicReference<Set<Long>> packageMenus = new AtomicReference<>();
        tenantService.handleTenantMenu(ids -> packageMenus.set(new HashSet<>(ids)));
        if (packageMenus.get() == null) throw invalid("当前租户套餐菜单未提供，不能确认授权边界");
        List<RoleDO> existingRoles = roleMapper.selectList();
        Map<String, List<RoleRow>> grouped = content.roles().stream().collect(Collectors.groupingBy(RoleRow::targetRole, TreeMap::new, Collectors.toList()));
        List<RolePlan> roles = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            try { roles.add(planRole(entry.getValue(), existingRoles, menus, packageMenus.get())); }
            catch (ServiceException ex) { errors.add("角色权限第" + entry.getValue().get(0).row() + "行：" + ex.getMessage()); }
        }
        Map<String, RolePlan> roleByCode = roles.stream().collect(Collectors.toMap(RolePlan::code, Function.identity()));
        Set<String> newNames = new HashSet<>();
        for (RolePlan role : roles) if (role.roleId() == null && !newNames.add(role.name().toLowerCase(Locale.ROOT)))
            errors.add("文件存在重名的新角色，请调整角色名称：" + role.name());
        Map<String, String> sourceToTarget = new HashMap<>(), targetToSource = new HashMap<>();
        List<AccountPlan> accounts = new ArrayList<>();
        var groupedAccounts = content.accounts().stream().collect(Collectors.groupingBy(AccountRow::targetUsername, TreeMap::new, Collectors.toList()));
        for (var entry : groupedAccounts.entrySet()) {
            try {
                List<AccountRow> rows = entry.getValue();
                for (var row : rows) {
                    if (row.sourceUsername().isBlank() || row.targetUsername().isBlank()) throw invalid("来源账号和目标账号均须明确填写");
                    if (!"启用".equals(row.sourceStatus())) throw invalid("来源账号停用，不能迁移其授权：" + row.sourceUsername());
                    String prior = sourceToTarget.putIfAbsent(row.sourceUsername(), row.targetUsername());
                    if (prior != null && !prior.equals(row.targetUsername())) throw invalid("同一来源账号存在不一致的目标映射：" + row.sourceUsername());
                    prior = targetToSource.putIfAbsent(row.targetUsername(), row.sourceUsername());
                    if (prior != null && !prior.equals(row.sourceUsername())) throw invalid("多个来源账号不能合并映射到同一目标账号：" + row.targetUsername());
                    RolePlan role = roleByCode.get(row.targetRole());
                    if (role == null || !role.sourceCode().equals(row.sourceRole())) throw invalid("账号关系引用不存在或不一致的角色：" + row.targetRole());
                }
                AdminUserDO user = userMapper.selectByUsername(entry.getKey());
                if (user == null || !user.getUsername().equals(entry.getKey())) throw invalid("目标账号不存在或大小写不一致：" + entry.getKey());
                user = userMapper.selectPermissionSubjectForUpdate(TenantContextHolder.getRequiredTenantId(), user.getId());
                if (user == null || !enabled(user.getStatus())) throw invalid("目标账号已停用：" + entry.getKey());
                Set<Long> current = new TreeSet<>(permissionService.getUserRoleIdListByUserId(user.getId()));
                Set<String> codes = rows.stream().map(AccountRow::targetRole).collect(Collectors.toCollection(TreeSet::new));
                List<String> added = codes.stream().filter(code -> roleByCode.get(code).roleId() == null
                        || !current.contains(roleByCode.get(code).roleId())).toList();
                List<String> currentNames = existingRoles.stream().filter(r -> current.contains(r.getId()))
                        .map(r -> r.getName() + " [" + r.getCode() + "]").sorted().toList();
                accounts.add(new AccountPlan(rows.get(0).sourceUsername(), rows.get(0).sourceName(), user.getUsername(),
                        user.getNickname(), user.getId(), current, currentNames, codes, added));
            } catch (ServiceException ex) { errors.add("账号角色第" + entry.getValue().get(0).row() + "行：" + ex.getMessage()); }
        }
        var categories = categoryService.getRoleCategoryList();
        var matches = categories.stream().filter(c -> CATEGORY.equalsIgnoreCase(c.getCode())).toList();
        Long categoryId = null;
        boolean createCategory = roles.stream().anyMatch(r -> r.roleId() == null) && matches.isEmpty();
        if (matches.size() > 1) errors.add("迁移分类编码不唯一：" + CATEGORY);
        else if (matches.size() == 1) {
            var category = matches.get(0);
            categoryId = category.getId();
            if (!CATEGORY.equals(category.getCode()) || !enabled(category.getStatus())) errors.add("迁移分类已停用或编码大小写冲突");
        } else if (createCategory && categories.stream().anyMatch(c -> CATEGORY_NAME.equalsIgnoreCase(c.getName()))) {
            errors.add("迁移分类名称已被其他编码占用：" + CATEGORY_NAME);
        }
        Map<String, AccountMapping> mappings = new TreeMap<>();
        content.accounts().forEach(row -> mappings.putIfAbsent(row.sourceUsername(),
                new AccountMapping(row.sourceUsername(), row.sourceName(), row.targetUsername())));
        return new Preview(List.copyOf(errors), roles, accounts, List.copyOf(mappings.values()), categoryId, createCategory, "");
    }

    private Content mappedContent(byte[] bytes, String mappings) throws IOException {
        Content content = read(bytes);
        if (mappings == null) return content; // No page edits: the explicit target column in the workbook is authoritative.
        if (mappings.length() > 500000) throw invalid("账号映射内容过长");
        List<AccountMapping> edits;
        try { edits = JsonUtils.parseArray(mappings, AccountMapping.class); }
        catch (RuntimeException ex) { throw invalid("账号映射格式不正确"); }
        Set<String> sourceNames = content.accounts().stream().map(AccountRow::sourceUsername).collect(Collectors.toSet());
        Map<String, String> targets = new HashMap<>();
        if (edits == null) throw invalid("账号映射必须为列表");
        for (AccountMapping edit : edits) {
            if (edit == null || !sourceNames.contains(edit.sourceUsername()) || edit.targetUsername() == null
                    || edit.targetUsername().isBlank() || edit.targetUsername().length() > 100
                    || targets.putIfAbsent(edit.sourceUsername(), edit.targetUsername().trim()) != null)
                throw invalid("账号映射存在未知、重复来源账号或空目标账号");
        }
        if (!targets.keySet().equals(sourceNames)) throw invalid("页面账号映射必须覆盖文件全部来源账号");
        return new Content(content.accounts().stream().map(r -> new AccountRow(r.row(), r.sourceUsername(), r.sourceName(),
                r.sourceStatus(), targets.get(r.sourceUsername()), r.sourceRole(), r.targetRole())).toList(), content.roles());
    }

    private RolePlan planRole(List<RoleRow> rows, List<RoleDO> existingRoles, Map<Long, MenuDO> menus, Set<Long> licensed) {
        RoleRow first = rows.get(0);
        if (first.sourceRole().isBlank() || isAdmin(first.sourceRole()) || !targetCode(first.sourceRole()).equals(first.targetRole()))
            throw invalid("目标编码必须与来源业务角色一致，禁止迁移管理员或任意角色编码");
        if (first.name().isBlank() || first.name().length() > 30) throw invalid("角色名称须为1至30字");
        int scope = scopeValue(first.dataScope());
        Set<MenuIdentity> requested = new LinkedHashSet<>();
        for (var row : rows) {
            if (!first.sourceRole().equals(row.sourceRole()) || !first.name().equals(row.name()) || !first.dataScope().equals(row.dataScope()))
                throw invalid("同一角色的来源编码、名称或数据范围不一致（第" + row.row() + "行）");
            if (row.permission().isEmpty()) {
                if (!BUSINESS_ROLES.contains(first.targetRole()) || rows.size() != 1) throw invalid("仅精确业务身份角色允许单行空菜单授权");
            } else {
                if (!catalog.containsKey(row.permission())) throw invalid("超出eDHR授权目录：" + row.permission());
                if (row.menuPath().isBlank()) throw invalid("菜单路径不能为空（第" + row.row() + "行）");
                requested.add(new MenuIdentity(row.permission(), row.menuPath()));
            }
        }
        Set<Long> ids = new TreeSet<>();
        for (MenuIdentity identity : requested) {
            var matches = menus.values().stream().filter(m -> identity.permission().equals(m.getPermission()))
                    .filter(m -> identity.path().equals(menuPath(m, menus))).toList();
            if (matches.size() != 1) throw invalid("目标菜单路径与权限必须唯一匹配：" + identity.path()
                    + " [" + identity.permission() + "]，实际匹配" + matches.size() + "个");
            addMenuChain(matches.get(0), menus, licensed, requested, ids, new HashSet<>());
        }
        var matches = existingRoles.stream().filter(r -> first.targetRole().equalsIgnoreCase(r.getCode())).toList();
        if (matches.size() > 1) throw invalid("目标角色编码不唯一：" + first.targetRole());
        RoleDO existing = matches.isEmpty() ? null : matches.get(0);
        Set<Long> current = new TreeSet<>();
        List<String> members = List.of();
        if (existing != null) {
            existing = roleMapper.selectPermissionSubjectForUpdate(TenantContextHolder.getRequiredTenantId(), existing.getId());
            if (existing == null || !first.targetRole().equals(existing.getCode()) || !enabled(existing.getStatus())
                    || !Objects.equals(existing.getType(), RoleTypeEnum.CUSTOM.getType()) || isAdmin(existing.getCode()))
                throw invalid("目标角色停用、受保护或编码大小写不一致：" + first.targetRole());
            if (!Objects.equals(scope, existing.getDataScope()) || (existing.getDataScopeDeptIds() != null && !existing.getDataScopeDeptIds().isEmpty()))
                throw invalid("目标角色数据范围与文件不一致：" + first.targetRole());
            current.addAll(permissionService.getRoleMenuListByRoleId(existing.getId()));
            if (!ids.containsAll(current)) throw invalid("目标角色包含文件范围之外的菜单，禁止复用：" + first.targetRole());
            var userIds = permissionService.getUserRoleIdListByRoleId(Set.of(existing.getId()));
            if (!userIds.isEmpty()) members = userMapper.selectByIds(userIds).stream()
                    .map(u -> u.getUsername() + "（" + u.getNickname() + "）").sorted().toList();
        } else if (existingRoles.stream().anyMatch(r -> first.name().equalsIgnoreCase(r.getName()))) {
            throw invalid("目标角色名称已被其他编码占用：" + first.name());
        }
        List<MenuDetail> details = ids.stream().map(menus::get).map(m -> new MenuDetail(m.getId(), m.getName(),
                Objects.toString(m.getPermission(), ""), m.getPermission() != null && catalog.containsKey(m.getPermission()) ? catalog.get(m.getPermission()).purpose() : "必要父级导航；无额外权限标识",
                !current.contains(m.getId()))).toList();
        return new RolePlan(first.sourceRole(), first.targetRole(), existing == null ? first.name() : existing.getName(),
                first.dataScope(), existing == null ? null : existing.getId(), current, ids, details, members);
    }

    private void addMenuChain(MenuDO menu, Map<Long, MenuDO> menus, Set<Long> licensed, Set<MenuIdentity> requested,
                              Set<Long> ids, Set<Long> visited) {
        if (!visited.add(menu.getId())) throw invalid("目标菜单存在循环父级：" + menu.getName());
        if (!enabled(menu.getStatus())) throw invalid("目标菜单已停用：" + menu.getName());
        if (!licensed.contains(menu.getId())) throw invalid("目标租户套餐未授权菜单：" + menu.getName());
        if (menu.getPermission() != null && !menu.getPermission().isBlank()
                && (!catalog.containsKey(menu.getPermission())
                || !requested.contains(new MenuIdentity(menu.getPermission(), menuPath(menu, menus)))))
            throw invalid("必要父菜单带有文件未声明权限：" + menu.getName() + " [" + menu.getPermission() + "]");
        ids.add(menu.getId());
        if (menu.getParentId() != null && menu.getParentId() != 0L) {
            MenuDO parent = menus.get(menu.getParentId());
            if (parent == null) throw invalid("缺少父级菜单：" + menu.getName());
            addMenuChain(parent, menus, licensed, requested, ids, visited);
        }
    }

    private static String menuPath(MenuDO menu, Map<Long, MenuDO> menus) {
        var names = new LinkedList<String>();
        Set<Long> visited = new HashSet<>();
        MenuDO current = menu;
        while (current != null) {
            if (!visited.add(current.getId())) throw invalid("菜单存在循环父级：" + menu.getName());
            if (current.getName() == null || current.getName().isBlank()) throw invalid("菜单名称缺失，无法确认路径");
            names.addFirst(current.getName());
            if (current.getParentId() == null || current.getParentId() == 0L) break;
            current = menus.get(current.getParentId());
            if (current == null) throw invalid("缺少父级菜单，无法确认路径：" + menu.getName());
        }
        return String.join(" / ", names);
    }

    private void requireTenant(String expected) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        var actor = SecurityFrameworkUtils.getLoginUser();
        if (actor == null || actor.getId() == null || !tenantId.equals(actor.getTenantId()))
            throw invalid("仅允许登录账号在所属租户执行，不支持跨租户访问或租户代入");
        tenantService.validTenant(tenantId);
        var tenant = tenantService.getTenant(tenantId);
        if (tenant == null || !expected.equals(tenant.getName())) throw invalid("本操作仅允许在【" + expected + "】租户执行");
    }

    static String targetCode(String source) {
        return BUSINESS_ROLES.contains(source) ? source : "EDHR_TRANSFER_" + DigestUtil.sha256Hex(source).substring(0, 16);
    }
    private static boolean isAdmin(String code) {
        return code == null || Arrays.stream(RoleCodeEnum.values()).anyMatch(r -> r.getCode().equalsIgnoreCase(code));
    }
    private static boolean enabled(Integer status) { return Objects.equals(0, status); }
    private static String scopeName(Integer scope) {
        if (Objects.equals(scope, DataScopeEnum.ALL.getScope())) return "全部数据";
        if (Objects.equals(scope, DataScopeEnum.SELF.getScope())) return "仅本人";
        if (Objects.equals(scope, DataScopeEnum.DEPT_CUSTOM.getScope())) return "指定部门（缺少目标部门映射，禁止导入）";
        if (Objects.equals(scope, DataScopeEnum.DEPT_ONLY.getScope())) return "本部门（缺少目标部门映射，禁止导入）";
        if (Objects.equals(scope, DataScopeEnum.DEPT_AND_CHILD.getScope())) return "本部门及下级（缺少目标部门映射，禁止导入）";
        throw invalid("来源角色数据范围未知，不能导出");
    }
    private static int scopeValue(String scope) {
        if ("全部数据".equals(scope)) return DataScopeEnum.ALL.getScope();
        if ("仅本人".equals(scope)) return DataScopeEnum.SELF.getScope();
        throw invalid("该角色使用部门数据范围或未支持的范围，缺少明确的目标部门映射，禁止导入；请移除该角色及其账号关系后重新预览，不得改为更大的范围");
    }

    private String receipt(byte[] bytes, Preview plan, long expires) {
        String input = expires + ":" + TenantContextHolder.getRequiredTenantId() + ":" + SecurityFrameworkUtils.getLoginUserId()
                + ":" + DigestUtil.sha256Hex(bytes) + ":" + JsonUtils.toJsonString(plan);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(receiptSecret, "HmacSHA256"));
            return expires + "." + HexFormat.of().formatHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) { throw new IllegalStateException("Cannot sign preview receipt", ex); }
    }
    private void verifyReceipt(byte[] bytes, Preview plan, String token) {
        if (token == null || !token.matches("[0-9]{10}\\.[a-f0-9]{64}")) throw invalid("请先上传文件完成授权预览");
        long expires = Long.parseLong(token.substring(0, 10));
        if (expires < Instant.now().getEpochSecond()) throw invalid("预览已过期，请重新上传预览");
        if (!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), receipt(bytes, plan, expires).getBytes(StandardCharsets.UTF_8)))
            throw invalid("文件、账号、角色或菜单授权在预览后已变化，请重新预览");
    }
}
