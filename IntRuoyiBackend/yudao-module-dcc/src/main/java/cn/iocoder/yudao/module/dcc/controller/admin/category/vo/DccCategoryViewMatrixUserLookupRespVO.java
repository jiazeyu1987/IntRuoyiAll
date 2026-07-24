package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import lombok.Data;

import java.util.List;

@Data
public class DccCategoryViewMatrixUserLookupRespVO {

    private Long categoryId;
    private String code;
    private String name;
    private String browseStatus;
    private String browseSource;
    private String browseReason;
    private String detailStatus;
    private String detailSource;
    private String detailReason;
    private String publishedPreviewStatus;
    private String publishedPreviewSource;
    private String publishedPreviewReason;
    private String pendingPreviewStatus;
    private String pendingPreviewSource;
    private String pendingPreviewReason;
    private String downloadStatus;
    private String downloadSource;
    private String downloadReason;
    private List<DccCategoryViewMatrixRowRespVO.Subject> viewSources;
    private List<DccCategoryViewMatrixRowRespVO.Risk> risks;

}
