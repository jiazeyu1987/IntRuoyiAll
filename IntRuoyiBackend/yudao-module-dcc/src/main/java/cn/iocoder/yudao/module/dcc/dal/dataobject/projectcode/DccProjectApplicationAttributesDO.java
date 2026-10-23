package cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("dcc_project_application_attributes")
@Data @EqualsAndHashCode(callSuper = true)
public class DccProjectApplicationAttributesDO extends TenantBaseDO {
    @TableId @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long id;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long projectCodeId;
    private String applicationType;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long applicationId;
    private Integer applicationRound;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long sourceApplicationId;
    private Integer sourceApplicationRound;
    private String defaultSourceJson;
    private String actualAttributesJson;
    private Boolean submitted;
}
