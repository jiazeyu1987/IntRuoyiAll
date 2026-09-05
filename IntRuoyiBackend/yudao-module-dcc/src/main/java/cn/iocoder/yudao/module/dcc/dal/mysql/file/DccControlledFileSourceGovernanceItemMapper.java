package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DccControlledFileSourceGovernanceItemMapper
        extends BaseMapperX<DccControlledFileSourceGovernanceItemDO> {

    @Select("""
            SELECT *
            FROM dcc_controlled_file_source_governance_item
            WHERE batch_id = #{batchId}
              AND tenant_id = #{tenantId}
              AND controlled_file_id = #{controlledFileId}
              AND deleted = 0
            LIMIT 1
            """)
    DccControlledFileSourceGovernanceItemDO selectByBatchAndControlledFile(
            @Param("batchId") Long batchId,
            @Param("tenantId") Long tenantId,
            @Param("controlledFileId") Long controlledFileId);

    @Select("""
            SELECT *
            FROM dcc_controlled_file_source_governance_item
            WHERE batch_id = #{batchId}
              AND tenant_id = #{tenantId}
              AND deleted = 0
            ORDER BY tenant_id, controlled_file_id, id
            """)
    List<DccControlledFileSourceGovernanceItemDO> selectByBatchAndTenant(
            @Param("batchId") Long batchId, @Param("tenantId") Long tenantId);

    @Select("""
            SELECT item_status AS itemStatus, COUNT(*) AS itemCount
            FROM dcc_controlled_file_source_governance_item
            WHERE batch_id = #{batchId}
              AND tenant_id = #{tenantId}
              AND deleted = 0
            GROUP BY item_status
            """)
    List<Map<String, Object>> selectStatusCountsByBatchAndTenant(
            @Param("batchId") Long batchId, @Param("tenantId") Long tenantId);
}
