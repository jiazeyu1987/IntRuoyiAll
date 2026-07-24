package cn.iocoder.yudao.module.bpm.formcenter.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FormSnapshot {

    private final FormSnapshotType snapshotType;
    private final Map<String, Object> formData;
    private final List<String> attachmentIds;
    private final BusinessActionContext businessContext;
    private final LocalDateTime createdTime;

    public FormSnapshot(FormSnapshotType snapshotType, Map<String, Object> formData,
            List<String> attachmentIds, BusinessActionContext businessContext) {
        this.snapshotType = Objects.requireNonNull(snapshotType, "snapshotType");
        this.formData = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(formData, "formData")));
        this.attachmentIds = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(attachmentIds, "attachmentIds")));
        this.businessContext = Objects.requireNonNull(businessContext, "businessContext");
        this.createdTime = LocalDateTime.now();
    }

    public FormSnapshotType getSnapshotType() {
        return snapshotType;
    }

    public Map<String, Object> getFormData() {
        return formData;
    }

    public List<String> getAttachmentIds() {
        return attachmentIds;
    }

    public BusinessActionContext getBusinessContext() {
        return businessContext;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

}
