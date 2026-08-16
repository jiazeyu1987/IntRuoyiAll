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
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
public class MesProductionPickListSourceServiceImpl implements MesProductionPickListSourceService {

    private static final String APPROVED_DOCUMENT_STATUS = "C";
    private static final String FIELD_PREFIX = "material.";
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
    private final ErpKingdeeProductionPickListMapper pickListMapper;
    private final ErpKingdeeProductionPickListItemMapper pickListItemMapper;

    public MesProductionPickListSourceServiceImpl(
            MesRouteDccProjectBindingMapper routeDccProjectBindingMapper,
            MesProRouteProductMapper routeProductMapper,
            MesProRouteProductBomMapper routeProductBomMapper,
            MesProRouteProcessMapper routeProcessMapper,
            MesMdItemMapper itemMapper,
            ErpKingdeeProductionPickListMapper pickListMapper,
            ErpKingdeeProductionPickListItemMapper pickListItemMapper) {
        this.routeDccProjectBindingMapper = routeDccProjectBindingMapper;
        this.routeProductMapper = routeProductMapper;
        this.routeProductBomMapper = routeProductBomMapper;
        this.routeProcessMapper = routeProcessMapper;
        this.itemMapper = itemMapper;
        this.pickListMapper = pickListMapper;
        this.pickListItemMapper = pickListItemMapper;
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
            throw contextRequired("routeId=" + routeId + " 未配置工序物料清单");
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
                || StrUtil.isBlank(command.productionOrderNo()) || StrUtil.isBlank(command.sourceFieldCode())) {
            throw contextRequired("申请放行缺少路线、工序、产品、DCC 项目、生产订单号或来源字段");
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
        boolean materialBelongsToProcess = routeProductBomMapper
                .selectList(command.routeId(), routeProcess.getProcessId(), command.productId()).stream()
                .anyMatch(bom -> Objects.equals(parsedField.itemId(), bom.getItemId()));
        if (!materialBelongsToProcess) {
            throw contextRequired("来源物料不属于当前产品和工序，itemId=" + parsedField.itemId());
        }
        MesMdItemDO material = itemMapper.selectById(parsedField.itemId());
        if (material == null || StrUtil.isBlank(material.getCode())) {
            throw contextRequired("来源物料不存在或缺少物料编码，itemId=" + parsedField.itemId());
        }
        List<ErpKingdeeProductionPickListItemDO> orderItems = pickListItemMapper
                .selectListByProductionOrderNo(StrUtil.trim(command.productionOrderNo()));
        List<ErpKingdeeProductionPickListItemDO> materialItems = orderItems == null ? List.of() : orderItems.stream()
                .filter(Objects::nonNull)
                .filter(item -> StrUtil.equalsIgnoreCase(StrUtil.trim(item.getMaterialNumber()),
                        StrUtil.trim(material.getCode())))
                .toList();
        if (materialItems.isEmpty()) {
            throw sourceRequired("生产订单 " + command.productionOrderNo() + " 的领料单没有物料 " + material.getCode());
        }
        List<Long> pickListIds = materialItems.stream().map(ErpKingdeeProductionPickListItemDO::getProductionPickListId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, ErpKingdeeProductionPickListDO> approvedHeaders = new LinkedHashMap<>();
        for (ErpKingdeeProductionPickListDO header : pickListMapper.selectBatchIds(pickListIds)) {
            if (header != null && header.getId() != null
                    && APPROVED_DOCUMENT_STATUS.equalsIgnoreCase(StrUtil.trim(header.getDocumentStatus()))) {
                approvedHeaders.put(header.getId(), header);
            }
        }
        if (approvedHeaders.size() != 1) {
            throw sourceRequired("生产订单 " + command.productionOrderNo()
                    + " 必须关联唯一已审核领料单，当前数量=" + approvedHeaders.size());
        }
        ErpKingdeeProductionPickListDO header = approvedHeaders.values().iterator().next();
        List<OrderedItem> orderedItems = new ArrayList<>();
        Set<Long> entryIds = new LinkedHashSet<>();
        for (ErpKingdeeProductionPickListItemDO item : materialItems) {
            if (!Objects.equals(header.getId(), item.getProductionPickListId())) {
                continue;
            }
            Long entryId = parsePositiveEntryId(item.getSourceEntryId());
            if (item.getId() == null || StrUtil.isBlank(item.getSourceLineKey()) || !entryIds.add(entryId)) {
                throw sourceRequired("领料单 " + header.getSourceBillNo() + " 的正式分录顺序缺失或重复");
            }
            orderedItems.add(new OrderedItem(entryId, item));
        }
        if (orderedItems.isEmpty()) {
            throw sourceRequired("领料单 " + header.getSourceBillNo() + " 没有可用的正式物料分录");
        }
        orderedItems.sort(Comparator.comparing(OrderedItem::entryId));
        ErpKingdeeProductionPickListItemDO first = orderedItems.get(0).item();
        Object value = parsedField.property().extractor().apply(first);
        if (!hasValue(value)) {
            throw sourceRequired("领料单 " + header.getSourceBillNo() + " 的第一条物料分录缺少字段 "
                    + parsedField.property().name());
        }
        return new ResolvedValue(header.getId(), first.getId(), value, evidenceHash(header, first));
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

    private ParsedField parseField(String fieldCode) {
        String normalized = StrUtil.trim(fieldCode);
        if (!normalized.startsWith(FIELD_PREFIX)) {
            throw sourceRequired("不支持的领料单来源字段 " + fieldCode);
        }
        String[] parts = normalized.split("\\.");
        if (parts.length != 3) {
            throw sourceRequired("不支持的领料单来源字段 " + fieldCode);
        }
        Long itemId;
        try {
            itemId = Long.valueOf(parts[1]);
        } catch (NumberFormatException ex) {
            throw sourceRequired("领料单来源字段缺少稳定物料编号 " + fieldCode);
        }
        MaterialProperty property = MATERIAL_PROPERTIES.stream()
                .filter(candidate -> candidate.code().equals(parts[2]))
                .findFirst()
                .orElseThrow(() -> sourceRequired("不支持的领料单来源字段 " + fieldCode));
        return new ParsedField(itemId, property);
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

    private String evidenceHash(ErpKingdeeProductionPickListDO header,
                                ErpKingdeeProductionPickListItemDO item) {
        return DigestUtil.sha256Hex(String.join("|", "PRODUCTION_PICK_LIST_SOURCE_V1",
                text(header.getId()), text(header.getSourceFormId()), text(header.getSourceFid()),
                text(header.getSourceBillNo()), text(header.getDocumentStatus()), text(item.getId()),
                text(item.getSourceEntryId()), text(item.getSourceLineKey()), text(item.getMaterialNumber()),
                text(item.getLotNumber()), decimal(item.getActualQuantity()), decimal(item.getRequestedQuantity()),
                text(item.getProductionOrderNo())));
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

    private record ParsedField(Long itemId, MaterialProperty property) {
    }

    private record OrderedItem(Long entryId, ErpKingdeeProductionPickListItemDO item) {
    }
}
