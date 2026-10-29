package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;
import lombok.Data;
import lombok.EqualsAndHashCode;
/** 人员选择只返回精简身份信息及状态。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserSelectionRespVO extends UserSimpleRespVO {
    private Integer status;
}
