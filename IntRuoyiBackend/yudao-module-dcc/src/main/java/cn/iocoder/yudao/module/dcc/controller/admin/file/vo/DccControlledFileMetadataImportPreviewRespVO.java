package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileMetadataImportPreviewRespVO {

    private Integer totalCount;
    private Integer updateCount;
    private Integer unchangedCount;
    private Integer failureCount;
    private List<DccControlledFileMetadataImportRowRespVO> rows;
}
