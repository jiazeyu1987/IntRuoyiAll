package cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("bpm_form_task_permission")
@KeySequence("bpm_form_task_permission_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormTaskPermissionDO extends BaseDO {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_REVOKED = "REVOKED";

    @TableId
    private Long id;

    private Long instanceId;

    private Long tenantId;

    private String bpmProcessInstanceId;

    private String taskId;

    private Long userId;

    private String permissionCodesJson;

    private String status;

}
