package cn.iocoder.yudao.module.showroom.dal.mysql.release;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseLegacyProjectionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShowroomReleaseLegacyProjectionMapper extends BaseMapperX<ShowroomReleaseLegacyProjectionDO> {

    default ShowroomReleaseLegacyProjectionDO selectByReleaseId(String releaseId) {
        return selectOne(new LambdaQueryWrapperX<ShowroomReleaseLegacyProjectionDO>()
                .eq(ShowroomReleaseLegacyProjectionDO::getReleaseId, releaseId)
                .last("LIMIT 1"));
    }

    default ShowroomReleaseLegacyProjectionDO selectByReleaseScope(Long tenantId, String siteKey, String stage,
                                                                   String releaseId) {
        return selectOne(new LambdaQueryWrapperX<ShowroomReleaseLegacyProjectionDO>()
                .eq(ShowroomReleaseLegacyProjectionDO::getTenantId, tenantId)
                .eq(ShowroomReleaseLegacyProjectionDO::getSiteKey, siteKey)
                .eq(ShowroomReleaseLegacyProjectionDO::getStage, stage)
                .eq(ShowroomReleaseLegacyProjectionDO::getReleaseId, releaseId)
                .last("LIMIT 1"));
    }
}
