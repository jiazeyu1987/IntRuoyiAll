package cn.iocoder.yudao.module.mes.service.pro.dccprojectgovernance;

import java.util.List;

public interface MesProDccProjectGovernanceService {

    List<MesProDccProjectGovernanceStatus> getStatus(List<String> projectNames);
}
