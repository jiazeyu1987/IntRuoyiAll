package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNameClaimDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DccControlledFileNameClaimMapper extends BaseMapperX<DccControlledFileNameClaimDO> {

    @Select("""
            SELECT id, tenant_id, normalized_name, master_id,
                   create_time, update_time, creator, updater, deleted
            FROM dcc_controlled_file_name_claim
            WHERE tenant_id = #{tenantId}
              AND normalized_name = #{normalizedName}
              AND deleted = 0
            LIMIT 1
            """)
    DccControlledFileNameClaimDO selectActiveByName(@Param("tenantId") Long tenantId,
                                                    @Param("normalizedName") String normalizedName);

    @Update("""
            UPDATE dcc_controlled_file_name_claim
            SET deleted = 1,
                updater = '',
                update_time = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND master_id = #{masterId}
              AND deleted = 0
            """)
    int releaseByMasterId(@Param("tenantId") Long tenantId, @Param("masterId") Long masterId);
}
