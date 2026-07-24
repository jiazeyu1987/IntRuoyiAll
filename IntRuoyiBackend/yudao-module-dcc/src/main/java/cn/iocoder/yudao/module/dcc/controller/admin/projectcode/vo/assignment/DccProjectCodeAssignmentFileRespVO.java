package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class DccProjectCodeAssignmentFileRespVO extends DccControlledFileRespVO {

    private Boolean metadataEditable;
    private Long metadataEditAssignmentId;
    private Integer changedFieldCount;
    private LocalDateTime lastChangedTime;

}
