package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNameClaimDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Mapper
public interface DccControlledFileNameClaimMapper extends BaseMapperX<DccControlledFileNameClaimDO> {

    @Select("""
            SELECT *
            FROM dcc_controlled_file_name_claim
            WHERE tenant_id = #{tenantId}
              AND source_name_key = #{sourceNameKey}
              AND deleted = 0
            LIMIT 1
            """)
    DccControlledFileNameClaimDO selectActiveBySourceNameKey(@Param("tenantId") Long tenantId,
                                                    @Param("sourceNameKey") byte[] sourceNameKey);

    default DccControlledFileNameClaimDO selectActiveByName(Long tenantId, String sourceName) {
        return selectActiveBySourceNameKey(tenantId, sourceName.getBytes(StandardCharsets.UTF_8));
    }

    @Select(DccLegacySourceNameSql.COUNT_UNRESOLVED)
    long countUnresolvedNames(@Param("tenantId") Long tenantId);

    @Select("SELECT * FROM dcc_controlled_file_name_claim WHERE tenant_id=#{tenantId} AND dcc_project_code_id=#{projectId} AND file_type_taxonomy_leaf_id=#{leafId} AND number_key=#{numberKey} AND deleted=0")
    DccControlledFileNameClaimDO selectActiveByNumber(@Param("tenantId") Long tenantId, @Param("projectId") Long projectId,
            @Param("leafId") Long leafId, @Param("numberKey") byte[] numberKey);

    @Update("""
            UPDATE dcc_controlled_file_name_claim
            SET obsolete_time=CASE WHEN obsolete_time IS NULL OR obsolete_time<#{obsoleteTime} THEN #{obsoleteTime} ELSE obsolete_time END,
                retain_until=CASE WHEN retain_until IS NULL OR retain_until<#{retainUntil} THEN #{retainUntil} ELSE retain_until END
            WHERE tenant_id=#{tenantId} AND master_id=#{masterId} AND deleted=0 AND source_original_file_name IS NOT NULL
            """)
    int retainByMasterId(@Param("tenantId") Long tenantId, @Param("masterId") Long masterId,
            @Param("obsoleteTime") LocalDateTime obsoleteTime, @Param("retainUntil") LocalDateTime retainUntil);

    @Select("SELECT COUNT(*) FROM dcc_controlled_file_name_claim WHERE tenant_id=#{tenantId} AND master_id=#{masterId} AND deleted=0 AND source_original_file_name IS NOT NULL AND (retain_until IS NULL OR retain_until>#{now})")
    long countUnexpiredByMasterId(@Param("tenantId") Long tenantId, @Param("masterId") Long masterId, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE dcc_controlled_file_name_claim
            SET deleted = 1,
                updater = '',
                update_time = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND master_id = #{masterId}
              AND deleted = 0
              AND source_original_file_name IS NOT NULL
            """)
    int releaseByMasterId(@Param("tenantId") Long tenantId, @Param("masterId") Long masterId);
}
