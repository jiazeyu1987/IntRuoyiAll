package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resourceadjustment.MesProScheduleResourceAdjustmentSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProScheduleResourceAdjustmentDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProScheduleResourceAdjustmentMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_ROUTE_REQUIRED;

@Service
@Validated
public class MesProScheduleResourceAdjustmentServiceImpl implements MesProScheduleResourceAdjustmentService {

    @Resource
    private MesProScheduleResourceAdjustmentMapper adjustmentMapper;
    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProRouteProcessService routeProcessService;

    @Override
    public List<MesProScheduleResourceAdjustmentDO> getAdjustmentList(Long routeId, LocalDate calendarDate) {
        validateRouteAndProcess(routeId, null);
        return adjustmentMapper.selectListByRouteAndDate(routeId, calendarDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAdjustment(MesProScheduleResourceAdjustmentSaveReqVO saveReqVO) {
        MesProRouteProcessDO currentRouteProcess =
                validateRouteAndProcess(saveReqVO.getRouteId(), saveReqVO.getRouteProcessId());
        Long routeProcessId = currentRouteProcess == null ? saveReqVO.getRouteProcessId() : currentRouteProcess.getId();
        MesProScheduleResourceAdjustmentDO existing = adjustmentMapper.selectAdjustment(
                routeProcessId, saveReqVO.getCalendarDate(), saveReqVO.getResourceType(),
                saveReqVO.getWorkstationMachineId(), saveReqVO.getMachineryId());
        if (existing == null) {
            adjustmentMapper.insert(buildDO(saveReqVO, routeProcessId));
            return;
        }
        MesProScheduleResourceAdjustmentDO update = buildDO(saveReqVO, routeProcessId);
        update.setId(existing.getId());
        adjustmentMapper.updateById(update);
    }

    private MesProScheduleResourceAdjustmentDO buildDO(MesProScheduleResourceAdjustmentSaveReqVO saveReqVO,
                                                       Long routeProcessId) {
        return MesProScheduleResourceAdjustmentDO.builder()
                .routeId(saveReqVO.getRouteId())
                .routeProcessId(routeProcessId)
                .calendarDate(saveReqVO.getCalendarDate())
                .resourceType(saveReqVO.getResourceType())
                .workstationId(saveReqVO.getWorkstationId())
                .workstationMachineId(saveReqVO.getWorkstationMachineId())
                .machineryId(saveReqVO.getMachineryId())
                .availableQuantityOverride(saveReqVO.getAvailableQuantityOverride())
                .workerQuantityOverride(saveReqVO.getWorkerQuantityOverride())
                .singleHourlyCapacityOverride(saveReqVO.getSingleHourlyCapacityOverride())
                .shiftHoursOverride(saveReqVO.getShiftHoursOverride())
                .reason(saveReqVO.getReason())
                .build();
    }

    private MesProRouteProcessDO validateRouteAndProcess(Long routeId, Long routeProcessId) {
        MesProRouteDO route = routeMapper.selectById(routeId);
        if (route == null) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_ROUTE_REQUIRED);
        }
        if (routeProcessId == null) {
            return null;
        }
        MesProRouteProcessDO routeProcess = routeProcessService.resolveCurrentRouteProcess(routeProcessId, routeId, null);
        if (routeProcess == null || !routeId.equals(routeProcess.getRouteId())) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED);
        }
        return routeProcess;
    }

}
