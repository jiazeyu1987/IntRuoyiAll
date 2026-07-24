package cn.iocoder.yudao.module.dcc.enums;

import cn.hutool.core.util.ArrayUtil;
import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DccControlledFileStatusEnum implements ArrayValuable<String> {

    DRAFT("DRAFT", "Draft"),
    PENDING_DOC_CONTROL_REVIEW("PENDING_DOC_CONTROL_REVIEW", "Pending doc control review"),
    PENDING_MATRIX_REVIEW("PENDING_MATRIX_REVIEW", "Pending matrix review"),
    PENDING_MATRIX_APPROVAL("PENDING_MATRIX_APPROVAL", "Pending matrix approval"),
    PENDING_DOC_CONTROL_APPROVAL("PENDING_DOC_CONTROL_APPROVAL", "Pending doc control approval"),
    PENDING_APPLICANT_REWORK("PENDING_APPLICANT_REWORK", "Pending applicant rework"),
    PENDING_APPLICANT_TRAINING_RECORD("PENDING_APPLICANT_TRAINING_RECORD", "Pending applicant training record"),
    READY_TO_PUBLISH("READY_TO_PUBLISH", "Ready to publish"),
    FINALIZING("FINALIZING", "Finalizing"),
    TRAINING_IN_PROGRESS("TRAINING_IN_PROGRESS", "Training in progress"),
    PENDING_MANUAL_DISTRIBUTION("PENDING_MANUAL_DISTRIBUTION", "Pending manual distribution"),
    ACTIVE("ACTIVE", "Active"),
    REJECTED("REJECTED", "Rejected"),
    WITHDRAWN("WITHDRAWN", "Withdrawn"),
    OBSOLETE("OBSOLETE", "Obsolete"),
    SUPERSEDED("SUPERSEDED", "Superseded"),
    FINALIZATION_FAILED("FINALIZATION_FAILED", "Finalization failed"),

    // Legacy statuses retained temporarily so the current service layer still compiles.
    SUBMIT_FAILED("SUBMIT_FAILED", "Submit failed"),
    APPROVING("APPROVING", "Legacy approving"),
    APPROVED("APPROVED", "Legacy approved"),
    STAMPING("STAMPING", "Legacy stamping"),
    STAMP_FAILED("STAMP_FAILED", "Legacy stamp failed"),
    STAMPED("STAMPED", "Legacy stamped");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(DccControlledFileStatusEnum::getStatus)
            .toArray(String[]::new);

    private final String status;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    public static DccControlledFileStatusEnum valueOfStatus(String status) {
        return ArrayUtil.firstMatch(item -> item.getStatus().equals(status), values());
    }

}
