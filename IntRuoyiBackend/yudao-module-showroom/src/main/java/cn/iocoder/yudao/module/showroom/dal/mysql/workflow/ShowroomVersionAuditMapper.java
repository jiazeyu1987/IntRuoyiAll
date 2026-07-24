package cn.iocoder.yudao.module.showroom.dal.mysql.workflow;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomVersionAuditDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShowroomVersionAuditMapper extends BaseMapperX<ShowroomVersionAuditDO> {

    default List<ShowroomVersionAuditDO> selectListByTarget(String targetType, Long targetId) {
        return selectList(new LambdaQueryWrapperX<ShowroomVersionAuditDO>()
                .eq(ShowroomVersionAuditDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomVersionAuditDO::getTargetType, targetType)
                .eq(ShowroomVersionAuditDO::getTargetId, targetId)
                .orderByAsc(ShowroomVersionAuditDO::getCreatedAt)
                .orderByAsc(ShowroomVersionAuditDO::getId));
    }

}
