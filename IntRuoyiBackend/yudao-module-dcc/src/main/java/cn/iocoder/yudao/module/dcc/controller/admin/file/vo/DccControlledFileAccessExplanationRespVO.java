package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

@Data
public class DccControlledFileAccessExplanationRespVO {

    private String detailSource;
    private String detailReason;
    private String detailDeniedReason;
    private String publishedPreviewSource;
    private String publishedPreviewReason;
    private String pendingPreviewSource;
    private String pendingPreviewReason;
    private String downloadSource;
    private String downloadReason;
    private String downloadDeniedReason;
}
