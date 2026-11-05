package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.MesMdWorkstationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.machine.MesMdWorkstationMachineSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryProcessMapper;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkshopService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;

import static cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters.invalid;

/** 创建流程时的工作站及设备资源随流程事务一起提交。 */
@Service
public class MesProRouteWorkstationService {
    @Resource private MesMdWorkshopService workshopService;
    @Resource @Lazy private MesMdWorkstationService workstationService;
    @Resource private MesMdWorkstationMachineService machineService;
    @Resource private MesDvMachineryProcessMapper machineryProcessMapper;

    public MesMdWorkshopDO requireWorkshop(Long workshopId) {
        if (workshopId == null || workshopId <= 0) throw invalid("请选择工作站所属车间");
        MesMdWorkshopDO workshop = workshopService.getWorkshop(workshopId);
        if (workshop == null || !CommonStatusEnum.ENABLE.getStatus().equals(workshop.getStatus())) {
            throw invalid("所选车间不存在或已停用，请重新选择");
        }
        return workshop;
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public Long create(Long routeId, int sort, MesProProcessDO process, Long workshopId) {
        requireWorkshop(workshopId);
        String suffix = routeId + "-" + sort;
        MesMdWorkstationSaveReqVO request = new MesMdWorkstationSaveReqVO();
        request.setCode("WS-" + suffix);
        // 仅缩短生成名称中的描述部分，完整工序名称仍保留在关联工序中。
        request.setName(StrUtil.sub(process.getName(), 0, 90) + "工作站-" + suffix);
        request.setWorkshopId(workshopId);
        request.setProcessId(process.getId());
        request.setStatus(CommonStatusEnum.ENABLE.getStatus());
        Long workstationId = workstationService.createWorkstation(request);
        bindMachinery(workstationId, machineryProcessMapper.selectListByProcessId(process.getId()).stream()
                .map(binding -> binding.getMachineryId()).toList());
        return workstationId;
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void bindMachinery(Long workstationId, Collection<Long> machineryIds) {
        if (workstationId == null || workstationId <= 0) throw invalid("工序工作站尚未创建");
        var existingIds = new LinkedHashSet<Long>();
        machineService.getWorkstationMachineListByWorkstationId(workstationId)
                .forEach(binding -> existingIds.add(binding.getMachineryId()));
        for (Long machineryId : new LinkedHashSet<>(machineryIds)) {
            if (machineryId == null || machineryId <= 0) throw invalid("工序设备关联缺少设备台账编号");
            if (!existingIds.add(machineryId)) continue;
            MesMdWorkstationMachineSaveReqVO request = new MesMdWorkstationMachineSaveReqVO();
            request.setWorkstationId(workstationId);
            request.setMachineryId(machineryId);
            request.setQuantity(1);
            machineService.createWorkstationMachine(request);
        }
    }
}
