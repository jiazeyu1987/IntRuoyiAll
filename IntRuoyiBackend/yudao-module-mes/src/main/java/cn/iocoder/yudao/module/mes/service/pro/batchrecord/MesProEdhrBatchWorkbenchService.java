package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchWorkbenchRespVO;

public interface MesProEdhrBatchWorkbenchService {

    EdhrBatchWorkbenchRespVO getWorkbench(Long batchExecutionId);
}
