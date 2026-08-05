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
        insertAudit(auditMapper, leaderUserId, leaderUserId, actionType, targetType, targetId,
                "SUCCESS", actionType, beforeSnapshot, afterSnapshot);
    }

    static void insertAudit(MesProcessPoolTeamMaintenanceAuditMapper auditMapper, Long leaderUserId,
                            Long operatorUserId, String actionType, String targetType, Long targetId,
                            String resultStatus, String changeSummary,
                            String beforeSnapshot, String afterSnapshot) {
        auditMapper.insert(MesProcessPoolTeamMaintenanceAuditDO.builder()
                .leaderUserId(leaderUserId)
                .operatorUserId(operatorUserId)
                .actionType(actionType)
                .targetType(targetType)
                .targetId(targetId)
                .resultStatus(resultStatus)
                .changeSummary(changeSummary)
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
