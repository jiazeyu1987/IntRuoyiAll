package cn.iocoder.yudao.module.mes.service.md.workstation;

import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

public interface MesMdProductionLineService {

    MesMdProductionLineDO validateProductionLineExists(Long id);

    MesMdProductionLineDO validateProductionLineExistsAndEnable(Long id);

    MesMdProductionLineDO getProductionLine(Long id);

    List<MesMdProductionLineDO> getProductionLineList(Collection<Long> ids);

    default Map<Long, MesMdProductionLineDO> getProductionLineMap(Collection<Long> ids) {
        return convertMap(getProductionLineList(ids), MesMdProductionLineDO::getId);
    }

}
