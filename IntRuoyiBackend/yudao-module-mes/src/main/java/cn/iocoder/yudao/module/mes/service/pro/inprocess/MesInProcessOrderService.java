package cn.iocoder.yudao.module.mes.service.pro.inprocess;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.inprocess.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.inprocess.MesInProcessOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

/** Current-tenant cross-leader read model. No assignment or lifecycle mutations. */
@Service
@RequiredArgsConstructor
public class MesInProcessOrderService {
    private final MesInProcessOrderMapper mapper;
    private final MesTeamLeaderActiveOrderService activeOrders;
    private final AdminUserApi users;
    private final MesProcessPoolReportAllocationMapper allocations;
    private final MesProProcessPoolEventMapper events;
    private final MesProcessPoolTeamDeviceMapper devices;
    private final MesPqcProcessInspectionAggregateDetailMapper inspections;

    @Transactional(readOnly = true)
    public PageResult<MesInProcessOrderRespVO> page(MesInProcessOrderPageReqVO request) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        var candidates = mapper.selectCandidates(tenantId, request);
        for (var order : candidates) {
            if (!Objects.equals(tenantId, order.getTenantId())) throw new IllegalStateException("在制工单租户归属不一致");
        }
        if (StrUtil.isNotBlank(request.getEquipment()) && !candidates.isEmpty()) {
            Set<Long> matched = matchEquipment(candidates, request.getEquipment().trim(), tenantId);
            candidates = candidates.stream().filter(order -> matched.contains(order.getId())).toList();
        }
        long total = candidates.size();
        int start = (int) Math.min(((long) request.getPageNo() - 1) * request.getPageSize(), total);
        int end = (int) Math.min((long) start + request.getPageSize(), total);
        if (start == end) return new PageResult<>(List.of(), total);
        var page = candidates.subList(start, end);
        var leaders = users.getUserMap(page.stream().map(MesProcessPoolActiveOrderDO::getLeaderUserId).distinct().toList());
        var rows = activeOrders.projectActiveOrders(page).stream().map(row -> {
            var result = BeanUtils.toBean(row, MesInProcessOrderRespVO.class);
            var leader = leaders.get(row.getLeaderUserId());
            if (leader == null) throw new IllegalStateException("在制工单生产组长不存在：" + row.getLeaderUserId());
            result.setLeaderName(leader.getNickname());
            return result;
        }).toList();
        return new PageResult<>(rows, total);
    }

    private Set<Long> matchEquipment(List<MesProcessPoolActiveOrderDO> orders, String keyword, Long tenantId) {
        var byId = orders.stream().collect(Collectors.toMap(MesProcessPoolActiveOrderDO::getId, order -> order));
        var allocationRows = allocations.selectListByActiveOrderIds(byId.keySet());
        var eventIds = allocationRows.stream().map(MesProcessPoolReportAllocationDO::getEventId).distinct().toList();
        Map<Long, MesProProcessPoolEventDO> eventMap = eventIds.isEmpty() ? Map.of() : events.selectBatchIds(eventIds)
                .stream().collect(Collectors.toMap(MesProProcessPoolEventDO::getId, event -> event));
        Set<Long> matchingDeviceIds = devices.selectList(new LambdaQueryWrapperX<MesProcessPoolTeamDeviceDO>()
                .eq(MesProcessPoolTeamDeviceDO::getTenantId, tenantId)).stream()
                .filter(device -> contains(device.getDeviceCode(), keyword) || contains(device.getDeviceName(), keyword))
                .map(MesProcessPoolTeamDeviceDO::getId).collect(Collectors.toSet());
        Set<Long> matched = new HashSet<>();
        for (var allocation : allocationRows) {
            var order = byId.get(allocation.getActiveOrderId());
            if (order == null || !Objects.equals(tenantId, allocation.getTenantId())
                    || !Objects.equals(order.getWorkOrderId(), allocation.getWorkOrderId()))
                throw new IllegalStateException("在制工单生产分配归属不一致");
            var event = eventMap.get(allocation.getEventId());
            if (event == null || !Objects.equals(tenantId, event.getTenantId()) || !"PRODUCTION_SUBMIT".equals(event.getEventType()))
                throw new IllegalStateException("在制工单生产分配对应的正式提交缺失或不一致");
            if (productionMatches(event.getRawPayload(), keyword, matchingDeviceIds)) matched.add(order.getId());
        }
        var details = inspections.selectList(new LambdaQueryWrapperX<MesPqcProcessInspectionAggregateDetailDO>()
                .in(MesPqcProcessInspectionAggregateDetailDO::getActiveOrderId, byId.keySet()));
        for (var detail : details) {
            var order = byId.get(detail.getActiveOrderId());
            if (order == null || !Objects.equals(tenantId, detail.getTenantId())
                    || !Objects.equals(order.getWorkOrderId(), detail.getWorkOrderId()))
                throw new IllegalStateException("在制工单检验记录归属不一致");
            if (detail.getSelectedEquipmentId() != null && (contains(detail.getSelectedEquipmentName(), keyword)
                    || contains(detail.getSelectedEquipmentCode(), keyword) || contains(detail.getSelectedEquipmentNumber(), keyword)))
                matched.add(order.getId());
        }
        return matched;
    }

    static boolean productionMatches(String payload, String keyword, Set<Long> matchingDeviceIds) {
        if (StrUtil.isBlank(payload)) throw new IllegalStateException("生产提交内容缺失，无法反查设备");
        JSONObject raw = JSON.parseObject(payload);
        if (raw == null) throw new IllegalStateException("生产提交内容不是JSON对象");
        boolean matched = selectedMatches(raw.get("selectedDevices"), keyword, matchingDeviceIds);
        Object materials = raw.get("materialDetails");
        if (materials != null) {
            if (!(materials instanceof Collection<?> values)) throw new IllegalStateException("生产物料明细不是数组");
            for (Object value : values) {
                if (!(value instanceof JSONObject material)) throw new IllegalStateException("生产物料明细格式错误");
                matched |= selectedMatches(material.get("selectedDevices"), keyword, matchingDeviceIds);
            }
        }
        return matched;
    }

    private static boolean selectedMatches(Object selected, String keyword, Set<Long> matchingDeviceIds) {
        if (selected == null) return false; // No equipment was selected for this submission.
        if (!(selected instanceof Collection<?> values)) throw new IllegalStateException("已选生产设备不是数组");
        boolean matched = false;
        for (Object value : values) {
            if (!(value instanceof JSONObject device) || device.getLong("deviceId") == null)
                throw new IllegalStateException("已选生产设备身份缺失");
            matched |= matchingDeviceIds.contains(device.getLong("deviceId"))
                    || contains(device.getString("deviceCode"), keyword) || contains(device.getString("deviceName"), keyword);
        }
        return matched;
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }
}
