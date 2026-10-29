package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.CatalogItem;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Category;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;

/** Batch-level AND lookup over saved business facts, before pagination. */
@Service
@RequiredArgsConstructor
public class MesProEdhrBatchLookupService {
    private final MesProEdhrTraceIdentityMapper identityMapper;
    private final MesProEdhrBatchExecutionOriginMapper originMapper;
    private final List<MesProEdhrReverseTraceSourceAdapter> adapters;
    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProcessPoolSubmissionReviewMapper reviewMapper;
    private final MesPqcProcessInspectionAggregateDetailMapper pqcDetailMapper;
    private final MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    private final MesProcessPoolActiveOrderPickListBindingMapper bindingMapper;
    private final MesProcessPoolActiveOrderPickListBindingItemMapper materialMapper;
    private final MesProEdhrReleaseTransactionMapper releaseMapper;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;

    public static boolean requested(EdhrBatchExecutionPageReqVO request) {
        return StrUtil.isNotBlank(request.getTracePerson()) || StrUtil.isNotBlank(request.getTraceEquipment())
                || StrUtil.isNotBlank(request.getTraceMaterial()) || StrUtil.isNotBlank(request.getTraceMaterialLot())
                || request.getTraceBusinessTime() != null;
    }

    public List<MesProEdhrBatchExecutionDO> filter(List<MesProEdhrBatchExecutionDO> batches, EdhrBatchExecutionPageReqVO request) {
        return prepareWindowFilter(request).apply(batches);
    }

    /** Validate only database-selected hits; never alter a database page or its total silently. */
    public void validateSearchHits(List<MesProEdhrBatchExecutionDO> batches, EdhrBatchExecutionPageReqVO request) {
        if (!requested(request) || batches.isEmpty()) return;
        List<MesProEdhrBatchExecutionDO> validated = filter(batches, request);
        if (validated.size() != batches.size()) {
            throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException(
                    "批记录反查命中与正式证据不一致，请重新查询；持续发生时请检查正式来源完整性");
        }
    }

    /** Explicit integrity operation, independent of search predicates and pagination. */
    public void validateAllFacts(List<MesProEdhrBatchExecutionDO> batches) {
        if (batches.isEmpty()) return;
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        for (var batch : batches) {
            if (!Objects.equals(tenantId, batch.getTenantId())) throw new IllegalStateException("批记录租户归属不一致");
        }
        Set<Category> categories = EnumSet.of(Category.PERSON, Category.EQUIPMENT, Category.MATERIAL);
        var window = preload(batches, categories, tenantId);
        for (var batch : batches) {
            try {
                readFacts(batch, categories, window);
            } catch (IllegalStateException ex) {
                throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException(
                        "批记录 {} 完整性检查失败：{}", batch.getBatchExecutionCode(), ex.getMessage());
            }
        }
    }

    /** Single-candidate caller; paged callers must use prepareWindowFilter. */
    public java.util.function.Predicate<MesProEdhrBatchExecutionDO> prepareFilter(EdhrBatchExecutionPageReqVO request) {
        var filter = prepareWindowFilter(request);
        return batch -> !filter.apply(List.of(batch)).isEmpty();
    }

    /** Request-scoped identity resolution; evidence is bounded to each candidate window. */
    public Function<List<MesProEdhrBatchExecutionDO>, List<MesProEdhrBatchExecutionDO>> prepareWindowFilter(
            EdhrBatchExecutionPageReqVO request) {
        if (!request.isTraceBusinessTimeValid()) throw new IllegalArgumentException("业务时间范围无效");
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Set<Long> employees = StrUtil.isBlank(request.getTracePerson()) ? Set.of() : identityMapper.employeeIds(tenantId, request.getTracePerson().trim());
        Set<Long> users = StrUtil.isBlank(request.getTracePerson()) ? Set.of() : identityMapper.userIds(tenantId, request.getTracePerson().trim());
        Set<Long> devices = StrUtil.isBlank(request.getTraceEquipment()) ? Set.of() : identityMapper.deviceIds(tenantId, request.getTraceEquipment().trim());
        Set<Category> categories = EnumSet.noneOf(Category.class);
        if (StrUtil.isNotBlank(request.getTracePerson()) || request.getTraceBusinessTime() != null) categories.add(Category.PERSON);
        if (StrUtil.isNotBlank(request.getTraceEquipment())) categories.add(Category.EQUIPMENT);
        if (StrUtil.isNotBlank(request.getTraceMaterial()) || StrUtil.isNotBlank(request.getTraceMaterialLot())) categories.add(Category.MATERIAL);
        return batches -> {
            if (batches.isEmpty()) return List.of();
            for (var batch : batches) {
                if (!Objects.equals(tenantId, batch.getTenantId())) throw new IllegalStateException("批记录租户归属不一致");
            }
            var window = preload(batches, categories, tenantId);
            return batches.stream().filter(batch -> {
                try {
                    return matches(readFacts(batch, categories, window), request, employees, users, devices);
                } catch (IllegalStateException ex) {
                    throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException(
                            "批记录 {} 反查失败：{}", batch.getBatchExecutionCode(), ex.getMessage());
                }
            }).toList();
        };
    }

    private WindowFacts preload(List<MesProEdhrBatchExecutionDO> batches, Set<Category> categories, Long tenantId) {
        var window = new WindowFacts();
        List<Long> batchIds = batches.stream().map(MesProEdhrBatchExecutionDO::getId).distinct().toList();
        var origins = originMapper.selectList(new LambdaQueryWrapperX<MesProEdhrBatchExecutionOriginDO>()
                .in(MesProEdhrBatchExecutionOriginDO::getBatchExecutionId, batchIds)
                .eq(MesProEdhrBatchExecutionOriginDO::getTenantId, tenantId)
                .orderByAsc(MesProEdhrBatchExecutionOriginDO::getId));
        window.origins = group(origins, MesProEdhrBatchExecutionOriginDO::getBatchExecutionId);
        List<MesProEdhrBatchExecutionDO> archived = new ArrayList<>();
        for (var batch : batches) {
            var batchOrigins = window.origins.getOrDefault(batch.getId(), List.of());
            try {
                validateOrigins(batch, batchOrigins);
            } catch (IllegalStateException ex) {
                throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException(
                        "批记录 {} 反查失败：{}", batch.getBatchExecutionCode(), ex.getMessage());
            }
            if (!batchOrigins.isEmpty() && batchOrigins.get(0).getCompletionBackfillReceiptId() != null) archived.add(batch);
        }
        if (!archived.isEmpty() && !categories.isEmpty()) {
            MesProEdhrReverseTraceSourceAdapter windowAdapter = null;
            for (Category category : categories) {
                var adapter = adapters.stream().filter(item -> item.category() == category).findFirst()
                        .orElseThrow(() -> new IllegalStateException("缺少批记录反查来源：" + category));
                if (windowAdapter == null) windowAdapter = adapter;
            }
            try {
                window.catalogs.putAll(windowAdapter.readCatalogs(archived, categories));
            } catch (IllegalStateException ex) {
                throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException(
                        "批记录反查失败：{}", ex.getMessage());
            }
        }
        List<Long> activeOrderIds = origins.stream().filter(o -> o.getCompletionBackfillReceiptId() == null)
                .map(MesProEdhrBatchExecutionOriginDO::getActiveOrderId).filter(Objects::nonNull).distinct().toList();
        if (!activeOrderIds.isEmpty() && (categories.contains(Category.PERSON) || categories.contains(Category.EQUIPMENT))) {
            var allocations = allocationMapper.selectListByActiveOrderIds(activeOrderIds);
            window.allocations = group(allocations, MesProcessPoolReportAllocationDO::getActiveOrderId);
            var details = pqcDetailMapper.selectList(new LambdaQueryWrapperX<MesPqcProcessInspectionAggregateDetailDO>()
                    .in(MesPqcProcessInspectionAggregateDetailDO::getActiveOrderId, activeOrderIds)
                    .eq(MesPqcProcessInspectionAggregateDetailDO::getTenantId, tenantId));
            window.details = group(details, MesPqcProcessInspectionAggregateDetailDO::getActiveOrderId);
            var eventIds = allocations.stream().map(MesProcessPoolReportAllocationDO::getEventId)
                    .filter(Objects::nonNull).distinct().toList();
            if (!eventIds.isEmpty()) window.events = index(eventMapper.selectBatchIds(eventIds), MesProProcessPoolEventDO::getId);
            if (categories.contains(Category.PERSON)) {
                var reviewIds = new LinkedHashSet<Long>();
                allocations.forEach(a -> reviewIds.add(a.getReviewId()));
                details.forEach(d -> reviewIds.add(d.getReviewId()));
                reviewIds.remove(null);
                if (!reviewIds.isEmpty()) window.reviews = index(reviewMapper.selectBatchIds(reviewIds), MesProcessPoolSubmissionReviewDO::getId);
                var recordIds = details.stream().map(MesPqcProcessInspectionAggregateDetailDO::getSourcePqcRecordId)
                        .filter(Objects::nonNull).distinct().toList();
                if (!recordIds.isEmpty()) window.records = index(pqcRecordMapper.selectBatchIds(recordIds), MesProProcessPoolPqcRecordDO::getId);
            }
        }
        if (!activeOrderIds.isEmpty() && categories.contains(Category.MATERIAL)) {
            var bindings = bindingMapper.selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderPickListBindingDO>()
                    .in(MesProcessPoolActiveOrderPickListBindingDO::getActiveOrderId, activeOrderIds)
                    .eq(MesProcessPoolActiveOrderPickListBindingDO::getTenantId, tenantId));
            window.bindings = group(bindings, MesProcessPoolActiveOrderPickListBindingDO::getActiveOrderId);
            var bindingIds = bindings.stream().filter(b -> "BOUND".equals(b.getBindingStatus()))
                    .map(MesProcessPoolActiveOrderPickListBindingDO::getId).distinct().toList();
            if (!bindingIds.isEmpty()) window.materials = group(materialMapper.selectList(
                    new LambdaQueryWrapperX<MesProcessPoolActiveOrderPickListBindingItemDO>()
                            .in(MesProcessPoolActiveOrderPickListBindingItemDO::getBindingId, bindingIds)
                            .eq(MesProcessPoolActiveOrderPickListBindingItemDO::getTenantId, tenantId)),
                    MesProcessPoolActiveOrderPickListBindingItemDO::getBindingId);
        }
        if (categories.contains(Category.PERSON)) {
            window.releases = group(releaseMapper.selectListByBatchExecutionIds(batchIds), MesProEdhrReleaseTransactionDO::getBatchExecutionId);
            window.applications = group(applicationMapper.selectListByBatchExecutionIds(batchIds), MesProcessPoolActiveOrderReleaseApplicationDO::getBatchExecutionId);
        }
        return window;
    }

    private static void validateOrigins(MesProEdhrBatchExecutionDO batch, List<MesProEdhrBatchExecutionOriginDO> origins) {
        if (origins.isEmpty()) return;
        var first = origins.get(0);
        for (var origin : origins) {
            if (!Objects.equals(origin.getBatchExecutionId(), batch.getId())
                    || !Objects.equals(origin.getTenantId(), batch.getTenantId())
                    || !Objects.equals(origin.getWorkOrderId(), batch.getWorkOrderId())
                    || !Objects.equals(first.getActiveOrderId(), origin.getActiveOrderId())
                    || !Objects.equals(first.getCompletionBackfillReceiptId(), origin.getCompletionBackfillReceiptId())) {
                throw new IllegalStateException("批记录正式来源关联不一致：" + batch.getBatchExecutionCode());
            }
        }
    }

    private static <T> Map<Long, List<T>> group(List<T> rows, Function<T, Long> key) {
        return rows.stream().collect(Collectors.groupingBy(key));
    }

    private static <T> Map<Long, T> index(List<T> rows, Function<T, Long> key) {
        Map<Long, T> indexed = new HashMap<>();
        for (T row : rows) {
            Long id = key.apply(row);
            if (id == null || indexed.putIfAbsent(id, row) != null) {
                throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException(
                        "正式证据记录身份缺失或重复：{}", id);
            }
        }
        return indexed;
    }

    private record LiveSource(Long tenantId, Long workOrderId, Long activeOrderId) { }

    /** Discarded after one window; no tenant-wide cache or singleton mutable state. */
    private static final class WindowFacts {
        Map<Long, List<MesProEdhrBatchExecutionOriginDO>> origins = Map.of();
        Map<Long, List<MesProcessPoolReportAllocationDO>> allocations = Map.of();
        Map<Long, List<MesPqcProcessInspectionAggregateDetailDO>> details = Map.of();
        Map<Long, MesProProcessPoolEventDO> events = Map.of();
        Map<Long, MesProcessPoolSubmissionReviewDO> reviews = Map.of();
        Map<Long, MesProProcessPoolPqcRecordDO> records = Map.of();
        Map<Long, List<MesProcessPoolActiveOrderPickListBindingDO>> bindings = Map.of();
        Map<Long, List<MesProcessPoolActiveOrderPickListBindingItemDO>> materials = Map.of();
        Map<Long, List<MesProEdhrReleaseTransactionDO>> releases = Map.of();
        Map<Long, List<MesProcessPoolActiveOrderReleaseApplicationDO>> applications = Map.of();
        Map<LiveSource, List<CatalogItem>> liveFacts = new HashMap<>();
        Map<Category, Map<Long, MesProEdhrReverseTraceSourceAdapter.CatalogResult>> catalogs = new EnumMap<>(Category.class);
    }

    private List<CatalogItem> readFacts(MesProEdhrBatchExecutionDO batch, Set<Category> categories, WindowFacts window) {
        List<CatalogItem> facts = new ArrayList<>();
        List<MesProEdhrBatchExecutionOriginDO> origins = window.origins.getOrDefault(batch.getId(), List.of());
        if (!origins.isEmpty()) {
            var first = origins.get(0);
            if (first.getCompletionBackfillReceiptId() != null) {
                for (Category category : categories) {
                    var categoryCatalogs = window.catalogs.get(category);
                    if (categoryCatalogs == null) throw new IllegalStateException("批记录正式反查目录缺失：" + category);
                    var catalog = categoryCatalogs.get(batch.getId());
                    if (catalog == null) throw new IllegalStateException("批记录正式反查目录缺失：" + category);
                    if (!Set.of("AVAILABLE", "NO_RECORDED_FACT").contains(catalog.status())) {
                        throw new IllegalStateException("批记录 " + batch.getBatchExecutionCode() + " 反查失败：" + catalog.reason());
                    }
                    facts.addAll(catalog.items());
                }
            } else if (first.getActiveOrderId() != null) {
                var key = new LiveSource(first.getTenantId(), first.getWorkOrderId(), first.getActiveOrderId());
                facts.addAll(window.liveFacts.computeIfAbsent(key, ignored -> {
                    List<CatalogItem> live = new ArrayList<>();
                    readLiveFacts(first, categories, live, window);
                    return live;
                }));
            }
        }
        if (categories.contains(Category.PERSON)) {
            for (var release : window.releases.getOrDefault(batch.getId(), List.of())) {
                if (!Objects.equals(batch.getId(), release.getBatchExecutionId())) throw new IllegalStateException("放行记录批次归属不一致");
                person(facts, "SYSTEM_USER", release.getSubmittedBy(), release.getSubmittedAt());
                person(facts, "SYSTEM_USER", release.getApprovedBy(), release.getApprovedAt());
            }
            for (var application : window.applications.getOrDefault(batch.getId(), List.of())) {
                if (!Objects.equals(batch.getTenantId(), application.getTenantId())) throw new IllegalStateException("PQC放行记录租户不一致");
                person(facts, "SYSTEM_USER", application.getAppliedBy(), application.getAppliedAt());
                person(facts, "SYSTEM_USER", application.getPqcDecidedBy(), application.getPqcDecidedAt());
            }
        }
        return facts;
    }

    private void readLiveFacts(MesProEdhrBatchExecutionOriginDO origin, Set<Category> categories, List<CatalogItem> facts, WindowFacts window) {
        if (categories.contains(Category.PERSON) || categories.contains(Category.EQUIPMENT)) {
            var allocations = window.allocations.getOrDefault(origin.getActiveOrderId(), List.of());
            Set<Long> events = new HashSet<>();
            Set<Long> reviews = new HashSet<>();
            for (var allocation : allocations) {
                if (!Objects.equals(origin.getTenantId(), allocation.getTenantId()) || !Objects.equals(origin.getWorkOrderId(), allocation.getWorkOrderId()))
                    throw new IllegalStateException("生产分配归属不一致");
                if (events.add(allocation.getEventId())) {
                    var event = allocation.getEventId() == null ? null : window.events.get(allocation.getEventId());
                    if (event == null || !Objects.equals(origin.getTenantId(), event.getTenantId()) || !"PRODUCTION_SUBMIT".equals(event.getEventType()))
                        throw new IllegalStateException("生产分配对应的正式提交事件缺失或不一致");
                    if (categories.contains(Category.PERSON)) {
                        person(facts, "EMPLOYEE", event.getActualEmployeeId(), event.getServerSubmitTime());
                        person(facts, "SYSTEM_USER", event.getSignatureUserId(), event.getServerSubmitTime());
                    }
                    if (categories.contains(Category.EQUIPMENT)) addProductionEquipment(facts, event.getRawPayload());
                }
                if (categories.contains(Category.PERSON) && reviews.add(allocation.getReviewId())) addReview(facts, allocation.getReviewId(), origin.getTenantId(), window);
            }
            Set<Long> records = new HashSet<>();
            for (var detail : window.details.getOrDefault(origin.getActiveOrderId(), List.of())) {
                if (!Objects.equals(origin.getTenantId(), detail.getTenantId()) || !Objects.equals(origin.getWorkOrderId(), detail.getWorkOrderId()))
                    throw new IllegalStateException("PQC明细归属不一致");
                if (categories.contains(Category.EQUIPMENT) && detail.getSelectedEquipmentId() != null) {
                    Map<String,String> context = new HashMap<>();
                    put(context, "selectedEquipmentCode", detail.getSelectedEquipmentCode());
                    put(context, "selectedEquipmentName", detail.getSelectedEquipmentName());
                    put(context, "selectedEquipmentNumber", detail.getSelectedEquipmentNumber());
                    facts.add(new CatalogItem().setCategory(Category.EQUIPMENT).setEvidenceKey("EQUIPMENT:PQC:" + detail.getSelectedEquipmentId())
                            .setLabel(detail.getSelectedEquipmentName()).setSavedValue(String.valueOf(detail.getSelectedEquipmentId())).setSourceContext(context));
                }
                if (categories.contains(Category.PERSON) && records.add(detail.getSourcePqcRecordId())) {
                    var record = detail.getSourcePqcRecordId() == null ? null : window.records.get(detail.getSourcePqcRecordId());
                    if (record == null || !Objects.equals(origin.getTenantId(), record.getTenantId())) throw new IllegalStateException("正式PQC检验记录缺失或租户不一致");
                    person(facts, "EMPLOYEE", record.getActualEmployeeId(), record.getServerSubmitTime());
                    person(facts, "SYSTEM_USER", record.getSignatureUserId(), record.getServerSubmitTime());
                }
                if (categories.contains(Category.PERSON) && reviews.add(detail.getReviewId())) addReview(facts, detail.getReviewId(), origin.getTenantId(), window);
            }
        }
        if (categories.contains(Category.MATERIAL)) {
            for (var binding : window.bindings.getOrDefault(origin.getActiveOrderId(), List.of())) {
                if (!Objects.equals(origin.getTenantId(), binding.getTenantId()) || !Objects.equals(origin.getWorkOrderId(), binding.getWorkOrderId()))
                    throw new IllegalStateException("领料关联归属不一致");
                if (!"BOUND".equals(binding.getBindingStatus())) continue;
                for (var material : window.materials.getOrDefault(binding.getId(), List.of())) {
                    Map<String,String> context = new HashMap<>();
                    put(context, "materialNumber", material.getMaterialNumber()); put(context, "lotNumber", material.getLotNumber());
                    facts.add(new CatalogItem().setCategory(Category.MATERIAL).setLabel(material.getMaterialName()).setSourceContext(context));
                }
            }
        }
    }

    private void addReview(List<CatalogItem> facts, Long reviewId, Long tenantId, WindowFacts window) {
        if (reviewId == null) throw new IllegalStateException("正式分配缺少复核记录编号");
        var review = window.reviews.get(reviewId);
        if (review == null || !Objects.equals(tenantId, review.getTenantId())) throw new IllegalStateException("正式复核记录缺失或租户不一致");
        person(facts, "SYSTEM_USER", review.getLeaderUserId(), review.getReviewedAt());
        person(facts, "SYSTEM_USER", review.getReviewSignatureUserId(), review.getReviewedAt());
    }

    static void addProductionEquipment(List<CatalogItem> facts, String payload) {
        if (StrUtil.isBlank(payload)) throw new IllegalStateException("生产提交内容缺失");
        JSONObject raw;
        try {
            raw = JSON.parseObject(payload);
        } catch (com.alibaba.fastjson.JSONException exception) {
            throw new IllegalStateException("生产提交内容不是有效JSON对象", exception);
        }
        if (raw == null) throw new IllegalStateException("生产提交内容不是JSON对象");
        addDevices(facts, raw.get("selectedDevices"));
        if (raw.get("materialDetails") != null) {
            if (!(raw.get("materialDetails") instanceof Collection<?> materials)) throw new IllegalStateException("生产物料明细不是数组");
            for (Object material : materials) {
                if (!(material instanceof JSONObject detail)) throw new IllegalStateException("生产物料明细不是JSON对象");
                addDevices(facts, detail.get("selectedDevices"));
            }
        }
    }

    private static void addDevices(List<CatalogItem> facts, Object selected) {
        if (selected == null) return;
        if (!(selected instanceof Collection<?> devices)) throw new IllegalStateException("已选生产设备不是数组");
        for (Object value : devices) {
            if (!(value instanceof JSONObject device)) throw new IllegalStateException("已选生产设备不是JSON对象");
            Long deviceId;
            try {
                deviceId = device.getLong("deviceId");
            } catch (com.alibaba.fastjson.JSONException | NumberFormatException exception) {
                throw new IllegalStateException("已选生产设备身份不是有效整数", exception);
            }
            if (deviceId == null) throw new IllegalStateException("已选生产设备身份缺失");
            Map<String,String> context = new HashMap<>();
            put(context, "deviceCode", device.getString("deviceCode")); put(context, "deviceName", device.getString("deviceName"));
            facts.add(new CatalogItem().setCategory(Category.EQUIPMENT).setEvidenceKey("EQUIPMENT:PRODUCTION:" + deviceId)
                    .setSavedValue(device.getString("deviceId")).setSourceContext(context));
        }
    }

    private static void person(List<CatalogItem> facts, String namespace, Long id, LocalDateTime time) {
        if (id != null) facts.add(new CatalogItem().setCategory(Category.PERSON).setEvidenceKey("PERSON:" + namespace + ":" + id)
                .setSavedValue(String.valueOf(id)).setRecordedAt(time));
    }

    static boolean matches(List<CatalogItem> facts, EdhrBatchExecutionPageReqVO request, Set<Long> employees, Set<Long> users, Set<Long> devices) {
        if (StrUtil.isNotBlank(request.getTracePerson()) && facts.stream().noneMatch(item -> item.getCategory() == Category.PERSON
                && (item.getEvidenceKey().startsWith("PERSON:EMPLOYEE:") ? employees : users).contains(Long.valueOf(item.getSavedValue())))) return false;
        if (StrUtil.isNotBlank(request.getTraceEquipment()) && facts.stream().noneMatch(item -> item.getCategory() == Category.EQUIPMENT
                && (contextMatches(item, request.getTraceEquipment(), "deviceCode", "deviceName", "selectedEquipmentCode", "selectedEquipmentName", "selectedEquipmentNumber")
                || item.getEvidenceKey().startsWith("EQUIPMENT:PRODUCTION:") && devices.contains(Long.valueOf(item.getSavedValue()))))) return false;
        if ((StrUtil.isNotBlank(request.getTraceMaterial()) || StrUtil.isNotBlank(request.getTraceMaterialLot()))
                && facts.stream().noneMatch(item -> item.getCategory() == Category.MATERIAL
                && (StrUtil.isBlank(request.getTraceMaterial()) || contains(item.getLabel(), request.getTraceMaterial()) || contextMatches(item, request.getTraceMaterial(), "materialNumber"))
                && (StrUtil.isBlank(request.getTraceMaterialLot()) || contextMatches(item, request.getTraceMaterialLot(), "lotNumber")))) return false;
        if (request.getTraceBusinessTime() != null && facts.stream().noneMatch(item -> item.getCategory() == Category.PERSON && item.getRecordedAt() != null
                && !item.getRecordedAt().isBefore(request.getTraceBusinessTime()[0]) && !item.getRecordedAt().isAfter(request.getTraceBusinessTime()[1]))) return false;
        return true;
    }

    private static boolean contextMatches(CatalogItem item, String keyword, String... keys) {
        if (item.getSourceContext() == null) return false;
        return Arrays.stream(keys).anyMatch(key -> contains(item.getSourceContext().get(key), keyword));
    }
    private static boolean contains(String value, String keyword) { return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT)); }
    private static void put(Map<String,String> context, String key, String value) { if (value != null) context.put(key, value); }
}
