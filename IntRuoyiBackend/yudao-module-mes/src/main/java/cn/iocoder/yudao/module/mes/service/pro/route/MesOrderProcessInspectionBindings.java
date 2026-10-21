package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** PROCESS_INSPECTION is an order-wide record, even when the same form is attached to several route nodes. */
public final class MesOrderProcessInspectionBindings {
    private MesOrderProcessInspectionBindings() { }

    public static List<MesProRouteFlowProcessBatchRecordDO> collapse(
            List<MesProRouteFlowProcessBatchRecordDO> bindings) {
        Map<Target, MesProRouteFlowProcessBatchRecordDO> inspection = new LinkedHashMap<>();
        List<MesProRouteFlowProcessBatchRecordDO> ordered = bindings.stream()
                .sorted(Comparator.comparing(MesProRouteFlowProcessBatchRecordDO::getId,
                        Comparator.nullsLast(Long::compareTo))).toList();
        for (var binding : ordered) {
            if ("PROCESS_INSPECTION".equals(binding.getFormSlotType())) {
                if (binding.getId() == null) throw new IllegalStateException("Process inspection binding has no identity");
                inspection.putIfAbsent(target(binding), binding);
            }
        }
        return bindings.stream().filter(binding -> !"PROCESS_INSPECTION".equals(binding.getFormSlotType())
                || inspection.get(target(binding)).getId().equals(binding.getId())).toList();
    }

    private static Target target(MesProRouteFlowProcessBatchRecordDO binding) {
        return new Target(binding.getRouteId(), binding.getBatchRecordReportId(),
                binding.getBatchRecordDefinitionId(), binding.getBatchRecordVersionId(),
                binding.getFormTemplateId(), binding.getLastPublishedTemplateVersionId(),
                binding.getRecordCategory(), binding.getValidationProfile(), binding.getOwnerRoleKey());
    }

    private record Target(Long routeId, String reportId, Long definitionId, Long reportVersionId,
                          Long templateId, Long templateVersionId, String category, String profile, String owner) { }
}
