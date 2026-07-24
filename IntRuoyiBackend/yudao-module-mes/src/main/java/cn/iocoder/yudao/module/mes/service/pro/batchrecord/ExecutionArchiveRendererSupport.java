package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ExecutionArchiveRendererSupport {

    static final String RENDER_SOURCE_VERSION = "EDHR_ARCHIVE_V1";
    static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private ExecutionArchiveRendererSupport() {
    }

    static RenderData requireRenderData(MesProBatchRecordExecutionArchiveRenderContext context) {
        if (context == null) {
            throw new IllegalArgumentException("EDHR archive render context is required");
        }
        MesProBatchRecordExecutionDO execution = Objects.requireNonNull(context.getExecution(),
                "EDHR archive execution is required");
        JSONObject executionSnapshot = context.getExecutionSnapshot();
        if (executionSnapshot == null || executionSnapshot.isEmpty()) {
            throw new IllegalArgumentException("EDHR archive execution snapshot is required");
        }
        Object cellValues = context.getCellValues();
        if (cellValues == null) {
            throw new IllegalArgumentException("EDHR archive cell values are required");
        }
        List<MesProBatchRecordExecutionSignatureDO> signatures = context.getSignatures();
        if (signatures == null || signatures.isEmpty()) {
            throw new IllegalArgumentException("EDHR archive signatures are required");
        }
        if (StrUtil.isBlank(context.getExecutionSnapshotHash())) {
            throw new IllegalArgumentException("EDHR archive execution snapshot hash is required");
        }
        if (StrUtil.isBlank(context.getCellValuesHash())) {
            throw new IllegalArgumentException("EDHR archive cell values hash is required");
        }
        if (StrUtil.isBlank(context.getSignatureHash())) {
            throw new IllegalArgumentException("EDHR archive signature hash is required");
        }
        if (context.getAttachments() == null) {
            throw new IllegalArgumentException("EDHR archive attachment manifest is required");
        }
        if (!context.getAttachments().isEmpty() && StrUtil.isBlank(context.getAttachmentManifestHeadHash())) {
            throw new IllegalArgumentException("EDHR archive attachment manifest head hash is required");
        }
        if (context.getApprovalSnapshotId() == null) {
            throw new IllegalArgumentException("EDHR archive approval snapshot id is required");
        }
        if (StrUtil.isBlank(context.getApprovalSnapshotHash())) {
            throw new IllegalArgumentException("EDHR archive approval snapshot hash is required");
        }
        if (context.getGeneratedBy() == null) {
            throw new IllegalArgumentException("EDHR archive generated user is required");
        }
        if (context.getGeneratedAt() == null) {
            throw new IllegalArgumentException("EDHR archive generated time is required");
        }
        return new RenderData(context, execution, executionSnapshot, cellValues, signatures,
                context.getAttachments());
    }

    static String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest algorithm is unavailable", ex);
        }
    }

    static String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    static String jsonValue(Object value) {
        return value == null ? "" : JSON.toJSONString(value);
    }

    static String sourceJson(RenderData data) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("execution", data.execution);
        source.put("executionSnapshot", data.executionSnapshot);
        source.put("cellValues", data.cellValues);
        source.put("signatures", data.signatures);
        source.put("executionSnapshotHash", data.context.getExecutionSnapshotHash());
        source.put("cellValuesHash", data.context.getCellValuesHash());
        source.put("signatureHash", data.context.getSignatureHash());
        source.put("attachments", data.attachments);
        source.put("attachmentManifestHeadHash", data.context.getAttachmentManifestHeadHash());
        source.put("approvalSnapshotId", data.context.getApprovalSnapshotId());
        source.put("approvalSnapshotHash", data.context.getApprovalSnapshotHash());
        source.put("formSlotManifest", data.context.getFormSlotManifest());
        return JSON.toJSONString(source);
    }

    static String shortText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return value(text);
        }
        return text.substring(0, maxLength) + "...";
    }

    static String safeFileToken(String value) {
        String token = StrUtil.blankToDefault(value, "execution");
        token = token.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        return StrUtil.subPre(token, 80);
    }

    static byte[] utf8(String value) {
        return value(value).getBytes(StandardCharsets.UTF_8);
    }

    record RenderData(MesProBatchRecordExecutionArchiveRenderContext context,
                      MesProBatchRecordExecutionDO execution,
                      JSONObject executionSnapshot,
                      Object cellValues,
                      List<MesProBatchRecordExecutionSignatureDO> signatures,
                      List<MesProBatchRecordExecutionAttachmentDO> attachments) {
    }
}
