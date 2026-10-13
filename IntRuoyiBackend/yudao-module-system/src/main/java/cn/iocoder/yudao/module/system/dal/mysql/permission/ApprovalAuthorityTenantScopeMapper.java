package cn.iocoder.yudao.module.system.dal.mysql.permission;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ApprovalAuthorityTenantScopeMapper {
    /** Menus are global. Formal role-to-tenant rows establish every affected tenant. */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT DISTINCT r.tenant_id FROM system_role r JOIN system_tenant t ON t.id = r.tenant_id "
            + "WHERE r.deleted = 0 AND t.deleted = 0 AND r.tenant_id > 0 ORDER BY r.tenant_id")
    List<Long> selectGlobalRoleTenantIds();
}
