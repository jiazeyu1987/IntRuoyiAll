package cn.iocoder.yudao.module.mdm.service.companyscope;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mdm.api.companyscope.dto.MdmRoleCompanyScopeCreateReqDTO;
import cn.iocoder.yudao.module.mdm.api.companyscope.dto.MdmUserCompanyScopeCreateReqDTO;
import cn.iocoder.yudao.module.mdm.controller.admin.companyscope.vo.MdmCompanyScopePageReqVO;
import cn.iocoder.yudao.module.mdm.controller.admin.companyscope.vo.MdmCompanyScopeRespVO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.companyscope.MdmRoleCompanyScopeDO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.companyscope.MdmUserCompanyScopeDO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.enterprise.MdmEnterpriseDO;
import cn.iocoder.yudao.module.mdm.dal.mysql.companyscope.MdmRoleCompanyScopeMapper;
import cn.iocoder.yudao.module.mdm.dal.mysql.companyscope.MdmUserCompanyScopeMapper;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseStatusEnum;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseTypeEnum;
import cn.iocoder.yudao.module.mdm.service.enterprise.MdmEnterpriseService;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_CONFIG_INVALID;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_FIELD_REQUIRED;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_SYSTEM_ROLE_INVALID;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_SYSTEM_USER_INVALID;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_WRITE_RESULT_INVALID;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_ROLE_COMPANY_SCOPE_DUPLICATE;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_USER_COMPANY_SCOPE_DENIED;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_USER_COMPANY_SCOPE_DISABLED;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_USER_COMPANY_SCOPE_DUPLICATE;

@Service
@Validated
public class MdmCompanyScopeServiceImpl implements MdmCompanyScopeService {

    private static final String SCOPE_TYPE_USER = "USER";
    private static final String SCOPE_TYPE_ROLE = "ROLE";
    private static final Set<String> OWNED_COMPANY = Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType());

    private static final String USER_SCOPE_UNIQUE_CONSTRAINT =
            "uk_mdm_user_company_scope_tenant_user_company";
    private static final String ROLE_SCOPE_UNIQUE_CONSTRAINT =
            "uk_mdm_role_company_scope_tenant_role_company";

    @Resource
    private MdmUserCompanyScopeMapper userScopeMapper;
    @Resource
    private MdmRoleCompanyScopeMapper roleScopeMapper;
    @Resource
    private MdmEnterpriseService enterpriseService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private RoleApi roleApi;
    @Resource
    private MdmCompanyScopeRecipientResolver recipientResolver;

    @Override
    public PageResult<MdmCompanyScopeRespVO> getCompanyScopePage(MdmCompanyScopePageReqVO reqVO) {
        if (reqVO == null) {
            throw exception(MDM_COMPANY_SCOPE_FIELD_REQUIRED, "request");
        }
        String scopeType = normalizeScopeType(reqVO.getScopeType());
        String status = normalizeOptionalStatus(reqVO.getStatus());
        String keyword = StrUtil.trimToNull(reqVO.getKeyword());

        List<MdmUserCompanyScopeDO> userScopes = scopeType == null || SCOPE_TYPE_USER.equals(scopeType)
                ? userScopeMapper.selectList(new LambdaQueryWrapperX<MdmUserCompanyScopeDO>()
                .eq(MdmUserCompanyScopeDO::getDeleted, false)
                .eqIfPresent(MdmUserCompanyScopeDO::getCompanyId, reqVO.getCompanyId())
                .eqIfPresent(MdmUserCompanyScopeDO::getStatus, status))
                : List.of();
        List<MdmRoleCompanyScopeDO> roleScopes = scopeType == null || SCOPE_TYPE_ROLE.equals(scopeType)
                ? roleScopeMapper.selectList(new LambdaQueryWrapperX<MdmRoleCompanyScopeDO>()
                .eq(MdmRoleCompanyScopeDO::getDeleted, false)
                .eqIfPresent(MdmRoleCompanyScopeDO::getCompanyId, reqVO.getCompanyId())
                .eqIfPresent(MdmRoleCompanyScopeDO::getStatus, status))
                : List.of();
        if (userScopes == null || roleScopes == null) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }

        List<Long> companyIds = new ArrayList<>();
        userScopes.forEach(row -> companyIds.add(requireScopeCompanyId(row)));
        roleScopes.forEach(row -> companyIds.add(requireScopeCompanyId(row)));
        List<Long> normalizedCompanyIds = companyIds.stream().distinct().sorted().toList();
        Map<Long, MdmEnterpriseDO> companies = companyMap(normalizedCompanyIds);
        Map<Long, AdminUserRespDTO> users = userMap(userScopes);
        Map<Long, RoleRespDTO> roles = roleMap(roleScopes);

        List<MdmCompanyScopeRespVO> rows = new ArrayList<>(userScopes.size() + roleScopes.size());
        userScopes.forEach(scope -> rows.add(toUserScopeResp(scope, users, companies)));
        roleScopes.forEach(scope -> rows.add(toRoleScopeResp(scope, roles, companies)));
        if (keyword != null) {
            String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
            rows.removeIf(row -> !containsIgnoreCase(row.getPrincipalName(), normalizedKeyword)
                    && !containsIgnoreCase(row.getPrincipalCode(), normalizedKeyword)
                    && !containsIgnoreCase(row.getCompanyCode(), normalizedKeyword)
                    && !containsIgnoreCase(row.getCompanyName(), normalizedKeyword));
        }
        rows.sort(Comparator
                .comparing(MdmCompanyScopeRespVO::getUpdateTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MdmCompanyScopeRespVO::getId, Comparator.reverseOrder())
                .thenComparing(MdmCompanyScopeRespVO::getScopeType));

        long total = rows.size();
        int fromIndex = Math.min((reqVO.getPageNo() - 1) * reqVO.getPageSize(), rows.size());
        int toIndex = Math.min(fromIndex + reqVO.getPageSize(), rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), total);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUserCompanyScope(MdmUserCompanyScopeCreateReqDTO reqDTO) {
        if (reqDTO == null) {
            throw exception(MDM_COMPANY_SCOPE_FIELD_REQUIRED, "request");
        }
        Long userId = requirePositiveId(reqDTO.getUserId(), "userId");
        Long companyId = requirePositiveId(reqDTO.getCompanyId(), "companyId");
        String status = requireStatus(reqDTO.getStatus());
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        validateEnabledOwnedCompany(companyId);
        validateEnabledUser(userId);

        MdmUserCompanyScopeDO scope = MdmUserCompanyScopeDO.builder()
                .userId(userId)
                .companyId(companyId)
                .status(status)
                .revision(1)
                .build();
        scope.setTenantId(tenantId);
        int affectedRows;
        try {
            affectedRows = userScopeMapper.insert(scope);
        } catch (DuplicateKeyException duplicateKeyException) {
            if (!isNamedUniqueConflict(duplicateKeyException, USER_SCOPE_UNIQUE_CONSTRAINT)) {
                throw duplicateKeyException;
            }
            throw exception(MDM_USER_COMPANY_SCOPE_DUPLICATE);
        }
        validateWriteResult(affectedRows, scope.getId());
        return scope.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRoleCompanyScope(MdmRoleCompanyScopeCreateReqDTO reqDTO) {
        if (reqDTO == null) {
            throw exception(MDM_COMPANY_SCOPE_FIELD_REQUIRED, "request");
        }
        Long roleId = requirePositiveId(reqDTO.getRoleId(), "roleId");
        Long companyId = requirePositiveId(reqDTO.getCompanyId(), "companyId");
        String status = requireStatus(reqDTO.getStatus());
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        validateEnabledOwnedCompany(companyId);
        validateEnabledRole(roleId);

        MdmRoleCompanyScopeDO scope = MdmRoleCompanyScopeDO.builder()
                .roleId(roleId)
                .companyId(companyId)
                .status(status)
                .revision(1)
                .build();
        scope.setTenantId(tenantId);
        int affectedRows;
        try {
            affectedRows = roleScopeMapper.insert(scope);
        } catch (DuplicateKeyException duplicateKeyException) {
            if (!isNamedUniqueConflict(duplicateKeyException, ROLE_SCOPE_UNIQUE_CONSTRAINT)) {
                throw duplicateKeyException;
            }
            throw exception(MDM_ROLE_COMPANY_SCOPE_DUPLICATE);
        }
        validateWriteResult(affectedRows, scope.getId());
        return scope.getId();
    }

    @Override
    public Set<Long> getEnabledCompanyIdsForUser(Long userId) {
        Long requiredUserId = requirePositiveId(userId, "userId");
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        validateEnabledUser(requiredUserId);
        List<MdmUserCompanyScopeDO> rows = userScopeMapper.selectByTenantUser(tenantId, requiredUserId);
        if (rows == null) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
        if (rows.isEmpty()) {
            throw exception(MDM_USER_COMPANY_SCOPE_DENIED);
        }
        Map<Long, MdmUserCompanyScopeDO> byCompanyId = new LinkedHashMap<>();
        for (MdmUserCompanyScopeDO row : rows) {
            Long rowCompanyId = row == null ? null : row.getCompanyId();
            validateRawUserScope(row, tenantId, requiredUserId, rowCompanyId);
            if (byCompanyId.putIfAbsent(rowCompanyId, row) != null) {
                throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
            }
        }
        List<Long> enabledCompanyIds = byCompanyId.values().stream()
                .filter(row -> !Boolean.TRUE.equals(row.getDeleted()))
                .filter(row -> MdmEnterpriseStatusEnum.ENABLE.getStatus().equals(row.getStatus()))
                .map(MdmUserCompanyScopeDO::getCompanyId)
                .sorted()
                .toList();
        if (enabledCompanyIds.isEmpty()) {
            throw exception(MDM_USER_COMPANY_SCOPE_DENIED);
        }
        validateEnabledOwnedCompaniesForUserAccess(enabledCompanyIds);
        return new LinkedHashSet<>(enabledCompanyIds);
    }

    @Override
    public void validateUserCompanyAccess(Long userId, Long companyId) {
        Long requiredUserId = requirePositiveId(userId, "userId");
        Long requiredCompanyId = requirePositiveId(companyId, "companyId");
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        validateEnabledUser(requiredUserId);
        List<MdmUserCompanyScopeDO> scopes = userScopeMapper.selectByTenantUserAndCompany(
                tenantId, requiredUserId, requiredCompanyId);
        if (scopes == null || scopes.size() > 1) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
        if (scopes.isEmpty()) {
            throw exception(MDM_USER_COMPANY_SCOPE_DENIED);
        }
        MdmUserCompanyScopeDO scope = scopes.get(0);
        validateRawUserScope(scope, tenantId, requiredUserId, requiredCompanyId);
        if (Boolean.TRUE.equals(scope.getDeleted())) {
            throw exception(MDM_USER_COMPANY_SCOPE_DENIED);
        }
        if (MdmEnterpriseStatusEnum.DISABLE.getStatus().equals(scope.getStatus())) {
            throw exception(MDM_USER_COMPANY_SCOPE_DISABLED);
        }
        validateEnabledOwnedCompaniesForUserAccess(List.of(requiredCompanyId));
    }

    @Override
    public void validateUserCompanyAccessBatch(Long userId, Collection<Long> companyIds) {
        Long requiredUserId = requirePositiveId(userId, "userId");
        List<Long> requiredCompanyIds = validateCompanyIds(companyIds);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        validateEnabledUser(requiredUserId);
        List<MdmUserCompanyScopeDO> scopes = userScopeMapper.selectByTenantUserAndCompanyIds(
                tenantId, requiredUserId, requiredCompanyIds);
        if (scopes == null) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
        Set<Long> requestedCompanyIds = new LinkedHashSet<>(requiredCompanyIds);
        Map<Long, MdmUserCompanyScopeDO> byCompanyId = new LinkedHashMap<>();
        for (MdmUserCompanyScopeDO scope : scopes) {
            Long rowCompanyId = scope == null ? null : scope.getCompanyId();
            if (!requestedCompanyIds.contains(rowCompanyId)) {
                throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
            }
            validateRawUserScope(scope, tenantId, requiredUserId, rowCompanyId);
            if (byCompanyId.putIfAbsent(rowCompanyId, scope) != null) {
                throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
            }
        }
        for (Long companyId : requiredCompanyIds) {
            MdmUserCompanyScopeDO scope = byCompanyId.get(companyId);
            if (scope == null || Boolean.TRUE.equals(scope.getDeleted())) {
                throw exception(MDM_USER_COMPANY_SCOPE_DENIED);
            }
            if (MdmEnterpriseStatusEnum.DISABLE.getStatus().equals(scope.getStatus())) {
                throw exception(MDM_USER_COMPANY_SCOPE_DISABLED);
            }
        }
        validateEnabledOwnedCompaniesForUserAccess(requiredCompanyIds);
    }

    @Override
    public Set<Long> resolveRecipientUserIds(Long companyId, Collection<Long> roleIds, String permission) {
        return recipientResolver.resolve(companyId, roleIds, permission);
    }

    private Map<Long, MdmEnterpriseDO> companyMap(List<Long> companyIds) {
        if (companyIds.isEmpty()) {
            return Map.of();
        }
        List<MdmEnterpriseDO> companies = enterpriseService.getEnabledEnterprises(companyIds, OWNED_COMPANY);
        if (companies == null || companies.size() != companyIds.size()) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
        Map<Long, MdmEnterpriseDO> result = new LinkedHashMap<>();
        for (MdmEnterpriseDO company : companies) {
            if (company == null || company.getId() == null || result.putIfAbsent(company.getId(), company) != null
                    || !companyIds.contains(company.getId())) {
                throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
            }
        }
        if (result.size() != companyIds.size()) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
        return result;
    }

    private Map<Long, AdminUserRespDTO> userMap(List<MdmUserCompanyScopeDO> scopes) {
        List<Long> ids = scopes.stream().map(this::requireScopeUserId).distinct().sorted().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<AdminUserRespDTO> users = adminUserApi.getUserList(ids);
        if (users == null || users.size() != ids.size()) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
        Map<Long, AdminUserRespDTO> result = new LinkedHashMap<>();
        for (AdminUserRespDTO user : users) {
            if (user == null || user.getId() == null || StrUtil.isBlank(user.getUsername())
                    || result.putIfAbsent(user.getId(), user) != null || !ids.contains(user.getId())) {
                throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
            }
        }
        if (result.size() != ids.size()) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
        return result;
    }

    private Map<Long, RoleRespDTO> roleMap(List<MdmRoleCompanyScopeDO> scopes) {
        List<Long> ids = scopes.stream().map(this::requireScopeRoleId).distinct().sorted().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<RoleRespDTO> roles = roleApi.getRoleList(ids);
        if (roles == null || roles.size() != ids.size()) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
        Map<Long, RoleRespDTO> result = new LinkedHashMap<>();
        for (RoleRespDTO role : roles) {
            if (role == null || role.getId() == null || StrUtil.isBlank(role.getName())
                    || StrUtil.isBlank(role.getCode()) || result.putIfAbsent(role.getId(), role) != null
                    || !ids.contains(role.getId())) {
                throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
            }
        }
        if (result.size() != ids.size()) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
        return result;
    }

    private MdmCompanyScopeRespVO toUserScopeResp(MdmUserCompanyScopeDO scope,
                                                   Map<Long, AdminUserRespDTO> users,
                                                   Map<Long, MdmEnterpriseDO> companies) {
        AdminUserRespDTO user = users.get(requireScopeUserId(scope));
        return buildScopeResp(scope.getId(), SCOPE_TYPE_USER, user.getId(), user.getNickname(),
                user.getUsername(), scope.getCompanyId(), companies.get(scope.getCompanyId()), scope.getStatus(),
                scope.getRevision(), scope.getUpdateTime());
    }

    private MdmCompanyScopeRespVO toRoleScopeResp(MdmRoleCompanyScopeDO scope,
                                                   Map<Long, RoleRespDTO> roles,
                                                   Map<Long, MdmEnterpriseDO> companies) {
        RoleRespDTO role = roles.get(requireScopeRoleId(scope));
        return buildScopeResp(scope.getId(), SCOPE_TYPE_ROLE, role.getId(), role.getName(), role.getCode(),
                scope.getCompanyId(), companies.get(scope.getCompanyId()), scope.getStatus(), scope.getRevision(),
                scope.getUpdateTime());
    }

    private MdmCompanyScopeRespVO buildScopeResp(Long id, String scopeType, Long principalId, String principalName,
                                                  String principalCode, Long companyId, MdmEnterpriseDO company,
                                                  String status, Integer revision, java.time.LocalDateTime updateTime) {
        if (company == null || !Objects.equals(companyId, company.getId())
                || StrUtil.isBlank(company.getEnterpriseCode()) || StrUtil.isBlank(company.getName())) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
        MdmCompanyScopeRespVO response = new MdmCompanyScopeRespVO();
        response.setId(id);
        response.setScopeType(scopeType);
        response.setPrincipalId(principalId);
        response.setPrincipalName(principalName);
        response.setPrincipalCode(principalCode);
        response.setCompanyId(companyId);
        response.setCompanyCode(company.getEnterpriseCode());
        response.setCompanyName(company.getName());
        response.setStatus(status);
        response.setRevision(revision);
        response.setUpdateTime(updateTime);
        return response;
    }

    private Long requireScopeCompanyId(MdmUserCompanyScopeDO scope) {
        if (scope == null || scope.getId() == null || scope.getId() <= 0 || scope.getCompanyId() == null
                || scope.getCompanyId() <= 0 || scope.getRevision() == null || scope.getRevision() <= 0
                || !MdmEnterpriseStatusEnum.isValid(scope.getStatus())) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
        return scope.getCompanyId();
    }

    private Long requireScopeCompanyId(MdmRoleCompanyScopeDO scope) {
        if (scope == null || scope.getId() == null || scope.getId() <= 0 || scope.getCompanyId() == null
                || scope.getCompanyId() <= 0 || scope.getRevision() == null || scope.getRevision() <= 0
                || !MdmEnterpriseStatusEnum.isValid(scope.getStatus())) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
        return scope.getCompanyId();
    }

    private Long requireScopeUserId(MdmUserCompanyScopeDO scope) {
        requireScopeCompanyId(scope);
        if (scope.getUserId() == null || scope.getUserId() <= 0) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
        return scope.getUserId();
    }

    private Long requireScopeRoleId(MdmRoleCompanyScopeDO scope) {
        requireScopeCompanyId(scope);
        if (scope.getRoleId() == null || scope.getRoleId() <= 0) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
        return scope.getRoleId();
    }

    private String normalizeScopeType(String value) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized != null && !Set.of(SCOPE_TYPE_USER, SCOPE_TYPE_ROLE).contains(normalized)) {
            throw exception(MDM_COMPANY_SCOPE_FIELD_REQUIRED, "scopeType");
        }
        return normalized;
    }

    private String normalizeOptionalStatus(String value) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized != null && !MdmEnterpriseStatusEnum.isValid(normalized)) {
            throw exception(MDM_COMPANY_SCOPE_FIELD_REQUIRED, "status");
        }
        return normalized;
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private void validateEnabledOwnedCompany(Long companyId) {
        enterpriseService.getEnabledEnterprises(List.of(companyId),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType()));
    }

    private void validateEnabledOwnedCompaniesForUserAccess(Collection<Long> companyIds) {
        try {
            enterpriseService.getEnabledEnterprises(companyIds,
                    Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType()));
        } catch (ServiceException enterpriseFailure) {
            throw exception(MDM_USER_COMPANY_SCOPE_DENIED);
        }
    }

    private void validateEnabledUser(Long userId) {
        List<AdminUserRespDTO> users = adminUserApi.getUserList(List.of(userId));
        if (users == null || users.size() != 1) {
            throw exception(MDM_COMPANY_SCOPE_SYSTEM_USER_INVALID, userId);
        }
        AdminUserRespDTO user = users.get(0);
        if (user == null || !Objects.equals(userId, user.getId()) || StrUtil.isBlank(user.getUsername())
                || !CommonStatusEnum.isEnable(user.getStatus())) {
            throw exception(MDM_COMPANY_SCOPE_SYSTEM_USER_INVALID, userId);
        }
    }

    private void validateEnabledRole(Long roleId) {
        List<RoleRespDTO> roles = roleApi.getRoleList(List.of(roleId));
        if (roles == null || roles.size() != 1) {
            throw exception(MDM_COMPANY_SCOPE_SYSTEM_ROLE_INVALID, roleId);
        }
        RoleRespDTO role = roles.get(0);
        if (role == null || !Objects.equals(roleId, role.getId()) || StrUtil.isBlank(role.getCode())
                || StrUtil.isBlank(role.getName()) || !CommonStatusEnum.isEnable(role.getStatus())) {
            throw exception(MDM_COMPANY_SCOPE_SYSTEM_ROLE_INVALID, roleId);
        }
    }

    private void validateRawUserScope(MdmUserCompanyScopeDO scope, Long tenantId, Long userId, Long companyId) {
        if (scope == null || scope.getId() == null || scope.getId() <= 0
                || !Objects.equals(tenantId, scope.getTenantId())
                || scope.getUserId() == null || scope.getUserId() <= 0
                || !Objects.equals(userId, scope.getUserId())
                || scope.getCompanyId() == null || scope.getCompanyId() <= 0
                || !Objects.equals(companyId, scope.getCompanyId())
                || scope.getDeleted() == null
                || scope.getRevision() == null || scope.getRevision() <= 0
                || !MdmEnterpriseStatusEnum.isValid(scope.getStatus())) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
    }

    private Long requirePositiveId(Long value, String field) {
        if (value == null || value <= 0) {
            throw exception(MDM_COMPANY_SCOPE_FIELD_REQUIRED, field);
        }
        return value;
    }

    private List<Long> validateCompanyIds(Collection<Long> companyIds) {
        if (companyIds == null || companyIds.isEmpty()
                || companyIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw exception(MDM_COMPANY_SCOPE_FIELD_REQUIRED, "companyIds");
        }
        if (new LinkedHashSet<>(companyIds).size() != companyIds.size()) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
        return companyIds.stream().sorted().toList();
    }

    private String requireStatus(String value) {
        String normalized = StrUtil.trimToNull(value);
        if (!MdmEnterpriseStatusEnum.isValid(normalized)) {
            throw exception(MDM_COMPANY_SCOPE_FIELD_REQUIRED, "status");
        }
        return normalized;
    }

    private void validateWriteResult(int affectedRows, Long id) {
        if (affectedRows != 1 || id == null || id <= 0) {
            throw exception(MDM_COMPANY_SCOPE_WRITE_RESULT_INVALID);
        }
    }

    private boolean isNamedUniqueConflict(DuplicateKeyException exception, String constraint) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(constraint)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

}
