package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.hutool.core.util.StrUtil;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileMetadataExportExcelVO {

    @ExcelProperty("受控文件ID")
    private Long controlledFileId;

    @ExcelProperty("文件名称")
    private String fileName;

    @ExcelProperty("文件编号")
    private String fileNumber;

    public static DccControlledFileMetadataExportExcelVO from(DccControlledFileDO file) {
        return DccControlledFileMetadataExportExcelVO.builder()
                .controlledFileId(file.getId())
                .fileName(StrUtil.trim(file.getFileName()))
                .fileNumber(StrUtil.trim(file.getFileNumber()))
                .build();
    }
}
