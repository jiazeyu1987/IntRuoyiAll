package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.system.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "管理后台 - 用户信息 Response VO")
@Data
@ExcelIgnoreUnannotated
public class UserRespVO{

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("用户编号")
    private Long id;

    @Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "yudao")
    @ExcelProperty("用户名称")
    private String username;

    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
    @ExcelProperty("用户昵称")
    private String nickname;

    @Schema(description = "备注", example = "我是一个用户")
    private String remark;

    @Schema(description = "部门ID", example = "我是一个用户")
    private Long deptId;
    @Schema(description = "部门名称", example = "IT 部")
    @ExcelProperty("部门名称")
    private String deptName;

    @Schema(description = "岗位编号数组", example = "1")
    private Set<Long> postIds;

    @Schema(description = "用户邮箱", example = "yudao@iocoder.cn")
    @ExcelProperty("用户邮箱")
    private String email;

    @Schema(description = "手机号码", example = "15601691300")
    @ExcelProperty("手机号码")
    private String mobile;

    @Schema(description = "用户性别，参见 SexEnum 枚举类", example = "1")
    @ExcelProperty(value = "用户性别", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.USER_SEX)
    private Integer sex;

    @Schema(description = "用户头像", example = "https://www.iocoder.cn/xxx.png")
    private String avatar;

    @Schema(description = "状态，参见 CommonStatusEnum 枚举类", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "帐号状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.COMMON_STATUS)
    private Integer status;

    @Schema(description = "登录失败次数", example = "3")
    @ExcelProperty("登录失败次数")
    private Integer loginFailureCount;

    @Schema(description = "登录锁定状态，0-未锁定，1-锁定", example = "1")
    @ExcelProperty("登录锁定状态")
    private Integer loginLocked;

    @Schema(description = "登录锁定时间", example = "时间戳格式")
    @ExcelProperty("登录锁定时间")
    private LocalDateTime loginLockedTime;

    @Schema(description = "账号生命周期单据类型，RESIGNATION-离职单，TRANSFER-转岗单")
    @ExcelProperty("账号生命周期单据类型")
    private String lifecycleDocumentType;

    @Schema(description = "离职/转岗单号")
    @ExcelProperty("离职/转岗单号")
    private String lifecycleDocumentNo;

    @Schema(description = "离职/转岗单据时间", example = "时间戳格式")
    @ExcelProperty("离职/转岗单据时间")
    private LocalDateTime lifecycleDocumentTime;

    @Schema(description = "账号停用生效时间", example = "时间戳格式")
    @ExcelProperty("账号停用生效时间")
    private LocalDateTime lifecycleEffectiveTime;

    @Schema(description = "账号联动停用时间", example = "时间戳格式")
    @ExcelProperty("账号联动停用时间")
    private LocalDateTime lifecycleDeactivatedTime;

    @Schema(description = "最后登录 IP", requiredMode = Schema.RequiredMode.REQUIRED, example = "192.168.1.1")
    @ExcelProperty("最后登录IP")
    private String loginIp;

    @Schema(description = "最后登录时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "时间戳格式")
    @ExcelProperty("最后登录时间")
    private LocalDateTime loginDate;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "时间戳格式")
    private LocalDateTime createTime;

}
