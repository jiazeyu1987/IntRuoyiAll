package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.util.List;

public interface MesFrontlineProcessMaterialService {

    List<MesFrontlineProcessMaterial> listFrozenMaterials(Long activeOrderId, Long routeId,
                                                          Long routeProcessId, Long processId);
}
