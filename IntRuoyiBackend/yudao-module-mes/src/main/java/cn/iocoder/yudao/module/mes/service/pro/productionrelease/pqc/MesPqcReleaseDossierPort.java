package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;

public interface MesPqcReleaseDossierPort {

    MesFlow6CompletionBackfillReceipt readCompletionReceipt(Long receiptId, Long tenantId);
}
