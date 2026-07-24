package cn.iocoder.yudao.module.mes.service.md.workstation;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.machine.MesMdWorkstationMachineSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMachineMapper;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_WORKSTATION_MACHINE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_WORKSTATION_MACHINE_NOT_EXISTS;

/**
 * MES 设备资源 Service 实现类
 */
@Service
@Validated
public class MesMdWorkstationMachineServiceImpl implements MesMdWorkstationMachineService {

    @Resource
    private MesMdWorkstationMachineMapper workstationMachineMapper;

    @Resource
    @Lazy
    private MesMdWorkstationService workstationService;
    @Resource
    private MesDvMachineryService machineryService;

    @Override
    public Long createWorkstationMachine(MesMdWorkstationMachineSaveReqVO createReqVO) {
        validateWorkstationMachineSaveData(createReqVO);

        MesMdWorkstationMachineDO machine = BeanUtils.toBean(createReqVO, MesMdWorkstationMachineDO.class);
        workstationMachineMapper.insert(machine);
        return machine.getId();
    }

    private void validateWorkstationMachineSaveData(MesMdWorkstationMachineSaveReqVO reqVO) {
        workstationService.validateWorkstationExists(reqVO.getWorkstationId());
        machineryService.validateMachineryExists(reqVO.getMachineryId());
        MesMdWorkstationMachineDO existing = workstationMachineMapper
                .selectByWorkstationIdAndMachineryId(reqVO.getWorkstationId(), reqVO.getMachineryId());
        if (existing != null) {
            throw exception(MD_WORKSTATION_MACHINE_EXISTS, reqVO.getWorkstationId());
        }
    }

    @Override
    public void deleteWorkstationMachine(Long id) {
        if (workstationMachineMapper.selectById(id) == null) {
            throw exception(MD_WORKSTATION_MACHINE_NOT_EXISTS);
        }
        workstationMachineMapper.deleteById(id);
    }

    @Override
    public List<MesMdWorkstationMachineDO> getWorkstationMachineListByWorkstationId(Long workstationId) {
        return workstationMachineMapper.selectListByWorkstationId(workstationId);
    }

    @Override
    public List<MesMdWorkstationMachineDO> getWorkstationMachineListByWorkstationIds(Collection<Long> workstationIds) {
        if (workstationIds == null || workstationIds.isEmpty()) {
            return Collections.emptyList();
        }
        return workstationMachineMapper.selectListByWorkstationIds(workstationIds);
    }

    @Override
    public void deleteWorkstationMachineByWorkstationId(Long workstationId) {
        workstationMachineMapper.deleteByWorkstationId(workstationId);
    }
}
