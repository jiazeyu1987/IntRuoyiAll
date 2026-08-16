package cn.iocoder.yudao.module.mdm.dal.mysql.enterprise;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mdm.dal.dataobject.enterprise.MdmEnterpriseDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MdmEnterpriseMapper extends BaseMapperX<MdmEnterpriseDO> {

    @Select("""
            <script>
            SELECT id,
                   tenant_id,
                   deleted,
                   enterprise_code,
                   name,
                   type,
                   status,
                   revision
            FROM mdm_enterprise
            WHERE id IN
            <foreach collection="enterpriseIds" item="enterpriseId" open="(" separator="," close=")">
                #{enterpriseId}
            </foreach>
            </script>
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<MdmEnterpriseDO> selectClassificationByIds(@Param("enterpriseIds") Collection<Long> enterpriseIds);

}
