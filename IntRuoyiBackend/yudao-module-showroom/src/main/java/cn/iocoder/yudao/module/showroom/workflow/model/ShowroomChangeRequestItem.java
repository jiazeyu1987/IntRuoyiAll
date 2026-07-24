package cn.iocoder.yudao.module.showroom.workflow.model;

import java.time.Instant;

public record ShowroomChangeRequestItem(String fieldCode, String oldValueJson, String newValueJson,
                                        String label, String oldValue, String newValue,
                                        String approvalStatus, Long approvedBy, Instant approvedAt,
                                        String comment) {

    public ShowroomChangeRequestItem(String fieldCode, String oldValueJson, String newValueJson) {
        this(fieldCode, oldValueJson, newValueJson, null, null, null,
                "PENDING", null, null, null);
    }

    public ShowroomChangeRequestItem(String fieldCode, String oldValueJson, String newValueJson,
                                     String approvalStatus, Long approvedBy, Instant approvedAt,
                                     String comment) {
        this(fieldCode, oldValueJson, newValueJson, null, null, null,
                approvalStatus, approvedBy, approvedAt, comment);
    }

    public ShowroomChangeRequestItem withDisplay(String displayLabel, String displayOldValue,
                                                 String displayNewValue) {
        return new ShowroomChangeRequestItem(fieldCode, oldValueJson, newValueJson,
                displayLabel, displayOldValue, displayNewValue,
                approvalStatus, approvedBy, approvedAt, comment);
    }
}
