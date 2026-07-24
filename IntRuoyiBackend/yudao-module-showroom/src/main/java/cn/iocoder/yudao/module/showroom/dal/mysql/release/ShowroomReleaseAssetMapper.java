package cn.iocoder.yudao.module.showroom.dal.mysql.release;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseAssetDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShowroomReleaseAssetMapper extends BaseMapperX<ShowroomReleaseAssetDO> {

    default ShowroomReleaseAssetDO selectByAssetIdAndContentHash(String assetId, String contentHash) {
        return selectOne(new LambdaQueryWrapperX<ShowroomReleaseAssetDO>()
                .eq(ShowroomReleaseAssetDO::getAssetId, assetId)
                .eq(ShowroomReleaseAssetDO::getContentHash, contentHash)
                .last("LIMIT 1"));
    }

    default ShowroomReleaseAssetDO selectByScopeAssetIdAndContentHash(Long tenantId, String siteKey, String stage,
                                                                      String assetId, String contentHash) {
        return selectOne(new LambdaQueryWrapperX<ShowroomReleaseAssetDO>()
                .eq(ShowroomReleaseAssetDO::getTenantId, tenantId)
                .eq(ShowroomReleaseAssetDO::getSiteKey, siteKey)
                .eq(ShowroomReleaseAssetDO::getStage, stage)
                .eq(ShowroomReleaseAssetDO::getAssetId, assetId)
                .eq(ShowroomReleaseAssetDO::getContentHash, contentHash)
                .last("LIMIT 1"));
    }

    @Select("SELECT id, asset_id, asset_type, content_hash, mime_type, bytes, storage_key, materialized_at, status, "
            + "create_time, update_time, creator, updater, deleted "
            + "FROM showroom_release_asset WHERE deleted = 0 AND asset_id = #{assetId} "
            + "AND content_hash = #{contentHash} LIMIT 1")
    ShowroomReleaseAssetDO selectManifestAssetByAssetIdAndContentHash(@Param("assetId") String assetId,
                                                                      @Param("contentHash") String contentHash);

    @Select("SELECT id, site_key, stage, asset_id, asset_type, content_hash, mime_type, bytes, storage_key, "
            + "materialized_at, status, create_time, update_time, creator, updater, deleted, tenant_id "
            + "FROM showroom_release_asset WHERE deleted = 0 AND tenant_id = #{tenantId} "
            + "AND site_key = #{siteKey} AND stage = #{stage} AND asset_id = #{assetId} "
            + "AND content_hash = #{contentHash} LIMIT 1")
    ShowroomReleaseAssetDO selectManifestAssetByScopeAssetIdAndContentHash(@Param("tenantId") Long tenantId,
                                                                          @Param("siteKey") String siteKey,
                                                                          @Param("stage") String stage,
                                                                          @Param("assetId") String assetId,
                                                                          @Param("contentHash") String contentHash);

    @Select("SELECT id, asset_id, asset_type, content_hash, mime_type, bytes, storage_key, materialized_at, status, "
            + "binary_content, create_time, update_time, creator, updater, deleted "
            + "FROM showroom_release_asset WHERE asset_id = #{assetId} AND content_hash = #{contentHash} LIMIT 1")
    ShowroomReleaseAssetDO selectAnyByAssetIdAndContentHash(@Param("assetId") String assetId,
                                                            @Param("contentHash") String contentHash);

    @Select("SELECT id, site_key, stage, asset_id, asset_type, content_hash, mime_type, bytes, storage_key, "
            + "materialized_at, status, binary_content, create_time, update_time, creator, updater, deleted, tenant_id "
            + "FROM showroom_release_asset WHERE tenant_id = #{tenantId} AND site_key = #{siteKey} "
            + "AND stage = #{stage} AND asset_id = #{assetId} AND content_hash = #{contentHash} LIMIT 1")
    ShowroomReleaseAssetDO selectAnyByScopeAssetIdAndContentHash(@Param("tenantId") Long tenantId,
                                                                 @Param("siteKey") String siteKey,
                                                                 @Param("stage") String stage,
                                                                 @Param("assetId") String assetId,
                                                                 @Param("contentHash") String contentHash);

    @Update("UPDATE showroom_release_asset SET tenant_id = #{asset.tenantId}, site_key = #{asset.siteKey}, "
            + "stage = #{asset.stage}, asset_type = #{asset.assetType}, mime_type = #{asset.mimeType}, "
            + "bytes = #{asset.bytes}, storage_key = #{asset.storageKey}, materialized_at = #{asset.materializedAt}, "
            + "status = #{asset.status}, binary_content = #{asset.binaryContent}, deleted = 0, update_time = NOW() "
            + "WHERE id = #{existingId}")
    int reviveAssetById(@Param("existingId") Long existingId,
                        @Param("asset") ShowroomReleaseAssetDO asset);
}
