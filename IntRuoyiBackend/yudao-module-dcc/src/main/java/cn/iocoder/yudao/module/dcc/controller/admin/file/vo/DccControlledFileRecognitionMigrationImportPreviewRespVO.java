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
public class DccControlledFileRecognitionMigrationImportPreviewRespVO {

    private Integer totalCount;
    private Integer applicableCount;
    private Integer blockedCount;
    private Integer failedRecognitionCount;
    private Integer appliedCount;
    private List<DccControlledFileRecognitionMigrationImportRowRespVO> rows;
}
