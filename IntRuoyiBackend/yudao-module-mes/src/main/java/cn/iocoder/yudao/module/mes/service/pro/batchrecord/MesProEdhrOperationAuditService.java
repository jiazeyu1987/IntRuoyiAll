package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOperationAuditPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOperationAuditRespVO;

public interface MesProEdhrOperationAuditService {

    MesProEdhrOperationAuditRespVO record(MesProEdhrOperationAuditCommand command);

    MesProEdhrOperationAuditRespVO recordInCallerTransaction(MesProEdhrOperationAuditCommand command);

    PageResult<MesProEdhrOperationAuditRespVO> getPage(MesProEdhrOperationAuditPageReqVO reqVO);

    MesProEdhrOperationAuditRespVO get(Long id);
}
