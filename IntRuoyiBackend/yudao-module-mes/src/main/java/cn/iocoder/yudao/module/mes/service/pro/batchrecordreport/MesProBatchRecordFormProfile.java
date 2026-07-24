package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

public interface MesProBatchRecordFormProfile {

    String formSlotType();

    default int priority() {
        return 100;
    }

    default boolean supportsSourceTable(MesProBatchRecordParsedTable table) {
        return false;
    }

    default boolean supportsSourceTables(List<MesProBatchRecordParsedTable> sourceTables) {
        if (sourceTables == null || sourceTables.isEmpty()) {
            return false;
        }
        for (MesProBatchRecordParsedTable sourceTable : sourceTables) {
            if (!supportsSourceTable(sourceTable)) {
                return false;
            }
        }
        return true;
    }

    default MesProBatchRecordParsedTable normalizeSourceTable(int templateIndex,
                                                             MesProBatchRecordParsedTable sourceTable) {
        return sourceTable;
    }

    default List<MesProBatchRecordParsedTable> normalizeSourceTables(List<MesProBatchRecordParsedTable> sourceTables) {
        if (sourceTables == null || sourceTables.isEmpty()) {
            return List.of();
        }
        List<MesProBatchRecordParsedTable> normalizedTables = new ArrayList<>();
        for (int index = 0; index < sourceTables.size(); index++) {
            MesProBatchRecordParsedTable sourceTable = sourceTables.get(index);
            int templateIndex = sourceTable.getSourceTableIndex() == null ? index + 1 : sourceTable.getSourceTableIndex();
            normalizedTables.add(normalizeSourceTable(templateIndex, sourceTable));
        }
        return normalizedTables;
    }

    default boolean supportsLegacyLayout(MesProBatchRecordReportDO metadata, JSONObject root) {
        return false;
    }

    default MesProBatchRecordParsedTable normalizeLegacyLayout(MesProBatchRecordReportDO metadata, JSONObject root) {
        throw new UnsupportedOperationException("legacy_layout_not_supported_" + formSlotType());
    }
}
