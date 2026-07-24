package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileMetadataImportRowRespVO {

    private Integer rowNo;
    private Long controlledFileId;
    private String fileName;
    private String fileNumber;
    private String importAction;
    private String failureReason;
}
