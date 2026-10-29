package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 用户分页 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserPageReqVO extends PageParam {

    @Schema(description = "用户账号，模糊匹配", example = "yudao")
    private String username;

    @Schema(description = "手机号码，模糊匹配", example = "yudao")
    private String mobile;

    @Schema(description = "展示状态，参见 CommonStatusEnum 枚举类", example = "1")
    private Integer status;

    @Schema(description = "创建时间", example = "[2022-07-01 00:00:00, 2022-07-01 23:59:59]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @Schema(description = "部门编号，同时筛选子部门", example = "1024")
    private Long deptId;

    @Schema(description = "多个部门编号，部门间取并集，是否包含子部门由 includeChildDepts 决定")
    private java.util.List<@jakarta.validation.constraints.Positive Long> deptIds;
    @Schema(description = "所选部门内人员范围：ALL 全部，MEMBER 组员，LEADER 部门主管")
    @jakarta.validation.constraints.Pattern(regexp = "ALL|MEMBER|LEADER")
    private String deptLeaderMode;

    @jakarta.validation.constraints.AssertTrue(message = "请选择部门后再筛选组员或主管")
    public boolean isDepartmentMemberFilterValid() {
        return deptLeaderMode == null || "ALL".equals(deptLeaderMode)
                || deptId != null || (deptIds != null && !deptIds.isEmpty());
    }

    @Schema(description = "角色编号", example = "1024")
    private Long roleId;
    @Schema(description = "用户昵称，模糊匹配")
    private String nickname;
    @Schema(description = "岗位编号")
    private Long postId;
    @Schema(description = "是否包含子部门，默认包含")
    private Boolean includeChildDepts;
    @Schema(description = "固定部门范围（含子部门），与筛选部门取交集")
    private Long scopeDeptId;
    private Long scopeRoleId;
    private Long scopePostId;
    private java.util.List<Long> ids;

}
