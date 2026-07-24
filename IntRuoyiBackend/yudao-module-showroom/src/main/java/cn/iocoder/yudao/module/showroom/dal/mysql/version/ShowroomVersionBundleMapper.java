package cn.iocoder.yudao.module.showroom.dal.mysql.version;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.version.ShowroomVersionBundleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShowroomVersionBundleMapper extends BaseMapperX<ShowroomVersionBundleDO> {

    default ShowroomVersionBundleDO selectByTargetAndRevision(String targetType, Long targetId, Long revisionId) {
        return selectOne(new LambdaQueryWrapperX<ShowroomVersionBundleDO>()
                .eq(ShowroomVersionBundleDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomVersionBundleDO::getTargetType, targetType)
                .eq(ShowroomVersionBundleDO::getTargetId, targetId)
                .eq(ShowroomVersionBundleDO::getRevisionId, revisionId)
                .last("LIMIT 1"));
    }

    default List<ShowroomVersionBundleDO> selectListByTarget(String targetType, Long targetId) {
        return selectList(new LambdaQueryWrapperX<ShowroomVersionBundleDO>()
                .eq(ShowroomVersionBundleDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomVersionBundleDO::getTargetType, targetType)
                .eq(ShowroomVersionBundleDO::getTargetId, targetId)
                .orderByDesc(ShowroomVersionBundleDO::getRevisionNo)
                .orderByDesc(ShowroomVersionBundleDO::getRevisionId));
    }
}
