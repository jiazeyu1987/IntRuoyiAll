package cn.iocoder.yudao.module.showroom.dal.mysql.release;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseDocumentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShowroomReleaseDocumentMapper extends BaseMapperX<ShowroomReleaseDocumentDO> {

    default ShowroomReleaseDocumentDO selectByReleaseIdAndDocumentId(String releaseId, String documentId) {
        return selectOne(new LambdaQueryWrapperX<ShowroomReleaseDocumentDO>()
                .eq(ShowroomReleaseDocumentDO::getReleaseId, releaseId)
                .eq(ShowroomReleaseDocumentDO::getDocumentId, documentId)
                .last("LIMIT 1"));
    }

    default ShowroomReleaseDocumentDO selectByReleaseScopeAndDocumentId(Long tenantId, String siteKey, String stage,
                                                                        String releaseId, String documentId) {
        return selectOne(new LambdaQueryWrapperX<ShowroomReleaseDocumentDO>()
                .eq(ShowroomReleaseDocumentDO::getTenantId, tenantId)
                .eq(ShowroomReleaseDocumentDO::getSiteKey, siteKey)
                .eq(ShowroomReleaseDocumentDO::getStage, stage)
                .eq(ShowroomReleaseDocumentDO::getReleaseId, releaseId)
                .eq(ShowroomReleaseDocumentDO::getDocumentId, documentId)
                .last("LIMIT 1"));
    }

    default List<ShowroomReleaseDocumentDO> selectListByReleaseId(String releaseId) {
        return selectList(new LambdaQueryWrapperX<ShowroomReleaseDocumentDO>()
                .eq(ShowroomReleaseDocumentDO::getReleaseId, releaseId)
                .orderByAsc(ShowroomReleaseDocumentDO::getDocumentId));
    }

    default List<ShowroomReleaseDocumentDO> selectListByReleaseScope(Long tenantId, String siteKey, String stage,
                                                                     String releaseId) {
        return selectList(new LambdaQueryWrapperX<ShowroomReleaseDocumentDO>()
                .eq(ShowroomReleaseDocumentDO::getTenantId, tenantId)
                .eq(ShowroomReleaseDocumentDO::getSiteKey, siteKey)
                .eq(ShowroomReleaseDocumentDO::getStage, stage)
                .eq(ShowroomReleaseDocumentDO::getReleaseId, releaseId)
                .orderByAsc(ShowroomReleaseDocumentDO::getDocumentId));
    }
}
