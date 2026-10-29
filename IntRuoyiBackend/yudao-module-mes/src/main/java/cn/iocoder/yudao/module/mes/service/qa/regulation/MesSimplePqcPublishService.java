package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.*;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentBatchConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.pqc.MesPqcItemEquipmentConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_SNAPSHOT_INVALID;

@Service
@RequiredArgsConstructor
public class MesSimplePqcPublishService {
    private final MesQaInspectionRegulationService regulations;
    private final MesPqcItemEquipmentConfigService equipment;
    private final cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper machinery;

    @Transactional(rollbackFor = Exception.class)
    public MesQaInspectionRegulationPublishedVersionRespVO publish(MesQaInspectionRegulationSaveReqVO request) {
        if (request.getProcesses() == null || request.getProcesses().isEmpty()) {
            throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, "缺少检验项目");
        }
        Map<String, List<MesQaInspectionRegulationSaveReqVO.InspectionItem>> groups = new LinkedHashMap<>();
        for (var process : request.getProcesses()) {
            if (process == null || process.getItems() == null) throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, "工序缺少检验项目");
            for (var item : process.getItems()) {
                if (item == null || item.getItemName() == null) throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, "缺少项目名称");
                // ponytail: per-version codes isolate tenant-level equipment bindings from other projects and historical versions.
                item.setItemCode("PQC-" + UUID.randomUUID());
                groups.computeIfAbsent(item.getItemName().trim(), ignored -> new ArrayList<>()).add(item);
            }
        }
        Map<String, List<MesPqcItemEquipmentConfigSaveReqVO.EquipmentGroup>> bindings = new LinkedHashMap<>();
        for (var entry : groups.entrySet()) {
            var first = toGroups(entry.getValue().get(0));
            for (var item : entry.getValue()) {
                if (!first.equals(toGroups(item))) throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID,
                        "同名检验项目的设备选择必须一致，请确认：" + entry.getKey());
            }
            bindings.put(entry.getKey(), first);
        }
        var published = regulations.publish(request);
        for (var entry : groups.entrySet()) {
            var config = new MesPqcItemEquipmentBatchConfigSaveReqVO();
            config.setDccProjectCodeId(request.getDccProjectCodeId());
            config.setItemCode(entry.getValue().get(0).getItemCode());
            config.setItemCodes(entry.getValue().stream().map(MesQaInspectionRegulationSaveReqVO.InspectionItem::getItemCode).toList());
            config.setItemNameSnapshot(entry.getKey());
            config.setEquipmentGroups(bindings.get(entry.getKey()));
            equipment.replaceItemConfigs(config);
        }
        return published;
    }

    private List<MesPqcItemEquipmentConfigSaveReqVO.EquipmentGroup> toGroups(MesQaInspectionRegulationSaveReqVO.InspectionItem item) {
        Map<Long, MesPqcItemEquipmentConfigSaveReqVO.EquipmentGroup> groups = new LinkedHashMap<>();
        for (var option : item.getEquipmentOptions() == null ? List.<MesQaInspectionRegulationSaveReqVO.EquipmentOption>of() : item.getEquipmentOptions()) {
            if (option == null || option.getEquipmentId() == null || option.getEquipmentNumber() == null || option.getEquipmentNumber().isBlank()) {
                throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, "设备必须有编号");
            }
            var device = machinery.selectById(option.getEquipmentId());
            if (device == null) throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, "所选设备不存在");
            option.setEquipmentCode(device.getCode());
            option.setEquipmentName(device.getName());
            var group = groups.computeIfAbsent(option.getEquipmentId(), id -> {
                var value = new MesPqcItemEquipmentConfigSaveReqVO.EquipmentGroup();
                value.setEquipmentId(id); value.setEnabled(true); value.setDefaultFlag(Boolean.TRUE.equals(option.getDefaultFlag()));
                value.setSort(groups.size() + 1); value.setEquipmentNumbers(new ArrayList<>()); return value;
            });
            var number = new MesPqcItemEquipmentConfigSaveReqVO.EquipmentNumber();
            number.setEquipmentNumber(option.getEquipmentNumber().trim()); number.setEnabled(true); number.setSort(group.getEquipmentNumbers().size() + 1);
            group.getEquipmentNumbers().add(number);
        }
        return new ArrayList<>(groups.values());
    }
}
