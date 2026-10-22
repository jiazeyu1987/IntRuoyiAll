package cn.iocoder.yudao.module.dcc.service.projectcode.productcreate;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
public class DccProjectReviewerRoleService {
    public static final String CONTRACT="ROLE_SINGLE_REVIEW_V1", ROLE_CODE="dcc_project_reviewer";
    public static final ErrorCode ROLE_INVALID=new ErrorCode(1_080_090_060,"请在角色管理配置唯一启用的项目审核员角色及有效审核成员");
    public static final ErrorCode ROLE_FORBIDDEN=new ErrorCode(1_080_090_061,"仅当前租户启用的项目审核员角色成员可审核本申请");
    public static final ErrorCode CONTRACT_INVALID=new ErrorCode(1_080_090_062,"项目创建审批合同身份不合法");
    @Resource private DataSource dataSource;
    @Resource private PermissionApi permissions;
    private JdbcTemplate jdbc(){return new JdbcTemplate(dataSource);}
    private record Role(Long id,String code,String name,Integer status) {}
    public record Member(@JsonFormat(shape=JsonFormat.Shape.STRING) Long userId,String username,String nickname) {}
    public record View(String reviewContract,@JsonFormat(shape=JsonFormat.Shape.STRING) Long roleId,
            String roleCode,String roleName,List<Member> candidates,boolean canReview) {}
    public View readiness(Long actorId) {
        Long tenant=tenant();requireAuthenticated(actorId,tenant);
        if(!enabledAccount(actorId,tenant) || !permissions.hasAnyPermissions(actorId,"dcc:project-code:query"))throw exception(ROLE_FORBIDDEN);
        var role=requireRole(false);var candidates=members(role,false);
        if(candidates.isEmpty())throw exception(ROLE_INVALID);
        return new View(CONTRACT,role.id(),role.code(),role.name(),candidates,candidates.stream().anyMatch(m->m.userId().equals(actorId)));
    }
    @Transactional(rollbackFor=Exception.class)
    public View freezeRole() {
        var role=requireRole(true);var candidates=members(role,true);
        if(candidates.isEmpty())throw exception(ROLE_INVALID);
        return new View(CONTRACT,role.id(),role.code(),role.name(),candidates,false);
    }
    public boolean canReview(Long actorId,DccProjectProductCreateRequestDO request) {
        if(!isRoleContract(request))return false;
        if(!Objects.equals(request.getTenantId(),tenant()))return false;
        var rows=roles(false);if(rows.size()!=1)return false;var role=rows.get(0);
        if(!validRole(role) || !Objects.equals(role.id(),request.getReviewerRoleId()) || !ROLE_CODE.equals(request.getReviewerRoleCode()))return false;
        return members(role,false).stream().anyMatch(m->m.userId().equals(actorId));
    }
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY,rollbackFor=Exception.class)
    public void assertReviewer(Long actorId,DccProjectProductCreateRequestDO request) {
        requireAuthenticated(actorId,tenant());
        if(!isRoleContract(request) || !Objects.equals(request.getTenantId(),tenant()))throw exception(CONTRACT_INVALID);
        var role=requireRole(true);assertFrozen(role,request);
        if(members(role,true).stream().noneMatch(m->m.userId().equals(actorId)))throw exception(ROLE_FORBIDDEN);
    }
    public List<Member> currentCandidates(DccProjectProductCreateRequestDO request) {
        if(!isRoleContract(request) || !Objects.equals(request.getTenantId(),tenant()))throw exception(CONTRACT_INVALID);
        var role=requireRole(false);assertFrozen(role,request);return members(role,false);
    }
    public boolean isRoleContract(DccProjectProductCreateRequestDO request) {
        if(request==null)throw exception(CONTRACT_INVALID);
        if(request.getReviewContract()==null){
            if(request.getReviewerRoleId()!=null || request.getReviewerRoleCode()!=null)throw exception(CONTRACT_INVALID);
            return false;
        }
        if(!CONTRACT.equals(request.getReviewContract()) || request.getReviewerRoleId()==null || request.getReviewerRoleId()<=0
                || !ROLE_CODE.equals(request.getReviewerRoleCode()))throw exception(CONTRACT_INVALID);
        return true;
    }
    private Long tenant(){if(TenantContextHolder.isIgnore() || TenantContextHolder.getTenantId()==null)throw exception(ROLE_FORBIDDEN);return TenantContextHolder.getRequiredTenantId();}
    private List<Role> roles(boolean lock){return jdbc().query("SELECT id,code,name,status FROM system_role WHERE tenant_id=? AND code=? AND deleted=0"+(lock?" FOR UPDATE":""),
        (row,index)->new Role(row.getLong("id"),row.getString("code"),row.getString("name"),row.getObject("status",Integer.class)),tenant(),ROLE_CODE);}
    private boolean validRole(Role role){return role.id()>0 && ROLE_CODE.equals(role.code()) && role.name()!=null && !role.name().isBlank() && Integer.valueOf(0).equals(role.status());}
    private Role requireRole(boolean lock){var rows=roles(lock);if(rows.size()!=1 || !validRole(rows.get(0)))throw exception(ROLE_INVALID);return rows.get(0);}
    private void assertFrozen(Role role,DccProjectProductCreateRequestDO request){if(!Objects.equals(role.id(),request.getReviewerRoleId()) || !ROLE_CODE.equals(request.getReviewerRoleCode()))throw exception(CONTRACT_INVALID);}
    private List<Member> members(Role role,boolean lock){
        Long tenant=tenant();
        var candidates=jdbc().query("SELECT u.id,u.username,u.nickname FROM system_user_role ur JOIN system_users u ON u.id=ur.user_id AND u.tenant_id=ur.tenant_id "
                +"WHERE ur.tenant_id=? AND ur.role_id=? AND ur.deleted=0 AND u.deleted=0 AND u.status=0 ORDER BY u.id"+(lock?" FOR UPDATE":""),
                (row,index)->new Member(row.getLong("id"),row.getString("username"),row.getString("nickname")),tenant,role.id());
        var ids=new HashSet<Long>();var result=new ArrayList<Member>();
        for(var member:candidates){
            if(member.userId()==null || member.userId()<=0 || !ids.add(member.userId()) || member.username()==null || member.username().isBlank() || member.nickname()==null || member.nickname().isBlank())throw exception(ROLE_INVALID);
            if(permissions.hasAnyPermissions(member.userId(),"dcc:project-code:review") && permissions.hasAnyPermissions(member.userId(),"dcc:project-code:query"))result.add(member);
        }
        return List.copyOf(result);
    }
    private boolean enabledAccount(Long actor,Long tenant){return actor!=null && actor>0 && jdbc().queryForList("SELECT id FROM system_users WHERE id=? AND tenant_id=? AND status=0 AND deleted=0",Long.class,actor,tenant).size()==1;}
    private void requireAuthenticated(Long actor,Long tenant){var login=SecurityFrameworkUtils.getLoginUser();if(login==null || !Objects.equals(actor,login.getId()) || !Objects.equals(tenant,login.getTenantId()) || !Objects.equals(UserTypeEnum.ADMIN.getValue(),login.getUserType()))throw exception(ROLE_FORBIDDEN);}
}
