package cn.iocoder.yudao.module.dcc.service.file.relations;

import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.Notification;

/** Platform adapter MUST enforce businessKey uniqueness and return the persisted platform message id. */
public interface DccRelationNotificationSender {
    Long send(Notification notification);
}
