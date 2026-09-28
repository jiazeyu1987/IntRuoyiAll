package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/** P2 read-only reverse-trace API contract. IDs are strings at the HTTP boundary. */
public final class MesProEdhrReverseTraceModels {

    private MesProEdhrReverseTraceModels() {
    }

    public enum Category {
        FIELD, PARAMETER, EQUIPMENT, PERSON, INSPECTION, MATERIAL
    }

    @Data
    @Accessors(chain = true)
    public static class CatalogRequest {
        @NotBlank(message = "锚点批次执行编号不能为空")
        private String anchorBatchExecutionId;

        @Valid
        @NotNull(message = "目标范围不能为空")
        private TargetScope targetScope = new TargetScope().setKind("RELEASED_HISTORY");

        private Category category;

        @NotNull(message = "页码不能为空")
        @Min(value = 1, message = "页码最小值为1")
        private Integer pageNo = 1;

        @NotNull(message = "页大小不能为空")
        @Min(value = 1, message = "页大小最小值为1")
        @Max(value = 100, message = "页大小最大值为100")
        private Integer pageSize = 20;
    }

    @Data
    @Accessors(chain = true)
    public static class QueryRequest {
        @NotBlank(message = "锚点批次执行编号不能为空")
        private String anchorBatchExecutionId;

        private String catalogVersion;

        private String sourceVersionDigest;

        @Valid
        @NotNull(message = "目标范围不能为空")
        private TargetScope targetScope;

        @NotBlank(message = "查询逻辑不能为空")
        private String logic;

        @Valid
        @NotEmpty(message = "查询条件不能为空")
        @Size(max = 10, message = "查询条件最多10条")
        private List<Condition> conditions = new ArrayList<>();

        @NotNull(message = "页码不能为空")
        @Min(value = 1, message = "页码最小值为1")
        private Integer pageNo = 1;

        @NotNull(message = "页大小不能为空")
        @Min(value = 1, message = "页大小最小值为1")
        @Max(value = 100, message = "页大小最大值为100")
        private Integer pageSize = 20;
    }

    @Data
    @Accessors(chain = true)
    public static class TargetScope {
        @NotBlank(message = "目标范围类型不能为空")
        private String kind;

        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime releaseApprovedFrom;

        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime releaseApprovedTo;
    }

    @Data
    @Accessors(chain = true)
    public static class Condition {
        @NotBlank(message = "条件编号不能为空")
        private String conditionId;

        @NotBlank(message = "证据目录引用不能为空")
        private String evidenceKey;

        @NotBlank(message = "来源视图不能为空")
        private String sourceView;

        @NotBlank(message = "运算符不能为空")
        private String operator;

        private Object value;

        private Map<String, String> qualifiers;

        @JsonIgnore
        @AssertTrue(message = "除超出当时标准外的条件必须提供条件值")
        public boolean isValueValidForOperator() {
            if ("OUT_OF_LIMIT".equals(operator)) {
                return true;
            }
            if (value == null) {
                return false;
            }
            return !(value instanceof CharSequence) || !value.toString().isBlank();
        }
    }

    @Data
    @Accessors(chain = true)
    public static class EvidenceRequest {
        @NotBlank(message = "锚点批次执行编号不能为空")
        private String anchorBatchExecutionId;

        private String catalogVersion;

        private String queryHash;

        private String sourceVersionDigest;

        @NotBlank(message = "目标批次执行编号不能为空")
        private String targetBatchExecutionId;

        @Valid
        @NotNull(message = "目标范围不能为空")
        private TargetScope targetScope;

        @NotBlank(message = "查询逻辑不能为空")
        private String logic;

        @Valid
        @NotEmpty(message = "查询条件不能为空")
        @Size(max = 10, message = "查询条件最多10条")
        private List<Condition> conditions = new ArrayList<>();

        @NotNull(message = "页码不能为空")
        @Min(value = 1, message = "页码最小值为1")
        private Integer pageNo = 1;

        @NotNull(message = "页大小不能为空")
        @Min(value = 1, message = "页大小最小值为1")
        @Max(value = 100, message = "页大小最大值为100")
        private Integer pageSize = 20;
    }

    @Data
    @Accessors(chain = true)
    public static class CatalogResponse {
        private String anchorBatchExecutionId;
        private String catalogVersion;
        private ChainContext chainContext;
        private List<CategoryStatus> categories = new ArrayList<>();
        private List<CatalogItem> items = new ArrayList<>();
        private Long total;
    }

    @Data
    @Accessors(chain = true)
    public static class CategoryStatus {
        private Category category;
        private String status;
        private String reasonCode;
        private String reason;
    }

    @Data
    @Accessors(chain = true)
    public static class CatalogItem {
        private String evidenceKey;
        private Category category;
        private String sourceView;
        private String sourceRef;
        private String semanticIdentity;
        private String label;
        private String valueType;
        private String savedValue;
        private String unit;
        private String lowerLimit;
        private String upperLimit;
        private String recordedStandard;
        private String parameterStatus;
        private String resultType;
        private String judgement;
        private Integer standardPrecision;
        private Map<String, String> qualifiers;
        private List<String> allowedOperators = new ArrayList<>();
        private List<String> allowedValues = new ArrayList<>();
        private LocalDateTime recordedAt;
        private String recordStatus;
    }

    @Data
    @Accessors(chain = true)
    public static class QueryResponse {
        private String catalogVersion;
        private String queryStatus;
        private String coverageStatus;
        private String reasonCode;
        private String reason;
        private String queryHash;
        private LocalDateTime evaluatedAt;
        private QueryRequest normalizedQuery;
        private Coverage coverage;
        private List<QueryItem> list = new ArrayList<>();
        private Long total;
    }

    @Data
    @Accessors(chain = true)
    public static class Coverage {
        private long candidateBatchCount;
        private long evaluatedBatchCount;
        private long notApplicableBatchCount;
    }

    @Data
    @Accessors(chain = true)
    public static class QueryItem {
        private String batchExecutionId;
        private String batchExecutionCode;
        private String batchCode;
        private String workOrderCode;
        private String productName;
        private String releaseStatus;
        private boolean anchor;
        private int matchCount;
        private List<String> conditionIds = new ArrayList<>();
        private String matchSummary;
    }

    @Data
    @Accessors(chain = true)
    public static class EvidenceResponse {
        private String catalogVersion;
        private String queryHash;
        private String evidenceStatus;
        private String reasonCode;
        private String reason;
        private String targetBatchExecutionId;
        private ChainContext chainContext;
        private List<EvidenceItem> items = new ArrayList<>();
        private Long total;
    }

    @Data
    @Accessors(chain = true)
    public static class EvidenceResult {
        private String sourceVersion;
        private String sourceIdentity;
        private String targetBatchExecutionId;
        private List<EvidenceItem> items = new ArrayList<>();
    }

    @Data
    @Accessors(chain = true)
    public static class EvidenceItem {
        private String conditionId;
        private String targetBatchExecutionId;
        private String sourceIdentity;
        private String sourceStage;
        private String sourceAction;
        private String sourceRef;
        private Map<String, String> sourceContext;
        private String actualValue;
        private String recordedStandard;
        private String recordStatus;
        private LocalDateTime occurredAt;
        private LocalDateTime recordedAt;
    }

    @Data
    @Accessors(chain = true)
    public static class ChainContext {
        private String anchorBatchExecutionId;
        private String targetBatchExecutionId;
        private String batchExecutionCode;
        private String activeOrderId;
        private String workOrderId;
        private String routeVersionId;
        private List<String> qaVersionRefs = new ArrayList<>();
        private String completionReceiptId;
        private String releaseApplicationId;
        private String pqcReleaseWorkTaskId;
        private String releaseTransactionId;
        private String releaseApprovalWorkTaskId;
        private String sourceStatus;
    }
}
