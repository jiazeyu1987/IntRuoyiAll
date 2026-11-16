package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.*;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentBatchConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.pqc.MesPqcItemEquipmentConfigService;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
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
    private final MesQaInspectionRegulationMapper regulationMapper;
    private static final Set<String> SENSORY_TOOLS = Set.of("目测", "手感", "目测、手感", "/", "无");

    @Transactional(rollbackFor = Exception.class)
    public MesQaInspectionRegulationPublishedVersionRespVO publish(MesQaInspectionRegulationSaveReqVO request) {
        if (request.getProcesses() == null || request.getProcesses().isEmpty()) {
            throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, "缺少检验项目");
        }
        Map<String, List<MesQaInspectionRegulationSaveReqVO.InspectionItem>> groups = new LinkedHashMap<>();
        MesQaInspectionRegulationPublishedVersionRespVO legacy = null;
        boolean legacyLoaded = false;
        for (var process : request.getProcesses()) {
            if (process == null || process.getItems() == null) throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, "工序缺少检验项目");
            for (var item : process.getItems()) {
                if (item == null || item.getItemName() == null) throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, "缺少项目名称");
                boolean bound = item.getEquipmentOptions() != null && !item.getEquipmentOptions().isEmpty();
                boolean hasReason = item.getEquipmentUnboundReason() != null && !item.getEquipmentUnboundReason().isBlank();
                if (Boolean.FALSE.equals(item.getEquipmentRequired()) && (bound || hasReason)) {
                    throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, "无需设备时不得保留设备绑定或待绑定原因：" + item.getItemName());
                }
                if (bound && hasReason) {
                    throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID,
                            "已绑定设备时请清空器具未绑定原因：" + item.getItemName());
                }
                if (!Boolean.FALSE.equals(item.getEquipmentRequired()) && !bound && !hasReason && !SENSORY_TOOLS.contains(Objects.toString(item.getInspectionTool(), "").trim())) {
                    if (!legacyLoaded) {
                        var old = regulationMapper.selectByDccProjectCodeId(request.getDccProjectCodeId(),
                                MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA);
                        if (old != null && Objects.equals(old.getDccProjectCodeId(), request.getDccProjectCodeId())
                                && Objects.equals(old.getOwnerModule(), MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA)) {
                            legacy = regulations.getPublishedVersion(request.getDccProjectCodeId(), null);
                            if (legacy != null && !Objects.equals(legacy.getRegulationId(), old.getId())) {
                                throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, "既有QA归属不一致");
                            }
                        }
                        legacyLoaded = true;
                    }
                    if (!isLegacyUnbound(legacy, request.getDccProjectCodeId(), process.getProcessName(), item)) {
                        throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID,
                                "请填写器具未绑定原因：" + item.getItemName());
                    }
                }
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

    private boolean isLegacyUnbound(MesQaInspectionRegulationPublishedVersionRespVO legacy, Long projectId,
                                    String processName, MesQaInspectionRegulationSaveReqVO.InspectionItem item) {
        if (legacy == null || !Objects.equals(legacy.getDccProjectCodeId(), projectId)
                || !Objects.equals(legacy.getLifecycleStatus(), "PUBLISHED") || legacy.getProcesses() == null) return false;
        var processes = legacy.getProcesses().stream()
                .filter(p -> Objects.equals(p.getProcessName(), processName)).toList();
        if (processes.size() != 1 || processes.get(0).getItems() == null) return false;
        var items = processes.get(0).getItems().stream()
                .filter(i -> Objects.equals(i.getItemName(), item.getItemName())).toList();
        if (items.size() != 1) return false;
        var old = items.get(0);
        if (!Objects.equals(old.getInspectionTool(), item.getInspectionTool())
                || (old.getEquipmentUnboundReason() != null && !old.getEquipmentUnboundReason().isBlank())
                || (old.getEquipmentOptions() != null && !old.getEquipmentOptions().isEmpty())) return false;
        // 只接受服务端既有正式 QA 的旧空绑定；客户端确认标记不能免除新待绑定原因。
        return equipment.listEnabledEquipmentOptionsByProjectVersionAndItemCodes(projectId,
                legacy.getPublishedVersionId(), List.of(old.getItemCode()))
                .getOrDefault(old.getItemCode(), List.of()).isEmpty();
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
