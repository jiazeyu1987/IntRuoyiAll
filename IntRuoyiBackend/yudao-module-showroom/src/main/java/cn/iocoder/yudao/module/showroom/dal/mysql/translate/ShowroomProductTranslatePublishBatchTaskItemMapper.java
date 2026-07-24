package cn.iocoder.yudao.module.showroom.dal.mysql.translate;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.translate.ShowroomProductTranslatePublishBatchTaskItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShowroomProductTranslatePublishBatchTaskItemMapper
        extends BaseMapperX<ShowroomProductTranslatePublishBatchTaskItemDO> {

    default List<ShowroomProductTranslatePublishBatchTaskItemDO> selectListByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<ShowroomProductTranslatePublishBatchTaskItemDO>()
                .eq(ShowroomProductTranslatePublishBatchTaskItemDO::getTenantId,
                        TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomProductTranslatePublishBatchTaskItemDO::getTaskId, taskId)
                .orderByAsc(ShowroomProductTranslatePublishBatchTaskItemDO::getId));
    }

    default ShowroomProductTranslatePublishBatchTaskItemDO selectByTaskIdAndProductId(Long taskId, Long productId) {
        return selectOne(new LambdaQueryWrapperX<ShowroomProductTranslatePublishBatchTaskItemDO>()
                .eq(ShowroomProductTranslatePublishBatchTaskItemDO::getTenantId,
                        TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomProductTranslatePublishBatchTaskItemDO::getTaskId, taskId)
                .eq(ShowroomProductTranslatePublishBatchTaskItemDO::getProductId, productId));
    }

    default ShowroomProductTranslatePublishBatchTaskItemDO selectRunningByTaskId(Long taskId) {
        return selectOne(new LambdaQueryWrapperX<ShowroomProductTranslatePublishBatchTaskItemDO>()
                .eq(ShowroomProductTranslatePublishBatchTaskItemDO::getTenantId,
                        TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomProductTranslatePublishBatchTaskItemDO::getTaskId, taskId)
                .eq(ShowroomProductTranslatePublishBatchTaskItemDO::getStatus, "RUNNING")
                .orderByAsc(ShowroomProductTranslatePublishBatchTaskItemDO::getId)
                .last("LIMIT 1"));
    }
}
