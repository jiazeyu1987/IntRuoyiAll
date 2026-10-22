package cn.iocoder.yudao.module.infra.dal.mysql.internuser;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.infra.dal.dataobject.internuser.InternUserTimeMaintenanceAuditDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface InternUserTimeMaintenanceAuditMapper extends BaseMapperX<InternUserTimeMaintenanceAuditDO> {

    default List<InternUserTimeMaintenanceAuditDO> selectListByTarget(String targetType, Long targetId) {
        return selectList(new LambdaQueryWrapperX<InternUserTimeMaintenanceAuditDO>()
                .eq(InternUserTimeMaintenanceAuditDO::getTargetType, targetType)
                .eq(InternUserTimeMaintenanceAuditDO::getTargetId, targetId)
                .orderByDesc(InternUserTimeMaintenanceAuditDO::getCreateTime)
                .orderByDesc(InternUserTimeMaintenanceAuditDO::getId));
    }

}
