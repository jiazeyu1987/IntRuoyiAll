package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

@Data
public class DccControlledFileRelatedFileRespVO {

    private Long controlledFileId;
    private Long masterId;
    private Long projectCodeId;
    private String fileNumber;
    private String fileName;
    private String versionNo;
    private String status;

}
