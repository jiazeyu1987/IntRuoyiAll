package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Canonical hash for PQC submissions after manual defect descriptions were removed. */
public final class CanonicalPqcSubmissionV2 {

    private CanonicalPqcSubmissionV2() {
    }

    public static String hash(MesPqcInspectionTaskDO task,
                              MesFrontlinePqcSubmitCommand command,
                              List<MesPqcInspectionPieceDetailDO> details) {
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("activeOrderId", task.getActiveOrderId());
        canonical.put("regulationVersionId", task.getRegulationVersionId());
        canonical.put("qaProcessId", task.getQaProcessId());
        canonical.put("qaItemCode", task.getQaItemCode());
        canonical.put("pqcTaskId", task.getId());
        canonical.put("inspectionRuleKey", task.getInspectionRuleKey());
        canonical.put("actualEmployeeId", command.getActualEmployeeId());
        canonical.put("productionSubmitEventId", command.getProductionSubmitEventId());
        canonical.put("actualInspectionQuantity", command.getActualInspectionQuantity());
        canonical.put("scrapQuantity", command.getScrapQuantity());
        canonical.put("itemResults", canonicalItems(details));
        return DigestUtil.sha256Hex(JsonUtils.toJsonString(canonical).getBytes(StandardCharsets.UTF_8));
    }

    private static List<Map<String, Object>> canonicalItems(List<MesPqcInspectionPieceDetailDO> details) {
        Map<String, List<MesPqcInspectionPieceDetailDO>> byItem = new LinkedHashMap<>();
        details.stream()
                .sorted(Comparator.comparing(MesPqcInspectionPieceDetailDO::getItemCode)
                        .thenComparing(MesPqcInspectionPieceDetailDO::getSampleNo))
                .forEach(detail -> byItem.computeIfAbsent(detail.getItemCode(), ignored -> new ArrayList<>())
                        .add(detail));
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map.Entry<String, List<MesPqcInspectionPieceDetailDO>> entry : byItem.entrySet()) {
            MesPqcInspectionPieceDetailDO first = entry.getValue().get(0);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("itemCode", entry.getKey());
            item.put("selectedEquipmentId", first.getSelectedEquipmentId());
            item.put("selectedEquipmentNumber", first.getSelectedEquipmentNumber());
            item.put("sampleValues", entry.getValue().stream()
                    .map(MesPqcInspectionPieceDetailDO::getMeasuredValue).toList());
            items.add(item);
        }
        return items;
    }
}
