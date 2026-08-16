package cn.iocoder.yudao.module.mdm.dal.mysql.companyscope;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mdm.dal.dataobject.companyscope.MdmRoleCompanyScopeDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MdmRoleCompanyScopeMapper extends BaseMapperX<MdmRoleCompanyScopeDO> {

    @Select("""
            <script>
            SELECT id, tenant_id, deleted, role_id, company_id, status, revision
            FROM mdm_role_company_scope
            WHERE tenant_id = #{tenantId}
              AND company_id = #{companyId}
              AND role_id IN
              <foreach collection="roleIds" item="roleId" open="(" separator="," close=")">
                #{roleId}
              </foreach>
            </script>
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<MdmRoleCompanyScopeDO> selectByTenantCompanyAndRoleIds(@Param("tenantId") Long tenantId,
                                                                @Param("companyId") Long companyId,
                                                                @Param("roleIds") Collection<Long> roleIds);

}
