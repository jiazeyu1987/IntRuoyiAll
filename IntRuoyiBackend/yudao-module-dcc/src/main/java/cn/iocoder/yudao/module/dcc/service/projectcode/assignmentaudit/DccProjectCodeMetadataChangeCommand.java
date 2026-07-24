package cn.iocoder.yudao.module.dcc.service.projectcode.assignmentaudit;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.service.projectcode.assignment.DccProjectCodeAssignmentAuthorization;

public record DccProjectCodeMetadataChangeCommand(Long operatorUserId,
                                                  DccProjectCodeAssignmentAuthorization authorization,
                                                  DccControlledFileDO beforeFile,
                                                  DccControlledFileDO afterFile,
                                                  String changeReason) {
}
