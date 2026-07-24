package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProEdhrInitIssuePageReqVO extends PageParam {

    @NotNull(message = "初始化批次不能为空")
    private Long initBatchId;

    private String issueLevel;

    private String issueStatus;

    private String packageType;

    private String sourceFileName;

    private String responsibleName;
}
