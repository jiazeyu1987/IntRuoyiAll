package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchAuthorityExport;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.*;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.*;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantMapper;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import java.time.*;
import java.util.*;

@Service
@ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class SystemWorkbenchAuthorityExport implements WorkbenchAuthorityExport {
    private final AdminUserMapper users;
    private final RoleMapper roles;
    private final UserRoleMapper userRoles;
    private final RoleMenuMapper roleMenus;
    private final MenuMapper menus;
    private final TemporaryRoleGrantMapper temporary;
    private final SystemEntitlementGrantMapper entitlements;
    private final TenantMapper tenants;
    public SystemWorkbenchAuthorityExport(AdminUserMapper users,RoleMapper roles,UserRoleMapper userRoles,
                                         RoleMenuMapper roleMenus,MenuMapper menus,TemporaryRoleGrantMapper temporary,
                                         SystemEntitlementGrantMapper entitlements,TenantMapper tenants) {
        this.users=users;this.roles=roles;this.userRoles=userRoles;this.roleMenus=roleMenus;this.menus=menus;this.temporary=temporary;this.entitlements=entitlements;this.tenants=tenants;
    }
    @Override public List<Grant> exportGrants(long tenant,Requirement requirement) {
        if(!TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Authority export requires frozen master transaction");
        // Full tenant projection must not depend on the writer's department scope or recurse through permissions.
        return DataPermissionUtils.executeIgnore(()->exportAllTenantGrants(tenant,requirement));
    }
    private List<Grant> exportAllTenantGrants(long tenant,Requirement requirement) {
        var tenantFact=tenants.selectById(tenant);
        if(tenantFact==null) throw new IllegalStateException("Formal tenant missing in authority export");
        if(!Integer.valueOf(0).equals(tenantFact.getStatus())) return List.of();
        if(tenantFact.getExpireTime()==null) throw new IllegalStateException("Formal tenant authorization expiry missing");
        Instant tenantEnd=tenantFact.getExpireTime().atZone(ZoneId.systemDefault()).toInstant();
        Set<Long> enabledUsers=new HashSet<>();
        Map<Long,Instant> userEnds=new HashMap<>();
        for(AdminUserDO user:users.selectList(new LambdaQueryWrapper<AdminUserDO>().eq(AdminUserDO::getTenantId,tenant).eq(AdminUserDO::getStatus,0))) {
            enabledUsers.add(user.getId());
            Instant end=tenantEnd;
            if(user.getLifecycleEffectiveTime()!=null) {
                Instant userEnd=user.getLifecycleEffectiveTime().atZone(ZoneId.systemDefault()).toInstant();
                if(userEnd.isBefore(end)) end=userEnd;
            }
            userEnds.put(user.getId(),end);
        }
        Map<Long,RoleDO> enabledRoles=new HashMap<>();
        for(RoleDO role:roles.selectList(new LambdaQueryWrapper<RoleDO>().eq(RoleDO::getTenantId,tenant).eq(RoleDO::getStatus,0))) enabledRoles.put(role.getId(),role);
        Set<Long> matchingRoles=new HashSet<>(),superRoles=new HashSet<>();
        enabledRoles.values().forEach(role->{ if(RoleCodeEnum.SUPER_ADMIN.getCode().equals(role.getCode())) superRoles.add(role.getId()); });
        if(requirement.mode()==Mode.ANY_ROLE_OR_SUPER_ADMIN)
            enabledRoles.values().forEach(role->{if(requirement.values().contains(role.getCode())||superRoles.contains(role.getId())) matchingRoles.add(role.getId());});
        else {
            Set<Long> matchingMenus=new HashSet<>();
            // Formal PermissionService matches a permission to existing Menu rows without a menu-status filter.
            for(MenuDO menu:menus.selectList(new LambdaQueryWrapper<MenuDO>().in(MenuDO::getPermission,requirement.values()))) matchingMenus.add(menu.getId());
            if(!matchingMenus.isEmpty()) for(RoleMenuDO relation:roleMenus.selectList(new LambdaQueryWrapper<RoleMenuDO>().eq(RoleMenuDO::getTenantId,tenant).in(RoleMenuDO::getMenuId,matchingMenus)))
                if(enabledRoles.containsKey(relation.getRoleId())) matchingRoles.add(relation.getRoleId());
        }
        List<Grant> grants=new ArrayList<>();
        for(UserRoleDO relation:userRoles.selectList(new LambdaQueryWrapper<UserRoleDO>().eq(UserRoleDO::getTenantId,tenant))) {
            if(enabledUsers.contains(relation.getUserId())&&(matchingRoles.contains(relation.getRoleId())||superRoles.contains(relation.getRoleId())))
                add(grants,relation.getUserId(),"role:"+relation.getId(),Instant.EPOCH,userEnds.get(relation.getUserId()));
        }
        for(TemporaryRoleGrantDO grant:temporary.selectList(new LambdaQueryWrapper<TemporaryRoleGrantDO>().eq(TemporaryRoleGrantDO::getTenantId,tenant).eq(TemporaryRoleGrantDO::getStatus,"ACTIVE"))) {
            if(!enabledUsers.contains(grant.getUserId())||!matchingRoles.contains(grant.getRoleId())) continue;
            if(grant.getEffectiveTime()==null||grant.getExpireTime()==null) throw new IllegalStateException("Temporary grant interval missing");
            Instant end=grant.getExpireTime().atZone(ZoneId.systemDefault()).toInstant(),userEnd=userEnds.get(grant.getUserId());
            if(userEnd!=null&&userEnd.isBefore(end)) end=userEnd;
            add(grants,grant.getUserId(),"temporary:"+grant.getId(),grant.getEffectiveTime().atZone(ZoneId.systemDefault()).toInstant(),end);
        }
        if(requirement.mode()==Mode.ANY_PERMISSION) {
            for(SystemEntitlementGrantDO grant:entitlements.selectList(new LambdaQueryWrapper<SystemEntitlementGrantDO>().eq(SystemEntitlementGrantDO::getTenantId,tenant).eq(SystemEntitlementGrantDO::getStatus,"ACTIVE").in(SystemEntitlementGrantDO::getPermissionCode,requirement.values())))
                if(enabledUsers.contains(grant.getResolvedUserId())) add(grants,grant.getResolvedUserId(),"entitlement:"+grant.getId(),Instant.EPOCH,userEnds.get(grant.getResolvedUserId()));
        }
        return List.copyOf(grants);
    }
    private static void add(List<Grant> grants,long user,String key,Instant from,Instant until) {
        if(until==null||from.isBefore(until)) grants.add(new Grant(user,key,from,until));
    }
}
