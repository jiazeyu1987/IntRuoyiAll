package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGlobalClaimDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DccControlledFileSourceGlobalClaimMapper
        extends BaseMapperX<DccControlledFileSourceGlobalClaimDO> {

    @TenantIgnore
    @Select("""
            SELECT *
            FROM dcc_controlled_file_source_global_claim
            WHERE source_file_id = #{sourceFileId}
              AND deleted = 0
            LIMIT 1
            """)
    DccControlledFileSourceGlobalClaimDO selectBySourceFileId(@Param("sourceFileId") Long sourceFileId);
}
