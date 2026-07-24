package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionRuleCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeSaveCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MesProRouteOwnerPermissionServiceImpl implements MesProRouteOwnerPermissionService {

    private static final String ROUTE_PERMISSION_OBJECT_TYPE = "ROUTE";
    private static final String PERMISSION_SUBJECT_TYPE_USER = "USER";
    private static final String PERMISSION_DECISION_ALLOW = "ALLOW";
    private static final String PERMISSION_STATUS_ENABLED = "ENABLED";
    private static final int ROUTE_OWNER_PERMISSION_PRIORITY = 10;
    private static final List<String> ROUTE_OWNER_ABILITIES = List.of(
            "VIEW", "ROUTE_EDIT", "PERMISSION_ADMIN");

    private final MesProEdhrPermissionScopeService permissionScopeService;

    public MesProRouteOwnerPermissionServiceImpl(MesProEdhrPermissionScopeService permissionScopeService) {
        this.permissionScopeService = permissionScopeService;
    }

    @Override
    public void bindCurrentUserAsOwner(Long routeId) {
        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();
        permissionScopeService.saveRules(new MesProEdhrPermissionScopeSaveCommand()
                .setScopeName("route-" + routeId)
                .setObjectType(ROUTE_PERMISSION_OBJECT_TYPE)
                .setObjectId(String.valueOf(routeId))
                .setActorUserId(actorUserId)
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setRules(ROUTE_OWNER_ABILITIES.stream()
                        .map(ability -> new MesProEdhrPermissionRuleCommand()
                                .setSubjectType(PERMISSION_SUBJECT_TYPE_USER)
                                .setSubjectId(actorUserId)
                                .setAbility(ability)
                                .setDecision(PERMISSION_DECISION_ALLOW)
                                .setPriority(ROUTE_OWNER_PERMISSION_PRIORITY)
                                .setStatus(PERMISSION_STATUS_ENABLED))
                        .toList()));
    }

}
