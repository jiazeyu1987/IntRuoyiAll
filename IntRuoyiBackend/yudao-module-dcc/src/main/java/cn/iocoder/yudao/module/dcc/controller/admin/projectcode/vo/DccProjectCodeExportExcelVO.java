package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccProjectCodeExportExcelVO {

    @ExcelProperty("文控")
    private String docControlNo;

    @ExcelProperty("项目名称")
    private String projectName;

    @ExcelProperty("项目代码")
    private String projectCode;

    @ExcelProperty("类别")
    private String category;

    @ExcelProperty("委托生产")
    private String commissionedProduction;

    @ExcelProperty("项目组负责人")
    private String projectLeader;

    @ExcelProperty("项目工程师")
    private String projectEngineer;

    @ExcelProperty("存放位置")
    private String storageLocation;

    @ExcelProperty("优先级")
    private String priority;

    public static DccProjectCodeExportExcelVO from(DccProjectCodeDO projectCode) {
        return DccProjectCodeExportExcelVO.builder()
                .docControlNo(projectCode.getDocControlNo())
                .projectName(projectCode.getProjectName())
                .projectCode(projectCode.getProjectCode())
                .category(projectCode.getCategory())
                .commissionedProduction(projectCode.getCommissionedProduction())
                .projectLeader(projectCode.getProjectLeader())
                .projectEngineer(projectCode.getProjectEngineer())
                .storageLocation(projectCode.getStorageLocation())
                .priority(projectCode.getPriority())
                .build();
    }
}
