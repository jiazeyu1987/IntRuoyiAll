package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleCategoryDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.MenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleCategoryMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.enums.permission.DataScopeEnum;
import cn.iocoder.yudao.module.system.enums.permission.RoleTypeEnum;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_CONTENT_INVALID;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_FILE_EMPTY;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_FORMAT_UNSUPPORTED;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_REFERENCE_MISSING;

@Service
@Slf4j
public class RoleConfigPackageServiceImpl implements RoleConfigPackageService {

    private static final String PACKAGE_VERSION = "3";
    private static final String MENU_KEY_PERMISSION_PREFIX = "permission:";
    private static final String MENU_KEY_ROUTE_PREFIX = "route:";
    private static final String MENU_KEY_SEPARATOR = "|";

    @Resource
    private RoleMapper roleMapper;
    @Resource
    private RoleCategoryMapper roleCategoryMapper;

    @Resource
    private MenuMapper menuMapper;

    @Resource
    private PermissionService permissionService;

    @Override
    public byte[] exportPackage() {
        Map<Long, String> menuKeyById = buildMenuKeyById();
        RoleConfigPackage payload = new RoleConfigPackage();
        payload.setPackageVersion(PACKAGE_VERSION);
        payload.setCategories(roleCategoryMapper.selectListOrderBySort().stream()
                .map(this::toCategoryItem)
                .toList());
        payload.setRoles(roleMapper.selectList().stream()
                .sorted(Comparator.comparing(RoleDO::getSort).thenComparing(RoleDO::getId))
                .map(role -> toItem(role, menuKeyById))
                .toList());
        return JsonUtils.toJsonByte(payload);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importPackage(byte[] content) {
        RoleConfigPackage payload = parsePayload(content);
        validatePayload(payload);
        payload.getCategories().forEach(this::validateCategoryItem);
        payload.getRoles().forEach(this::validateItem);
        Map<String, Long> menuIdByKey = buildMenuIdByKey();
        Map<String, Long> categoryIdByCode = resolveCategories(payload.getCategories());
        for (RoleConfigItem item : payload.getRoles()) {
            Set<Long> menuIds = resolveMenuIds(item, menuIdByKey);
            Long categoryId = resolveCategoryId(item, categoryIdByCode);
            RoleDO existing = roleMapper.selectByCode(item.getCode());
            if (existing == null) {
                RoleDO role = new RoleDO();
                role.setName(item.getName());
                role.setCode(item.getCode());
                role.setSort(item.getSort());
                role.setStatus(item.getStatus());
                role.setType(defaultRoleType(item.getType()));
                role.setRemark(item.getRemark());
                role.setCategoryId(categoryId);
                role.setDataScope(defaultDataScope(item.getDataScope()));
                role.setDataScopeDeptIds(item.getDataScopeDeptIds());
                roleMapper.insert(role);
                permissionService.assignRoleMenu(role.getId(), menuIds);
                if (!Objects.equals(role.getType(), RoleTypeEnum.SYSTEM.getType())) {
                    permissionService.assignRoleDataScope(role.getId(), role.getDataScope(), role.getDataScopeDeptIds());
                }
                continue;
            }
            existing.setName(item.getName());
            existing.setSort(item.getSort());
            existing.setStatus(item.getStatus());
            existing.setRemark(item.getRemark());
            existing.setCategoryId(categoryId);
            existing.setDataScope(defaultDataScope(item.getDataScope()));
            existing.setDataScopeDeptIds(item.getDataScopeDeptIds());
            roleMapper.updateById(existing);
            permissionService.assignRoleMenu(existing.getId(), menuIds);
            if (!Objects.equals(existing.getType(), RoleTypeEnum.SYSTEM.getType())) {
                permissionService.assignRoleDataScope(existing.getId(), existing.getDataScope(), existing.getDataScopeDeptIds());
            }
        }
    }

    private RoleConfigPackage parsePayload(byte[] content) {
        if (content == null || content.length == 0) {
            throw exception(CONFIG_PACKAGE_FILE_EMPTY);
        }
        try {
            return JsonUtils.parseObject(content, RoleConfigPackage.class);
        } catch (RuntimeException ex) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "权限角色配置包 JSON 非法");
        }
    }

    private RoleConfigItem toItem(RoleDO role, Map<Long, String> menuKeyById) {
        RoleConfigItem item = new RoleConfigItem();
        item.setCode(role.getCode());
        item.setName(role.getName());
        item.setSort(role.getSort());
        item.setStatus(role.getStatus());
        item.setType(role.getType());
        item.setRemark(role.getRemark());
        item.setCategoryCode(resolveCategoryCode(role));
        item.setDataScope(role.getDataScope());
        item.setDataScopeDeptIds(role.getDataScopeDeptIds());
        item.setMenuKeys(buildMenuKeys(permissionService.getRoleMenuListByRoleId(role.getId()), menuKeyById));
        return item;
    }

    private void validatePayload(RoleConfigPackage payload) {
        if (payload == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "权限角色配置包 JSON 非法");
        }
        if (!PACKAGE_VERSION.equals(payload.getPackageVersion())) {
            throw exception(CONFIG_PACKAGE_FORMAT_UNSUPPORTED, payload.getPackageVersion());
        }
        if (payload.getRoles() == null || payload.getRoles().isEmpty()) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "权限角色配置包 roles 不能为空");
        }
        if (payload.getCategories() == null || payload.getCategories().isEmpty()) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "权限角色配置包 categories 不能为空");
        }
    }

    private void validateItem(RoleConfigItem item) {
        if (item == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "权限角色配置包存在空角色");
        }
        if (!StringUtils.hasText(item.getCode())) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "权限角色配置包缺少 role code");
        }
        if (!StringUtils.hasText(item.getName())) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "权限角色配置包缺少 role name，code={}", item.getCode());
        }
        if (item.getSort() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "权限角色配置包缺少 role sort，code={}", item.getCode());
        }
        if (item.getStatus() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "权限角色配置包缺少 role status，code={}", item.getCode());
        }
        if (!StringUtils.hasText(item.getCategoryCode())) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "权限角色配置包缺少 role categoryCode，code={}", item.getCode());
        }
        if (item.getMenuKeys() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "权限角色配置包缺少 menuKeys，code={}", item.getCode());
        }
    }

    private RoleCategoryConfigItem toCategoryItem(RoleCategoryDO category) {
        RoleCategoryConfigItem item = new RoleCategoryConfigItem();
        item.setCode(category.getCode());
        item.setName(category.getName());
        item.setSort(category.getSort());
        item.setStatus(category.getStatus());
        item.setRemark(category.getRemark());
        return item;
    }

    private Map<String, Long> resolveCategories(List<RoleCategoryConfigItem> items) {
        Map<String, Long> result = new HashMap<>();
        for (RoleCategoryConfigItem item : items) {
            RoleCategoryDO existing = roleCategoryMapper.selectByCode(item.getCode());
            if (existing == null) {
                throw exception0(CONFIG_PACKAGE_REFERENCE_MISSING.getCode(),
                        CONFIG_PACKAGE_REFERENCE_MISSING.getMsg(),
                        "分类标识【" + item.getCode() + "】在目标环境不存在");
            }
            result.put(existing.getCode(), existing.getId());
        }
        return result;
    }

    private void validateCategoryItem(RoleCategoryConfigItem item) {
        if (item == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "权限角色配置包存在空分类");
        }
        if (!StringUtils.hasText(item.getCode())) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "权限角色配置包缺少 category code");
        }
        if (!StringUtils.hasText(item.getName())) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "权限角色配置包缺少 category name，code={}", item.getCode());
        }
        if (item.getSort() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "权限角色配置包缺少 category sort，code={}", item.getCode());
        }
        if (item.getStatus() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "权限角色配置包缺少 category status，code={}", item.getCode());
        }
    }

    private Long resolveCategoryId(RoleConfigItem item, Map<String, Long> categoryIdByCode) {
        Long categoryId = categoryIdByCode.get(item.getCategoryCode());
        if (categoryId == null) {
            throw exception0(CONFIG_PACKAGE_REFERENCE_MISSING.getCode(),
                    CONFIG_PACKAGE_REFERENCE_MISSING.getMsg(),
                    "角色【" + item.getCode() + "】引用的分类标识【" + item.getCategoryCode() + "】在目标环境不存在");
        }
        return categoryId;
    }

    private String resolveCategoryCode(RoleDO role) {
        if (role.getCategoryId() == null) {
            throw exception0(CONFIG_PACKAGE_REFERENCE_MISSING.getCode(),
                    CONFIG_PACKAGE_REFERENCE_MISSING.getMsg(),
                    "角色【" + role.getCode() + "】缺少分类");
        }
        RoleCategoryDO category = roleCategoryMapper.selectById(role.getCategoryId());
        if (category == null) {
            throw exception0(CONFIG_PACKAGE_REFERENCE_MISSING.getCode(),
                    CONFIG_PACKAGE_REFERENCE_MISSING.getMsg(),
                    "角色【" + role.getCode() + "】引用的分类编号【" + role.getCategoryId() + "】不存在");
        }
        return category.getCode();
    }

    private List<String> buildMenuKeys(Set<Long> menuIds, Map<Long, String> menuKeyById) {
        return menuIds.stream()
                .map(menuKeyById::get)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    private Map<Long, String> buildMenuKeyById() {
        List<MenuDO> menus = menuMapper.selectList();
        Map<Long, MenuDO> menuById = convertMap(menus, MenuDO::getId);
        Map<Long, String> keyById = new HashMap<>();
        for (MenuDO menu : menus) {
            keyById.put(menu.getId(), buildMenuKey(menu, menuById, keyById));
        }
        return keyById;
    }

    private Map<String, Long> buildMenuIdByKey() {
        Map<String, Long> result = new HashMap<>();
        Map<Long, String> menuKeyById = buildMenuKeyById();
        for (Map.Entry<Long, String> entry : menuKeyById.entrySet()) {
            Long duplicatedMenuId = result.put(entry.getValue(), entry.getKey());
            if (duplicatedMenuId != null && !Objects.equals(duplicatedMenuId, entry.getKey())) {
                throw exception(CONFIG_PACKAGE_REFERENCE_MISSING,
                        "目标环境存在重复菜单标识【{}】",
                        entry.getValue());
            }
        }
        return result;
    }

    private Set<Long> resolveMenuIds(RoleConfigItem item, Map<String, Long> menuIdByKey) {
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        List<String> missingMenuKeys = new ArrayList<>();
        for (String menuKey : item.getMenuKeys()) {
            Long menuId = menuIdByKey.get(menuKey);
            if (menuId == null) {
                missingMenuKeys.add(menuKey);
                continue;
            }
            result.add(menuId);
        }
        if (!missingMenuKeys.isEmpty()) {
            throw exception0(CONFIG_PACKAGE_REFERENCE_MISSING.getCode(),
                    CONFIG_PACKAGE_REFERENCE_MISSING.getMsg(),
                    "角色【" + item.getCode() + "】引用的菜单标识【" + String.join("，", missingMenuKeys) + "】在目标环境不存在");
        }
        return result;
    }

    private String buildMenuKey(MenuDO menu, Map<Long, MenuDO> menuById, Map<Long, String> keyById) {
        String cached = keyById.get(menu.getId());
        if (cached != null) {
            return cached;
        }
        if (StringUtils.hasText(menu.getPermission())) {
            return MENU_KEY_PERMISSION_PREFIX + menu.getPermission().trim();
        }
        String parentKey = "";
        if (menu.getParentId() != null && menu.getParentId() > 0) {
            MenuDO parent = menuById.get(menu.getParentId());
            if (parent != null) {
                parentKey = buildMenuKey(parent, menuById, keyById);
            }
        }
        return MENU_KEY_ROUTE_PREFIX
                + normalizeSegment(parentKey) + MENU_KEY_SEPARATOR
                + normalizeSegment(menu.getType()) + MENU_KEY_SEPARATOR
                + normalizeSegment(menu.getPath()) + MENU_KEY_SEPARATOR
                + normalizeSegment(menu.getComponent()) + MENU_KEY_SEPARATOR
                + normalizeSegment(menu.getComponentName()) + MENU_KEY_SEPARATOR
                + normalizeSegment(menu.getName());
    }

    private String normalizeSegment(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private Integer defaultRoleType(Integer type) {
        if (Objects.equals(type, RoleTypeEnum.SYSTEM.getType())) {
            return RoleTypeEnum.SYSTEM.getType();
        }
        return RoleTypeEnum.CUSTOM.getType();
    }

    private Integer defaultDataScope(Integer dataScope) {
        return dataScope != null ? dataScope : DataScopeEnum.ALL.getScope();
    }

    @Data
    public static class RoleConfigPackage {
        private String packageVersion;
        private List<RoleCategoryConfigItem> categories = new ArrayList<>();
        private List<RoleConfigItem> roles = new ArrayList<>();
    }

    @Data
    public static class RoleCategoryConfigItem {
        private String code;
        private String name;
        private Integer sort;
        private Integer status = CommonStatusEnum.ENABLE.getStatus();
        private String remark;
    }

    @Data
    public static class RoleConfigItem {
        private String code;
        private String name;
        private Integer sort;
        private Integer status = CommonStatusEnum.ENABLE.getStatus();
        private Integer type = RoleTypeEnum.CUSTOM.getType();
        private String remark;
        private String categoryCode;
        private Integer dataScope = DataScopeEnum.ALL.getScope();
        private Set<Long> dataScopeDeptIds;
        private List<String> menuKeys = new ArrayList<>();
    }
}
