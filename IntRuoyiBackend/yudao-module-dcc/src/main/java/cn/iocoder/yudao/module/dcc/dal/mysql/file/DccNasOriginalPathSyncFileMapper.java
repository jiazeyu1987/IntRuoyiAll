package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccNasOriginalPathSyncFileDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface DccNasOriginalPathSyncFileMapper extends BaseMapperX<DccNasOriginalPathSyncFileDO> {

    default DccNasOriginalPathSyncFileDO selectActiveByPathHash(String nasShareName, String pathHash) {
        return selectOne(new LambdaQueryWrapperX<DccNasOriginalPathSyncFileDO>()
                .eq(DccNasOriginalPathSyncFileDO::getNasShareName, nasShareName)
                .eq(DccNasOriginalPathSyncFileDO::getPathHash, pathHash)
                .eq(DccNasOriginalPathSyncFileDO::getSyncStatus, "ACTIVE")
                .last("LIMIT 1"));
    }

    default List<DccNasOriginalPathSyncFileDO> selectActiveByPathHashes(String nasShareName,
                                                                        Collection<String> pathHashes) {
        if (pathHashes == null || pathHashes.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<DccNasOriginalPathSyncFileDO>()
                .eq(DccNasOriginalPathSyncFileDO::getNasShareName, nasShareName)
                .in(DccNasOriginalPathSyncFileDO::getPathHash, pathHashes)
                .eq(DccNasOriginalPathSyncFileDO::getSyncStatus, "ACTIVE")
                .orderByAsc(DccNasOriginalPathSyncFileDO::getId));
    }

    @Select("""
            SELECT id,
                   audit_task_id,
                   audit_file_id,
                   transfer_task_id,
                   transfer_task_item_id,
                   source_file_id,
                   nas_share_name,
                   root_path,
                   normalized_relative_path,
                   path_hash,
                   file_name,
                   file_size,
                   modified_at,
                   source_signature,
                   sync_status,
                   synced_by_user_id,
                   synced_at,
                   deleted_by_user_id,
                   deleted_at,
                   tenant_id
            FROM dcc_nas_original_path_sync_file
            WHERE deleted = b'0'
              AND tenant_id = #{tenantId}
              AND nas_share_name = #{nasShareName}
              AND sync_status = 'ACTIVE'
            ORDER BY path_hash, id
            """)
    List<DccNasOriginalPathSyncFileDO> selectActiveRows(@Param("tenantId") Long tenantId,
                                                        @Param("nasShareName") String nasShareName);

    default int softDeleteActiveById(Long id, Long userId, LocalDateTime deletedAt) {
        return update(null, new LambdaUpdateWrapper<DccNasOriginalPathSyncFileDO>()
                .eq(DccNasOriginalPathSyncFileDO::getId, id)
                .eq(DccNasOriginalPathSyncFileDO::getSyncStatus, "ACTIVE")
                .set(DccNasOriginalPathSyncFileDO::getSyncStatus, "DELETED")
                .set(DccNasOriginalPathSyncFileDO::getDeletedByUserId, userId)
                .set(DccNasOriginalPathSyncFileDO::getDeletedAt, deletedAt));
    }
}
