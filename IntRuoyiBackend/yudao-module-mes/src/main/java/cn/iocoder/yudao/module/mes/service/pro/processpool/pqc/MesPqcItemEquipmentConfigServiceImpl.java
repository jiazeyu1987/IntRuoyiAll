package cn.iocoder.yudao.module.mes.service.pro.processpool.pqc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentItemRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcItemEquipmentConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcItemEquipmentNumberConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcItemEquipmentConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcItemEquipmentNumberConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID;

@Service
@Validated
public class MesPqcItemEquipmentConfigServiceImpl implements MesPqcItemEquipmentConfigService {

    private final MesPqcItemEquipmentConfigMapper configMapper;
    private final MesPqcItemEquipmentNumberConfigMapper numberConfigMapper;
    private final MesQaInspectionRegulationItemMapper regulationItemMapper;
    private final MesDvMachineryService machineryService;

    public MesPqcItemEquipmentConfigServiceImpl(MesPqcItemEquipmentConfigMapper configMapper,
                                                MesPqcItemEquipmentNumberConfigMapper numberConfigMapper,
                                                MesQaInspectionRegulationItemMapper regulationItemMapper,
                                                MesDvMachineryService machineryService) {
        this.configMapper = configMapper;
        this.numberConfigMapper = numberConfigMapper;
        this.regulationItemMapper = regulationItemMapper;
        this.machineryService = machineryService;
    }

    @Override
    public List<MesPqcItemEquipmentItemRespVO> listConfigurableItems() {
        Map<String, MesQaInspectionRegulationItemDO> itemByCode = loadConfigurableItemMap();
        return itemByCode.values().stream()
                .map(item -> new MesPqcItemEquipmentItemRespVO()
                        .setItemCode(item.getItemCode())
                        .setItemName(item.getItemName())
                        .setInspectionMethod(item.getInspectionMethod())
                        .setStandardText(item.getStandardText())
                        .setSamplingPlanText(item.getSamplingPlanText()))
                .toList();
    }

    @Override
    public MesPqcItemEquipmentConfigRespVO getItemConfig(String itemCode) {
        String normalizedItemCode = normalizeItemCode(itemCode);
        MesQaInspectionRegulationItemDO item = requireConfigurableItem(normalizedItemCode);
        List<MesPqcItemEquipmentConfigDO> configs = configMapper.selectListByItemCode(normalizedItemCode);
        Map<Long, List<MesPqcItemEquipmentNumberConfigDO>> numbersByConfigId = loadNumbersByConfigId(configs, false);
        return toConfigRespVO(item.getItemCode(), item.getItemName(), configs, numbersByConfigId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesPqcItemEquipmentConfigRespVO replaceItemConfig(MesPqcItemEquipmentConfigSaveReqVO reqVO) {
        if (reqVO == null) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID, "itemEquipmentConfig.request");
        }
        String itemCode = normalizeItemCode(reqVO.getItemCode());
        MesQaInspectionRegulationItemDO item = requireConfigurableItem(itemCode);
        List<MesPqcItemEquipmentConfigSaveReqVO.EquipmentGroup> groups =
                reqVO.getEquipmentGroups() == null ? List.of() : reqVO.getEquipmentGroups();
        validateConfigGroups(groups);

        Long tenantId = TenantContextHolder.getRequiredTenantId();
        numberConfigMapper.physicalDeleteByTenantIdAndItemCode(tenantId, itemCode);
        configMapper.physicalDeleteByTenantIdAndItemCode(tenantId, itemCode);

        if (groups.isEmpty()) {
            return toConfigRespVO(itemCode, item.getItemName(), List.of(), Map.of());
        }

        Set<Long> equipmentIds = groups.stream()
                .map(MesPqcItemEquipmentConfigSaveReqVO.EquipmentGroup::getEquipmentId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesDvMachineryDO> machineryMap = machineryService.getMachineryMap(equipmentIds);
        List<MesPqcItemEquipmentConfigDO> configs = new ArrayList<>();
        int groupIndex = 0;
        for (MesPqcItemEquipmentConfigSaveReqVO.EquipmentGroup group : groups) {
            MesDvMachineryDO machinery = machineryMap.get(group.getEquipmentId());
            if (machinery == null || StrUtil.hasBlank(machinery.getCode(), machinery.getName())) {
                throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                        itemCode + ".equipmentId=" + group.getEquipmentId());
            }
            configs.add(MesPqcItemEquipmentConfigDO.builder()
                    .itemCode(itemCode)
                    .itemNameSnapshot(StrUtil.blankToDefault(reqVO.getItemNameSnapshot(), item.getItemName()))
                    .equipmentId(machinery.getId())
                    .equipmentCode(machinery.getCode())
                    .equipmentName(machinery.getName())
                    .enabled(group.getEnabled() == null || Boolean.TRUE.equals(group.getEnabled()))
                    .defaultFlag(Boolean.TRUE.equals(group.getDefaultFlag()))
                    .sort(group.getSort() == null ? groupIndex : group.getSort())
                    .build());
            groupIndex += 1;
        }
        if (!Boolean.TRUE.equals(configMapper.insertBatch(configs))) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID, itemCode + ".equipmentConfig");
        }

        List<MesPqcItemEquipmentNumberConfigDO> numbers = new ArrayList<>();
        for (int index = 0; index < groups.size(); index += 1) {
            MesPqcItemEquipmentConfigSaveReqVO.EquipmentGroup group = groups.get(index);
            MesPqcItemEquipmentConfigDO config = configs.get(index);
            int numberIndex = 0;
            for (MesPqcItemEquipmentConfigSaveReqVO.EquipmentNumber number : group.getEquipmentNumbers()) {
                numbers.add(MesPqcItemEquipmentNumberConfigDO.builder()
                        .configId(config.getId())
                        .itemCode(itemCode)
                        .equipmentId(config.getEquipmentId())
                        .equipmentNumber(normalizeEquipmentNumber(number.getEquipmentNumber()))
                        .enabled(number.getEnabled() == null || Boolean.TRUE.equals(number.getEnabled()))
                        .sort(number.getSort() == null ? numberIndex : number.getSort())
                        .build());
                numberIndex += 1;
            }
        }
        if (!numbers.isEmpty() && !Boolean.TRUE.equals(numberConfigMapper.insertBatch(numbers))) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID, itemCode + ".equipmentNumberConfig");
        }
        return getItemConfig(itemCode);
    }

    @Override
    public Map<String, List<MesPqcItemEquipmentOption>> listEnabledEquipmentOptionsByItemCodes(
            Collection<String> itemCodes) {
        Set<String> normalizedCodes = itemCodes == null ? Set.of() : itemCodes.stream()
                .filter(StrUtil::isNotBlank)
                .map(this::normalizeItemCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedCodes.isEmpty()) {
            return Map.of();
        }
        List<MesPqcItemEquipmentConfigDO> configs = configMapper.selectEnabledListByItemCodes(normalizedCodes);
        if (configs.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<MesPqcItemEquipmentNumberConfigDO>> numbersByConfigId = loadNumbersByConfigId(configs, true);
        Map<String, List<MesPqcItemEquipmentOption>> optionsByItemCode = new LinkedHashMap<>();
        for (MesPqcItemEquipmentConfigDO config : configs) {
            List<MesPqcItemEquipmentNumberConfigDO> numbers =
                    numbersByConfigId.getOrDefault(config.getId(), List.of());
            for (MesPqcItemEquipmentNumberConfigDO number : numbers) {
                optionsByItemCode.computeIfAbsent(config.getItemCode(), ignored -> new ArrayList<>())
                        .add(new MesPqcItemEquipmentOption(config.getItemCode(), config.getEquipmentId(),
                                config.getEquipmentCode(), config.getEquipmentName(),
                                number.getEquipmentNumber(), config.getDefaultFlag(), number.getSort()));
            }
        }
        return optionsByItemCode.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .sorted(Comparator.comparing(MesPqcItemEquipmentOption::sort,
                                                Comparator.nullsLast(Integer::compareTo))
                                        .thenComparing(MesPqcItemEquipmentOption::equipmentId)
                                        .thenComparing(MesPqcItemEquipmentOption::equipmentNumber))
                                .toList(),
                        (left, right) -> left, LinkedHashMap::new));
    }

    private Map<String, MesQaInspectionRegulationItemDO> loadConfigurableItemMap() {
        List<MesQaInspectionRegulationItemDO> rows = regulationItemMapper.selectList(
                new LambdaQueryWrapperX<MesQaInspectionRegulationItemDO>()
                        .orderByAsc(MesQaInspectionRegulationItemDO::getItemCode)
                        .orderByAsc(MesQaInspectionRegulationItemDO::getItemSort)
                        .orderByAsc(MesQaInspectionRegulationItemDO::getId));
        Map<String, MesQaInspectionRegulationItemDO> itemByCode = new LinkedHashMap<>();
        for (MesQaInspectionRegulationItemDO row : rows) {
            if (row != null && StrUtil.isNotBlank(row.getItemCode())) {
                itemByCode.putIfAbsent(row.getItemCode().trim(), row);
            }
        }
        return itemByCode;
    }

    private MesQaInspectionRegulationItemDO requireConfigurableItem(String itemCode) {
        MesQaInspectionRegulationItemDO item = loadConfigurableItemMap().get(itemCode);
        if (item == null) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                    "itemEquipmentConfig.itemCode=" + itemCode);
        }
        return item;
    }

    private void validateConfigGroups(List<MesPqcItemEquipmentConfigSaveReqVO.EquipmentGroup> groups) {
        Set<Long> equipmentIds = new LinkedHashSet<>();
        for (MesPqcItemEquipmentConfigSaveReqVO.EquipmentGroup group : groups) {
            if (group == null || group.getEquipmentId() == null || group.getEquipmentId() <= 0
                    || !equipmentIds.add(group.getEquipmentId())
                    || CollUtil.isEmpty(group.getEquipmentNumbers())) {
                throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID, "equipmentGroups");
            }
            Set<String> numbers = new LinkedHashSet<>();
            for (MesPqcItemEquipmentConfigSaveReqVO.EquipmentNumber number : group.getEquipmentNumbers()) {
                String normalizedNumber = number == null ? null : normalizeEquipmentNumber(number.getEquipmentNumber());
                if (StrUtil.isBlank(normalizedNumber) || !numbers.add(normalizedNumber)) {
                    throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID, "equipmentNumbers");
                }
            }
        }
    }

    private Map<Long, List<MesPqcItemEquipmentNumberConfigDO>> loadNumbersByConfigId(
            List<MesPqcItemEquipmentConfigDO> configs, boolean onlyEnabled) {
        if (CollUtil.isEmpty(configs)) {
            return Map.of();
        }
        List<Long> configIds = configs.stream()
                .map(MesPqcItemEquipmentConfigDO::getId)
                .filter(Objects::nonNull)
                .toList();
        List<MesPqcItemEquipmentNumberConfigDO> numbers = onlyEnabled
                ? numberConfigMapper.selectEnabledListByConfigIds(configIds)
                : numberConfigMapper.selectListByConfigIds(configIds);
        return numbers.stream()
                .collect(Collectors.groupingBy(MesPqcItemEquipmentNumberConfigDO::getConfigId,
                        LinkedHashMap::new, Collectors.toList()));
    }

    private MesPqcItemEquipmentConfigRespVO toConfigRespVO(
            String itemCode,
            String itemName,
            List<MesPqcItemEquipmentConfigDO> configs,
            Map<Long, List<MesPqcItemEquipmentNumberConfigDO>> numbersByConfigId) {
        MesPqcItemEquipmentConfigRespVO respVO = new MesPqcItemEquipmentConfigRespVO();
        respVO.setItemCode(itemCode);
        respVO.setItemName(itemName);
        respVO.setEquipmentGroups(configs.stream()
                .map(config -> toGroupRespVO(config, numbersByConfigId.getOrDefault(config.getId(), List.of())))
                .toList());
        return respVO;
    }

    private MesPqcItemEquipmentConfigRespVO.EquipmentGroup toGroupRespVO(
            MesPqcItemEquipmentConfigDO config,
            List<MesPqcItemEquipmentNumberConfigDO> numbers) {
        MesPqcItemEquipmentConfigRespVO.EquipmentGroup group =
                new MesPqcItemEquipmentConfigRespVO.EquipmentGroup();
        group.setId(config.getId());
        group.setEquipmentId(config.getEquipmentId());
        group.setEquipmentCode(config.getEquipmentCode());
        group.setEquipmentName(config.getEquipmentName());
        group.setEnabled(config.getEnabled());
        group.setDefaultFlag(config.getDefaultFlag());
        group.setSort(config.getSort());
        group.setEquipmentNumbers(numbers.stream()
                .map(number -> new MesPqcItemEquipmentConfigRespVO.EquipmentNumber()
                        .setId(number.getId())
                        .setEquipmentNumber(number.getEquipmentNumber())
                        .setEnabled(number.getEnabled())
                        .setSort(number.getSort()))
                .toList());
        return group;
    }

    private String normalizeItemCode(String itemCode) {
        if (StrUtil.isBlank(itemCode)) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID, "itemCode");
        }
        return itemCode.trim();
    }

    private String normalizeEquipmentNumber(String equipmentNumber) {
        if (StrUtil.isBlank(equipmentNumber)) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID, "equipmentNumber");
        }
        return equipmentNumber.trim();
    }
}
