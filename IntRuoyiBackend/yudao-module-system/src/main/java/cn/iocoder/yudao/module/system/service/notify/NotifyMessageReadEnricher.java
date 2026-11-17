package cn.iocoder.yudao.module.system.service.notify;

import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyMessageDO;

/** Domain-specific read projection. Implementations must never persist changes. */
public interface NotifyMessageReadEnricher {
    void enrich(NotifyMessageDO message);
}
