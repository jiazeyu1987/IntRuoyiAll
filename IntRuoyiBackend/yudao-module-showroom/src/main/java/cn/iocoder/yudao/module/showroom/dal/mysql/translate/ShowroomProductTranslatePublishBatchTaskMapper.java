package cn.iocoder.yudao.module.showroom.dal.mysql.translate;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.translate.ShowroomProductTranslatePublishBatchTaskDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShowroomProductTranslatePublishBatchTaskMapper
        extends BaseMapperX<ShowroomProductTranslatePublishBatchTaskDO> {

    default ShowroomProductTranslatePublishBatchTaskDO selectActiveTask() {
        return selectOne(new LambdaQueryWrapperX<ShowroomProductTranslatePublishBatchTaskDO>()
                .eq(ShowroomProductTranslatePublishBatchTaskDO::getTenantId,
                        TenantContextHolder.getRequiredTenantId())
                .in(ShowroomProductTranslatePublishBatchTaskDO::getStatus, "WAITING", "RUNNING")
                .orderByDesc(ShowroomProductTranslatePublishBatchTaskDO::getId)
                .last("LIMIT 1"));
    }

    default ShowroomProductTranslatePublishBatchTaskDO selectLatestTask() {
        return selectOne(new LambdaQueryWrapperX<ShowroomProductTranslatePublishBatchTaskDO>()
                .eq(ShowroomProductTranslatePublishBatchTaskDO::getTenantId,
                        TenantContextHolder.getRequiredTenantId())
                .orderByDesc(ShowroomProductTranslatePublishBatchTaskDO::getId)
                .last("LIMIT 1"));
    }
}
