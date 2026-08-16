package cn.iocoder.yudao.module.mdm.dal.mysql.companyscope;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mdm.dal.dataobject.companyscope.MdmUserCompanyScopeDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MdmUserCompanyScopeMapper extends BaseMapperX<MdmUserCompanyScopeDO> {

    @Select("""
            SELECT id, tenant_id, deleted, user_id, company_id, status, revision
            FROM mdm_user_company_scope
            WHERE tenant_id = #{tenantId}
              AND user_id = #{userId}
              AND company_id = #{companyId}
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<MdmUserCompanyScopeDO> selectByTenantUserAndCompany(@Param("tenantId") Long tenantId,
                                                              @Param("userId") Long userId,
                                                              @Param("companyId") Long companyId);

    @Select("""
            <script>
            SELECT id, tenant_id, deleted, user_id, company_id, status, revision
            FROM mdm_user_company_scope
            WHERE tenant_id = #{tenantId}
              AND user_id = #{userId}
              AND company_id IN
              <foreach collection="companyIds" item="companyId" open="(" separator="," close=")">
                #{companyId}
              </foreach>
            </script>
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<MdmUserCompanyScopeDO> selectByTenantUserAndCompanyIds(@Param("tenantId") Long tenantId,
                                                                 @Param("userId") Long userId,
                                                                 @Param("companyIds") Collection<Long> companyIds);

    @Select("""
            SELECT id, tenant_id, deleted, user_id, company_id, status, revision
            FROM mdm_user_company_scope
            WHERE tenant_id = #{tenantId}
              AND user_id = #{userId}
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<MdmUserCompanyScopeDO> selectByTenantUser(@Param("tenantId") Long tenantId,
                                                    @Param("userId") Long userId);

    @Select("""
            SELECT id, tenant_id, deleted, user_id, company_id, status, revision
            FROM mdm_user_company_scope
            WHERE tenant_id = #{tenantId}
              AND company_id = #{companyId}
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<MdmUserCompanyScopeDO> selectByTenantCompany(@Param("tenantId") Long tenantId,
                                                       @Param("companyId") Long companyId);

}
