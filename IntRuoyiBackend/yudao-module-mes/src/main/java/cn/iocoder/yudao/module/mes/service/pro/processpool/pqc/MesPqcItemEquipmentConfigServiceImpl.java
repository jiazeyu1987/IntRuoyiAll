package cn.iocoder.yudao.module.mes.service.pro.processpool.pqc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentBatchConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentItemRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcItemEquipmentConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcItemEquipmentNumberConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcItemEquipmentConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcItemEquipmentNumberConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
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
    private final MesQaInspectionRegulationVersionMapper regulationVersionMapper;
    private final MesQaInspectionRegulationMapper regulationMapper;
    private final DccProjectCodeMapper dccProjectCodeMapper;
    private final MesDvMachineryService machineryService;

    public MesPqcItemEquipmentConfigServiceImpl(MesPqcItemEquipmentConfigMapper configMapper,
                                                MesPqcItemEquipmentNumberConfigMapper numberConfigMapper,
                                                MesQaInspectionRegulationItemMapper regulationItemMapper,
                                                MesQaInspectionRegulationVersionMapper regulationVersionMapper,
                                                MesQaInspectionRegulationMapper regulationMapper,
                                                DccProjectCodeMapper dccProjectCodeMapper,
                                                MesDvMachineryService machineryService) {
        this.configMapper = configMapper;
        this.numberConfigMapper = numberConfigMapper;
        this.regulationItemMapper = regulationItemMapper;
        this.regulationVersionMapper = regulationVersionMapper;
        this.regulationMapper = regulationMapper;
        this.dccProjectCodeMapper = dccProjectCodeMapper;
        this.machineryService = machineryService;
    }

    @Override
    public List<MesPqcItemEquipmentItemRespVO> listConfigurableItems(Long dccProjectCodeId) {
        if (dccProjectCodeId == null || dccProjectCodeId <= 0) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID, "itemEquipmentConfig.dccProjectCodeId");
        }
        Map<String, ConfigurableItem> itemByCode = loadConfigurableItemMap(dccProjectCodeId);
        return itemByCode.values().stream()
                .collect(Collectors.groupingBy(ConfigurableItem::itemName,
                        LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(items -> {
                    ConfigurableItem item = items.get(0);
                    return new MesPqcItemEquipmentItemRespVO()
                            .setDccProjectCodeId(item.dccProjectCodeId())
                            .setItemCode(item.itemCode())
                            .setItemCodes(items.stream().map(ConfigurableItem::itemCode).toList())
                            .setProjectName(item.projectName())
                            .setItemName(item.itemName())
                            .setInspectionMethod(item.inspectionMethod())
                            .setStandardText(item.standardText())
                            .setSamplingPlanText(item.samplingPlanText());
                })
                .toList();
    }

    @Override
    public MesPqcItemEquipmentConfigRespVO getItemConfig(String itemCode) {
        String normalizedItemCode = normalizeItemCode(itemCode);
        ConfigurableItem item = requireConfigurableItem(normalizedItemCode);
        List<MesPqcItemEquipmentConfigDO> configs = configMapper.selectListByItemCode(normalizedItemCode);
        Map<Long, List<MesPqcItemEquipmentNumberConfigDO>> numbersByConfigId = loadNumbersByConfigId(configs, false);
        return toConfigRespVO(item.itemCode(), List.of(item.itemCode()), item.itemName(), configs, numbersByConfigId)
                .setConfigurationConsistent(true);
    }

    @Override
    public MesPqcItemEquipmentConfigRespVO getItemConfig(Long dccProjectCodeId, Collection<String> itemCodes) {
        if (dccProjectCodeId == null || dccProjectCodeId <= 0) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID, "itemEquipmentConfig.dccProjectCodeId");
        }
        List<String> normalizedCodes = normalizeItemCodes(itemCodes);
        Map<String, ConfigurableItem> itemMap = loadConfigurableItemMap(dccProjectCodeId);
        List<ConfigurableItem> items = normalizedCodes.stream()
                .map(code -> itemMap.get(code))
                .toList();
        if (items.stream().anyMatch(Objects::isNull)) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                    "itemEquipmentConfig.itemCodes.projectMismatch=" + normalizedCodes);
        }
        ConfigurableItem first = items.get(0);
        if (items.stream().anyMatch(item -> !Objects.equals(first.itemName(), item.itemName()))) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                    "itemEquipmentConfig.itemCodes.itemNameMismatch=" + normalizedCodes);
        }
        requireCompleteSameNameItemCodes(itemMap, first.itemName(), normalizedCodes);
        List<MesPqcItemEquipmentConfigDO> referenceConfigs = List.of();
        String referenceSignature = null;
        boolean consistent = true;
        for (String itemCode : normalizedCodes) {
            List<MesPqcItemEquipmentConfigDO> configs = configMapper.selectListByItemCode(itemCode);
            String signature = configurationSignature(configs);
            if (referenceSignature == null) {
                referenceSignature = signature;
            } else if (!Objects.equals(referenceSignature, signature)) {
                consistent = false;
            }
            if (referenceConfigs.isEmpty() && !configs.isEmpty()) {
                referenceConfigs = configs;
            }
        }
        Map<Long, List<MesPqcItemEquipmentNumberConfigDO>> numbersByConfigId =
                loadNumbersByConfigId(referenceConfigs, false);
        return toConfigRespVO(first.itemCode(), normalizedCodes, first.itemName(), referenceConfigs,
                numbersByConfigId).setConfigurationConsistent(consistent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesPqcItemEquipmentConfigRespVO replaceItemConfig(MesPqcItemEquipmentConfigSaveReqVO reqVO) {
        if (reqVO == null) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID, "itemEquipmentConfig.request");
        }
        String itemCode = normalizeItemCode(reqVO.getItemCode());
        ConfigurableItem item = requireConfigurableItem(itemCode);
        List<MesPqcItemEquipmentConfigSaveReqVO.EquipmentGroup> groups =
                reqVO.getEquipmentGroups() == null ? List.of() : reqVO.getEquipmentGroups();
        validateConfigGroups(groups);

        replaceSingleItemConfig(itemCode, item, reqVO.getItemNameSnapshot(), groups);
        return getItemConfig(itemCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesPqcItemEquipmentConfigRespVO replaceItemConfigs(MesPqcItemEquipmentBatchConfigSaveReqVO reqVO) {
        if (reqVO == null || reqVO.getDccProjectCodeId() == null || reqVO.getDccProjectCodeId() <= 0) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID, "itemEquipmentConfig.request");
        }
        List<String> itemCodes = normalizeItemCodes(reqVO.getItemCodes());
        Map<String, ConfigurableItem> itemMap = loadConfigurableItemMap(reqVO.getDccProjectCodeId());
        List<ConfigurableItem> items = itemCodes.stream().map(itemMap::get).toList();
        if (items.stream().anyMatch(Objects::isNull)) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                    "itemEquipmentConfig.itemCodes.projectMismatch=" + itemCodes);
        }
        ConfigurableItem first = items.get(0);
        if (items.stream().anyMatch(item -> !Objects.equals(first.itemName(), item.itemName()))) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                    "itemEquipmentConfig.itemCodes.itemNameMismatch=" + itemCodes);
        }
        requireCompleteSameNameItemCodes(itemMap, first.itemName(), itemCodes);
        List<MesPqcItemEquipmentConfigSaveReqVO.EquipmentGroup> groups =
                reqVO.getEquipmentGroups() == null ? List.of() : reqVO.getEquipmentGroups();
        validateConfigGroups(groups);
        for (ConfigurableItem item : items) {
            replaceSingleItemConfig(item.itemCode(), item, reqVO.getItemNameSnapshot(), groups);
        }
        return getItemConfig(reqVO.getDccProjectCodeId(), itemCodes);
    }

    private void replaceSingleItemConfig(String itemCode, ConfigurableItem item, String itemNameSnapshot,
                                         List<MesPqcItemEquipmentConfigSaveReqVO.EquipmentGroup> groups) {

        Long tenantId = TenantContextHolder.getRequiredTenantId();
        numberConfigMapper.physicalDeleteByTenantIdAndItemCode(tenantId, itemCode);
        configMapper.physicalDeleteByTenantIdAndItemCode(tenantId, itemCode);

        if (groups.isEmpty()) {
            return;
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
            if (group.getEquipmentNumbers() == null || group.getEquipmentNumbers().size() != 1
                    || !Objects.equals(normalizeEquipmentNumber(
                    group.getEquipmentNumbers().get(0).getEquipmentNumber()), StrUtil.trim(machinery.getCode()))) {
                throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                        itemCode + ".equipmentNumberMustMatchLedgerCode");
            }
            configs.add(MesPqcItemEquipmentConfigDO.builder()
                    .itemCode(itemCode)
                    .itemNameSnapshot(StrUtil.blankToDefault(itemNameSnapshot, item.itemName()))
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

    @Override
    public Map<String, List<MesPqcItemEquipmentOption>> listEnabledEquipmentOptionsByProjectAndItemCodes(
            Long dccProjectCodeId, Collection<String> itemCodes) {
        return listEnabledEquipmentOptionsByProjectVersionAndItemCodes(dccProjectCodeId, null, itemCodes);
    }

    @Override
    public Map<String, List<MesPqcItemEquipmentOption>> listEnabledEquipmentOptionsByProjectVersionAndItemCodes(
            Long dccProjectCodeId, Long regulationVersionId, Collection<String> itemCodes) {
        if (dccProjectCodeId == null || dccProjectCodeId <= 0) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                    "itemEquipmentConfig.dccProjectCodeId");
        }
        List<String> normalizedCodes = normalizeItemCodes(itemCodes);
        if (normalizedCodes.isEmpty()) {
            return Map.of();
        }
        Map<String, ConfigurableItem> itemMap = regulationVersionId == null
                ? loadConfigurableItemMap(dccProjectCodeId)
                : loadConfigurableItemMap(dccProjectCodeId, regulationVersionId);
        if (normalizedCodes.stream().anyMatch(code -> !itemMap.containsKey(code))) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                    "itemEquipmentConfig.itemCodes.projectMismatch=" + normalizedCodes);
        }
        Map<String, List<MesPqcItemEquipmentOption>> directOptions =
                listEnabledEquipmentOptionsByItemCodes(normalizedCodes);
        Map<String, List<String>> codesByItemName = itemMap.values().stream()
                .filter(item -> normalizedCodes.contains(item.itemCode()))
                .collect(Collectors.groupingBy(ConfigurableItem::itemName,
                        LinkedHashMap::new,
                        Collectors.mapping(ConfigurableItem::itemCode, Collectors.toList())));
        Map<String, List<MesPqcItemEquipmentOption>> expanded = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : codesByItemName.entrySet()) {
            List<List<MesPqcItemEquipmentOption>> configuredOptions = entry.getValue().stream()
                    .map(directOptions::get)
                    .filter(CollUtil::isNotEmpty)
                    .distinct()
                    .toList();
            if (configuredOptions.size() > 1) {
                throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                        "itemEquipmentConfig.itemName.inconsistent=" + entry.getKey());
            }
            if (configuredOptions.isEmpty()) {
                continue;
            }
            entry.getValue().forEach(code -> expanded.put(code, configuredOptions.get(0)));
        }
        return expanded;
    }

    private Map<String, ConfigurableItem> loadConfigurableItemMap() {
        return loadConfigurableItemMap(null);
    }

    private Map<String, ConfigurableItem> loadConfigurableItemMap(Long dccProjectCodeId) {
        List<MesQaInspectionRegulationItemDO> rows;
        if (dccProjectCodeId == null) {
            rows = regulationItemMapper.selectList(
                    new LambdaQueryWrapperX<MesQaInspectionRegulationItemDO>()
                            .orderByAsc(MesQaInspectionRegulationItemDO::getItemCode)
                            .orderByAsc(MesQaInspectionRegulationItemDO::getItemSort)
                            .orderByAsc(MesQaInspectionRegulationItemDO::getId));
        } else {
            MesQaInspectionRegulationDO regulation =
                    regulationMapper.selectByDccProjectCodeId(dccProjectCodeId);
            if (regulation == null || regulation.getId() == null) {
                return Map.of();
            }
            MesQaInspectionRegulationVersionDO version =
                    regulationVersionMapper.selectLatestDraftByRegulationId(regulation.getId());
            if (version == null) {
                version = regulationVersionMapper.selectLatestPublishedByRegulationId(regulation.getId());
            }
            rows = version == null ? List.of() : regulationItemMapper.selectListByVersionId(version.getId());
        }
        Map<Long, MesQaInspectionRegulationVersionDO> versionById = loadVersionsById(rows);
        Map<Long, MesQaInspectionRegulationDO> regulationById = loadRegulationsById(versionById.values());
        Map<Long, DccProjectCodeDO> projectById = loadProjectsById(regulationById.values());
        Map<String, ConfigurableItem> itemByCode = new LinkedHashMap<>();
        for (MesQaInspectionRegulationItemDO row : rows) {
            if (row != null && StrUtil.isNotBlank(row.getItemCode())) {
                if (dccProjectCodeId != null) {
                    MesQaInspectionRegulationVersionDO version = versionById.get(row.getRegulationVersionId());
                    MesQaInspectionRegulationDO regulation = version == null
                            ? null : regulationById.get(version.getRegulationId());
                    if (regulation == null
                            || !Objects.equals(regulation.getDccProjectCodeId(), dccProjectCodeId)) {
                        continue;
                    }
                }
                ConfigurableItem configurableItem = toConfigurableItem(row, versionById, regulationById, projectById);
                if (dccProjectCodeId != null
                        && !Objects.equals(configurableItem.dccProjectCodeId(), dccProjectCodeId)) {
                    continue;
                }
                ConfigurableItem existing = itemByCode.putIfAbsent(configurableItem.itemCode(), configurableItem);
                if (existing != null && (!Objects.equals(existing.projectName(), configurableItem.projectName())
                        || !Objects.equals(existing.itemName(), configurableItem.itemName()))) {
                    throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                            "itemEquipmentConfig.itemCode.displayNameAmbiguous=" + configurableItem.itemCode());
                }
            }
        }
        return itemByCode;
    }

    private Map<String, ConfigurableItem> loadConfigurableItemMap(Long dccProjectCodeId, Long regulationVersionId) {
        if (regulationVersionId == null || regulationVersionId <= 0) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                    "itemEquipmentConfig.regulationVersionId=" + regulationVersionId);
        }
        MesQaInspectionRegulationVersionDO version = regulationVersionMapper.selectById(regulationVersionId);
        if (version == null || version.getRegulationId() == null) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                    "itemEquipmentConfig.regulationVersionId=" + regulationVersionId);
        }
        MesQaInspectionRegulationDO regulation = regulationMapper.selectById(version.getRegulationId());
        if (regulation == null || !Objects.equals(regulation.getDccProjectCodeId(), dccProjectCodeId)) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                    "itemEquipmentConfig.dccProjectCodeId=" + dccProjectCodeId);
        }
        List<MesQaInspectionRegulationItemDO> rows = regulationItemMapper.selectListByVersionId(regulationVersionId);
        Map<Long, MesQaInspectionRegulationVersionDO> versionById = Map.of(version.getId(), version);
        Map<Long, MesQaInspectionRegulationDO> regulationById = Map.of(regulation.getId(), regulation);
        Map<Long, DccProjectCodeDO> projectById = loadProjectsById(List.of(regulation));
        Map<String, ConfigurableItem> itemByCode = new LinkedHashMap<>();
        for (MesQaInspectionRegulationItemDO row : rows) {
            if (row != null && StrUtil.isNotBlank(row.getItemCode())) {
                ConfigurableItem configurableItem = toConfigurableItem(row, versionById, regulationById, projectById);
                ConfigurableItem existing = itemByCode.putIfAbsent(configurableItem.itemCode(), configurableItem);
                if (existing != null && (!Objects.equals(existing.projectName(), configurableItem.projectName())
                        || !Objects.equals(existing.itemName(), configurableItem.itemName()))) {
                    throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                            "itemEquipmentConfig.itemCode.displayNameAmbiguous=" + configurableItem.itemCode());
                }
            }
        }
        return itemByCode;
    }

    private Map<Long, MesQaInspectionRegulationVersionDO> loadVersionsById(
            List<MesQaInspectionRegulationItemDO> rows) {
        Set<Long> versionIds = rows.stream()
                .filter(Objects::nonNull)
                .map(MesQaInspectionRegulationItemDO::getRegulationVersionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return regulationVersionMapper.selectList(MesQaInspectionRegulationVersionDO::getId, versionIds).stream()
                .collect(Collectors.toMap(MesQaInspectionRegulationVersionDO::getId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, MesQaInspectionRegulationDO> loadRegulationsById(
            Collection<MesQaInspectionRegulationVersionDO> versions) {
        Set<Long> regulationIds = versions.stream()
                .filter(Objects::nonNull)
                .map(MesQaInspectionRegulationVersionDO::getRegulationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return regulationMapper.selectList(MesQaInspectionRegulationDO::getId, regulationIds).stream()
                .collect(Collectors.toMap(MesQaInspectionRegulationDO::getId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, DccProjectCodeDO> loadProjectsById(
            Collection<MesQaInspectionRegulationDO> regulations) {
        Set<Long> projectIds = regulations.stream()
                .filter(Objects::nonNull)
                .map(MesQaInspectionRegulationDO::getDccProjectCodeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return dccProjectCodeMapper.selectList(DccProjectCodeDO::getId, projectIds).stream()
                .collect(Collectors.toMap(DccProjectCodeDO::getId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
    }

    private ConfigurableItem toConfigurableItem(
            MesQaInspectionRegulationItemDO row,
            Map<Long, MesQaInspectionRegulationVersionDO> versionById,
            Map<Long, MesQaInspectionRegulationDO> regulationById,
            Map<Long, DccProjectCodeDO> projectById) {
        MesQaInspectionRegulationVersionDO version = versionById.get(row.getRegulationVersionId());
        if (version == null) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                    "itemEquipmentConfig.regulationVersionId=" + row.getRegulationVersionId());
        }
        MesQaInspectionRegulationDO regulation = regulationById.get(version.getRegulationId());
        if (regulation == null) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                    "itemEquipmentConfig.regulationId=" + version.getRegulationId());
        }
        DccProjectCodeDO project = projectById.get(regulation.getDccProjectCodeId());
        if (project == null) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                    "itemEquipmentConfig.dccProjectCodeId=" + regulation.getDccProjectCodeId());
        }
        String itemCode = row.getItemCode().trim();
        return new ConfigurableItem(regulation.getDccProjectCodeId(), itemCode,
                requireDisplayName(project.getProjectName(), "itemEquipmentConfig.projectName=" + itemCode),
                requireDisplayName(row.getItemName(), "itemEquipmentConfig.itemName=" + itemCode),
                row.getInspectionMethod(), row.getStandardText(), row.getSamplingPlanText());
    }

    private ConfigurableItem requireConfigurableItem(String itemCode) {
        ConfigurableItem item = loadConfigurableItemMap().get(itemCode);
        if (item == null) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                    "itemEquipmentConfig.itemCode=" + itemCode);
        }
        return item;
    }

    private String requireDisplayName(String displayName, String field) {
        if (StrUtil.isBlank(displayName)) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID, field);
        }
        return displayName.trim();
    }

    private void requireCompleteSameNameItemCodes(Map<String, ConfigurableItem> itemMap, String itemName,
                                                  Collection<String> submittedCodes) {
        Set<String> expectedCodes = itemMap.values().stream()
                .filter(item -> Objects.equals(itemName, item.itemName()))
                .map(ConfigurableItem::itemCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!expectedCodes.equals(new LinkedHashSet<>(submittedCodes))) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID,
                    "itemEquipmentConfig.itemCodes.incomplete=" + submittedCodes);
        }
    }

    private record ConfigurableItem(
            Long dccProjectCodeId,
            String itemCode,
            String projectName,
            String itemName,
            String inspectionMethod,
            String standardText,
            String samplingPlanText) {
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
            List<String> itemCodes,
            String itemName,
            List<MesPqcItemEquipmentConfigDO> configs,
            Map<Long, List<MesPqcItemEquipmentNumberConfigDO>> numbersByConfigId) {
        MesPqcItemEquipmentConfigRespVO respVO = new MesPqcItemEquipmentConfigRespVO();
        respVO.setItemCode(itemCode);
        respVO.setItemCodes(itemCodes);
        respVO.setItemName(itemName);
        respVO.setEquipmentGroups(configs.stream()
                .map(config -> toGroupRespVO(config, numbersByConfigId.getOrDefault(config.getId(), List.of())))
                .toList());
        return respVO;
    }

    private String configurationSignature(List<MesPqcItemEquipmentConfigDO> configs) {
        Map<Long, List<MesPqcItemEquipmentNumberConfigDO>> numbersByConfigId =
                loadNumbersByConfigId(configs, false);
        StringBuilder signature = new StringBuilder();
        for (MesPqcItemEquipmentConfigDO config : configs) {
            signature.append(config.getEquipmentId()).append('|')
                    .append(config.getEquipmentCode()).append('|')
                    .append(config.getEquipmentName()).append('|')
                    .append(config.getEnabled()).append('|')
                    .append(config.getDefaultFlag()).append('|')
                    .append(config.getSort()).append(':');
            for (MesPqcItemEquipmentNumberConfigDO number :
                    numbersByConfigId.getOrDefault(config.getId(), List.of())) {
                signature.append(number.getEquipmentNumber()).append('|')
                        .append(number.getEnabled()).append('|')
                        .append(number.getSort()).append(';');
            }
            signature.append('/');
        }
        return signature.toString();
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

    private List<String> normalizeItemCodes(Collection<String> itemCodes) {
        List<String> normalizedCodes = itemCodes == null ? List.of() : itemCodes.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
        if (normalizedCodes.isEmpty()) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID, "itemCodes");
        }
        return normalizedCodes;
    }

    private String normalizeEquipmentNumber(String equipmentNumber) {
        if (StrUtil.isBlank(equipmentNumber)) {
            throw exception(PRO_FRONTLINE_PQC_RESULT_CONTRACT_INVALID, "equipmentNumber");
        }
        return equipmentNumber.trim();
    }
}
