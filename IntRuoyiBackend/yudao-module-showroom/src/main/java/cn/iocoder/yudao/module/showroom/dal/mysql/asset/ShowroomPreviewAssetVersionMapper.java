package cn.iocoder.yudao.module.showroom.dal.mysql.asset;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetStatus;
import cn.iocoder.yudao.module.showroom.dal.dataobject.asset.ShowroomPreviewAssetVersionDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShowroomPreviewAssetVersionMapper extends BaseMapperX<ShowroomPreviewAssetVersionDO> {

    @Delete("""
            DELETE FROM showroom_preview_asset_version
            WHERE tenant_id = #{tenantId}
              AND target_type = #{targetType}
              AND target_id = #{targetId}
            """)
    int deleteByTarget(@Param("tenantId") Long tenantId,
                       @Param("targetType") String targetType,
                       @Param("targetId") Long targetId);

    default ShowroomPreviewAssetVersionDO selectLatestByKey(String targetType, Long targetId) {
        return selectOne(new LambdaQueryWrapperX<ShowroomPreviewAssetVersionDO>()
                .eq(ShowroomPreviewAssetVersionDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomPreviewAssetVersionDO::getTargetType, targetType)
                .eq(ShowroomPreviewAssetVersionDO::getTargetId, targetId)
                .orderByDesc(ShowroomPreviewAssetVersionDO::getVersionNo)
                .orderByDesc(ShowroomPreviewAssetVersionDO::getId)
                .last("LIMIT 1"));
    }

    default ShowroomPreviewAssetVersionDO selectLatestPublishedByKey(String targetType, Long targetId) {
        return selectOne(new LambdaQueryWrapperX<ShowroomPreviewAssetVersionDO>()
                .eq(ShowroomPreviewAssetVersionDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomPreviewAssetVersionDO::getTargetType, targetType)
                .eq(ShowroomPreviewAssetVersionDO::getTargetId, targetId)
                .eq(ShowroomPreviewAssetVersionDO::getStatus, ShowroomPreviewAssetStatus.PUBLISHED.name())
                .orderByDesc(ShowroomPreviewAssetVersionDO::getVersionNo)
                .orderByDesc(ShowroomPreviewAssetVersionDO::getId)
                .last("LIMIT 1"));
    }

    default List<ShowroomPreviewAssetVersionDO> selectPublishedByTargetAndSourceRevision(String targetType, Long targetId,
                                                                                         Long sourceRevisionId) {
        return selectList(new LambdaQueryWrapperX<ShowroomPreviewAssetVersionDO>()
                .eq(ShowroomPreviewAssetVersionDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomPreviewAssetVersionDO::getTargetType, targetType)
                .eq(ShowroomPreviewAssetVersionDO::getTargetId, targetId)
                .eq(ShowroomPreviewAssetVersionDO::getSourceRevisionId, sourceRevisionId)
                .eq(ShowroomPreviewAssetVersionDO::getStatus, ShowroomPreviewAssetStatus.PUBLISHED.name())
                .orderByDesc(ShowroomPreviewAssetVersionDO::getVersionNo)
                .orderByDesc(ShowroomPreviewAssetVersionDO::getId));
    }

}
