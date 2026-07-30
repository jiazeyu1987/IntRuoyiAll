package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;

import java.time.LocalDateTime;

final class TeamMaintenanceAuditSupport {

    private TeamMaintenanceAuditSupport() {
    }

    static void insertAudit(MesProcessPoolTeamMaintenanceAuditMapper auditMapper, Long leaderUserId,
                            String actionType, String targetType, Long targetId,
                            String beforeSnapshot, String afterSnapshot) {
        auditMapper.insert(MesProcessPoolTeamMaintenanceAuditDO.builder()
                .leaderUserId(leaderUserId)
                .actionType(actionType)
                .targetType(targetType)
                .targetId(targetId)
                .beforeSnapshot(toSnapshotJson(beforeSnapshot))
                .afterSnapshot(toSnapshotJson(afterSnapshot))
                .auditTime(LocalDateTime.now())
                .build());
    }

    private static String toSnapshotJson(String snapshot) {
        if (snapshot == null) {
            return null;
        }
        return JSONUtil.createObj()
                .set("snapshotText", snapshot)
                .toString();
    }
}
