package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDingTalkImportExcelVO {

    @ExcelProperty("员工UserID")
    private String employeeUserId;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("工号")
    private String employeeNo;

    @ExcelProperty("部门主管")
    private String departmentManagerName;

    @ExcelProperty("1级部门")
    private String companyName;

    @ExcelProperty("主部门ID")
    private String sourceDepartmentId;

    @ExcelProperty("2级部门")
    private String level2DepartmentName;

    @ExcelProperty("3级部门")
    private String level3DepartmentName;

    @ExcelProperty("4级部门")
    private String level4DepartmentName;

    @ExcelProperty("5级部门")
    private String level5DepartmentName;

    @ExcelProperty("6级部门")
    private String level6DepartmentName;

    @ExcelProperty("7级部门")
    private String level7DepartmentName;

}
