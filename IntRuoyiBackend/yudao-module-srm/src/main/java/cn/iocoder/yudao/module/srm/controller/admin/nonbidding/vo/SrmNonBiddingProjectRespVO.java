package cn.iocoder.yudao.module.srm.controller.admin.nonbidding.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SrmNonBiddingProjectRespVO {

    private Long id;
    private String projectNo;
    private String projectTitle;
    private String projectType;
    private String projectTypeLabel;
    private String projectStatus;
    private String projectStatusLabel;
    private Long sourcePlanId;
    private String sourcePlanNo;
    private BigDecimal expectedAmount;
    private String quoteMode;
    private String quoteModeLabel;
    private LocalDateTime quoteStartTime;
    private LocalDateTime quoteEndTime;
    private String publishAttachmentUrl;
    private LocalDateTime publishedTime;
    private Long dealQuoteId;
    private Long dealSupplierId;
    private String dealSupplierName;
    private BigDecimal dealAmount;
    private String dealRemark;
    private LocalDateTime dealTime;
    private Long contractId;
    private LocalDateTime createTime;
    private List<Line> lines;
    private List<SupplierScope> supplierScopes;
    private List<Quote> quotes;
    private ComparisonSummary comparisonSummary;
    private List<PriceTrend> priceTrends;

    @Data
    public static class Line {
        private Long id;
        private Long sourcePlanLineId;
        private String lineNo;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal quantity;
        private String unit;
        private LocalDate requiredDate;
    }

    @Data
    public static class SupplierScope {
        private Long id;
        private Long supplierId;
        private String supplierName;
    }

    @Data
    public static class Quote {
        private Long id;
        private Long supplierId;
        private String supplierName;
        private BigDecimal quoteAmount;
        private String quoteStatus;
        private String attachmentUrl;
        private String quotedName;
        private LocalDateTime quotedTime;
        private List<QuoteLine> lines;
    }

    @Data
    public static class QuoteLine {
        private Long id;
        private Long projectLineId;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal lineAmount;
    }

    @Data
    public static class ComparisonSummary {
        private Integer supplierQuoteCount;
        private BigDecimal lowestQuoteAmount;
        private Long lowestQuoteSupplierId;
        private String lowestQuoteSupplierName;
        private BigDecimal highestQuoteAmount;
        private BigDecimal averageQuoteAmount;
        private List<QuoteRanking> quoteRankings;
    }

    @Data
    public static class QuoteRanking {
        private Integer rankNo;
        private Long quoteId;
        private Long supplierId;
        private String supplierName;
        private BigDecimal quoteAmount;
        private LocalDateTime quotedTime;
    }

    @Data
    public static class PriceTrend {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private List<PriceTrendPoint> points;
    }

    @Data
    public static class PriceTrendPoint {
        private Long projectId;
        private String projectNo;
        private Long quoteId;
        private Long supplierId;
        private String supplierName;
        private BigDecimal unitPrice;
        private BigDecimal lineAmount;
        private LocalDateTime quotedTime;
    }
}
