package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrPermissionRuleCommand {

    private String subjectType;

    private Long subjectId;

    private String ability;

    private String decision;

    private Integer priority;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    private String status;
}
