package cn.iocoder.yudao.module.showroom.dal.mysql.workflow;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomFieldAssignmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShowroomFieldAssignmentMapper extends BaseMapperX<ShowroomFieldAssignmentDO> {

    default List<ShowroomFieldAssignmentDO> selectListByTarget(String targetType, Long targetId) {
        Long tenantId = TenantContextHolder.getTenantId();
        return selectList(new LambdaQueryWrapperX<ShowroomFieldAssignmentDO>()
                .eqIfPresent(ShowroomFieldAssignmentDO::getTenantId, tenantId)
                .eq(ShowroomFieldAssignmentDO::getTargetType, targetType)
                .eq(ShowroomFieldAssignmentDO::getTargetId, targetId)
                .orderByDesc(ShowroomFieldAssignmentDO::getCreatedAt)
                .orderByDesc(ShowroomFieldAssignmentDO::getId));
    }

    default ShowroomFieldAssignmentDO selectLatestByTargetAndField(String targetType, Long targetId,
                                                                   String fieldCode) {
        Long tenantId = TenantContextHolder.getTenantId();
        return selectOne(new LambdaQueryWrapperX<ShowroomFieldAssignmentDO>()
                .eqIfPresent(ShowroomFieldAssignmentDO::getTenantId, tenantId)
                .eq(ShowroomFieldAssignmentDO::getTargetType, targetType)
                .eq(ShowroomFieldAssignmentDO::getTargetId, targetId)
                .eq(ShowroomFieldAssignmentDO::getFieldCode, fieldCode)
                .orderByDesc(ShowroomFieldAssignmentDO::getCreatedAt)
                .orderByDesc(ShowroomFieldAssignmentDO::getId)
                .last("LIMIT 1"));
    }

    default ShowroomFieldAssignmentDO selectLatestOpenByTargetAndField(String targetType, Long targetId,
                                                                       String fieldCode) {
        Long tenantId = TenantContextHolder.getTenantId();
        return selectOne(new LambdaQueryWrapperX<ShowroomFieldAssignmentDO>()
                .eqIfPresent(ShowroomFieldAssignmentDO::getTenantId, tenantId)
                .eq(ShowroomFieldAssignmentDO::getTargetType, targetType)
                .eq(ShowroomFieldAssignmentDO::getTargetId, targetId)
                .eq(ShowroomFieldAssignmentDO::getFieldCode, fieldCode)
                .eq(ShowroomFieldAssignmentDO::getStatus, "OPEN")
                .orderByDesc(ShowroomFieldAssignmentDO::getCreatedAt)
                .orderByDesc(ShowroomFieldAssignmentDO::getId)
                .last("LIMIT 1"));
    }

    default ShowroomFieldAssignmentDO selectLatestByLastChangeRequestId(String targetType, String fieldCode,
                                                                        Long lastChangeRequestId) {
        Long tenantId = TenantContextHolder.getTenantId();
        return selectOne(new LambdaQueryWrapperX<ShowroomFieldAssignmentDO>()
                .eqIfPresent(ShowroomFieldAssignmentDO::getTenantId, tenantId)
                .eq(ShowroomFieldAssignmentDO::getTargetType, targetType)
                .eq(ShowroomFieldAssignmentDO::getFieldCode, fieldCode)
                .eq(ShowroomFieldAssignmentDO::getLastChangeRequestId, lastChangeRequestId)
                .orderByDesc(ShowroomFieldAssignmentDO::getCreatedAt)
                .orderByDesc(ShowroomFieldAssignmentDO::getId)
                .last("LIMIT 1"));
    }

}
