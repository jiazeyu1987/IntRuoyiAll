package cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionMaterialListMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
public class MesProductionPickListSourceServiceImpl implements MesProductionPickListSourceService {

    private static final String APPROVED_DOCUMENT_STATUS = "C";
    private static final String FIELD_PREFIX = "material.";
    private static final String MATERIAL_CODE_FIELD_PREFIX = "materialCode.";
    private static final List<MaterialProperty> MATERIAL_PROPERTIES = List.of(
            new MaterialProperty("materialNumber", "物料编码", "STRING", ErpKingdeeProductionPickListItemDO::getMaterialNumber),
            new MaterialProperty("materialName", "物料名称", "STRING", ErpKingdeeProductionPickListItemDO::getMaterialName),
            new MaterialProperty("materialSpecification", "规格型号", "STRING", ErpKingdeeProductionPickListItemDO::getMaterialSpecification),
            new MaterialProperty("unitName", "单位", "STRING", ErpKingdeeProductionPickListItemDO::getUnitName),
            new MaterialProperty("lotNumber", "物料批次号", "STRING", ErpKingdeeProductionPickListItemDO::getLotNumber),
            new MaterialProperty("actualQuantity", "实际领料数量", "NUMBER", ErpKingdeeProductionPickListItemDO::getActualQuantity),
            new MaterialProperty("requestedQuantity", "申请领料数量", "NUMBER", ErpKingdeeProductionPickListItemDO::getRequestedQuantity),
            new MaterialProperty("sourceBillNo", "领料单号", "STRING", ErpKingdeeProductionPickListItemDO::getSourceBillNo));

    private final MesRouteDccProjectBindingMapper routeDccProjectBindingMapper;
    private final MesProRouteProductMapper routeProductMapper;
    private final MesProRouteProductBomMapper routeProductBomMapper;
    private final MesProRouteProcessMapper routeProcessMapper;
    private final MesMdItemMapper itemMapper;
    private final MesKingdeeProductionMaterialListMapper productionMaterialListMapper;
    private final ErpKingdeeProductionPickListMapper pickListMapper;
    private final ErpKingdeeProductionPickListItemMapper pickListItemMapper;
    private final MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper;
    private final MesProcessPoolActiveOrderPickListBindingItemMapper pickListBindingItemMapper;

    public MesProductionPickListSourceServiceImpl(
            MesRouteDccProjectBindingMapper routeDccProjectBindingMapper,
            MesProRouteProductMapper routeProductMapper,
            MesProRouteProductBomMapper routeProductBomMapper,
            MesProRouteProcessMapper routeProcessMapper,
            MesMdItemMapper itemMapper,
            MesKingdeeProductionMaterialListMapper productionMaterialListMapper,
            ErpKingdeeProductionPickListMapper pickListMapper,
            ErpKingdeeProductionPickListItemMapper pickListItemMapper,
            MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper,
            MesProcessPoolActiveOrderPickListBindingItemMapper pickListBindingItemMapper) {
        this.routeDccProjectBindingMapper = routeDccProjectBindingMapper;
        this.routeProductMapper = routeProductMapper;
        this.routeProductBomMapper = routeProductBomMapper;
        this.routeProcessMapper = routeProcessMapper;
        this.itemMapper = itemMapper;
        this.productionMaterialListMapper = productionMaterialListMapper;
        this.pickListMapper = pickListMapper;
        this.pickListItemMapper = pickListItemMapper;
        this.pickListBindingMapper = pickListBindingMapper;
        this.pickListBindingItemMapper = pickListBindingItemMapper;
    }

    @Override
    public List<SourceField> listSourceFields(Long routeId) {
        requireRouteDccBinding(routeId, null);
        List<MesProRouteProductDO> routeProducts = routeProductMapper.selectListByRouteId(routeId);
        if (routeProducts == null || routeProducts.isEmpty()
                || routeProducts.stream().anyMatch(product -> product == null || product.getItemId() == null)) {
            throw contextRequired("routeId=" + routeId + " 未配置正式关联产品");
        }
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(routeId);
        if (routeProcesses == null || routeProcesses.isEmpty()) {
            throw contextRequired("routeId=" + routeId + " 未配置正式工序");
        }
        Map<Long, List<MesProRouteProcessDO>> processesByProcessId = new LinkedHashMap<>();
        for (MesProRouteProcessDO process : routeProcesses) {
            if (process == null || process.getId() == null || process.getProcessId() == null) {
                throw contextRequired("routeId=" + routeId + " 存在缺少稳定编号的工序");
            }
            processesByProcessId.computeIfAbsent(process.getProcessId(), ignored -> new ArrayList<>()).add(process);
        }
        List<MesProRouteProductBomDO> bomRows = new ArrayList<>();
        for (MesProRouteProductDO product : routeProducts) {
            List<MesProRouteProductBomDO> productBom = routeProductBomMapper
                    .selectListByRouteIdAndProductId(routeId, product.getItemId());
            if (productBom != null) {
                bomRows.addAll(productBom);
            }
        }
        if (bomRows.isEmpty()) {
            return listErpMaterialCatalogFields(routeId, routeProducts, routeProcesses);
        }
        LinkedHashSet<Long> itemIds = new LinkedHashSet<>();
        for (MesProRouteProductBomDO bom : bomRows) {
            if (bom == null || bom.getProcessId() == null || bom.getItemId() == null
                    || !processesByProcessId.containsKey(bom.getProcessId())) {
                throw contextRequired("routeId=" + routeId + " 的工序物料清单缺少正式工序或物料");
            }
            itemIds.add(bom.getItemId());
        }
        Map<Long, MesMdItemDO> items = new LinkedHashMap<>();
        for (MesMdItemDO item : itemMapper.selectListByIds(List.copyOf(itemIds))) {
            if (item != null && item.getId() != null) {
                items.put(item.getId(), item);
            }
        }
        if (items.size() != itemIds.size()) {
            throw contextRequired("routeId=" + routeId + " 的工序物料清单引用了不存在的物料");
        }
        Map<String, SourceField> result = new LinkedHashMap<>();
        for (MesProRouteProductBomDO bom : bomRows) {
            MesMdItemDO item = items.get(bom.getItemId());
            if (StrUtil.isBlank(item.getCode()) || StrUtil.isBlank(item.getName())) {
                throw contextRequired("itemId=" + item.getId() + " 缺少正式物料编码或名称");
            }
            for (MesProRouteProcessDO process : processesByProcessId.get(bom.getProcessId())) {
                for (MaterialProperty property : MATERIAL_PROPERTIES) {
                    SourceField field = new SourceField(fieldCode(item.getId(), property.code()),
                            item.getName() + "（" + item.getCode() + "）- " + property.name(),
                            property.valueType(), process.getId());
                    result.putIfAbsent(process.getId() + ":" + field.fieldCode(), field);
                }
            }
        }
        return result.values().stream()
                .sorted(Comparator.comparing(SourceField::routeProcessId)
                        .thenComparing(SourceField::fieldCode))
                .toList();
    }

    @Override
    public ResolvedValue resolveValue(ResolveCommand command) {
        if (command == null || command.routeId() == null || command.routeProcessId() == null
                || command.productId() == null || command.dccProjectCodeId() == null
                || command.pickListBindingId() == null
                || StrUtil.isBlank(command.productionOrderNo()) || StrUtil.isBlank(command.sourceFieldCode())) {
            throw contextRequired("申请放行缺少路线、工序、产品、DCC 项目、领料绑定、生产订单号或来源字段");
        }
        requireRouteDccBinding(command.routeId(), command.dccProjectCodeId());
        MesProRouteProductDO routeProduct = routeProductMapper.selectByRouteIdAndItemId(
                command.routeId(), command.productId());
        if (routeProduct == null) {
            throw contextRequired("当前产品不属于 DCC 项目对应的工艺路线，productId=" + command.productId());
        }
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectById(command.routeProcessId());
        if (routeProcess == null || !Objects.equals(command.routeId(), routeProcess.getRouteId())
                || routeProcess.getProcessId() == null) {
            throw contextRequired("当前工序不属于 DCC 项目对应的工艺路线，routeProcessId=" + command.routeProcessId());
        }
        ParsedField parsedField = parseField(command.sourceFieldCode());
        MesProcessPoolActiveOrderPickListBindingDO binding = pickListBindingMapper.selectById(command.pickListBindingId());
        if (binding == null || !Objects.equals(binding.getId(), command.pickListBindingId())
                || binding.getPickListId() == null || StrUtil.isBlank(binding.getSourceSnapshotHash())) {
            throw sourceRequired("领料绑定不存在或缺少来源快照，pickListBindingId=" + command.pickListBindingId());
        }
        if (!StrUtil.equals("BOUND", StrUtil.trim(binding.getBindingStatus()))) {
            throw sourceRequired("领料绑定不是已绑定状态，pickListBindingId=" + binding.getId());
        }
        List<BindingSnapshot> bindingSnapshots = loadBoundPickListSnapshots(binding);
        String materialCode = resolveFormalMaterialCode(command, routeProcess, parsedField,
                bindingSnapshots.stream().flatMap(snapshot -> snapshot.items().stream()).toList());
        BindingMatch match = findFirstMaterialEntry(bindingSnapshots, materialCode, binding.getId());
        Object value = extract(match.item(), parsedField.property(), match.binding().getSourceBillNo());
        if (!hasValue(value)) {
            throw sourceRequired("绑定领料单的第一条物料分录缺少字段 "
                    + parsedField.property().name());
        }
        return new ResolvedValue(match.binding().getPickListId(), match.item().getPickListItemId(), value,
                evidenceHash(match.binding(), match.items()));
    }

    @Override
    public ResolvedValue resolveValueFromAll(ResolveAllCommand command) {
        if (command == null || command.pickListBindingIds() == null || command.pickListBindingIds().isEmpty()
                || command.pickListBindingIds().stream().anyMatch(Objects::isNull)) {
            throw contextRequired("申请放行缺少完整领料绑定集合");
        }
        List<MesProcessPoolActiveOrderPickListBindingDO> requested = command.pickListBindingIds().stream()
                .map(pickListBindingMapper::selectById).toList();
        if (requested.stream().anyMatch(Objects::isNull)) {
            throw sourceRequired("领料绑定集合包含不存在的绑定");
        }
        Long activeOrderId = requested.get(0).getActiveOrderId();
        List<Long> persistedIds = pickListBindingMapper.selectListByActiveOrderId(activeOrderId).stream()
                .map(MesProcessPoolActiveOrderPickListBindingDO::getId).sorted().toList();
        List<Long> requestedIds = command.pickListBindingIds().stream().sorted().toList();
        if (!persistedIds.equals(requestedIds)) {
            throw sourceRequired("领料绑定集合与活跃订单冻结来源不一致");
        }
        return resolveValue(new ResolveCommand(command.routeId(), command.routeProcessId(), command.productId(),
                command.dccProjectCodeId(), requestedIds.get(0), command.productionOrderNo(),
                command.sourceFieldCode()));
    }

    private List<BindingSnapshot> loadBoundPickListSnapshots(MesProcessPoolActiveOrderPickListBindingDO requested) {
        if (requested.getActiveOrderId() == null) {
            throw sourceRequired("领料绑定缺少活跃订单上下文，pickListBindingId=" + requested.getId());
        }
        List<MesProcessPoolActiveOrderPickListBindingDO> bindings =
                pickListBindingMapper.selectListByActiveOrderId(requested.getActiveOrderId());
        if (bindings == null || bindings.isEmpty()) {
            throw sourceRequired("活跃订单缺少领料绑定，activeOrderId=" + requested.getActiveOrderId());
        }
        List<BindingSnapshot> snapshots = new ArrayList<>();
        for (MesProcessPoolActiveOrderPickListBindingDO binding : bindings) {
            if (binding == null || binding.getId() == null || binding.getPickListId() == null
                    || StrUtil.isBlank(binding.getSourceSnapshotHash())) {
                throw sourceRequired("领料绑定不存在或缺少来源快照，activeOrderId=" + requested.getActiveOrderId());
            }
            if (!StrUtil.equals("BOUND", StrUtil.trim(binding.getBindingStatus()))) {
                throw sourceRequired("领料绑定不是已绑定状态，pickListBindingId=" + binding.getId());
            }
            List<MesProcessPoolActiveOrderPickListBindingItemDO> items =
                    pickListBindingItemMapper.selectListByBindingId(binding.getId());
            if (items == null || items.isEmpty()) {
                throw sourceRequired("领料绑定缺少完整明细快照，pickListBindingId=" + binding.getId());
            }
            snapshots.add(new BindingSnapshot(binding, items));
        }
        return snapshots;
    }

    private BindingMatch findFirstMaterialEntry(List<BindingSnapshot> snapshots, String materialCode,
                                                Long requestedBindingId) {
        List<BindingSnapshot> orderedSnapshots = snapshots.stream()
                .sorted(Comparator
                        .comparing((BindingSnapshot snapshot) -> !Objects.equals(snapshot.binding().getId(),
                                requestedBindingId))
                        .thenComparing(snapshot -> snapshot.binding().getBoundAt(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(snapshot -> snapshot.binding().getId()))
                .toList();
        for (BindingSnapshot snapshot : orderedSnapshots) {
            List<OrderedItem> orderedItems = new ArrayList<>();
            Set<Long> entryIds = new LinkedHashSet<>();
            for (MesProcessPoolActiveOrderPickListBindingItemDO item : snapshot.items()) {
                Long entryId = parsePositiveEntryId(item.getSourceEntryId());
                if (item.getPickListItemId() == null || StrUtil.isBlank(item.getSourceLineKey())
                        || !entryIds.add(entryId)) {
                    throw sourceRequired("领料绑定的正式分录顺序缺失或重复，pickListBindingId="
                            + snapshot.binding().getId());
                }
                orderedItems.add(new OrderedItem(entryId, item));
            }
            orderedItems.sort(Comparator.comparing(OrderedItem::entryId));
            for (OrderedItem orderedItem : orderedItems) {
                MesProcessPoolActiveOrderPickListBindingItemDO item = orderedItem.item();
                if (StrUtil.equalsIgnoreCase(StrUtil.trim(item.getMaterialNumber()), materialCode)) {
                    return new BindingMatch(snapshot.binding(), snapshot.items(), item);
                }
            }
        }
        throw sourceRequired("绑定领料单没有物料 " + materialCode + ", activeOrderPickListBindingCount="
                + snapshots.size());
    }

    private MesRouteDccProjectBindingDO requireRouteDccBinding(Long routeId, Long expectedDccProjectCodeId) {
        if (routeId == null) {
            throw contextRequired("缺少工艺路线编号");
        }
        MesRouteDccProjectBindingDO binding = routeDccProjectBindingMapper.selectCurrentByRouteId(routeId);
        if (binding == null || binding.getDccProjectCodeId() == null
                || expectedDccProjectCodeId != null
                && !Objects.equals(expectedDccProjectCodeId, binding.getDccProjectCodeId())) {
            throw contextRequired("routeId=" + routeId + " 未绑定当前活跃订单的唯一 DCC 项目代码");
        }
        return binding;
    }

    private List<SourceField> listErpMaterialCatalogFields(Long routeId,
                                                           List<MesProRouteProductDO> routeProducts,
                                                           List<MesProRouteProcessDO> routeProcesses) {
        LinkedHashSet<Long> productIds = new LinkedHashSet<>();
        routeProducts.forEach(product -> productIds.add(product.getItemId()));
        Map<Long, MesMdItemDO> products = new LinkedHashMap<>();
        for (MesMdItemDO product : itemMapper.selectListByIds(List.copyOf(productIds))) {
            if (product != null && product.getId() != null) {
                products.put(product.getId(), product);
            }
        }
        if (products.size() != productIds.size()) {
            throw contextRequired("routeId=" + routeId + " 的正式关联产品不存在");
        }
        Map<String, CatalogMaterial> materials = new LinkedHashMap<>();
        for (MesProRouteProductDO routeProduct : routeProducts) {
            MesMdItemDO product = products.get(routeProduct.getItemId());
            if (StrUtil.isBlank(product.getCode())) {
                throw contextRequired("productId=" + product.getId() + " 缺少正式产品编码");
            }
            List<MesKingdeeProductionMaterialListDO> rows = productionMaterialListMapper
                    .selectListByProductCode(StrUtil.trim(product.getCode()));
            if (rows == null) {
                throw contextRequired("productId=" + product.getId() + " 的 ERP 生产用料目录查询结果缺失");
            }
            for (MesKingdeeProductionMaterialListDO row : rows) {
                CatalogMaterial material = toCatalogMaterial(routeId, product.getCode(), row);
                CatalogMaterial existing = materials.putIfAbsent(normalizeMaterialCode(material.code()), material);
                if (existing != null && !StrUtil.equals(StrUtil.trim(existing.name()), StrUtil.trim(material.name()))) {
                    throw contextRequired("routeId=" + routeId + " 的 ERP 生产用料编码 " + material.code()
                            + " 对应多个物料名称");
                }
            }
        }
        if (materials.isEmpty()) {
            throw contextRequired("routeId=" + routeId + " 未配置工序物料清单，且路线产品未同步 ERP 生产用料清单");
        }
        Map<String, SourceField> result = new LinkedHashMap<>();
        for (MesProRouteProcessDO process : routeProcesses) {
            for (CatalogMaterial material : materials.values()) {
                for (MaterialProperty property : MATERIAL_PROPERTIES) {
                    SourceField field = new SourceField(fieldCode(material.code(), property.code()),
                            material.name() + "（" + material.code() + "）- " + property.name(),
                            property.valueType(), process.getId());
                    result.put(process.getId() + ":" + field.fieldCode(), field);
                }
            }
        }
        return result.values().stream()
                .sorted(Comparator.comparing(SourceField::routeProcessId)
                        .thenComparing(SourceField::fieldCode))
                .toList();
    }

    private CatalogMaterial toCatalogMaterial(Long routeId, String productCode,
                                               MesKingdeeProductionMaterialListDO row) {
        if (row == null || StrUtil.isBlank(row.getProductCode())
                || !StrUtil.equalsIgnoreCase(StrUtil.trim(productCode), StrUtil.trim(row.getProductCode()))
                || StrUtil.isBlank(row.getChildMaterialCode()) || StrUtil.isBlank(row.getChildMaterialName())) {
            throw contextRequired("routeId=" + routeId + " 的 ERP 生产用料清单缺少正式产品或物料标识");
        }
        return new CatalogMaterial(StrUtil.trim(row.getChildMaterialCode()), StrUtil.trim(row.getChildMaterialName()));
    }

    private String resolveFormalMaterialCode(ResolveCommand command, MesProRouteProcessDO routeProcess,
                                             ParsedField parsedField,
                                             List<MesProcessPoolActiveOrderPickListBindingItemDO> snapshotItems) {
        if (parsedField.itemId() != null) {
            List<MesProRouteProductBomDO> processBomRows = routeProductBomMapper
                    .selectList(command.routeId(), routeProcess.getProcessId(), command.productId());
            boolean materialBelongsToProcess = processBomRows != null && processBomRows.stream()
                    .anyMatch(bom -> Objects.equals(parsedField.itemId(), bom.getItemId()));
            if (!materialBelongsToProcess) {
                throw contextRequired("来源物料不属于当前产品和工序，itemId=" + parsedField.itemId());
            }
            MesMdItemDO material = itemMapper.selectById(parsedField.itemId());
            if (material == null || StrUtil.isBlank(material.getCode())) {
                throw contextRequired("来源物料不存在或缺少物料编码，itemId=" + parsedField.itemId());
            }
            return StrUtil.trim(material.getCode());
        }
        List<MesProRouteProductBomDO> productBomRows = routeProductBomMapper
                .selectListByRouteIdAndProductId(command.routeId(), command.productId());
        if (productBomRows != null && !productBomRows.isEmpty()) {
            LinkedHashSet<Long> processMaterialIds = new LinkedHashSet<>();
            productBomRows.stream()
                    .filter(bom -> Objects.equals(routeProcess.getProcessId(), bom.getProcessId()))
                    .map(MesProRouteProductBomDO::getItemId)
                    .filter(Objects::nonNull)
                    .forEach(processMaterialIds::add);
            List<MesMdItemDO> processMaterials = processMaterialIds.isEmpty()
                    ? List.of() : itemMapper.selectListByIds(List.copyOf(processMaterialIds));
            boolean matched = processMaterials.stream().filter(Objects::nonNull)
                    .anyMatch(material -> StrUtil.equalsIgnoreCase(StrUtil.trim(material.getCode()),
                            parsedField.materialCode()));
            if (!matched) {
                throw contextRequired("来源物料不属于当前产品和工序，materialCode="
                        + parsedField.materialCode());
            }
            return parsedField.materialCode();
        }
        MesMdItemDO product = itemMapper.selectById(command.productId());
        if (product == null || StrUtil.isBlank(product.getCode())) {
            throw contextRequired("当前路线产品不存在或缺少产品编码，productId=" + command.productId());
        }
        boolean matched = snapshotItems.stream().anyMatch(item ->
                StrUtil.equalsIgnoreCase(StrUtil.trim(item.getMaterialNumber()), parsedField.materialCode()));
        if (!matched) {
            throw contextRequired("来源物料不属于当前 DCC 路线产品，materialCode="
                    + parsedField.materialCode());
        }
        return parsedField.materialCode();
    }

    private ParsedField parseField(String fieldCode) {
        String normalized = StrUtil.trim(fieldCode);
        String[] parts = normalized.split("\\.");
        if (parts.length != 3) {
            throw sourceRequired("不支持的领料单来源字段 " + fieldCode);
        }
        MaterialProperty property = MATERIAL_PROPERTIES.stream()
                .filter(candidate -> candidate.code().equals(parts[2]))
                .findFirst()
                .orElseThrow(() -> sourceRequired("不支持的领料单来源字段 " + fieldCode));
        if (normalized.startsWith(FIELD_PREFIX)) {
            try {
                return new ParsedField(Long.valueOf(parts[1]), null, property);
            } catch (NumberFormatException ex) {
                throw sourceRequired("领料单来源字段缺少稳定物料编号 " + fieldCode);
            }
        }
        if (normalized.startsWith(MATERIAL_CODE_FIELD_PREFIX)) {
            try {
                String materialCode = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                if (StrUtil.isBlank(materialCode)
                        || !parts[1].equals(encodeMaterialCode(StrUtil.trim(materialCode)))) {
                    throw new IllegalArgumentException("non-canonical material code");
                }
                return new ParsedField(null, StrUtil.trim(materialCode), property);
            } catch (IllegalArgumentException ex) {
                throw sourceRequired("领料单来源字段缺少稳定物料编码 " + fieldCode);
            }
        }
        throw sourceRequired("不支持的领料单来源字段 " + fieldCode);
    }

    private Long parsePositiveEntryId(String sourceEntryId) {
        try {
            long value = Long.parseLong(StrUtil.trim(sourceEntryId));
            if (value <= 0) {
                throw new NumberFormatException("non-positive");
            }
            return value;
        } catch (Exception ex) {
            throw sourceRequired("领料分录缺少可排序的正式 sourceEntryId=" + sourceEntryId);
        }
    }

    private boolean hasValue(Object value) {
        return value != null && (!(value instanceof String text) || StrUtil.isNotBlank(text));
    }

    private String evidenceHash(MesProcessPoolActiveOrderPickListBindingDO binding,
                                List<MesProcessPoolActiveOrderPickListBindingItemDO> items) {
        String payload = "PRODUCTION_PICK_LIST_BINDING_SOURCE_V1|" + text(binding.getId()) + "|"
                + text(binding.getPickListId()) + "|" + text(binding.getSourceSnapshotHash()) + "|"
                + items.stream().map(item -> String.join("|", text(item.getId()), text(item.getPickListItemId()),
                text(item.getSourceEntryId()), text(item.getSourceLineKey()), text(item.getMaterialNumber()),
                text(item.getLotNumber()), decimal(item.getActualQuantity()), decimal(item.getRequestedQuantity()),
                text(item.getProductionOrderNo()))).sorted().reduce((a, b) -> a + "||" + b).orElse("");
        return DigestUtil.sha256Hex(payload);
    }

    private Object extract(MesProcessPoolActiveOrderPickListBindingItemDO item, MaterialProperty property,
                           String sourceBillNo) {
        return switch (property.code()) {
            case "materialNumber" -> item.getMaterialNumber();
            case "materialName" -> item.getMaterialName();
            case "materialSpecification" -> item.getMaterialSpecification();
            case "unitName" -> item.getUnitName();
            case "lotNumber" -> item.getLotNumber();
            case "actualQuantity" -> item.getActualQuantity();
            case "requestedQuantity" -> item.getRequestedQuantity();
            case "sourceBillNo" -> sourceBillNo;
            default -> throw sourceRequired("不支持的绑定快照字段 " + property.code());
        };
    }

    private String text(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }

    private String decimal(BigDecimal value) {
        return value == null ? "null" : value.stripTrailingZeros().toPlainString();
    }

    private String fieldCode(Long itemId, String property) {
        return FIELD_PREFIX + itemId + "." + property;
    }

    private String fieldCode(String materialCode, String property) {
        return MATERIAL_CODE_FIELD_PREFIX + encodeMaterialCode(materialCode) + "." + property;
    }

    private String encodeMaterialCode(String materialCode) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(StrUtil.trim(materialCode).getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeMaterialCode(String materialCode) {
        return StrUtil.trim(materialCode).toUpperCase(Locale.ROOT);
    }

    private RuntimeException contextRequired(String reason) {
        return exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_PICK_LIST_CONTEXT_REQUIRED,
                reason);
    }

    private RuntimeException sourceRequired(String reason) {
        return exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_PICK_LIST_SOURCE_VALUE_REQUIRED,
                reason);
    }

    private record MaterialProperty(String code, String name, String valueType,
                                    Function<ErpKingdeeProductionPickListItemDO, Object> extractor) {
    }

    private record ParsedField(Long itemId, String materialCode, MaterialProperty property) {
    }

    private record CatalogMaterial(String code, String name) {
    }

    private record OrderedItem(Long entryId, MesProcessPoolActiveOrderPickListBindingItemDO item) {
    }

    private record BindingSnapshot(MesProcessPoolActiveOrderPickListBindingDO binding,
                                   List<MesProcessPoolActiveOrderPickListBindingItemDO> items) {
    }

    private record BindingMatch(MesProcessPoolActiveOrderPickListBindingDO binding,
                                List<MesProcessPoolActiveOrderPickListBindingItemDO> items,
                                MesProcessPoolActiveOrderPickListBindingItemDO item) {
    }
}
