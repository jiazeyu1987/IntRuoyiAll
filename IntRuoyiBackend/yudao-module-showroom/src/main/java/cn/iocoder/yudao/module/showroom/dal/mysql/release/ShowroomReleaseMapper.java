package cn.iocoder.yudao.module.showroom.dal.mysql.release;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShowroomReleaseMapper extends BaseMapperX<ShowroomReleaseDO> {

    String STATUS_PUBLISHED = "PUBLISHED";

    default ShowroomReleaseDO selectByReleaseId(String releaseId) {
        return selectOne(new LambdaQueryWrapperX<ShowroomReleaseDO>()
                .eq(ShowroomReleaseDO::getReleaseId, releaseId)
                .last("LIMIT 1"));
    }

    default ShowroomReleaseDO selectByReleaseScope(Long tenantId, String siteKey, String stage, String releaseId) {
        return selectOne(new LambdaQueryWrapperX<ShowroomReleaseDO>()
                .eq(ShowroomReleaseDO::getTenantId, tenantId)
                .eq(ShowroomReleaseDO::getSiteKey, siteKey)
                .eq(ShowroomReleaseDO::getStage, stage)
                .eq(ShowroomReleaseDO::getReleaseId, releaseId)
                .last("LIMIT 1"));
    }

    default List<ShowroomReleaseDO> selectPublishedOrderByPublishedAtDesc() {
        return selectList(new LambdaQueryWrapperX<ShowroomReleaseDO>()
                .eq(ShowroomReleaseDO::getStatus, STATUS_PUBLISHED)
                .orderByDesc(ShowroomReleaseDO::getPublishedAt)
                .orderByDesc(ShowroomReleaseDO::getId));
    }

    default List<ShowroomReleaseDO> selectPublishedByScopeOrderByPublishedAtDesc(Long tenantId, String siteKey,
                                                                                 String stage) {
        return selectList(new LambdaQueryWrapperX<ShowroomReleaseDO>()
                .eq(ShowroomReleaseDO::getTenantId, tenantId)
                .eq(ShowroomReleaseDO::getSiteKey, siteKey)
                .eq(ShowroomReleaseDO::getStage, stage)
                .eq(ShowroomReleaseDO::getStatus, STATUS_PUBLISHED)
                .orderByDesc(ShowroomReleaseDO::getPublishedAt)
                .orderByDesc(ShowroomReleaseDO::getId));
    }
}
