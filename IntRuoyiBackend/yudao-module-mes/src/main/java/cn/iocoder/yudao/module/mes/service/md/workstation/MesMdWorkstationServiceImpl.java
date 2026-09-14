package cn.iocoder.yudao.module.mes.service.md.workstation;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.MesMdWorkstationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.MesMdWorkstationSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseAreaDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseLocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.enums.wm.BarcodeBizTypeEnum;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.wm.barcode.MesWmBarcodeService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseAreaService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseLocationService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_WORKSHOP_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_WORKSTATION_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_WORKSTATION_IS_DISABLE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_WORKSTATION_NAME_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_WORKSTATION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_WORKSTATION_PRODUCTION_LINE_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.WM_WAREHOUSE_AREA_RELATION_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.WM_WAREHOUSE_LOCATION_RELATION_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.WM_WAREHOUSE_LOCATION_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.WM_WAREHOUSE_REQUIRED;

/**
 * MES 工作站 Service 实现类
 */
@Service
@Validated
public class MesMdWorkstationServiceImpl implements MesMdWorkstationService {

    @Resource
    private MesMdWorkstationMapper workstationMapper;

    @Resource
    @Lazy
    private MesMdWorkstationMachineService workstationMachineService;
    @Resource
    private MesMdWorkstationToolService workstationToolService;
    @Resource
    private MesMdWorkstationWorkerService workstationWorkerService;
    @Resource
    private MesMdWorkshopService workshopService;
    @Resource
    private MesMdProductionLineService productionLineService;
    @Resource
    private MesWmWarehouseService warehouseService;
    @Resource
    private MesWmWarehouseLocationService locationService;
    @Resource
    private MesWmWarehouseAreaService areaService;
    @Resource
    private MesWmBarcodeService barcodeService;
    @Resource
    @Lazy
    private MesProProcessService processService;
    @Resource
    private MesProRouteProcessService routeProcessService;

    @Override
    public Long createWorkstation(MesMdWorkstationSaveReqVO createReqVO) {
        validateWorkstationSaveData(null, createReqVO);

        MesMdWorkstationDO workstation = BeanUtils.toBean(createReqVO, MesMdWorkstationDO.class);
        workstationMapper.insert(workstation);
        barcodeService.autoGenerateBarcode(BarcodeBizTypeEnum.WORKSTATION.getValue(),
                workstation.getId(), workstation.getCode(), workstation.getName());
        return workstation.getId();
    }

    @Override
    public void updateWorkstation(MesMdWorkstationSaveReqVO updateReqVO) {
        validateWorkstationExists(updateReqVO.getId());
        validateWorkstationSaveData(updateReqVO.getId(), updateReqVO);

        MesMdWorkstationDO updateObj = BeanUtils.toBean(updateReqVO, MesMdWorkstationDO.class);
        workstationMapper.updateById(updateObj);
    }

    private void validateWorkstationSaveData(Long id, MesMdWorkstationSaveReqVO reqVO) {
        validateWorkstationCodeUnique(id, reqVO.getCode());
        validateWorkstationNameUnique(id, reqVO.getName());
        validateWorkshopExists(reqVO.getWorkshopId());
        processService.validateProcessExistsAndEnable(reqVO.getProcessId());
        validateProductionLine(reqVO.getWorkshopId(), reqVO.getProductionLineId());
        handleWarehouseHierarchy(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkstation(Long id) {
        validateWorkstationExists(id);

        workstationMachineService.deleteWorkstationMachineByWorkstationId(id);
        workstationToolService.deleteWorkstationToolByWorkstationId(id);
        workstationWorkerService.deleteWorkstationWorkerByWorkstationId(id);
        workstationMapper.deleteById(id);
    }

    @Override
    public MesMdWorkstationDO validateWorkstationExists(Long id) {
        MesMdWorkstationDO workstation = workstationMapper.selectById(id);
        if (workstation == null) {
            throw exception(MD_WORKSTATION_NOT_EXISTS);
        }
        return workstation;
    }

    @Override
    public MesMdWorkstationDO validateWorkstationExistsAndEnable(Long id) {
        MesMdWorkstationDO workstation = validateWorkstationExists(id);
        if (ObjUtil.notEqual(CommonStatusEnum.ENABLE.getStatus(), workstation.getStatus())) {
            throw exception(MD_WORKSTATION_IS_DISABLE);
        }
        return workstation;
    }

    private void validateWorkshopExists(Long workshopId) {
        MesMdWorkshopDO workshop = workshopService.getWorkshop(workshopId);
        if (workshop == null) {
            throw exception(MD_WORKSHOP_NOT_EXISTS);
        }
    }

    private void validateProductionLine(Long workshopId, Long productionLineId) {
        if (productionLineId == null) {
            return;
        }
        MesMdProductionLineDO line = productionLineService.validateProductionLineExistsAndEnable(productionLineId);
        if (ObjUtil.notEqual(line.getWorkshopId(), workshopId)) {
            throw exception(MD_WORKSTATION_PRODUCTION_LINE_MISMATCH);
        }
    }

    private void handleWarehouseHierarchy(MesMdWorkstationSaveReqVO reqVO) {
        Long warehouseId = reqVO.getWarehouseId();
        Long locationId = reqVO.getLocationId();
        Long areaId = reqVO.getAreaId();
        if (warehouseId == null && locationId == null && areaId == null) {
            return;
        }

        if (warehouseId != null) {
            warehouseService.validateWarehouseExists(warehouseId);
        }

        if (locationId != null) {
            MesWmWarehouseLocationDO location = locationService.validateWarehouseLocationExists(locationId);
            if (warehouseId == null) {
                throw exception(WM_WAREHOUSE_REQUIRED);
            }
            if (ObjUtil.notEqual(location.getWarehouseId(), warehouseId)) {
                throw exception(WM_WAREHOUSE_LOCATION_RELATION_INVALID);
            }
        }

        if (areaId == null) {
            return;
        }
        MesWmWarehouseAreaDO area = areaService.validateWarehouseAreaExists(areaId);
        if (locationId == null) {
            throw exception(WM_WAREHOUSE_LOCATION_REQUIRED);
        }
        if (ObjUtil.notEqual(area.getLocationId(), locationId)) {
            throw exception(WM_WAREHOUSE_AREA_RELATION_INVALID);
        }
    }

    private void validateWorkstationCodeUnique(Long id, String code) {
        MesMdWorkstationDO workstation = workstationMapper.selectByCode(code);
        if (workstation == null) {
            return;
        }
        if (ObjUtil.notEqual(workstation.getId(), id)) {
            throw exception(MD_WORKSTATION_CODE_DUPLICATE);
        }
    }

    private void validateWorkstationNameUnique(Long id, String name) {
        MesMdWorkstationDO workstation = workstationMapper.selectByName(name);
        if (workstation == null) {
            return;
        }
        if (ObjUtil.notEqual(workstation.getId(), id)) {
            throw exception(MD_WORKSTATION_NAME_DUPLICATE);
        }
    }

    @Override
    public MesMdWorkstationDO getWorkstation(Long id) {
        return workstationMapper.selectById(id);
    }

    @Override
    public PageResult<MesMdWorkstationDO> getWorkstationPage(MesMdWorkstationPageReqVO pageReqVO) {
        return workstationMapper.selectPage(pageReqVO);
    }

    @Override
    public List<MesMdWorkstationDO> getWorkstationListByStatus(Integer status) {
        return workstationMapper.selectListByStatus(status);
    }

    @Override
    public Long getWorkstationCountByWorkshopId(Long workshopId) {
        return workstationMapper.selectCountByWorkshopId(workshopId);
    }

    @Override
    public Long getWorkstationCountByWarehouseId(Long warehouseId) {
        return workstationMapper.selectCountByWarehouseId(warehouseId);
    }

    @Override
    public Long getWorkstationCountByLocationId(Long locationId) {
        return workstationMapper.selectCountByLocationId(locationId);
    }

    @Override
    public Long getWorkstationCountByAreaId(Long areaId) {
        return workstationMapper.selectCountByAreaId(areaId);
    }

    @Override
    public List<MesMdWorkstationDO> getWorkstationList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return workstationMapper.selectByIds(ids);
    }

    @Override
    public List<MesMdWorkstationDO> getWorkstationListByProcessIds(Collection<Long> processIds) {
        return getWorkstationListByProcessIds(processIds, null);
    }

    @Override
    public List<MesMdWorkstationDO> getWorkstationListByProcessIds(Collection<Long> processIds, Integer status) {
        if (CollUtil.isEmpty(processIds)) {
            return Collections.emptyList();
        }
        Map<Long, Long> identityMap = routeProcessService.getProcessIdentityMap(processIds);
        if (identityMap.isEmpty()) {
            return Collections.emptyList();
        }
        List<MesMdWorkstationDO> workstations =
                workstationMapper.selectListByProcessIds(identityMap.keySet(), status);
        workstations.forEach(workstation -> workstation.setProcessId(identityMap.get(workstation.getProcessId())));
        return workstations;
    }
}
