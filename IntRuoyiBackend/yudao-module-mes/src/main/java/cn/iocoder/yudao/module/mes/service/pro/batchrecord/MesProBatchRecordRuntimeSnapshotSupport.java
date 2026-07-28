package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMigrationItemMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordCellRuleSupport;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_CELL_RULE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_CELL_RULE_UNREVIEWED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_SNAPSHOT_SOURCE_UNAVAILABLE;

@Service
public class MesProBatchRecordRuntimeSnapshotSupport {

    private static final String SNAPSHOT_VERSION = "EDHR_EXECUTION_V1";
    private static final String BATCH_RECORD_VERSION_STATUS_APPROVED = "APPROVED";

    @Resource
    private MesProBatchRecordVersionMapper versionMapper;
    @Resource
    private MesProBatchRecordVersionMigrationItemMapper versionMigrationItemMapper;

    public RuntimeSnapshot buildRuntimeSnapshot(MesProBatchRecordReportDO report, String reportJson) {
        if (report == null || StrUtil.isBlank(reportJson)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SNAPSHOT_SOURCE_UNAVAILABLE);
        }
        JSONObject root = JSON.parseObject(StrUtil.trim(reportJson));
        materializeApprovedVersionCellRuleSnapshot(report, root);
        validateConfirmedCellRules(root);
        JSONObject layout = buildSnapshotLayout(root);
        JSONObject meta = buildSnapshotMeta(root, report);
        JSONObject snapshot = new JSONObject(true);
        snapshot.put("snapshotVersion", SNAPSHOT_VERSION);
        snapshot.put("source", buildSnapshotSource(report));
        snapshot.put("layout", layout);
        snapshot.put("meta", meta);
        snapshot.put("fields", extractSnapshotFields(root));
        snapshot.put("assistRows", extractSnapshotAssistRows(root));
        return new RuntimeSnapshot(layout.toJSONString(), meta.toJSONString(), snapshot.toJSONString());
    }

    private void materializeApprovedVersionCellRuleSnapshot(MesProBatchRecordReportDO report, JSONObject root) {
        if (report == null || report.getBatchRecordVersionId() == null) {
            return;
        }
        MesProBatchRecordVersionDO version = versionMapper.selectById(report.getBatchRecordVersionId());
        if (version == null || !BATCH_RECORD_VERSION_STATUS_APPROVED.equals(version.getStatus())) {
            return;
        }
        if (versionMigrationItemMapper.countBlockingItems(version.getId()) > 0
                || !versionMigrationItemMapper.existsCellRuleReconciledEvidence(version.getId())) {
            return;
        }
        MesProBatchRecordCellRuleSupport.materializeVersionApprovedCellRules(root, report.getReportCode());
    }

    private JSONObject buildSnapshotSource(MesProBatchRecordReportDO report) {
        JSONObject source = new JSONObject(true);
        source.put("type", "JMREPORT");
        source.put("reportId", report.getReportId());
        source.put("reportCode", report.getReportCode());
        source.put("reportName", report.getReportName());
        return source;
    }

    private JSONObject buildSnapshotLayout(JSONObject root) {
        JSONObject layout = new JSONObject(true);
        layout.put("rows", root.getJSONObject("rows"));
        layout.put("cols", root.getJSONObject("cols"));
        layout.put("merges", root.getJSONArray("merges"));
        return layout;
    }

    private JSONObject buildSnapshotMeta(JSONObject root, MesProBatchRecordReportDO report) {
        JSONObject meta = new JSONObject(true);
        meta.put("name", root.getString("name"));
        meta.put("tableTitle", report.getTableTitle());
        meta.put("sourceTableIndex", report.getSourceTableIndex());
        meta.put("fillFormInfo", root.getJSONObject("fillFormInfo"));
        meta.put("printConfig", root.getJSONObject("printConfig"));
        meta.put("dataRectWidth", root.get("dataRectWidth"));
        return meta;
    }

    private JSONArray extractSnapshotFields(JSONObject root) {
        JSONArray fields = new JSONArray();
        JSONObject rows = root.getJSONObject("rows");
        if (rows == null || rows.isEmpty()) {
            return fields;
        }
        List<Integer> rowIndexes = rows.keySet().stream()
                .filter(StrUtil::isNumeric)
                .map(Integer::valueOf)
                .sorted()
                .toList();
        for (Integer rowIndex : rowIndexes) {
            JSONObject row = rows.getJSONObject(String.valueOf(rowIndex));
            JSONObject cells = row == null ? null : row.getJSONObject("cells");
            if (cells == null || cells.isEmpty()) {
                continue;
            }
            List<Integer> columnIndexes = cells.keySet().stream()
                    .filter(StrUtil::isNumeric)
                    .map(Integer::valueOf)
                    .sorted()
                    .toList();
            for (Integer columnIndex : columnIndexes) {
                JSONObject cell = cells.getJSONObject(String.valueOf(columnIndex));
                if (cell == null) {
                    continue;
                }
                JSONObject fillForm = cell.getJSONObject("fillForm");
                if (fillForm == null || StrUtil.isBlank(fillForm.getString("field"))) {
                    continue;
                }
                JSONObject cellRule = cell.getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY);
                if (cellRule == null && MesProBatchRecordCellRuleSupport.hasValidSignatureMarker(cell)) {
                    continue;
                }
                JSONObject field = new JSONObject(true);
                field.put("fieldPath", buildSnapshotFieldPath(rowIndex, columnIndex, fillForm.getString("field")));
                field.put("fieldKey", fillForm.getString("field"));
                field.put("label", resolveFieldLabel(rows, rowIndex, columnIndex, cell, fillForm, cellRule));
                field.put("rowIndex", rowIndex);
                field.put("columnIndex", columnIndex);
                String valueType = cellRule.getString("valueType");
                field.put("valueType", valueType);
                field.put("component", MesProBatchRecordCellRuleSupport.defaultComponentFlag(valueType,
                        StrUtil.blankToDefault(cellRule.getString("componentFlag"),
                                StrUtil.blankToDefault(fillForm.getString("componentFlag"),
                                        StrUtil.blankToDefault(fillForm.getString("component"), "input-text")))));
                field.put("required", Boolean.TRUE.equals(cellRule.getBoolean("required")));
                putIfPresent(field, "placeholder", firstNonBlank(
                        cellRule.getString("placeholder"),
                        fillForm.getString("placeholder")));
                putIfPresent(field, "helpText", firstNonBlank(
                        cellRule.getString("helpText"),
                        fillForm.getString("helpText")));
                JSONObject snapshotCellRule = copySnapshotJsonObject(cellRule);
                field.put("constraints", copySnapshotJsonObject(snapshotCellRule.getJSONObject("constraints")));
                putIfPresent(field, "options", resolveSnapshotFieldOptions(snapshotCellRule, fillForm));
                JSONObject attachmentRule = snapshotCellRule.getJSONObject("attachmentRule");
                if (attachmentRule != null && !attachmentRule.isEmpty()) {
                    field.put("attachmentRule", copySnapshotJsonObject(attachmentRule));
                }
                putIfPresent(field, "unit", snapshotCellRule.getString("unit"));
                field.put(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY, snapshotCellRule);
                putIfPresent(field, "defaultValue", copySnapshotJsonValue(fillForm.get("defaultValue")));
                putIfPresent(field, "value", copySnapshotJsonValue(fillForm.get("value")));
                fields.add(field);
            }
        }
        return fields;
    }

    private JSONArray extractSnapshotAssistRows(JSONObject root) {
        JSONArray assistRows = root == null
                ? null : root.getJSONArray(MesProBatchRecordCellRuleSupport.ASSIST_ROWS_KEY);
        if (assistRows == null) {
            return new JSONArray();
        }
        MesProBatchRecordCellRuleSupport.validateAssistRows(
                root, MesProBatchRecordCellRuleSupport.extractAssistRows(root));
        return JSON.parseArray(assistRows.toJSONString());
    }

    private void validateConfirmedCellRules(JSONObject root) {
        List<String> unreviewedCoordinates = MesProBatchRecordCellRuleSupport.unreviewedFillableCoordinates(root);
        if (!unreviewedCoordinates.isEmpty()) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CELL_RULE_UNREVIEWED,
                    String.join("\u3001", unreviewedCoordinates));
        }
        try {
            MesProBatchRecordCellRuleSupport.forEachCell(root, (rowIndex, columnIndex, cell) -> {
                JSONObject rule = cell.getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY);
                if (rule == null) {
                    return;
                }
                MesProBatchRecordCellRuleSupport.validateRule(
                        MesProBatchRecordCellRuleSupport.toRuleVO(rowIndex, columnIndex, rule), cell);
            });
        } catch (IllegalArgumentException ex) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CELL_RULE_INVALID, ex.getMessage());
        }
    }

    private void putIfPresent(JSONObject target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private JSONObject copySnapshotJsonObject(JSONObject source) {
        return source == null ? new JSONObject(true) : JSON.parseObject(source.toJSONString());
    }

    private Object copySnapshotJsonValue(Object value) {
        if (value instanceof JSONObject || value instanceof JSONArray) {
            return JSON.parse(JSON.toJSONString(value));
        }
        return value;
    }

    private Object resolveSnapshotFieldOptions(JSONObject cellRule, JSONObject fillForm) {
        JSONObject constraints = cellRule == null ? null : cellRule.getJSONObject("constraints");
        if (constraints != null && constraints.get("options") != null) {
            return constraints.get("options");
        }
        if (cellRule != null && cellRule.get("options") != null) {
            return cellRule.get("options");
        }
        return fillForm == null ? null : fillForm.get("options");
    }

    private String buildSnapshotFieldPath(Integer rowIndex, Integer columnIndex, String fieldKey) {
        return String.format(Locale.ROOT, "sheet[0].rows[%d].cells[%d].%s", rowIndex, columnIndex, fieldKey);
    }

    private String resolveFieldLabel(JSONObject rows, Integer rowIndex, Integer columnIndex,
                                     JSONObject cell, JSONObject fillForm, JSONObject cellRule) {
        String direct = firstNonBlank(
                cellRule.getString("label"),
                fillForm.getString("label"),
                fillForm.getString("labelText"),
                cell.getString("text"));
        if (StrUtil.isNotBlank(direct)) {
            return direct.trim();
        }
        JSONObject row = rows.getJSONObject(String.valueOf(rowIndex));
        JSONObject cells = row == null ? null : row.getJSONObject("cells");
        if (cells == null) {
            return fillForm.getString("field");
        }
        for (int cursor = columnIndex - 1; cursor >= 0; cursor--) {
            JSONObject leftCell = cells.getJSONObject(String.valueOf(cursor));
            if (leftCell == null) {
                continue;
            }
            String text = StrUtil.trim(leftCell.getString("text"));
            if (StrUtil.isNotBlank(text)) {
                return text;
            }
        }
        return fillForm.getString("field");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    public record RuntimeSnapshot(String sheetLayoutJson, String metaJson, String executionSnapshotJson) {
    }
}