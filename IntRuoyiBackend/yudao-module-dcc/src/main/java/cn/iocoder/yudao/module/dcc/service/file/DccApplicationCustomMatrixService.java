package cn.iocoder.yudao.module.dcc.service.file;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import cn.iocoder.yudao.module.dcc.service.file.DccApplicationCustomMatrix.ApprovalSourceMode;

/** Pure application source contract. It has no public-route persistence or personnel authorization. */
public final class DccApplicationCustomMatrixService {
    public static final String FORMAT = "DCC_APPLICATION_CUSTOM_MATRIX_V1";
    public static final String PREVIEW_FORMAT = "DCC_APPLICATION_CUSTOM_MATRIX_PREVIEW_V1";
    private static final ObjectMapper JSON = new ObjectMapper(new JsonFactory()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION))
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final Set<String> ROOT_FIELDS = Set.of("formatVersion", "approvalSourceMode", "context",
            "applicationCustomMatrix", "selectedSignoffDepartmentIds");
    private static final Set<String> CONTEXT_FIELDS = Set.of("tenantId", "projectId", "categoryId", "taxonomyId",
            "requesterId", "controlledFileId", "attributeRound", "applicationType", "applicationContextKey",
            "processDefinitionId", "definitionDigest");
    private static final Set<String> PREVIEW_CONTEXT_FIELDS = Set.of("tenantId", "projectId", "categoryId", "taxonomyId",
            "requesterId", "applicationType", "applicationContextKey", "processDefinitionId", "definitionDigest");
    private static final Set<String> MATRIX_FIELDS = Set.of("approverUserId", "documentControlUserId");

    /** Reserved exact application context only; not a client-provided grant to use a custom source. */
    public record SourceContext(ApprovalSourceMode approvalSourceMode, Long tenantId, Long projectId,
            Long categoryId, Long taxonomyId, Long requesterId, Long controlledFileId, Integer attributeRound,
            String applicationType, String applicationContextKey, String processDefinitionId, String definitionDigest) {}
    /** Unallocated preview has no application ID or reserved round, and can never impersonate a saved context. */
    public record PreviewSourceContext(ApprovalSourceMode approvalSourceMode, Long tenantId, Long projectId,
            Long categoryId, Long taxonomyId, Long requesterId, String applicationType,
            String applicationContextKey, String processDefinitionId, String definitionDigest) {}
    public record CanonicalSelection(String canonical, String digest) {}
    public record SavedSelection(DccApplicationCustomMatrix applicationCustomMatrix, List<Long> selectedSignoffDepartmentIds) {
        public SavedSelection {
            selectedSignoffDepartmentIds = selectedSignoffDepartmentIds == null ? null : List.copyOf(selectedSignoffDepartmentIds);
        }
    }
    public record Node(String stageCode, int stageOrder, String candidateSourceType, List<Long> candidateSourceIds,
            String approveMethod, Integer approveRatio, boolean requireAllApprovals) {
        public Node { candidateSourceIds = List.copyOf(candidateSourceIds); }
    }
    public DccApplicationCustomMatrix validateDraft(DccApplicationCustomMatrix value) {
        if (value == null) return null;
        account(value.approverUserId(), "本次批准人");
        account(value.documentControlUserId(), "本次文控审核人");
        return value;
    }
    public CanonicalSelection canonicalizePreview(PreviewSourceContext context, DccApplicationCustomMatrix value, List<Long> departments) {
        validatePreviewContext(context);
        return encode(PREVIEW_FORMAT, context.approvalSourceMode(), previewContextValue(context), value, departments);
    }
    public SavedSelection readPreviewCanonical(String canonical, String expectedDigest, PreviewSourceContext expected) {
        validatePreviewContext(expected);
        var root = decode(canonical, expectedDigest, PREVIEW_FORMAT);
        var context = root.get("context");
        exact(context, PREVIEW_CONTEXT_FIELDS);
        var actual = new PreviewSourceContext(ApprovalSourceMode.APPLICATION_CUSTOM,
                longIdentity(context.get("tenantId")), longIdentity(context.get("projectId")), longIdentity(context.get("categoryId")),
                longIdentity(context.get("taxonomyId")), longIdentity(context.get("requesterId")), text(context.get("applicationType")),
                text(context.get("applicationContextKey")), text(context.get("processDefinitionId")), text(context.get("definitionDigest")));
        validatePreviewContext(actual);
        if (!actual.equals(expected)) throw error("CUSTOM_MATRIX_CONTEXT_MISMATCH", "本次预检身份或正式定义不一致");
        var selection = readSelection(root);
        if (!canonical.equals(canonicalizePreview(actual, selection.applicationCustomMatrix(), selection.selectedSignoffDepartmentIds()).canonical()))
            throw canonicalInvalid("预检JSON不是正式规范格式");
        return selection;
    }
    /** Pure structural binding only. Formal callers must verify their signed preview and organization proof first. */
    public CanonicalSelection bindPreviewToReserved(String canonical, String expectedDigest,
            PreviewSourceContext preview, SourceContext reserved) {
        var selection = readPreviewCanonical(canonical, expectedDigest, preview);
        validateContext(reserved);
        if (!preview.equals(previewOf(reserved))) throw error("CUSTOM_MATRIX_CONTEXT_MISMATCH", "预检与本次已保留申请身份不一致");
        return canonicalize(reserved, selection.applicationCustomMatrix(), selection.selectedSignoffDepartmentIds());
    }

    /** Null/empty draft selections remain distinct. Identities serialize as strings, never doubles. */
    public CanonicalSelection canonicalize(SourceContext context, DccApplicationCustomMatrix value, List<Long> departments) {
        validateContext(context);
        return encode(FORMAT, context.approvalSourceMode(), contextValue(context), value, departments);
    }
    private CanonicalSelection encode(String format, ApprovalSourceMode mode, Map<String, Object> context,
            DccApplicationCustomMatrix value, List<Long> departments) {
        var actual = validateDraft(value);
        var selected = normalizeDepartments(departments);
        var root = new TreeMap<String, Object>();
        root.put("formatVersion", format);
        root.put("approvalSourceMode", mode.name());
        root.put("context", context);
        root.put("applicationCustomMatrix", matrixValue(actual));
        root.put("selectedSignoffDepartmentIds", selected == null ? null : selected.stream().map(String::valueOf).toList());
        final String canonical;
        try {
            canonical = JSON.writeValueAsString(root);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("CUSTOM_MATRIX_ENCODING_FAILED：本次配置无法生成正式保存格式", failure);
        }
        return new CanonicalSelection(canonical, hash(canonical));
    }

    /** Integrity/context check, not an HMAC or personnel authorization. The caller must verify its formal proof. */
    public SavedSelection readCanonical(String canonical, String expectedDigest, SourceContext expected) {
        validateContext(expected);
        var root = decode(canonical, expectedDigest, FORMAT);
        var rawContext = root.get("context");
        exact(rawContext, CONTEXT_FIELDS);
        var restoredContext = new SourceContext(ApprovalSourceMode.APPLICATION_CUSTOM,
                longIdentity(rawContext.get("tenantId")), longIdentity(rawContext.get("projectId")),
                longIdentity(rawContext.get("categoryId")), longIdentity(rawContext.get("taxonomyId")),
                longIdentity(rawContext.get("requesterId")), longIdentity(rawContext.get("controlledFileId")),
                integer(rawContext.get("attributeRound")), text(rawContext.get("applicationType")),
                text(rawContext.get("applicationContextKey")), text(rawContext.get("processDefinitionId")), text(rawContext.get("definitionDigest")));
        validateContext(restoredContext);
        if (!restoredContext.equals(expected)) throw error("CUSTOM_MATRIX_CONTEXT_MISMATCH", "本次申请身份或轮次不一致");
        var selection = readSelection(root);
        var restored = canonicalize(restoredContext, selection.applicationCustomMatrix(), selection.selectedSignoffDepartmentIds());
        if (!canonical.equals(restored.canonical())) throw canonicalInvalid("保存JSON不是正式规范格式，不能替换为重新推测的值");
        return selection;
    }
    private JsonNode decode(String canonical, String expectedDigest, String format) {
        if (canonical == null || canonical.isEmpty()) throw canonicalInvalid("保存原配置缺失");
        if (expectedDigest == null || !expectedDigest.matches("[a-f0-9]{64}")
                || !MessageDigest.isEqual(hash(canonical).getBytes(StandardCharsets.US_ASCII), expectedDigest.getBytes(StandardCharsets.US_ASCII)))
            throw error("CUSTOM_MATRIX_DIGEST_INVALID", "本次自定义矩阵保存摘要不一致或缺失");
        final JsonNode root;
        try {
            root = JSON.readTree(canonical);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException("CUSTOM_MATRIX_CANONICAL_INVALID：本次配置JSON不合法或包含重复字段/多余内容", failure);
        }
        exact(root, ROOT_FIELDS);
        if (!format.equals(text(root.get("formatVersion")))) throw canonicalInvalid("保存格式版本或预检/保留阶段不一致");
        if (!ApprovalSourceMode.APPLICATION_CUSTOM.name().equals(text(root.get("approvalSourceMode"))))
            throw error("CUSTOM_MATRIX_SOURCE_REQUIRED", "当前正式类别不使用本次自定义矩阵");
        return root;
    }
    private SavedSelection readSelection(JsonNode root) {
        var rawMatrix = root.get("applicationCustomMatrix");
        DccApplicationCustomMatrix matrix = null;
        if (!rawMatrix.isNull()) {
            exact(rawMatrix, MATRIX_FIELDS);
            matrix = new DccApplicationCustomMatrix(nullableIdentity(rawMatrix.get("approverUserId")),
                    nullableIdentity(rawMatrix.get("documentControlUserId")));
        }
        var rawDepartments = root.get("selectedSignoffDepartmentIds");
        List<Long> departments = null;
        if (!rawDepartments.isNull()) {
            if (!rawDepartments.isArray()) throw canonicalInvalid("会签部门保存值必须为明确数组或null");
            departments = new ArrayList<>();
            for (var id : rawDepartments) departments.add(longIdentity(id));
        }
        validateDraft(matrix);
        return new SavedSelection(matrix, normalizeDepartments(departments));
    }

    /** Compiles structural department obligations/actors only. Current organization/readiness is checked by formal callers. */
    public List<Node> compile(ApprovalSourceMode mode, DccApplicationCustomMatrix value, List<Long> departments) {
        requireCustomSource(mode);
        var selected = normalizeDepartments(departments);
        if (selected == null || selected.isEmpty()) throw error("CUSTOM_MATRIX_SIGNOFF_REQUIRED", "请至少选择一个会签部门");
        var actual = validateDraft(value);
        if (actual == null || actual.approverUserId() == null) throw error("CUSTOM_MATRIX_APPROVER_REQUIRED", "请选择本次批准人");
        if (actual.documentControlUserId() == null) throw error("CUSTOM_MATRIX_DOCUMENT_CONTROL_REQUIRED", "请选择本次文控审核人");
        return List.of(
                new Node("MATRIX_REVIEW", 1, "DEPT", selected, "ALL", 100, true),
                new Node("MATRIX_APPROVAL", 2, "USER", List.of(actual.approverUserId()), "ANY", null, false),
                new Node("DOC_CONTROL_REVIEW", 3, "USER", List.of(actual.documentControlUserId()), "ANY", null, false));
    }

    private void requireCustomSource(ApprovalSourceMode mode) {
        if (mode != ApprovalSourceMode.APPLICATION_CUSTOM) throw error("CUSTOM_MATRIX_SOURCE_REQUIRED", "当前正式类别不使用本次自定义矩阵");
    }
    private void account(Long id, String role) {
        if (id != null && id <= 0) throw error("CUSTOM_MATRIX_ACCOUNT_INVALID", role + "账号身份无效");
    }
    private List<Long> normalizeDepartments(List<Long> departments) {
        if (departments == null) return null;
        var seen = new HashSet<Long>();
        for (Long id : departments) {
            if (id == null || id <= 0) throw error("CUSTOM_MATRIX_DEPARTMENT_INVALID", "会签部门身份无效");
            if (!seen.add(id)) throw error("CUSTOM_MATRIX_DEPARTMENT_DUPLICATE", "会签部门不能重复");
        }
        return departments.stream().sorted().toList();
    }
    private void validateContext(SourceContext context) {
        if (context == null) throw error("CUSTOM_MATRIX_CONTEXT_INVALID", "本次已保留申请上下文缺失");
        validatePreviewContext(previewOf(context));
        if (context.controlledFileId() == null || context.controlledFileId() <= 0) throw error("CUSTOM_MATRIX_CONTEXT_INVALID", "本次已保留申请身份无效：controlledFileId");
        if (context.attributeRound() == null || context.attributeRound() <= 0) throw error("CUSTOM_MATRIX_CONTEXT_INVALID", "本次已保留申请轮次无效：attributeRound");
    }
    private void validatePreviewContext(PreviewSourceContext context) {
        if (context == null) throw error("CUSTOM_MATRIX_CONTEXT_INVALID", "本次正式预检上下文缺失");
        requireCustomSource(context.approvalSourceMode());
        var ids = new Long[]{context.tenantId(), context.projectId(), context.categoryId(), context.taxonomyId(), context.requesterId()};
        var fields = new String[]{"tenantId", "projectId", "categoryId", "taxonomyId", "requesterId"};
        for (int index = 0; index < ids.length; index++) {
            if (ids[index] == null || ids[index] <= 0) throw error("CUSTOM_MATRIX_CONTEXT_INVALID", "本次已保留申请身份无效：" + fields[index]);
        }
        if (!Set.of("UPLOAD", "REVISION").contains(context.applicationType() == null ? "" : context.applicationType()))
            throw error("CUSTOM_MATRIX_CONTEXT_INVALID", "本次申请类型无效：applicationType");
        contextText(context.applicationContextKey(), "applicationContextKey");
        contextText(context.processDefinitionId(), "processDefinitionId");
        if (context.definitionDigest() == null || !context.definitionDigest().matches("[a-f0-9]{64}"))
            throw error("CUSTOM_MATRIX_CONTEXT_INVALID", "本次精确流程定义摘要无效：definitionDigest");
    }
    private PreviewSourceContext previewOf(SourceContext context) {
        return new PreviewSourceContext(context.approvalSourceMode(), context.tenantId(), context.projectId(), context.categoryId(), context.taxonomyId(),
                context.requesterId(), context.applicationType(), context.applicationContextKey(), context.processDefinitionId(), context.definitionDigest());
    }
    private void contextText(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.strip()) || value.length() > 1024)
            throw error("CUSTOM_MATRIX_CONTEXT_INVALID", "本次正式上下文文本无效：" + field);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isISOControl(character)) throw error("CUSTOM_MATRIX_CONTEXT_INVALID", "本次正式上下文包含控制字符：" + field);
            if (Character.isHighSurrogate(character)) {
                if (++index >= value.length() || !Character.isLowSurrogate(value.charAt(index)))
                    throw error("CUSTOM_MATRIX_CONTEXT_INVALID", "本次正式上下文Unicode无效：" + field);
            } else if (Character.isLowSurrogate(character)) throw error("CUSTOM_MATRIX_CONTEXT_INVALID", "本次正式上下文Unicode无效：" + field);
        }
    }
    private Map<String, Object> contextValue(SourceContext context) {
        var map = new TreeMap<>(previewContextValue(previewOf(context)));
        map.put("controlledFileId", context.controlledFileId().toString());
        map.put("attributeRound", context.attributeRound()); return map;
    }
    private Map<String, Object> previewContextValue(PreviewSourceContext context) {
        var map = new TreeMap<String, Object>();
        map.put("tenantId", context.tenantId().toString()); map.put("projectId", context.projectId().toString());
        map.put("categoryId", context.categoryId().toString()); map.put("taxonomyId", context.taxonomyId().toString());
        map.put("requesterId", context.requesterId().toString()); map.put("applicationType", context.applicationType());
        map.put("applicationContextKey", context.applicationContextKey()); map.put("processDefinitionId", context.processDefinitionId());
        map.put("definitionDigest", context.definitionDigest()); return map;
    }
    private Map<String, Object> matrixValue(DccApplicationCustomMatrix matrix) {
        if (matrix == null) return null;
        var map = new TreeMap<String, Object>();
        map.put("approverUserId", matrix.approverUserId() == null ? null : matrix.approverUserId().toString());
        map.put("documentControlUserId", matrix.documentControlUserId() == null ? null : matrix.documentControlUserId().toString());
        return map;
    }
    private void exact(JsonNode node, Set<String> fields) {
        if (node == null || !node.isObject()) throw canonicalInvalid("正式保存对象缺失");
        var actual = new HashSet<String>(); node.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(fields)) throw canonicalInvalid("正式保存字段缺失或包含未知字段");
    }
    private String text(JsonNode node) {
        if (node == null || !node.isTextual()) throw canonicalInvalid("正式保存文本类型无效");
        return node.textValue();
    }
    private int integer(JsonNode node) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) throw canonicalInvalid("正式保存轮次类型无效");
        return node.intValue();
    }
    private Long nullableIdentity(JsonNode node) { return node.isNull() ? null : longIdentity(node); }
    private Long longIdentity(JsonNode node) {
        String value = text(node);
        if (!value.matches("[1-9][0-9]*")) throw canonicalInvalid("正式Long身份必须为正整数字符串");
        try { return Long.valueOf(value); }
        catch (NumberFormatException failure) { throw new IllegalArgumentException("CUSTOM_MATRIX_CANONICAL_INVALID：正式Long身份超出范围", failure); }
    }
    private String hash(String canonical) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException failure) { throw new IllegalStateException("CUSTOM_MATRIX_HASH_UNAVAILABLE：SHA-256不可用", failure); }
    }
    private IllegalArgumentException canonicalInvalid(String detail) { return error("CUSTOM_MATRIX_CANONICAL_INVALID", "本次自定义矩阵正式保存合同无效：" + detail); }
    private IllegalArgumentException error(String code, String message) { return new IllegalArgumentException(code + "：" + message); }
}
