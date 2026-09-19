package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProcessPoolTeamMaintenanceAuditMapper extends BaseMapperX<MesProcessPoolTeamMaintenanceAuditDO> {

    default List<MesProcessPoolTeamMaintenanceAuditDO> selectSuccessfulListByActiveOrderId(Long activeOrderId) {
        if (activeOrderId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolTeamMaintenanceAuditDO>()
                .eq(MesProcessPoolTeamMaintenanceAuditDO::getTargetType, "ACTIVE_ORDER")
                .eq(MesProcessPoolTeamMaintenanceAuditDO::getTargetId, activeOrderId)
                .eq(MesProcessPoolTeamMaintenanceAuditDO::getResultStatus, "SUCCESS")
                .orderByDesc(MesProcessPoolTeamMaintenanceAuditDO::getAuditTime)
                .orderByDesc(MesProcessPoolTeamMaintenanceAuditDO::getId));
    }
}
