package cn.iocoder.yudao.module.mes.service.dv.machinery;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachinerySaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.type.MesDvMachineryTypeListReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryTypeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.enums.wm.BarcodeBizTypeEnum;
import cn.iocoder.yudao.module.mes.service.dv.checkplan.MesDvCheckPlanMachineryService;
import cn.iocoder.yudao.module.mes.service.dv.checkrecord.MesDvCheckRecordService;
import cn.iocoder.yudao.module.mes.service.dv.maintenrecord.MesDvMaintenRecordService;
import cn.iocoder.yudao.module.mes.service.dv.repair.MesDvRepairService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkshopService;
import cn.iocoder.yudao.module.mes.service.wm.barcode.MesWmBarcodeService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.DV_MACHINERY_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.DV_MACHINERY_HAS_CHECK_PLAN;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.DV_MACHINERY_HAS_CHECK_RECORD;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.DV_MACHINERY_HAS_MAINTEN_RECORD;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.DV_MACHINERY_HAS_REPAIR;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.DV_MACHINERY_IMPORT_LIST_IS_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.DV_MACHINERY_NOT_EXISTS;

/**
 * MES 设备台账 Service 实现类
 */
@Service
@Validated
public class MesDvMachineryServiceImpl implements MesDvMachineryService {

    @Resource
    private MesDvMachineryMapper machineryMapper;

    @Resource
    @Lazy
    private MesDvMachineryTypeService machineryTypeService;
    @Resource
    @Lazy
    private MesMdWorkshopService workshopService;
    @Resource
    private MesWmBarcodeService barcodeService;
    @Resource
    @Lazy
    private MesDvCheckPlanMachineryService checkPlanMachineryService;
    @Resource
    @Lazy
    private MesDvCheckRecordService checkRecordService;
    @Resource
    @Lazy
    private MesDvMaintenRecordService maintenRecordService;
    @Resource
    @Lazy
    private MesDvRepairService repairService;

    @Override
    public Long createMachinery(MesDvMachinerySaveReqVO createReqVO) {
        machineryTypeService.getMachineryType(createReqVO.getMachineryTypeId());
        workshopService.getWorkshop(createReqVO.getWorkshopId());
        validateMachineryCodeUnique(null, createReqVO.getCode());

        MesDvMachineryDO machinery = BeanUtils.toBean(createReqVO, MesDvMachineryDO.class);
        machineryMapper.insert(machinery);
        barcodeService.autoGenerateBarcode(BarcodeBizTypeEnum.MACHINERY.getValue(),
                machinery.getId(), machinery.getCode(), machinery.getName());
        return machinery.getId();
    }

    @Override
    public void updateMachinery(MesDvMachinerySaveReqVO updateReqVO) {
        validateMachineryExists(updateReqVO.getId());
        machineryTypeService.getMachineryType(updateReqVO.getMachineryTypeId());
        workshopService.getWorkshop(updateReqVO.getWorkshopId());
        validateMachineryCodeUnique(updateReqVO.getId(), updateReqVO.getCode());

        MesDvMachineryDO updateObj = BeanUtils.toBean(updateReqVO, MesDvMachineryDO.class);
        machineryMapper.updateById(updateObj);
    }

    @Override
    public void deleteMachinery(Long id) {
        validateMachineryExists(id);
        if (checkPlanMachineryService.getCheckPlanMachineryCountByMachineryId(id) > 0) {
            throw exception(DV_MACHINERY_HAS_CHECK_PLAN);
        }
        if (checkRecordService.getCheckRecordCountByMachineryId(id) > 0) {
            throw exception(DV_MACHINERY_HAS_CHECK_RECORD);
        }
        if (maintenRecordService.getMaintenRecordCountByMachineryId(id) > 0) {
            throw exception(DV_MACHINERY_HAS_MAINTEN_RECORD);
        }
        if (repairService.getRepairCountByMachineryId(id) > 0) {
            throw exception(DV_MACHINERY_HAS_REPAIR);
        }

        machineryMapper.deleteById(id);
    }

    @Override
    public void validateMachineryExists(Long id) {
        if (machineryMapper.selectById(id) == null) {
            throw exception(DV_MACHINERY_NOT_EXISTS);
        }
    }

    private void validateMachineryCodeUnique(Long id, String code) {
        if (code == null) {
            return;
        }
        MesDvMachineryDO machinery = machineryMapper.selectByCode(code);
        if (machinery == null) {
            return;
        }
        if (ObjUtil.notEqual(machinery.getId(), id)) {
            throw exception(DV_MACHINERY_CODE_DUPLICATE);
        }
    }

    @Override
    public MesDvMachineryDO getMachinery(Long id) {
        return machineryMapper.selectById(id);
    }

    @Override
    public PageResult<MesDvMachineryDO> getMachineryPage(MesDvMachineryPageReqVO pageReqVO) {
        if (pageReqVO.getMachineryTypeId() != null) {
            List<MesDvMachineryTypeDO> children = machineryTypeService.getMachineryTypeChildrenList(
                    pageReqVO.getMachineryTypeId());
            Set<Long> typeIds = new HashSet<>();
            typeIds.add(pageReqVO.getMachineryTypeId());
            children.forEach(child -> typeIds.add(child.getId()));
            pageReqVO.setMachineryTypeIds(typeIds);
        }
        return machineryMapper.selectPage(pageReqVO);
    }

    @Override
    public Long getMachineryCountByMachineryTypeId(Long machineryTypeId) {
        return machineryMapper.selectCountByMachineryTypeId(machineryTypeId);
    }

    @Override
    public void updateMachineryLastCheckTime(Long machineryId, LocalDateTime lastCheckTime) {
        machineryMapper.updateById(new MesDvMachineryDO().setId(machineryId).setLastCheckTime(lastCheckTime));
    }

    @Override
    public void updateMachineryLastMaintenTime(Long machineryId, LocalDateTime lastMaintenTime) {
        machineryMapper.updateById(new MesDvMachineryDO().setId(machineryId).setLastMaintenTime(lastMaintenTime));
    }

    @Override
    public List<MesDvMachineryDO> getMachineryList() {
        return machineryMapper.selectList();
    }

    @Override
    public List<MesDvMachineryDO> getMachineryList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return machineryMapper.selectByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesDvMachineryImportRespVO importMachineryList(List<MesDvMachineryImportExcelVO> importMachineryList,
                                                          boolean updateSupport) {
        if (CollUtil.isEmpty(importMachineryList)) {
            throw exception(DV_MACHINERY_IMPORT_LIST_IS_EMPTY);
        }

        List<MesDvMachineryTypeDO> allTypes = machineryTypeService.getMachineryTypeList(
                new MesDvMachineryTypeListReqVO());
        Map<String, MesDvMachineryTypeDO> typeCodeMap = allTypes.stream()
                .collect(Collectors.toMap(MesDvMachineryTypeDO::getCode, t -> t, (a, b) -> a));
        List<MesMdWorkshopDO> allWorkshops = workshopService.getWorkshopListByStatus(
                cn.iocoder.yudao.framework.common.enums.CommonStatusEnum.ENABLE.getStatus());
        Map<String, MesMdWorkshopDO> workshopCodeMap = allWorkshops.stream()
                .collect(Collectors.toMap(MesMdWorkshopDO::getCode, w -> w, (a, b) -> a));

        MesDvMachineryImportRespVO respVO = MesDvMachineryImportRespVO.builder()
                .createCodes(new ArrayList<>())
                .updateCodes(new ArrayList<>())
                .failureCodes(new LinkedHashMap<>())
                .build();
        AtomicInteger index = new AtomicInteger(1);
        importMachineryList.forEach(importItem -> {
            int currentIndex = index.getAndIncrement();
            String key = StrUtil.blankToDefault(importItem.getCode(), "第" + currentIndex + "行");
            if (StrUtil.isBlank(importItem.getCode())) {
                respVO.getFailureCodes().put(key, "设备编码不能为空");
                return;
            }
            if (StrUtil.isBlank(importItem.getName())) {
                respVO.getFailureCodes().put(key, "设备名称不能为空");
                return;
            }
            if (StrUtil.isBlank(importItem.getMachineryTypeCode())) {
                respVO.getFailureCodes().put(key, "设备类型编码不能为空");
                return;
            }
            MesDvMachineryTypeDO machineryType = typeCodeMap.get(importItem.getMachineryTypeCode());
            if (machineryType == null) {
                respVO.getFailureCodes().put(key,
                        "设备类型编码[" + importItem.getMachineryTypeCode() + "]不存在");
                return;
            }
            if (StrUtil.isBlank(importItem.getWorkshopCode())) {
                respVO.getFailureCodes().put(key, "车间编码不能为空");
                return;
            }
            MesMdWorkshopDO workshop = workshopCodeMap.get(importItem.getWorkshopCode());
            if (workshop == null) {
                respVO.getFailureCodes().put(key,
                        "车间编码[" + importItem.getWorkshopCode() + "]不存在");
                return;
            }
            if (importItem.getStandardHourlyCapacity() != null
                    && importItem.getStandardHourlyCapacity().compareTo(BigDecimal.ZERO) <= 0) {
                respVO.getFailureCodes().put(key, "设备标准小时产能必须大于 0");
                return;
            }

            MesDvMachineryDO existMachinery = machineryMapper.selectByCode(importItem.getCode());
            if (existMachinery == null) {
                MesDvMachineryDO machinery = BeanUtils.toBean(importItem, MesDvMachineryDO.class);
                machinery.setMachineryTypeId(machineryType.getId());
                machinery.setWorkshopId(workshop.getId());
                machineryMapper.insert(machinery);
                barcodeService.autoGenerateBarcode(BarcodeBizTypeEnum.MACHINERY.getValue(),
                        machinery.getId(), machinery.getCode(), machinery.getName());
                respVO.getCreateCodes().add(importItem.getCode());
                return;
            }
            if (!updateSupport) {
                respVO.getFailureCodes().put(key, "设备编码已存在");
                return;
            }

            MesDvMachineryDO updateObj = BeanUtils.toBean(importItem, MesDvMachineryDO.class);
            updateObj.setId(existMachinery.getId());
            updateObj.setMachineryTypeId(machineryType.getId());
            updateObj.setWorkshopId(workshop.getId());
            machineryMapper.updateById(updateObj);
            respVO.getUpdateCodes().add(importItem.getCode());
        });
        return respVO;
    }
}
