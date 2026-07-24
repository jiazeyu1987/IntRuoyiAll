package cn.iocoder.yudao.module.mes.service.dv.machinery;

import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryProcessMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Validated
public class MesDvMachineryProcessServiceImpl implements MesDvMachineryProcessService {

    @Resource
    private MesDvMachineryProcessMapper machineryProcessMapper;
    @Resource
    private MesProRouteProcessService routeProcessService;

    @Override
    public List<MesDvMachineryProcessDO> getMachineryProcessListByMachineryId(Long machineryId) {
        return machineryProcessMapper.selectListByMachineryId(machineryId);
    }

    @Override
    public List<MesDvMachineryProcessDO> getMachineryProcessListByMachineryIds(Collection<Long> machineryIds) {
        if (machineryIds == null || machineryIds.isEmpty()) {
            return Collections.emptyList();
        }
        return machineryProcessMapper.selectListByMachineryIds(machineryIds);
    }

    @Override
    public List<MesDvMachineryProcessDO> getMachineryProcessListByMachineryIdsAndProcessIds(
            Collection<Long> machineryIds, Collection<Long> processIds) {
        if (machineryIds == null || machineryIds.isEmpty()
                || processIds == null || processIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Long> identityMap = routeProcessService.getProcessIdentityMap(processIds);
        if (identityMap.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, SelectedMachineryProcess> selectedByIdentity = new LinkedHashMap<>();
        for (MesDvMachineryProcessDO row : machineryProcessMapper.selectListByMachineryIds(machineryIds)) {
            Long canonicalProcessId = identityMap.get(row.getProcessId());
            if (canonicalProcessId == null) {
                continue;
            }
            String key = buildMachineryProcessKey(row.getMachineryId(), canonicalProcessId);
            SelectedMachineryProcess selected = new SelectedMachineryProcess(row, canonicalProcessId,
                    Objects.equals(row.getProcessId(), canonicalProcessId));
            selectedByIdentity.merge(key, selected, this::pickMachineryProcessCapacity);
        }
        return selectedByIdentity.values().stream()
                .map(selected -> {
                    selected.row().setProcessId(selected.canonicalProcessId());
                    return selected.row();
                })
                .toList();
    }

    private SelectedMachineryProcess pickMachineryProcessCapacity(SelectedMachineryProcess existing,
                                                                  SelectedMachineryProcess current) {
        if (existing.explicitTarget() && !current.explicitTarget()) {
            return existing;
        }
        if (!existing.explicitTarget() && current.explicitTarget()) {
            return current;
        }
        BigDecimal existingCapacity = existing.row().getStandardHourlyCapacity();
        BigDecimal currentCapacity = current.row().getStandardHourlyCapacity();
        if (existingCapacity == null) {
            return current;
        }
        if (currentCapacity == null || existingCapacity.compareTo(currentCapacity) == 0) {
            return existing;
        }
        throw new IllegalStateException(String.format("设备工序产能存在冲突: machineryId=%s, processId=%s",
                current.row().getMachineryId(), current.canonicalProcessId()));
    }

    private String buildMachineryProcessKey(Long machineryId, Long processId) {
        return machineryId + ":" + processId;
    }

    private record SelectedMachineryProcess(MesDvMachineryProcessDO row, Long canonicalProcessId,
                                            boolean explicitTarget) {
    }
}
