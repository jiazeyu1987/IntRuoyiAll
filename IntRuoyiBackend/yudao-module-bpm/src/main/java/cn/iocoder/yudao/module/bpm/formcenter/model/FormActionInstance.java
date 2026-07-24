package cn.iocoder.yudao.module.bpm.formcenter.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FormActionInstance {

    private final String instanceCode;
    private final FormActionResolution resolution;
    private final BusinessActionContext businessContext;
    private final Long applicantUserId;
    private final String idempotencyKey;
    private final Map<Long, Set<FormInstancePermissionCode>> instancePermissions = new HashMap<>();
    private final Map<String, Map<Long, Set<FormTaskPermissionCode>>> taskPermissions = new LinkedHashMap<>();
    private final List<FormSnapshot> snapshots = new ArrayList<>();
    private final List<FormEffectExecution> effectExecutions = new ArrayList<>();
    private FormInstanceStatus status = FormInstanceStatus.DRAFT;
    private FormBpmBinding bpmBinding;
    private Map<String, Object> formData = Map.of();

    public FormActionInstance(String instanceCode, FormActionResolution resolution,
            BusinessActionContext businessContext, Long applicantUserId, String idempotencyKey) {
        this.instanceCode = instanceCode;
        this.resolution = resolution;
        this.businessContext = businessContext;
        this.applicantUserId = applicantUserId;
        this.idempotencyKey = idempotencyKey;
    }

    public boolean hasInstancePermission(Long userId, FormInstancePermissionCode permissionCode) {
        return instancePermissions.getOrDefault(userId, Set.of()).contains(permissionCode);
    }

    public void grantInstancePermissions(Long userId, Set<FormInstancePermissionCode> permissionCodes) {
        instancePermissions.computeIfAbsent(userId, key -> EnumSet.noneOf(FormInstancePermissionCode.class))
                .addAll(permissionCodes);
    }

    public boolean hasTaskPermission(String taskId, Long userId, FormTaskPermissionCode permissionCode) {
        return taskPermissions.getOrDefault(taskId, Map.of()).getOrDefault(userId, Set.of()).contains(permissionCode);
    }

    public void grantTaskPermissions(String taskId, Long userId, Set<FormTaskPermissionCode> permissionCodes) {
        taskPermissions.computeIfAbsent(taskId, key -> new HashMap<>())
                .computeIfAbsent(userId, key -> EnumSet.noneOf(FormTaskPermissionCode.class))
                .addAll(permissionCodes);
    }

    public void revokeTaskPermissions(String taskId) {
        taskPermissions.remove(taskId);
    }

    public void addSnapshot(FormSnapshot snapshot) {
        snapshots.add(snapshot);
    }

    public void addEffectExecution(FormEffectExecution effectExecution) {
        effectExecutions.add(effectExecution);
    }

    public FormEffectExecution findEffectExecution(String idempotencyKey) {
        return effectExecutions.stream()
                .filter(execution -> execution.getIdempotencyKey().equals(idempotencyKey))
                .findFirst()
                .orElse(null);
    }

    public String getInstanceCode() {
        return instanceCode;
    }

    public FormActionResolution getResolution() {
        return resolution;
    }

    public BusinessActionContext getBusinessContext() {
        return businessContext;
    }

    public Long getApplicantUserId() {
        return applicantUserId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public FormInstanceStatus getStatus() {
        return status;
    }

    public void setStatus(FormInstanceStatus status) {
        this.status = status;
    }

    public FormBpmBinding getBpmBinding() {
        return bpmBinding;
    }

    public void setBpmBinding(FormBpmBinding bpmBinding) {
        this.bpmBinding = bpmBinding;
    }

    public Map<String, Object> getFormData() {
        return Collections.unmodifiableMap(formData);
    }

    public void setFormData(Map<String, Object> formData) {
        this.formData = formData == null ? Map.of() : new LinkedHashMap<>(formData);
    }

    public List<FormSnapshot> getSnapshots() {
        return Collections.unmodifiableList(snapshots);
    }

    public List<FormEffectExecution> getEffectExecutions() {
        return Collections.unmodifiableList(effectExecutions);
    }

}
