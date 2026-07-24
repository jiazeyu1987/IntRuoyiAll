package cn.iocoder.yudao.module.mes.service.dv.machinery;

import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;

import java.util.Collection;
import java.util.List;

public interface MesDvMachineryProcessService {

    List<MesDvMachineryProcessDO> getMachineryProcessListByMachineryId(Long machineryId);

    List<MesDvMachineryProcessDO> getMachineryProcessListByMachineryIds(Collection<Long> machineryIds);

    List<MesDvMachineryProcessDO> getMachineryProcessListByMachineryIdsAndProcessIds(
            Collection<Long> machineryIds, Collection<Long> processIds);
}
