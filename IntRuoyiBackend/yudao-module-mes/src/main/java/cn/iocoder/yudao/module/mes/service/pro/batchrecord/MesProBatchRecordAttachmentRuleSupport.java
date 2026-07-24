package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class MesProBatchRecordAttachmentRuleSupport {

    private MesProBatchRecordAttachmentRuleSupport() {
    }

    static List<String> collectMissingRequiredAttachments(String executionSnapshotJson,
                                                           List<MesProBatchRecordExecutionAttachmentDO> attachments) {
        List<RequiredAttachmentRule> rules = extractRequiredAttachmentRules(executionSnapshotJson);
        if (rules.isEmpty()) {
            return List.of();
        }
        List<String> blockers = new ArrayList<>();
        List<MesProBatchRecordExecutionAttachmentDO> activeAttachments = attachments == null ? List.of()
                : attachments.stream().filter(MesProBatchRecordAttachmentRuleSupport::isActiveAttachment).toList();
        for (RequiredAttachmentRule rule : rules) {
            long count = activeAttachments.stream()
                    .filter(attachment -> matchesRule(rule, attachment))
                    .count();
            if (count < rule.minCount()) {
                blockers.add(rule.label() + ": 缺少必需附件，要求至少 " + rule.minCount() + " 个，当前 " + count + " 个");
            }
        }
        return blockers;
    }

    private static List<RequiredAttachmentRule> extractRequiredAttachmentRules(String executionSnapshotJson) {
        if (StrUtil.isBlank(executionSnapshotJson)) {
            return List.of();
        }
        JSONObject snapshot = JSON.parseObject(executionSnapshotJson);
        JSONArray fields = snapshot == null ? null : snapshot.getJSONArray("fields");
        if (CollUtil.isEmpty(fields)) {
            return List.of();
        }
        List<RequiredAttachmentRule> rules = new ArrayList<>();
        for (int index = 0; index < fields.size(); index++) {
            JSONObject field = fields.getJSONObject(index);
            JSONObject attachmentRule = field == null ? null : field.getJSONObject("attachmentRule");
            if (attachmentRule == null || !Boolean.TRUE.equals(attachmentRule.getBoolean("required"))) {
                continue;
            }
            rules.add(new RequiredAttachmentRule(
                    field.getString("fieldKey"),
                    field.getString("fieldPath"),
                    field.getInteger("rowIndex"),
                    field.getInteger("columnIndex"),
                    StrUtil.blankToDefault(field.getString("label"), field.getString("fieldKey")),
                    StrUtil.blankToDefault(attachmentRule.getString("attachmentType"), null),
                    StrUtil.blankToDefault(attachmentRule.getString("groupKey"), null),
                    Math.max(1, attachmentRule.getInteger("minCount") == null
                            ? 1 : attachmentRule.getInteger("minCount"))));
        }
        return rules;
    }

    private static boolean matchesRule(RequiredAttachmentRule rule,
                                       MesProBatchRecordExecutionAttachmentDO attachment) {
        if (attachment == null) {
            return false;
        }
        if (StrUtil.isNotBlank(rule.groupKey())
                && !Objects.equals(rule.groupKey(), attachment.getAttachmentGroupKey())) {
            return false;
        }
        if (StrUtil.isNotBlank(rule.attachmentType())
                && !Objects.equals(rule.attachmentType(), attachment.getAttachmentType())) {
            return false;
        }
        if (StrUtil.isNotBlank(rule.fieldPath())) {
            return Objects.equals(rule.fieldPath(), attachment.getFieldPath());
        }
        if (StrUtil.isNotBlank(rule.fieldKey())) {
            return Objects.equals(rule.fieldKey(), attachment.getFieldKey());
        }
        return Objects.equals(rule.rowIndex(), attachment.getRowIndex())
                && Objects.equals(rule.columnIndex(), attachment.getColumnIndex());
    }

    private static boolean isActiveAttachment(MesProBatchRecordExecutionAttachmentDO attachment) {
        return attachment != null
                && ("ADD".equals(attachment.getAttachmentAction())
                || "REPLACE".equals(attachment.getAttachmentAction()));
    }

    private record RequiredAttachmentRule(String fieldKey,
                                          String fieldPath,
                                          Integer rowIndex,
                                          Integer columnIndex,
                                          String label,
                                          String attachmentType,
                                          String groupKey,
                                          int minCount) {
    }
}
