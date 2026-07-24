package cn.iocoder.yudao.module.showroom.dal.mysql.release;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseAssetRefDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShowroomReleaseAssetRefMapper extends BaseMapperX<ShowroomReleaseAssetRefDO> {

    default List<ShowroomReleaseAssetRefDO> selectListByReleaseId(String releaseId) {
        return selectList(new LambdaQueryWrapperX<ShowroomReleaseAssetRefDO>()
                .eq(ShowroomReleaseAssetRefDO::getReleaseId, releaseId)
                .orderByAsc(ShowroomReleaseAssetRefDO::getDocumentId)
                .orderByAsc(ShowroomReleaseAssetRefDO::getAssetId));
    }

    default List<ShowroomReleaseAssetRefDO> selectListByReleaseScope(Long tenantId, String siteKey, String stage,
                                                                     String releaseId) {
        return selectList(new LambdaQueryWrapperX<ShowroomReleaseAssetRefDO>()
                .eq(ShowroomReleaseAssetRefDO::getTenantId, tenantId)
                .eq(ShowroomReleaseAssetRefDO::getSiteKey, siteKey)
                .eq(ShowroomReleaseAssetRefDO::getStage, stage)
                .eq(ShowroomReleaseAssetRefDO::getReleaseId, releaseId)
                .orderByAsc(ShowroomReleaseAssetRefDO::getDocumentId)
                .orderByAsc(ShowroomReleaseAssetRefDO::getAssetId));
    }

    default long countRetainedByAsset(String assetId, String contentHash) {
        return selectCount(new LambdaQueryWrapperX<ShowroomReleaseAssetRefDO>()
                .eq(ShowroomReleaseAssetRefDO::getAssetId, assetId)
                .eq(ShowroomReleaseAssetRefDO::getContentHash, contentHash));
    }

    default long countRetainedByAssetScope(Long tenantId, String siteKey, String stage,
                                           String assetId, String contentHash) {
        return selectCount(new LambdaQueryWrapperX<ShowroomReleaseAssetRefDO>()
                .eq(ShowroomReleaseAssetRefDO::getTenantId, tenantId)
                .eq(ShowroomReleaseAssetRefDO::getSiteKey, siteKey)
                .eq(ShowroomReleaseAssetRefDO::getStage, stage)
                .eq(ShowroomReleaseAssetRefDO::getAssetId, assetId)
                .eq(ShowroomReleaseAssetRefDO::getContentHash, contentHash));
    }
}
