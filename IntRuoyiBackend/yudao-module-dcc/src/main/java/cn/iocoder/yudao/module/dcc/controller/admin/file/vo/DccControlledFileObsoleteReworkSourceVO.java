package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/** Real terminated source choice; selection does not authorize writes against this old form. */
@Data
public class DccControlledFileObsoleteReworkSourceVO {
    @JsonFormat(shape=JsonFormat.Shape.STRING) private Long reworkSourceFormInstanceId;
    private String reworkSourceFormInstanceCode;
    private String reworkSourceBpmProcessInstanceId;
    private Integer reworkSourceAttributeRound;
    private String reworkSourceDigest;
    private String reworkSourceStatus;
    private String reworkSourceVersionNo;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
    private String reason;
    private boolean available;
    private String unavailableReason;
}
