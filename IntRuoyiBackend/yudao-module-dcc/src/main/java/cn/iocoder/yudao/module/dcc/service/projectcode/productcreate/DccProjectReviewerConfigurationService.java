package cn.iocoder.yudao.module.dcc.service.projectcode.productcreate;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectConfigurationAuditService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.sql.DataSource;
import java.util.Objects;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/** A tenant-owned configuration; each submission freezes the actual reviewer identity separately. */
@Service
public class DccProjectReviewerConfigurationService {
    public static final ErrorCode CONFIG_MISSING=new ErrorCode(1_080_090_015,"请先配置项目及产品创建审核人");
    public static final ErrorCode ACCOUNT_INVALID=new ErrorCode(1_080_090_016,"审核人必须是当前租户存在且启用的正式系统账号");
    public static final ErrorCode REVIEWER_REQUIRED=new ErrorCode(1_080_090_017,"仅本次申请提交时指定的审核人可审核");
    public static final ErrorCode CONFIG_FORBIDDEN=new ErrorCode(1_080_090_018,"没有项目及产品审核人配置权限");
    public record Save(@NotNull @Positive Long reviewerUserId,@NotBlank @Size(max=500) String reason) {}
    public record View(boolean configured,@JsonFormat(shape=JsonFormat.Shape.STRING) Long reviewerUserId,
                       String reviewerUsername,String reviewerNickname,boolean enabled) {}
    private record Row(long reviewerUserId,String username,String nickname,int version) {}
    @Resource private DataSource dataSource;
    @Resource private AdminUserApi users;
    @Resource private PermissionApi permissions;
    @Resource private DccProjectConfigurationAuditService audit;
    private JdbcTemplate jdbc(){return new JdbcTemplate(dataSource);}
    private Row row(boolean lock){
        var rows=jdbc().query("SELECT reviewer_user_id,reviewer_username,reviewer_nickname,version_no FROM dcc_project_reviewer_config WHERE tenant_id=?"+(lock?" FOR UPDATE":""),
                (r,n)->new Row(r.getLong(1),r.getString(2),r.getString(3),r.getInt(4)),TenantContextHolder.getRequiredTenantId());
        return rows.isEmpty()?null:rows.get(0);
    }
    public View get(){
        var row=row(false);if(row==null)return new View(false,null,null,null,false);
        var user=users.getUser(row.reviewerUserId());
        return new View(true,row.reviewerUserId(),row.username(),row.nickname(),valid(user));
    }
    private boolean valid(AdminUserRespDTO user){return user!=null && Objects.equals(user.getStatus(),0)
            && Objects.equals(user.getTenantId(),TenantContextHolder.getRequiredTenantId())
            && user.getUsername()!=null && !user.getUsername().isBlank() && user.getNickname()!=null && !user.getNickname().isBlank();}
    public AdminUserRespDTO requireAccount(Long id){var user=id==null?null:users.getUser(id);if(!valid(user))throw exception(ACCOUNT_INVALID);return user;}
    @Transactional(rollbackFor=Exception.class)
    public AdminUserRespDTO requireConfigured(){var row=row(true);if(row==null)throw exception(CONFIG_MISSING);return requireAccount(row.reviewerUserId());}
    @Transactional(rollbackFor=Exception.class)
    public View save(Long operator,Save request){
        if(operator==null || !permissions.hasAnyPermissions(operator,"dcc:project-code:update")
                || !permissions.hasAnyRoles(operator,"doc_control"))throw exception(CONFIG_FORBIDDEN);
        if(request==null)throw exception(ACCOUNT_INVALID);audit.validateReason(request.reason());
        var account=requireAccount(request.reviewerUserId());var before=row(true);Long tenant=TenantContextHolder.getRequiredTenantId();
        int changed=before==null?jdbc().update("INSERT INTO dcc_project_reviewer_config(tenant_id,reviewer_user_id,reviewer_username,reviewer_nickname,version_no,updated_by,change_reason) VALUES(?,?,?,?,1,?,?)",
                tenant,account.getId(),account.getUsername(),account.getNickname(),operator,request.reason().trim())
                :jdbc().update("UPDATE dcc_project_reviewer_config SET reviewer_user_id=?,reviewer_username=?,reviewer_nickname=?,version_no=version_no+1,updated_by=?,change_reason=?,update_time=CURRENT_TIMESTAMP WHERE tenant_id=? AND version_no=?",
                account.getId(),account.getUsername(),account.getNickname(),operator,request.reason().trim(),tenant,before.version());
        if(changed!=1)throw new IllegalStateException("reviewer configuration write incomplete");
        var after=row(true);audit.append("dcc.project-product.reviewer-config",tenant,String.valueOf(after.version()),request.reason(),
                before==null?null:JsonUtils.toJsonString(before),JsonUtils.toJsonString(after));
        return get();
    }
    public void assertFrozenReviewer(Long actor,DccProjectProductCreateRequestDO request){
        if(actor==null || !Objects.equals(actor,request.getConfiguredReviewerUserId()))throw exception(REVIEWER_REQUIRED);
        requireAccount(actor);
    }
}
