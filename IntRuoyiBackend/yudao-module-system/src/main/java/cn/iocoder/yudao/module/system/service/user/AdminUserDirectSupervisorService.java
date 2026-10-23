package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.module.system.api.user.dto.AdminUserDirectSupervisorRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserDirectSupervisorRespVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserDirectSupervisorSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.service.user.AdminUserDirectSupervisorErrors.*;

@Service
public class AdminUserDirectSupervisorService {
    @Resource private AdminUserMapper users;
    @Resource private PermissionApi permissions;
    @Resource private GxpAuditService audit;

    @Transactional(readOnly=true)
    @DataPermission(enable=false)
    public AdminUserDirectSupervisorRespDTO getDirectSupervisorRelation(Long userId) {
        Long tenant=tenant();var user=enabled(users.selectDirectSupervisorUser(tenant,userId),tenant,userId);
        version(user);
        if(user.getDirectSupervisorUserId()!=null) {
            if(Objects.equals(userId,user.getDirectSupervisorUserId()))throw exception(INVALID);
            enabled(users.selectDirectSupervisorUser(tenant,user.getDirectSupervisorUserId()),tenant,user.getDirectSupervisorUserId());
        }
        return snapshot(user);
    }

    @Transactional(readOnly=true)
    public UserDirectSupervisorRespVO getRelation(Long actorId, Long userId) {
        Long tenant=tenant();requireActor(actorId,tenant,"system:user:query");
        var user=enabled(users.selectDirectSupervisorUser(tenant,userId),tenant,userId);version(user);
        return projection(user,tenant);
    }

    @Transactional(rollbackFor=Exception.class)
    @GxpWriteOperation(operationId="system.user.direct-supervisor.update")
    public UserDirectSupervisorRespVO saveRelation(Long actorId, UserDirectSupervisorSaveReqVO request) {
        Long tenant=tenant();requireActor(actorId,tenant,"system:user:update");
        if(request==null || request.getUserId()==null || request.getUserId()<=0 || request.getChangeReason()==null
                || request.getChangeReason().isBlank() || request.getChangeReason().length()>500)throw exception(INVALID);
        Long expected=request.getExpectedRelationVersion();
        if(expected==null || expected<0)throw exception(VERSION_INVALID);
        boolean clear="CLEAR".equals(request.getOperation());Long selected=request.getSupervisorUserId();
        if((!clear && !"SET".equals(request.getOperation())) || (clear && selected!=null)
                || (!clear && (selected==null || selected<=0)) || Objects.equals(request.getUserId(),selected))throw exception(INVALID);
        // A stable account lock order protects target/new supervisor enabled facts without A->B/B->A deadlocks.
        var ids=new TreeSet<Long>();ids.add(actorId);ids.add(request.getUserId());if(selected!=null)ids.add(selected);
        var locked=new HashMap<Long,AdminUserDO>();
        for(Long id:ids)locked.put(id,enabled(users.selectDirectSupervisorUserForUpdate(tenant,id),tenant,id));
        var target=locked.get(request.getUserId());long previous=version(target);
        if(previous!=expected)throw exception(CONFLICT);
        if(Objects.equals(target.getDirectSupervisorUserId(),selected))return projection(target,tenant);
        if(previous==Long.MAX_VALUE)throw exception(VERSION_INVALID);
        long next=Math.addExact(previous,1L);var before=snapshot(target);
        if(users.updateDirectSupervisorRelation(tenant,target.getId(),selected,previous,next,actorId)!=1)throw exception(CONFLICT);
        var persisted=enabled(users.selectDirectSupervisorUserForUpdate(tenant,target.getId()),tenant,target.getId());
        if(!Objects.equals(selected,persisted.getDirectSupervisorUserId()) || version(persisted)!=next)throw exception(CONFLICT);
        audit.append(GxpAuditCommand.builder().operationId("system.user.direct-supervisor.update")
                .subjectId("SYSTEM_USER_DIRECT_SUPERVISOR:"+target.getId()).subjectVersion(String.valueOf(next))
                .reason(request.getChangeReason().trim()).source("AdminUserDirectSupervisorService.saveRelation")
                .idempotencyKey("SYSTEM:DIRECT_SUPERVISOR:"+tenant+":"+target.getId()+":"+next)
                .beforeState(GxpAuditStateEnvelope.builder().state("PRESENT").objectVersion(String.valueOf(previous)).canonicalJson(JsonUtils.toJsonString(before)).build())
                .afterState(GxpAuditStateEnvelope.builder().state("PRESENT").objectVersion(String.valueOf(next)).canonicalJson(JsonUtils.toJsonString(snapshot(persisted))).build()).build());
        return projection(persisted,tenant);
    }
    private Long tenant() {
        if(TenantContextHolder.isIgnore() || TenantContextHolder.getTenantId()==null)throw exception(INVALID);
        return TenantContextHolder.getRequiredTenantId();
    }
    private void requireActor(Long id,Long tenant,String permission) {
        var login=SecurityFrameworkUtils.getLoginUser();
        if(login==null || !Objects.equals(id,login.getId()) || !Objects.equals(tenant,login.getTenantId())
                || !Objects.equals(UserTypeEnum.ADMIN.getValue(),login.getUserType()))throw exception(INVALID);
        enabled(users.selectDirectSupervisorUser(tenant,id),tenant,id);
        if(!permissions.hasAnyPermissions(id,permission))throw exception(FORBIDDEN);
    }
    private AdminUserDO enabled(AdminUserDO user,Long tenant,Long id) {
        if(id==null || id<=0 || user==null || !Objects.equals(user.getId(),id) || !Objects.equals(user.getTenantId(),tenant)
                || !Integer.valueOf(0).equals(user.getStatus()) || user.getUsername()==null || user.getUsername().isBlank()
                || user.getNickname()==null || user.getNickname().isBlank())throw exception(INVALID);
        return user;
    }
    private long version(AdminUserDO user) {
        if(user.getDirectSupervisorRevision()==null || user.getDirectSupervisorRevision()<0)throw exception(VERSION_INVALID);
        return user.getDirectSupervisorRevision();
    }
    private AdminUserDirectSupervisorRespDTO snapshot(AdminUserDO user) {
        return new AdminUserDirectSupervisorRespDTO(user.getTenantId(),user.getId(),user.getDirectSupervisorUserId(),user.getDirectSupervisorRevision());
    }
    private UserDirectSupervisorRespVO projection(AdminUserDO user,Long tenant) {
        var response=new UserDirectSupervisorRespVO();response.setTenantId(tenant);response.setUserId(user.getId());
        response.setUsername(user.getUsername());response.setNickname(user.getNickname());
        response.setSupervisorUserId(user.getDirectSupervisorUserId());response.setRelationVersion(user.getDirectSupervisorRevision());
        if(user.getDirectSupervisorUserId()!=null) {
            var supervisor=users.selectDirectSupervisorUser(tenant,user.getDirectSupervisorUserId());
            if(supervisor!=null) {
                if(!Objects.equals(supervisor.getId(),user.getDirectSupervisorUserId()) || !Objects.equals(supervisor.getTenantId(),tenant))throw exception(INVALID);
                response.setSupervisorUsername(supervisor.getUsername());response.setSupervisorNickname(supervisor.getNickname());response.setSupervisorStatus(supervisor.getStatus());
            }
        }
        return response;
    }
}
