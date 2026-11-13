package cn.iocoder.yudao.module.mes.service.pro.inprocess;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterValueUtils;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
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

    static final int LIVE_WINDOW_SIZE = 100;
    private static final Map<String, ListFilterValueUtils.Field<MesInProcessOrderRespVO>> LIVE_FIELDS = Map.of(
            "productionProgressPercent", ListFilterValueUtils.number(MesInProcessOrderRespVO::getProductionProgressPercent),
            "inspectionProgressPercent", ListFilterValueUtils.number(MesInProcessOrderRespVO::getInspectionProgressPercent),
            "routeName", ListFilterValueUtils.text(MesInProcessOrderRespVO::getRouteName),
            "status", ListFilterValueUtils.text(MesInProcessOrderService::statusLabel),
            "readBlocked", ListFilterValueUtils.bool(MesInProcessOrderRespVO::getReadBlocked));

    @Transactional(readOnly = true)
    public PageResult<MesInProcessOrderRespVO> page(MesInProcessOrderPageReqVO request) {
        if (request == null || request.getPageNo() == null || request.getPageNo() < 1
                || request.getPageSize() == null || request.getPageSize() < 1 || request.getPageSize() > 200) {
            throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException("在制工单仅支持1至200条分页查询");
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        var split = ListFilterValueUtils.split(request.getListFilters(), MesInProcessOrderMapper.STATIC_FIELDS.keySet(), LIVE_FIELDS);
        var staticRequest = new cn.iocoder.yudao.framework.common.pojo.PageParam();
        staticRequest.setListFilters(split.staticFilters());
        var filters = ListFilterQueryUtils.wrapper(staticRequest, MesInProcessOrderMapper.STATIC_FIELDS);
        long offset = ((long) request.getPageNo() - 1) * request.getPageSize();
        boolean equipmentFilter = StrUtil.isNotBlank(request.getEquipment());
        if (!split.hasDynamicFilters() && !equipmentFilter) {
            long total = mapper.countCandidates(tenantId, request, filters);
            if (total <= offset) return new PageResult<>(List.of(), total);
            var candidates = mapper.selectCandidates(tenantId, request, filters, null, null, offset, request.getPageSize());
            requireTenant(candidates, tenantId);
            return new PageResult<>(project(candidates), total);
        }
        // Live facts remain authoritative. SQL only supplies a fixed keyset window; retain a count and this page.
        Set<Long> matchingDeviceIds = equipmentFilter ? matchingDeviceIds(request.getEquipment().trim(), tenantId) : Set.of();
        List<MesInProcessOrderRespVO> page = new ArrayList<>(request.getPageSize());
        long exactTotal = 0;
        java.time.LocalDateTime cursorJoinedAt = null;
        Long cursorId = null;
        while (true) {
            var candidates = mapper.selectCandidates(tenantId, request, filters, cursorJoinedAt, cursorId, 0L, LIVE_WINDOW_SIZE);
            requireTenant(candidates, tenantId);
            if (candidates.isEmpty()) break;
            var last = candidates.get(candidates.size() - 1);
            if (Objects.equals(cursorId, last.getId())) throw new IllegalStateException("在制工单分页游标未前进");
            cursorJoinedAt = last.getJoinedAt();
            cursorId = Objects.requireNonNull(last.getId(), "在制工单身份缺失");
            var matched = equipmentFilter ? matchEquipment(candidates, request.getEquipment().trim(), tenantId, matchingDeviceIds) : null;
            var eligible = matched == null ? candidates : candidates.stream().filter(row -> matched.contains(row.getId())).toList();
            if (split.hasDynamicFilters()) {
                for (var row : project(eligible)) {
                    if (!split.matches(row)) continue;
                    if (exactTotal >= offset && page.size() < request.getPageSize()) page.add(row);
                    exactTotal++;
                }
            } else {
                List<MesProcessPoolActiveOrderDO> selected = new ArrayList<>();
                for (var order : eligible) {
                    if (exactTotal >= offset && page.size() + selected.size() < request.getPageSize()) selected.add(order);
                    exactTotal++;
                }
                page.addAll(project(selected));
            }
            if (candidates.size() < LIVE_WINDOW_SIZE) break;
        }
        return new PageResult<>(page, exactTotal);
    }

    private static void requireTenant(List<MesProcessPoolActiveOrderDO> orders, Long tenantId) {
        for (var order : orders) {
            if (!Objects.equals(tenantId, order.getTenantId())) throw new IllegalStateException("在制工单租户归属不一致");
        }
    }

    private List<MesInProcessOrderRespVO> project(List<MesProcessPoolActiveOrderDO> page) {
        if (page.isEmpty()) return List.of();
        var leaders = users.getUserMap(page.stream().map(MesProcessPoolActiveOrderDO::getLeaderUserId).distinct().toList());
        var projected = activeOrders.projectActiveOrders(page);
        if (projected.size() != page.size()) throw new IllegalStateException("在制工单实时投影数量不一致");
        return projected.stream().map(row -> {
            var result = BeanUtils.toBean(row, MesInProcessOrderRespVO.class);
            var leader = leaders.get(row.getLeaderUserId());
            if (leader == null) throw new IllegalStateException("在制工单生产组长不存在：" + row.getLeaderUserId());
            result.setLeaderName(leader.getNickname());
            return result;
        }).toList();
    }

    static String statusLabel(MesInProcessOrderRespVO row) {
        if (Boolean.TRUE.equals(row.getReadBlocked())) return "数据异常";
        String release = row.getReleaseApplicationStatus();
        if ("NONCONFORMANCE_VOID".equals(release)) return "QA已作废";
        if ("NONCONFORMANCE_REWORK".equals(release)) return "已结束并转入返工";
        if ("VERSION_UPGRADE_PENDING".equals(row.getBusinessStatus())) return "升版审批中";
        if (Boolean.TRUE.equals(row.getAbnormal())) return "异常处理中";
        if (StrUtil.isNotEmpty(release)) return switch (release) {
            case "PQC_RELEASE_PENDING" -> "待PQC放行";
            case "PQC_RELEASE_REJECTED" -> "PQC放行已驳回";
            case "REPORT_UPLOAD_PENDING" -> "待补放行资料";
            case "MANAGER_RELEASE_PENDING" -> "待上市放行";
            case "MARKET_RELEASE_REJECTED" -> "上市放行已驳回";
            case "MARKET_RELEASE_WITHDRAWN" -> "上市放行已撤回";
            case "RELEASED" -> "已上市放行";
            default -> "未知放行状态：" + release;
        };
        return "COMPLETED".equals(row.getBusinessStatus()) ? "已完工待放行" : "在制";
    }

    private Set<Long> matchEquipment(List<MesProcessPoolActiveOrderDO> orders, String keyword, Long tenantId, Set<Long> matchingDeviceIds) {
        if (orders.isEmpty()) return Set.of();
        var byId = orders.stream().collect(Collectors.toMap(MesProcessPoolActiveOrderDO::getId, order -> order));
        var allocationRows = allocations.selectListByActiveOrderIds(byId.keySet());
        var eventIds = allocationRows.stream().map(MesProcessPoolReportAllocationDO::getEventId).distinct().toList();
        Map<Long, MesProProcessPoolEventDO> eventMap = eventIds.isEmpty() ? Map.of() : events.selectBatchIds(eventIds)
                .stream().collect(Collectors.toMap(MesProProcessPoolEventDO::getId, event -> event));
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

    private Set<Long> matchingDeviceIds(String keyword, Long tenantId) {
        return devices.selectList(new LambdaQueryWrapperX<MesProcessPoolTeamDeviceDO>()
                .eq(MesProcessPoolTeamDeviceDO::getTenantId, tenantId)
                .and(query -> query.apply("LOCATE(LOWER({0}),LOWER(device_code))>0", keyword)
                        .or().apply("LOCATE(LOWER({0}),LOWER(device_name))>0", keyword))).stream()
                .filter(device -> contains(device.getDeviceCode(), keyword) || contains(device.getDeviceName(), keyword))
                .map(MesProcessPoolTeamDeviceDO::getId).collect(Collectors.toSet());
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
