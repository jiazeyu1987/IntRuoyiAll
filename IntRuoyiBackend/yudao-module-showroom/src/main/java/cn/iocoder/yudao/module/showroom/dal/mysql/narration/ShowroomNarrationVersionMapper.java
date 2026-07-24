package cn.iocoder.yudao.module.showroom.dal.mysql.narration;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.narration.ShowroomNarrationVersionDO;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationStatus;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShowroomNarrationVersionMapper extends BaseMapperX<ShowroomNarrationVersionDO> {

    @Delete("""
            DELETE FROM showroom_narration_version
            WHERE tenant_id = #{tenantId}
              AND target_type = #{targetType}
              AND target_id = #{targetId}
            """)
    int deleteByTarget(@Param("tenantId") Long tenantId,
                       @Param("targetType") String targetType,
                       @Param("targetId") Long targetId);

    default ShowroomNarrationVersionDO selectLatestByKey(String targetType, Long targetId,
                                                         String audienceType, String language) {
        return selectOne(new LambdaQueryWrapperX<ShowroomNarrationVersionDO>()
                .eq(ShowroomNarrationVersionDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomNarrationVersionDO::getTargetType, targetType)
                .eq(ShowroomNarrationVersionDO::getTargetId, targetId)
                .eq(ShowroomNarrationVersionDO::getAudienceType, audienceType)
                .eq(ShowroomNarrationVersionDO::getLanguage, language)
                .orderByDesc(ShowroomNarrationVersionDO::getVersionNo)
                .orderByDesc(ShowroomNarrationVersionDO::getId)
                .last("LIMIT 1"));
    }

    default ShowroomNarrationVersionDO selectLatestByKeyAndSourceRevision(String targetType, Long targetId,
                                                                          String audienceType, String language,
                                                                          Long sourceRevisionId) {
        return selectOne(new LambdaQueryWrapperX<ShowroomNarrationVersionDO>()
                .eq(ShowroomNarrationVersionDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomNarrationVersionDO::getTargetType, targetType)
                .eq(ShowroomNarrationVersionDO::getTargetId, targetId)
                .eq(ShowroomNarrationVersionDO::getAudienceType, audienceType)
                .eq(ShowroomNarrationVersionDO::getLanguage, language)
                .eq(ShowroomNarrationVersionDO::getSourceRevisionId, sourceRevisionId)
                .orderByDesc(ShowroomNarrationVersionDO::getVersionNo)
                .orderByDesc(ShowroomNarrationVersionDO::getId)
                .last("LIMIT 1"));
    }

    default ShowroomNarrationVersionDO selectLatestPublishedByKey(String targetType, Long targetId,
                                                                  String audienceType, String language) {
        return selectOne(new LambdaQueryWrapperX<ShowroomNarrationVersionDO>()
                .eq(ShowroomNarrationVersionDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomNarrationVersionDO::getTargetType, targetType)
                .eq(ShowroomNarrationVersionDO::getTargetId, targetId)
                .eq(ShowroomNarrationVersionDO::getAudienceType, audienceType)
                .eq(ShowroomNarrationVersionDO::getLanguage, language)
                .eq(ShowroomNarrationVersionDO::getStatus, ShowroomNarrationStatus.PUBLISHED.name())
                .orderByDesc(ShowroomNarrationVersionDO::getVersionNo)
                .orderByDesc(ShowroomNarrationVersionDO::getId)
                .last("LIMIT 1"));
    }

    default List<ShowroomNarrationVersionDO> selectPublishedByTargetAndSourceRevision(String targetType, Long targetId,
                                                                                      String audienceType,
                                                                                      String language,
                                                                                      Long sourceRevisionId) {
        return selectList(new LambdaQueryWrapperX<ShowroomNarrationVersionDO>()
                .eq(ShowroomNarrationVersionDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomNarrationVersionDO::getTargetType, targetType)
                .eq(ShowroomNarrationVersionDO::getTargetId, targetId)
                .eq(ShowroomNarrationVersionDO::getAudienceType, audienceType)
                .eq(ShowroomNarrationVersionDO::getLanguage, language)
                .eq(ShowroomNarrationVersionDO::getSourceRevisionId, sourceRevisionId)
                .eq(ShowroomNarrationVersionDO::getStatus, ShowroomNarrationStatus.PUBLISHED.name())
                .orderByDesc(ShowroomNarrationVersionDO::getVersionNo)
                .orderByDesc(ShowroomNarrationVersionDO::getId));
    }

    default ShowroomNarrationVersionDO selectLatestPublishedByKeyAndSourceRevision(String targetType, Long targetId,
                                                                                   String audienceType, String language,
                                                                                   Long sourceRevisionId) {
        return selectOne(new LambdaQueryWrapperX<ShowroomNarrationVersionDO>()
                .eq(ShowroomNarrationVersionDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomNarrationVersionDO::getTargetType, targetType)
                .eq(ShowroomNarrationVersionDO::getTargetId, targetId)
                .eq(ShowroomNarrationVersionDO::getAudienceType, audienceType)
                .eq(ShowroomNarrationVersionDO::getLanguage, language)
                .eq(ShowroomNarrationVersionDO::getSourceRevisionId, sourceRevisionId)
                .eq(ShowroomNarrationVersionDO::getStatus, ShowroomNarrationStatus.PUBLISHED.name())
                .orderByDesc(ShowroomNarrationVersionDO::getVersionNo)
                .orderByDesc(ShowroomNarrationVersionDO::getId)
                .last("LIMIT 1"));
    }

}
