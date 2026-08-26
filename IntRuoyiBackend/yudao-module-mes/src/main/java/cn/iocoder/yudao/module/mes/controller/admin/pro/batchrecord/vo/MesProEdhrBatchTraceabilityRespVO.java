package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesProEdhrBatchTraceabilityRespVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long batchExecutionId;
    private List<Origin> origins;
    private List<TraceLink> traceLinks;
    private Manifest latestManifest;
    private List<Manifest> manifestHistory;

    @Data
    @Accessors(chain = true)
    public static class Origin {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long batchExecutionId;
        private String entryType;
        private String originKey;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long activeOrderId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long workOrderId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long completionTransactionId;
        private Integer completionVersion;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long completionBackfillReceiptId;
        private String completionBackfillReceiptHash;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long pickListBindingId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long pickListId;
        private Integer pickListBindingVersion;
        private Boolean hasActualLoss;
        private String sourceSnapshotHash;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long batchProvisionReceiptId;
        private String batchProvisionStatus;
        private String sourceCredentialId;
        private String sourceCredentialHash;
        private String sourceBundleHash;
        private String idempotencyKey;
        private String relationStatus;
        private String relationReason;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long capturedBy;
        private LocalDateTime capturedAt;
    }

    @Data
    @Accessors(chain = true)
    public static class TraceLink {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long originId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long batchExecutionId;
        private String linkType;
        private String sourceObjectType;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long sourceObjectId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long sourceLineId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long sourceEventId;
        private Integer sourceVersion;
        private String sourceIdentityKey;
        private String idempotencyKey;
        private String snapshotJson;
        private String snapshotHash;
        private String relationStatus;
        private String relationReason;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long capturedBy;
        private LocalDateTime capturedAt;
    }

    @Data
    @Accessors(chain = true)
    public static class Manifest {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long batchExecutionId;
        private Integer manifestVersion;
        private String previousManifestHash;
        private String manifestJson;
        private String manifestHash;
        private String sealReason;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long sealedBy;
        private LocalDateTime sealedAt;
    }
}
