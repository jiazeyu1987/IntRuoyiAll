package cn.iocoder.yudao.module.mes.service.pro.schedule.component;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 排产工序快照的直接前置集合读写规则。
 */
public final class ScheduleTopologyPredecessors {

    private ScheduleTopologyPredecessors() {
    }

    /**
     * 读取完整前置集合。历史快照没有 JSON 字段时，只能按正式旧字段投影单前置关系。
     */
    public static Set<Long> resolve(MesProScheduleOrderProcessDO process) {
        if (process == null) {
            return Collections.emptySet();
        }
        if (StrUtil.isBlank(process.getPredecessorRouteProcessIdsJson())) {
            return process.getPredecessorRouteProcessId() == null
                    ? Collections.emptySet()
                    : Set.of(process.getPredecessorRouteProcessId());
        }
        List<Long> predecessorIds;
        try {
            predecessorIds = JsonUtils.parseArray(
                    process.getPredecessorRouteProcessIdsJson(), Long.class);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("排产工序前置关系快照格式无效，routeProcessId="
                    + process.getRouteProcessId(), ex);
        }
        if (predecessorIds == null) {
            throw new IllegalStateException("排产工序前置关系快照格式无效，routeProcessId="
                    + process.getRouteProcessId());
        }
        Set<Long> result = new LinkedHashSet<>();
        for (Long predecessorId : predecessorIds) {
            if (predecessorId == null || !result.add(predecessorId)) {
                throw new IllegalStateException("排产工序前置关系快照包含空值或重复边，routeProcessId="
                        + process.getRouteProcessId());
            }
        }
        return result;
    }

    public static String serialize(Set<Long> predecessorIds) {
        if (predecessorIds == null || predecessorIds.isEmpty()) {
            return "[]";
        }
        return JsonUtils.toJsonString(new TreeSet<>(predecessorIds));
    }

    public static Long legacyScalar(Set<Long> predecessorIds) {
        return predecessorIds == null || predecessorIds.size() != 1
                ? null : predecessorIds.iterator().next();
    }
}
