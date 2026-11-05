package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.idev.excel.annotation.ExcelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 一人一行；班组长和用工类型分别表达，禁止由姓名猜测角色。 */
@Data
public class MesTeamRosterRow {
    @ExcelProperty("车间")
    @NotBlank(message = "车间不能为空")
    private String workshop;
    @ExcelProperty("组员名称")
    @NotBlank(message = "组员名称不能为空")
    private String name;
    @ExcelProperty("用工类型")
    private String employmentType;
    @ExcelProperty("人员角色")
    @NotBlank(message = "人员角色不能为空")
    private String role;
    @ExcelProperty("用户账号")
    private String username;
}
