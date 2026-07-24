package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordAttachmentRuleSupportTest {

    @Test
    void collectMissingRequiredAttachments_reportsFieldWhenRequiredAttachmentAbsent() {
        String snapshot = """
                {
                  "fields": [
                    {
                      "fieldKey": "sterilizationReport",
                      "fieldPath": "sheet[0].rows[1].cells[2].sterilizationReport",
                      "label": "灭菌报告",
                      "rowIndex": 1,
                      "columnIndex": 2,
                      "attachmentRule": {
                        "required": true,
                        "minCount": 1,
                        "attachmentType": "FILE",
                        "groupKey": "sterilization-report"
                      }
                    }
                  ]
                }
                """;

        List<String> blockers = MesProBatchRecordAttachmentRuleSupport.collectMissingRequiredAttachments(
                snapshot, List.of());

        assertEquals(1, blockers.size());
        assertTrue(blockers.get(0).contains("灭菌报告"));
        assertTrue(blockers.get(0).contains("缺少必需附件"));
    }

    @Test
    void collectMissingRequiredAttachments_passesWhenMatchingActiveAttachmentExists() {
        String snapshot = """
                {
                  "fields": [
                    {
                      "fieldKey": "sterilizationReport",
                      "fieldPath": "sheet[0].rows[1].cells[2].sterilizationReport",
                      "label": "灭菌报告",
                      "rowIndex": 1,
                      "columnIndex": 2,
                      "attachmentRule": {
                        "required": true,
                        "minCount": 1,
                        "attachmentType": "FILE",
                        "groupKey": "sterilization-report"
                      }
                    }
                  ]
                }
                """;
        MesProBatchRecordExecutionAttachmentDO attachment = new MesProBatchRecordExecutionAttachmentDO()
                .setFieldPath("sheet[0].rows[1].cells[2].sterilizationReport")
                .setFieldKey("sterilizationReport")
                .setAttachmentType("FILE")
                .setAttachmentGroupKey("sterilization-report")
                .setAttachmentAction("ADD");

        List<String> blockers = MesProBatchRecordAttachmentRuleSupport.collectMissingRequiredAttachments(
                snapshot, List.of(attachment));

        assertTrue(blockers.isEmpty());
    }
}
