package cn.iocoder.yudao.module.erp.service.nastablesync;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncPlanItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncPlanItemSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncPlanRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncPlanSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncRunItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncRunOnceRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncRunPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncRunRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncTestWriteReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncTestWriteRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncTypeRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.nastablesync.ErpNasTableSyncPlanDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.nastablesync.ErpNasTableSyncPlanItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.nastablesync.ErpNasTableSyncRunDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.nastablesync.ErpNasTableSyncRunItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.nastablesync.ErpNasTableSyncPlanItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.nastablesync.ErpNasTableSyncPlanMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.nastablesync.ErpNasTableSyncRunItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.nastablesync.ErpNasTableSyncRunMapper;
import cn.iocoder.yudao.module.erp.enums.nastablesync.ErpNasTableSyncRunStatusEnum;
import cn.iocoder.yudao.module.erp.enums.nastablesync.ErpNasTableSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.nastablesync.ErpNasTableSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseOrderService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOrderService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.enums.job.JobStatusEnum;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.job.JobService;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.NAS_TABLE_SYNC_DIRECTORY_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.NAS_TABLE_SYNC_EXPORT_FAILED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.NAS_TABLE_SYNC_JOB_SAVE_FAILED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.NAS_TABLE_SYNC_PLAN_DISABLED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.NAS_TABLE_SYNC_PLAN_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.NAS_TABLE_SYNC_START_TIME_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.NAS_TABLE_SYNC_TYPE_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.NAS_TABLE_SYNC_TYPE_UNSUPPORTED;

@Service
@Validated
public class ErpNasTableSyncServiceImpl implements ErpNasTableSyncService {

    private static final String HANDLER_NAME = "erpNasTableAutoSyncJob";
    private static final String HANDLER_PARAM = "";
    private static final String DISPATCHER_CRON = "0 * * * * ?";
    private static final String DEFAULT_FILE_NAME_PATTERN = "ERP_NAS_TABLE_SYNC_{yyyyMMdd_HHmmss}.xlsx";
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Resource
    private ErpNasTableSyncPlanMapper planMapper;
    @Resource
    private ErpNasTableSyncPlanItemMapper planItemMapper;
    @Resource
    private ErpNasTableSyncRunMapper runMapper;
    @Resource
    private ErpNasTableSyncRunItemMapper runItemMapper;
    @Resource
    private JobService jobService;
    @Resource
    private NasBrowserService nasBrowserService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpPurchaseOrderService purchaseOrderService;
    @Resource
    private ErpSaleOrderService saleOrderService;

    @Override
    public ErpNasTableSyncPlanRespVO getPlan() {
        ErpNasTableSyncPlanDO plan = planMapper.selectCurrentTenantPlan();
        if (plan == null) {
            return defaultPlanResp();
        }
        return buildPlanResp(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpNasTableSyncPlanRespVO savePlan(ErpNasTableSyncPlanSaveReqVO reqVO) {
        List<ErpNasTableSyncPlanItemDO> items = normalizeItems(reqVO.getItems());
        if (Boolean.TRUE.equals(reqVO.getEnabled())) {
            validateEnabledPlan(reqVO, items);
        }
        ErpNasTableSyncPlanDO plan = planMapper.selectCurrentTenantPlan();
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (plan == null) {
            plan = new ErpNasTableSyncPlanDO().setTenantId(tenantId);
            planMapper.insert(plan);
        }
        plan.setEnabled(Boolean.TRUE.equals(reqVO.getEnabled()));
        plan.setDailyStartTime(reqVO.getDailyStartTime());
        plan.setCronExpression(reqVO.getDailyStartTime() == null ? null : buildBusinessCron(reqVO.getDailyStartTime()));
        plan.setNasDirectory(normalizeNasPath(reqVO.getNasDirectory()));
        plan.setFileNamePattern(StrUtil.blankToDefault(reqVO.getFileNamePattern(), DEFAULT_FILE_NAME_PATTERN));
        if (Boolean.TRUE.equals(plan.getEnabled())) {
            plan.setJobId(ensureDispatcherJob());
        }
        planMapper.updateById(plan);

        savePlanItems(plan.getId(), tenantId, items);
        return buildPlanResp(plan);
    }

    @Override
    public List<ErpNasTableSyncTypeRespVO> getSyncTypes() {
        return ErpNasTableSyncTypeEnum.list().stream()
                .map(type -> new ErpNasTableSyncTypeRespVO(type.getType(), type.getLabel(), type.getDefaultSheetName()))
                .toList();
    }

    @Override
    public ErpNasTableSyncTestWriteRespVO testNasWrite(ErpNasTableSyncTestWriteReqVO reqVO) {
        ErpNasTableSyncPlanDO plan = planMapper.selectCurrentTenantPlan();
        String directory = reqVO == null ? null : reqVO.getNasDirectory();
        if (StrUtil.isBlank(directory) && plan != null) {
            directory = plan.getNasDirectory();
        }
        String normalizedDirectory = normalizeNasPath(directory);
        if (StrUtil.isBlank(normalizedDirectory)) {
            throw exception(NAS_TABLE_SYNC_DIRECTORY_REQUIRED);
        }
        String outputPath = joinNasPath(normalizedDirectory,
                "nas-table-sync-write-test-" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".txt");
        byte[] content = ("NAS table auto sync write test at " + LocalDateTime.now()).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        nasBrowserService.writeFile(outputPath, new ByteArrayInputStream(content));
        return new ErpNasTableSyncTestWriteRespVO(outputPath);
    }

    @Override
    public ErpNasTableSyncRunOnceRespVO runOnce() {
        ErpNasTableSyncPlanDO plan = validateExecutablePlan();
        ErpNasTableSyncRunDO run = executePlan(plan, ErpNasTableSyncTriggerTypeEnum.MANUAL.getType());
        return new ErpNasTableSyncRunOnceRespVO()
                .setRunId(run.getId())
                .setStatus(run.getStatus())
                .setOutputPath(run.getOutputPath())
                .setFailureMessage(run.getFailureMessage());
    }

    @Override
    public PageResult<ErpNasTableSyncRunRespVO> getRunPage(ErpNasTableSyncRunPageReqVO pageReqVO) {
        PageResult<ErpNasTableSyncRunDO> pageResult = runMapper.selectPage(pageReqVO);
        List<ErpNasTableSyncRunRespVO> list = BeanUtils.toBean(pageResult.getList(), ErpNasTableSyncRunRespVO.class);
        for (ErpNasTableSyncRunRespVO respVO : list) {
            respVO.setItems(BeanUtils.toBean(runItemMapper.selectListByRunId(respVO.getId()),
                    ErpNasTableSyncRunItemRespVO.class));
        }
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public String executeAutoForCurrentTenant() {
        ErpNasTableSyncPlanDO plan = planMapper.selectCurrentTenantPlan();
        if (plan == null || !Boolean.TRUE.equals(plan.getEnabled())) {
            return "skipped: NAS table sync disabled";
        }
        if (!isAutoDue(plan)) {
            return "skipped: not due";
        }
        if (alreadyRanToday(plan)) {
            return "skipped: already run today";
        }
        ErpNasTableSyncRunDO run = executePlan(plan, ErpNasTableSyncTriggerTypeEnum.AUTO.getType());
        return "runId=" + run.getId() + ", status=" + run.getStatus();
    }

    private ErpNasTableSyncPlanRespVO defaultPlanResp() {
        ErpNasTableSyncPlanRespVO respVO = new ErpNasTableSyncPlanRespVO();
        respVO.setEnabled(false);
        respVO.setFileNamePattern(DEFAULT_FILE_NAME_PATTERN);
        respVO.setItems(getSyncTypes().stream()
                .map(type -> new ErpNasTableSyncPlanItemRespVO()
                        .setSyncType(type.getSyncType())
                        .setSheetName(type.getDefaultSheetName())
                        .setEnabled(false)
                        .setSortOrder(0))
                .toList());
        return respVO;
    }

    private ErpNasTableSyncPlanRespVO buildPlanResp(ErpNasTableSyncPlanDO plan) {
        ErpNasTableSyncPlanRespVO respVO = BeanUtils.toBean(plan, ErpNasTableSyncPlanRespVO.class);
        respVO.setItems(BeanUtils.toBean(planItemMapper.selectListByPlanId(plan.getId()),
                ErpNasTableSyncPlanItemRespVO.class));
        return respVO;
    }

    private List<ErpNasTableSyncPlanItemDO> normalizeItems(List<ErpNasTableSyncPlanItemSaveReqVO> reqItems) {
        if (CollUtil.isEmpty(reqItems)) {
            return List.of();
        }
        Set<String> seen = new HashSet<>();
        List<ErpNasTableSyncPlanItemDO> items = new ArrayList<>();
        int index = 0;
        for (ErpNasTableSyncPlanItemSaveReqVO reqItem : reqItems) {
            ErpNasTableSyncTypeEnum type = validateSyncType(reqItem.getSyncType());
            if (!seen.add(type.getType())) {
                continue;
            }
            boolean enabled = Boolean.TRUE.equals(reqItem.getEnabled());
            items.add(new ErpNasTableSyncPlanItemDO()
                    .setSyncType(type.getType())
                    .setEnabled(enabled)
                    .setSortOrder(reqItem.getSortOrder() == null ? index * 10 : reqItem.getSortOrder())
                    .setSheetName(StrUtil.blankToDefault(reqItem.getSheetName(), type.getDefaultSheetName())));
            index++;
        }
        return items;
    }

    private void savePlanItems(Long planId, Long tenantId, List<ErpNasTableSyncPlanItemDO> items) {
        Map<String, ErpNasTableSyncPlanItemDO> existingByType = planItemMapper.selectListByPlanId(planId).stream()
                .collect(java.util.stream.Collectors.toMap(ErpNasTableSyncPlanItemDO::getSyncType, item -> item));
        Set<String> requestedTypes = new HashSet<>();
        for (ErpNasTableSyncPlanItemDO item : items) {
            requestedTypes.add(item.getSyncType());
            ErpNasTableSyncPlanItemDO existing = existingByType.get(item.getSyncType());
            if (existing == null) {
                item.setTenantId(tenantId);
                item.setPlanId(planId);
                planItemMapper.insert(item);
                continue;
            }
            existing.setEnabled(item.getEnabled())
                    .setSortOrder(item.getSortOrder())
                    .setSheetName(item.getSheetName());
            planItemMapper.updateById(existing);
        }
        existingByType.values().stream()
                .filter(existing -> !requestedTypes.contains(existing.getSyncType()))
                .forEach(existing -> {
                    existing.setEnabled(false);
                    planItemMapper.updateById(existing);
                });
    }

    private ErpNasTableSyncTypeEnum validateSyncType(String syncType) {
        try {
            return ErpNasTableSyncTypeEnum.requiredOf(syncType);
        } catch (IllegalArgumentException ex) {
            throw exception(NAS_TABLE_SYNC_TYPE_UNSUPPORTED, syncType);
        }
    }

    private void validateEnabledPlan(ErpNasTableSyncPlanSaveReqVO reqVO, List<ErpNasTableSyncPlanItemDO> items) {
        if (reqVO.getDailyStartTime() == null) {
            throw exception(NAS_TABLE_SYNC_START_TIME_REQUIRED);
        }
        if (StrUtil.isBlank(normalizeNasPath(reqVO.getNasDirectory()))) {
            throw exception(NAS_TABLE_SYNC_DIRECTORY_REQUIRED);
        }
        if (items.stream().noneMatch(item -> Boolean.TRUE.equals(item.getEnabled()))) {
            throw exception(NAS_TABLE_SYNC_TYPE_REQUIRED);
        }
    }

    private ErpNasTableSyncPlanDO validateExecutablePlan() {
        ErpNasTableSyncPlanDO plan = planMapper.selectCurrentTenantPlan();
        if (plan == null) {
            throw exception(NAS_TABLE_SYNC_PLAN_NOT_EXISTS);
        }
        if (!Boolean.TRUE.equals(plan.getEnabled())) {
            throw exception(NAS_TABLE_SYNC_PLAN_DISABLED);
        }
        List<ErpNasTableSyncPlanItemDO> enabledItems = enabledItems(plan);
        if (CollUtil.isEmpty(enabledItems)) {
            throw exception(NAS_TABLE_SYNC_TYPE_REQUIRED);
        }
        if (StrUtil.isBlank(plan.getNasDirectory())) {
            throw exception(NAS_TABLE_SYNC_DIRECTORY_REQUIRED);
        }
        return plan;
    }

    private Long ensureDispatcherJob() {
        JobSaveReqVO saveReqVO = new JobSaveReqVO();
        saveReqVO.setName("NAS 表格自动同步 Job");
        saveReqVO.setHandlerName(HANDLER_NAME);
        saveReqVO.setHandlerParam("");
        saveReqVO.setCronExpression(DISPATCHER_CRON);
        saveReqVO.setRetryCount(0);
        saveReqVO.setRetryInterval(0);
        saveReqVO.setMonitorTimeout(0);
        try {
            JobDO job = findDispatcherJob();
            if (job == null) {
                return jobService.createJob(saveReqVO);
            }
            if (Objects.equals(job.getStatus(), JobStatusEnum.STOP.getStatus())) {
                jobService.updateJobStatus(job.getId(), JobStatusEnum.NORMAL.getStatus());
                job = jobService.getJob(job.getId());
            }
            if (Objects.equals(job.getStatus(), JobStatusEnum.NORMAL.getStatus())) {
                saveReqVO.setId(job.getId());
                jobService.updateJob(saveReqVO);
            }
            return job.getId();
        } catch (SchedulerException ex) {
            throw exception(NAS_TABLE_SYNC_JOB_SAVE_FAILED, StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()));
        }
    }

    private JobDO findDispatcherJob() {
        JobPageReqVO reqVO = new JobPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setHandlerName(HANDLER_NAME);
        return jobService.getJobPage(reqVO).getList().stream()
                .filter(job -> HANDLER_NAME.equals(job.getHandlerName()))
                .findFirst()
                .orElse(null);
    }

    private ErpNasTableSyncRunDO executePlan(ErpNasTableSyncPlanDO plan, String triggerType) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<ErpNasTableSyncPlanItemDO> enabledItems = enabledItems(plan);
        ErpNasTableSyncRunDO run = new ErpNasTableSyncRunDO()
                .setTenantId(tenantId)
                .setPlanId(plan.getId())
                .setTriggerType(triggerType)
                .setStatus(ErpNasTableSyncRunStatusEnum.RUNNING.getStatus())
                .setStartedAt(LocalDateTime.now())
                .setTotalTableCount(enabledItems.size())
                .setSuccessTableCount(0)
                .setFailedTableCount(0);
        runMapper.insert(run);
        List<ExportedTable> tables = new ArrayList<>();
        int successCount = 0;
        try {
            for (ErpNasTableSyncPlanItemDO item : enabledItems) {
                ExportedTable table = exportTable(item);
                tables.add(table);
                runItemMapper.insert(new ErpNasTableSyncRunItemDO()
                        .setTenantId(tenantId)
                        .setRunId(run.getId())
                        .setSyncType(item.getSyncType())
                        .setStatus(ErpNasTableSyncRunStatusEnum.SUCCESS.getStatus())
                        .setSheetName(table.sheetName())
                        .setRowCount(table.rowCount()));
                successCount++;
            }
            String outputPath = buildOutputPath(plan, run.getStartedAt());
            nasBrowserService.writeFile(outputPath, new ByteArrayInputStream(buildWorkbook(tables)));
            run.setStatus(ErpNasTableSyncRunStatusEnum.SUCCESS.getStatus())
                    .setEndedAt(LocalDateTime.now())
                    .setOutputPath(outputPath)
                    .setSuccessTableCount(successCount)
                    .setFailedTableCount(0);
        } catch (Exception ex) {
            String failureMessage = rootMessage(ex);
            run.setStatus(ErpNasTableSyncRunStatusEnum.FAILED.getStatus())
                    .setEndedAt(LocalDateTime.now())
                    .setSuccessTableCount(successCount)
                    .setFailedTableCount(Math.max(enabledItems.size() - successCount, 1))
                    .setFailureMessage(failureMessage);
        }
        runMapper.updateById(run);
        planMapper.updateById(new ErpNasTableSyncPlanDO()
                .setId(plan.getId())
                .setLastRunId(run.getId())
                .setLastStatus(run.getStatus()));
        return run;
    }

    private List<ErpNasTableSyncPlanItemDO> enabledItems(ErpNasTableSyncPlanDO plan) {
        return planItemMapper.selectListByPlanId(plan.getId()).stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .sorted(Comparator.comparing(ErpNasTableSyncPlanItemDO::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private ExportedTable exportTable(ErpNasTableSyncPlanItemDO item) {
        ErpNasTableSyncTypeEnum type = validateSyncType(item.getSyncType());
        return switch (type) {
            case PRODUCT -> exportProduct(item.getSheetName());
            case STOCK -> exportStock(item.getSheetName());
            case PURCHASE_ORDER -> exportPurchaseOrder(item.getSheetName());
            case SALE_ORDER -> exportSaleOrder(item.getSheetName());
        };
    }

    private ExportedTable exportProduct(String sheetName) {
        ErpProductPageReqVO reqVO = new ErpProductPageReqVO();
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<?> rows = productService.getProductVOPage(reqVO).getList();
        return exportedBeans(sheetName, rows);
    }

    private ExportedTable exportStock(String sheetName) {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<?> rows = stockService.getStockPage(reqVO).getList();
        return exportedBeans(sheetName, rows);
    }

    private ExportedTable exportPurchaseOrder(String sheetName) {
        ErpPurchaseOrderPageReqVO reqVO = new ErpPurchaseOrderPageReqVO();
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<?> rows = purchaseOrderService.getPurchaseOrderPage(reqVO).getList();
        return exportedBeans(sheetName, rows);
    }

    private ExportedTable exportSaleOrder(String sheetName) {
        ErpSaleOrderPageReqVO reqVO = new ErpSaleOrderPageReqVO();
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<?> rows = saleOrderService.getSaleOrderPage(reqVO).getList();
        return exportedBeans(sheetName, rows);
    }

    private ExportedTable exportedBeans(String sheetName, List<?> rows) {
        if (rows == null) {
            rows = List.of();
        }
        List<String> headers = rows.isEmpty() ? List.of("empty") : propertyNames(rows.get(0).getClass());
        List<List<String>> values = new ArrayList<>();
        for (Object row : rows) {
            values.add(readValues(row, headers));
        }
        return new ExportedTable(sanitizeSheetName(sheetName), headers, values);
    }

    private List<String> propertyNames(Class<?> type) {
        try {
            List<String> names = new ArrayList<>();
            for (PropertyDescriptor descriptor : Introspector.getBeanInfo(type, Object.class).getPropertyDescriptors()) {
                if (descriptor.getReadMethod() == null || "transMap".equals(descriptor.getName())) {
                    continue;
                }
                names.add(descriptor.getName());
            }
            names.sort(String::compareTo);
            return names;
        } catch (IntrospectionException ex) {
            throw exception(NAS_TABLE_SYNC_EXPORT_FAILED, rootMessage(ex));
        }
    }

    private List<String> readValues(Object bean, List<String> headers) {
        try {
            List<PropertyDescriptor> descriptors = Arrays.asList(Introspector.getBeanInfo(bean.getClass(), Object.class).getPropertyDescriptors());
            List<String> values = new ArrayList<>();
            for (String header : headers) {
                PropertyDescriptor descriptor = descriptors.stream()
                        .filter(item -> header.equals(item.getName()))
                        .findFirst()
                        .orElse(null);
                Object value = descriptor == null ? null : descriptor.getReadMethod().invoke(bean);
                values.add(value == null ? "" : String.valueOf(value));
            }
            return values;
        } catch (IntrospectionException | IllegalAccessException | InvocationTargetException ex) {
            throw exception(NAS_TABLE_SYNC_EXPORT_FAILED, rootMessage(ex));
        }
    }

    private byte[] buildWorkbook(List<ExportedTable> tables) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            for (ExportedTable table : tables) {
                Sheet sheet = workbook.createSheet(table.sheetName());
                Row headerRow = sheet.createRow(0);
                for (int column = 0; column < table.headers().size(); column++) {
                    headerRow.createCell(column).setCellValue(table.headers().get(column));
                }
                for (int rowIndex = 0; rowIndex < table.rows().size(); rowIndex++) {
                    Row row = sheet.createRow(rowIndex + 1);
                    List<String> values = table.rows().get(rowIndex);
                    for (int column = 0; column < values.size(); column++) {
                        Cell cell = row.createCell(column);
                        cell.setCellValue(values.get(column));
                    }
                }
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private String buildOutputPath(ErpNasTableSyncPlanDO plan, LocalDateTime startedAt) {
        String fileName = StrUtil.blankToDefault(plan.getFileNamePattern(), DEFAULT_FILE_NAME_PATTERN)
                .replace("{yyyyMMdd_HHmmss}", startedAt.format(FILE_TIME_FORMATTER))
                .replace("{date}", startedAt.toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE));
        if (!fileName.toLowerCase().endsWith(".xlsx")) {
            fileName = fileName + ".xlsx";
        }
        return joinNasPath(plan.getNasDirectory(), fileName);
    }

    private boolean isAutoDue(ErpNasTableSyncPlanDO plan) {
        LocalTime startTime = plan.getDailyStartTime();
        return startTime != null && !LocalTime.now().isBefore(startTime);
    }

    private boolean alreadyRanToday(ErpNasTableSyncPlanDO plan) {
        ErpNasTableSyncRunDO latest = runMapper.selectLatestByPlanId(plan.getId());
        return latest != null
                && ErpNasTableSyncTriggerTypeEnum.AUTO.getType().equals(latest.getTriggerType())
                && latest.getStartedAt() != null
                && LocalDate.now().equals(latest.getStartedAt().toLocalDate());
    }

    private static String buildBusinessCron(LocalTime time) {
        return "0 " + time.getMinute() + " " + time.getHour() + " * * ?";
    }

    private static String normalizeNasPath(String pathText) {
        String raw = pathText == null ? "" : pathText.replace("\\", "/");
        if (raw.isBlank()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (String part : raw.split("/")) {
            if (part.isEmpty() || ".".equals(part) || "..".equals(part)) {
                continue;
            }
            parts.add(part);
        }
        return String.join("/", parts);
    }

    private static String joinNasPath(String directory, String fileName) {
        String normalizedDirectory = normalizeNasPath(directory);
        String normalizedFileName = normalizeNasPath(fileName);
        return StrUtil.isBlank(normalizedDirectory) ? normalizedFileName : normalizedDirectory + "/" + normalizedFileName;
    }

    private static String sanitizeSheetName(String sheetName) {
        String value = StrUtil.blankToDefault(sheetName, "数据").replaceAll("[\\\\/:*?\\[\\]]", "_");
        return value.length() > 31 ? value.substring(0, 31) : value;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return StrUtil.blankToDefault(current.getMessage(), current.getClass().getSimpleName());
    }

    private record ExportedTable(String sheetName, List<String> headers, List<List<String>> rows) {
        int rowCount() {
            return rows.size();
        }
    }
}
