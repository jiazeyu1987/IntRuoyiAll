package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationPageReqVO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.*;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

/** system_users remains in the SQL so the same department/user data permission interceptor applies. */
@Mapper
public interface DccSignatureAuthorizationUserMapper {
    @Select("""
        <script>SELECT u.id,u.username,u.nickname,u.dept_id,u.mobile,u.status,u.login_date FROM system_users u
        LEFT JOIN dcc_electronic_signature_authorization a ON a.user_id=u.id AND a.deleted=0
        LEFT JOIN system_dept d ON d.id=u.dept_id AND d.deleted=0
        LEFT JOIN dcc_electronic_signature_authorization_audit audit ON audit.id=(SELECT z.id FROM dcc_electronic_signature_authorization_audit z WHERE z.target_user_id=u.id AND z.deleted=0 ORDER BY z.operated_at DESC,z.id DESC LIMIT 1)
        LEFT JOIN system_users op ON op.id=audit.operator_id AND op.deleted=0
        ${ew.customSqlSegment}</script>
        """)
    IPage<AdminUserDO> selectPage(IPage<AdminUserDO> page,@Param("ew") QueryWrapper<AdminUserDO> wrapper);

    static String displayName(String alias) {
        String nickname="TRIM(COALESCE("+alias+".nickname,''))";
        return "CASE WHEN "+nickname+"='' OR "+nickname+"='-' OR "+nickname+" REGEXP '[?�□]' THEN TRIM(COALESCE("+alias+".username,'')) ELSE "+nickname+" END";
    }
    static String state() {
        return "CASE WHEN a.id IS NULL THEN 'UNAUTHORIZED' WHEN a.locked_until>NOW() OR (a.authorization_state='LOCKED' AND a.locked_until IS NULL) THEN 'LOCKED' WHEN a.electronic_signature_enabled=1 AND (a.authorization_state='ENABLED' OR (a.authorization_state='LOCKED' AND a.locked_until<=NOW())) THEN 'ENABLED' ELSE 'DISABLED' END";
    }
    static String stateLabel(String expression) {
        return "CASE "+expression+" WHEN 'UNAUTHORIZED' THEN '未授权' WHEN 'ENABLED' THEN '已启用' WHEN 'DISABLED' THEN '已停用' WHEN 'LOCKED' THEN '已锁定' ELSE COALESCE("+expression+",'-') END";
    }
    default PageResult<AdminUserDO> selectAuthorizationPage(DccElectronicSignatureAuthorizationPageReqVO request) {
        QueryWrapper<AdminUserDO> wrapper=new QueryWrapper<>();
        wrapper.eq("u.deleted",false).like(request.getUsername()!=null&&!request.getUsername().isBlank(),"u.username",request.getUsername())
            .like(request.getMobile()!=null&&!request.getMobile().isBlank(),"u.mobile",request.getMobile())
            .eq(request.getStatus()!=null,"u.status",request.getStatus());
        String name=displayName("u");
        String user="CASE WHEN ("+name+")='' THEN CONCAT('用户#',u.id) WHEN TRIM(COALESCE(u.username,''))<>'' AND BINARY TRIM(u.username)<>BINARY ("+name+") THEN CONCAT("+name+",' (',TRIM(u.username),')') ELSE "+name+" END";
        ListFilterQueryUtils.apply(request,wrapper,java.util.Map.ofEntries(
            java.util.Map.entry("user",text(user)),java.util.Map.entry("username",text("u.username")),
            java.util.Map.entry("deptName",text("d.name")),java.util.Map.entry("mobile",text("u.mobile")),
            java.util.Map.entry("status",text("CASE WHEN u.status=0 THEN '启用' ELSE '禁用' END")),
            java.util.Map.entry("authorizationState",text(stateLabel(state()))),
            java.util.Map.entry("electronicSignatureEnabled",bool("("+state()+"='ENABLED')")),
            java.util.Map.entry("locked",text("CASE WHEN "+state()+"='LOCKED' THEN '已锁定' ELSE '未锁定' END")),
            java.util.Map.entry("lockedUntil",text("CASE WHEN "+state()+"='LOCKED' THEN COALESCE(DATE_FORMAT(a.locked_until,'%Y-%m-%d %H:%i:%s'),'-') ELSE '-' END")),
            java.util.Map.entry("latestAuditReason",text("audit.reason")),java.util.Map.entry("latestAuditOperatorName",text(displayName("op"))),
            java.util.Map.entry("latestAuditAt",dateTime("audit.operated_at"))));
        wrapper.orderByDesc("u.id");
        var page=cn.iocoder.yudao.framework.mybatis.core.util.MyBatisUtils.<AdminUserDO>buildPage(request);
        page.setOptimizeCountSql(false);selectPage(page,wrapper);
        return new PageResult<>(page.getRecords(),page.getTotal());
    }
}
