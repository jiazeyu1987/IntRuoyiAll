package cn.iocoder.yudao.module.erp.service.sync;

import java.math.BigDecimal;
import java.util.*;

/** Reconcile source entry identities without changing IDs referenced by downstream documents. */
public final class ErpKingdeeOrderLineIdentity {
    private ErpKingdeeOrderLineIdentity() { }

    public record Line(Long id, String sourceId, Long productId, Long unitId, BigDecimal price,
                       BigDecimal count, BigDecimal taxPercent, String remark) { }

    public static Map<String, Long> reconcile(List<Line> stored, List<Line> incoming, List<Line> previous) {
        uniqueSourceIds(incoming);
        Map<String, Long> identities = new HashMap<>();
        Set<Long> used = new HashSet<>();
        for (Line old : stored) {
            if (old.sourceId() != null && !old.sourceId().isBlank()) {
                if (identities.put(old.sourceId(), old.id()) != null) {
                    throw new IllegalStateException("本地ERP明细标识重复：" + old.sourceId());
                }
                used.add(old.id());
            }
        }
        // One-time identity adoption uses the previously persisted source snapshot, never row order or current values.
        if (stored.stream().anyMatch(line -> !used.contains(line.id()))) {
            uniqueSourceIds(previous);
            for (Line old : stored) {
                if (used.contains(old.id())) continue;
                List<Line> matches = previous.stream().filter(line -> sameSnapshot(old, line)).toList();
                if (matches.size() != 1 || identities.containsKey(matches.get(0).sourceId())) {
                    throw new IllegalStateException("历史明细无法唯一绑定源EntryID，本地明细ID=" + old.id() + "，需核对原始快照");
                }
                identities.put(matches.get(0).sourceId(), old.id());
                used.add(old.id());
            }
        }
        return identities;
    }

    private static void uniqueSourceIds(List<Line> lines) {
        Set<String> seen = new HashSet<>();
        for (Line line : lines) {
            if (line.sourceId() == null || line.sourceId().isBlank() || !seen.add(line.sourceId())) {
                throw new IllegalStateException("源ERP明细EntryID缺失或重复：" + line.sourceId());
            }
        }
    }

    private static boolean sameSnapshot(Line a, Line b) {
        return Objects.equals(a.productId(), b.productId()) && Objects.equals(a.unitId(), b.unitId())
                && decimalEquals(a.price(), b.price()) && decimalEquals(a.count(), b.count())
                && decimalEquals(a.taxPercent(), b.taxPercent()) && Objects.equals(a.remark(), b.remark());
    }

    private static boolean decimalEquals(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }

    public static BigDecimal quantity(Map<Long, BigDecimal> actual, Long id, BigDecimal stored) {
        BigDecimal value = actual.getOrDefault(id, BigDecimal.ZERO);
        if (value == null || value.signum() < 0) throw new IllegalStateException("关联单据数量无效，明细ID=" + id);
        if (stored != null && stored.compareTo(value) != 0) {
            throw new IllegalStateException("订单汇总与关联单据数量不一致，明细ID=" + id + "，请核对实际出入库记录");
        }
        return value;
    }
}
