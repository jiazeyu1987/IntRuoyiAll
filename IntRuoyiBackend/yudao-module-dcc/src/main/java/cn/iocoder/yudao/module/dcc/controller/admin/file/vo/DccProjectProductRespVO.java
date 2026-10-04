package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DccProjectProductRespVO {
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long projectCodeId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long productMasterId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long productCatalogId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long productRelationId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long productCreateRequestId;
    private String productCode;
    private String productName;
    private String source;
}
