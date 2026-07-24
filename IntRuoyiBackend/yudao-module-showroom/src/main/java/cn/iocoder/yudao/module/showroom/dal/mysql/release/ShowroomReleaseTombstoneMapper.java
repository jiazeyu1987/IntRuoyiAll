package cn.iocoder.yudao.module.showroom.dal.mysql.release;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseTombstoneDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ShowroomReleaseTombstoneMapper extends BaseMapperX<ShowroomReleaseTombstoneDO> {

    default ShowroomReleaseTombstoneDO selectByResource(String resourceType, String resourceKey) {
        return selectOne(new LambdaQueryWrapperX<ShowroomReleaseTombstoneDO>()
                .eq(ShowroomReleaseTombstoneDO::getResourceType, resourceType)
                .eq(ShowroomReleaseTombstoneDO::getResourceKey, resourceKey)
                .last("LIMIT 1"));
    }

    default ShowroomReleaseTombstoneDO selectByScopedResource(Long tenantId, String siteKey, String stage,
                                                             String resourceType, String resourceKey) {
        return selectOne(new LambdaQueryWrapperX<ShowroomReleaseTombstoneDO>()
                .eq(ShowroomReleaseTombstoneDO::getTenantId, tenantId)
                .eq(ShowroomReleaseTombstoneDO::getSiteKey, siteKey)
                .eq(ShowroomReleaseTombstoneDO::getStage, stage)
                .eq(ShowroomReleaseTombstoneDO::getResourceType, resourceType)
                .eq(ShowroomReleaseTombstoneDO::getResourceKey, resourceKey)
                .last("LIMIT 1"));
    }

    @Select("SELECT id, site_key, stage, resource_type, resource_key, purged_at, reason, tenant_id, "
            + "create_time, update_time, creator, updater, deleted "
            + "FROM showroom_release_tombstone WHERE tenant_id = #{tenantId} AND site_key = #{siteKey} "
            + "AND stage = #{stage} AND resource_type = #{resourceType} AND resource_key = #{resourceKey} LIMIT 1")
    ShowroomReleaseTombstoneDO selectAnyByScopedResource(@Param("tenantId") Long tenantId,
                                                         @Param("siteKey") String siteKey,
                                                         @Param("stage") String stage,
                                                         @Param("resourceType") String resourceType,
                                                         @Param("resourceKey") String resourceKey);

    @Update("UPDATE showroom_release_tombstone SET purged_at = #{tombstone.purgedAt}, "
            + "reason = #{tombstone.reason}, deleted = 0, update_time = NOW() WHERE id = #{existingId}")
    int reviveTombstoneById(@Param("existingId") Long existingId,
                            @Param("tombstone") ShowroomReleaseTombstoneDO tombstone);

    default int deleteByResource(String resourceType, String resourceKey) {
        return delete(new LambdaQueryWrapperX<ShowroomReleaseTombstoneDO>()
                .eq(ShowroomReleaseTombstoneDO::getResourceType, resourceType)
                .eq(ShowroomReleaseTombstoneDO::getResourceKey, resourceKey));
    }

    default int deleteByScopedResource(Long tenantId, String siteKey, String stage, String resourceType,
                                       String resourceKey) {
        return delete(new LambdaQueryWrapperX<ShowroomReleaseTombstoneDO>()
                .eq(ShowroomReleaseTombstoneDO::getTenantId, tenantId)
                .eq(ShowroomReleaseTombstoneDO::getSiteKey, siteKey)
                .eq(ShowroomReleaseTombstoneDO::getStage, stage)
                .eq(ShowroomReleaseTombstoneDO::getResourceType, resourceType)
                .eq(ShowroomReleaseTombstoneDO::getResourceKey, resourceKey));
    }
}
