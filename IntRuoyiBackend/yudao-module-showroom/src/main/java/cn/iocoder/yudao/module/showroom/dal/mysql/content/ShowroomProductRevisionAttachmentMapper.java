package cn.iocoder.yudao.module.showroom.dal.mysql.content;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductRevisionAttachmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ShowroomProductRevisionAttachmentMapper extends BaseMapperX<ShowroomProductRevisionAttachmentDO> {

    default List<ShowroomProductRevisionAttachmentDO> selectByRevisionId(Long productRevisionId) {
        return selectList(new LambdaQueryWrapperX<ShowroomProductRevisionAttachmentDO>()
                .eq(ShowroomProductRevisionAttachmentDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomProductRevisionAttachmentDO::getProductRevisionId, productRevisionId)
                .orderByAsc(ShowroomProductRevisionAttachmentDO::getDisplayOrder)
                .orderByAsc(ShowroomProductRevisionAttachmentDO::getId));
    }

    default List<ShowroomProductRevisionAttachmentDO> selectByRevisionIds(Collection<Long> productRevisionIds) {
        if (productRevisionIds == null || productRevisionIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<ShowroomProductRevisionAttachmentDO>()
                .eq(ShowroomProductRevisionAttachmentDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .in(ShowroomProductRevisionAttachmentDO::getProductRevisionId, productRevisionIds)
                .orderByAsc(ShowroomProductRevisionAttachmentDO::getProductRevisionId)
                .orderByAsc(ShowroomProductRevisionAttachmentDO::getDisplayOrder)
                .orderByAsc(ShowroomProductRevisionAttachmentDO::getId));
    }
}
