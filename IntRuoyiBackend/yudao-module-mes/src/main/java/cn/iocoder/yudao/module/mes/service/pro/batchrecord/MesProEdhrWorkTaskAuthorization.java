package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_CANDIDATE_POOL_EMPTY;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID;

final class MesProEdhrWorkTaskAuthorization {

    private MesProEdhrWorkTaskAuthorization() {
    }

    static List<Long> parseCandidateSnapshotUserIds(String candidateUserSnapshot) {
        if (StrUtil.isBlank(candidateUserSnapshot)) {
            return List.of();
        }
        Set<Long> userIds = new LinkedHashSet<>();
        for (String item : candidateUserSnapshot.split(",")) {
            String token = StrUtil.trim(item);
            if (StrUtil.isBlank(token)) {
                continue;
            }
            userIds.add(parseUserIdToken(token));
        }
        return List.copyOf(userIds);
    }

    static Set<Long> parseRequiredCandidateSnapshotUserIds(String candidateUserSnapshot) {
        Set<Long> userIds = new LinkedHashSet<>(parseCandidateSnapshotUserIds(candidateUserSnapshot));
        if (userIds.isEmpty()) {
            throw exception(PRO_EDHR_WORK_TASK_CANDIDATE_POOL_EMPTY);
        }
        return userIds;
    }

    static Long resolveFirstRequiredCandidateUserId(String candidateUserSnapshot) {
        List<Long> userIds = parseCandidateSnapshotUserIds(candidateUserSnapshot);
        if (userIds.isEmpty()) {
            throw exception(PRO_EDHR_WORK_TASK_CANDIDATE_POOL_EMPTY);
        }
        return userIds.get(0);
    }

    static boolean containsCandidate(String candidateUserSnapshot, Long userId) {
        if (userId == null) {
            return false;
        }
        return parseCandidateSnapshotUserIds(candidateUserSnapshot).contains(userId);
    }

    static boolean isAssignedOrCandidate(MesProEdhrWorkTaskDO workTask, Long userId) {
        return workTask != null
                && userId != null
                && (Objects.equals(workTask.getAssigneeUserId(), userId)
                || containsCandidate(workTask.getCandidateUserSnapshot(), userId));
    }

    private static Long parseUserIdToken(String token) {
        try {
            return Long.valueOf(token);
        } catch (NumberFormatException ex) {
            throw exception(PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID);
        }
    }
}
