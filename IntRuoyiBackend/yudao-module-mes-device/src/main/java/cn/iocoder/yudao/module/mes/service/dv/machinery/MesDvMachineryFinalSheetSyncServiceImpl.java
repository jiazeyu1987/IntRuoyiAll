package cn.iocoder.yudao.module.mes.service.dv.machinery;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryFinalSyncRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.wm.barcode.vo.MesWmBarcodeSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryTypeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.barcode.MesWmBarcodeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryTypeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkshopMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.enums.dv.MesDvMachineryStatusEnum;
import cn.iocoder.yudao.module.mes.enums.wm.BarcodeBizTypeEnum;
import cn.iocoder.yudao.module.mes.service.dv.checkplan.MesDvCheckPlanMachineryService;
import cn.iocoder.yudao.module.mes.service.dv.checkrecord.MesDvCheckRecordService;
import cn.iocoder.yudao.module.mes.service.dv.maintenrecord.MesDvMaintenRecordService;
import cn.iocoder.yudao.module.mes.service.dv.repair.MesDvRepairService;
import cn.iocoder.yudao.module.mes.service.wm.barcode.MesWmBarcodeService;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
public class MesDvMachineryFinalSheetSyncServiceImpl implements MesDvMachineryFinalSheetSyncService {

    private static final String SHEET_NAME = "Sheet1";
    private static final String PLACEHOLDER_CODE = "/";
    private static final String DEFAULT_WORKSHOP_CODE = "AUTO-WSHOP";
    private static final String DEFAULT_TYPE_CODE = "DEFAULT-MACHINERY-TYPE";
    private static final String DEFAULT_TYPE_NAME = "\u9ED8\u8BA4\u8BBE\u5907\u7C7B\u578B";
    private static final BigDecimal DAY_HOURS = new BigDecimal("10.5");
    private static final List<String> EXPECTED_HEADERS = List.of(
            "\u4EA7\u54C1\u540D\u79F0",
            "\u7269\u6599\u7F16\u7801",
            "\u8BBE\u5907\u7F16\u7801",
            "\u5DE5\u5E8F\u540D\u79F0",
            "\u8BBE\u5907\u540D\u79F0",
            "\u8BBE\u5907\u6570\u91CF",
            "10.5\u5C0F\u65F6\u65E5\u4EA7\u80FD",
            "\u4EBA\u5DE5"
    );

    @Resource
    private MesDvMachineryMapper machineryMapper;
    @Resource
    private MesDvMachineryProcessMapper machineryProcessMapper;
    @Resource
    private MesDvMachineryTypeMapper machineryTypeMapper;
    @Resource
    private MesMdWorkshopMapper workshopMapper;
    @Resource
    private MesProProcessMapper processMapper;
    @Resource
    private Sheet1MachineryProcessExcelParser parser;
    @Resource
    private MesWmBarcodeService barcodeService;
    @Resource
    private MesDvCheckPlanMachineryService checkPlanMachineryService;
    @Resource
    private MesDvCheckRecordService checkRecordService;
    @Resource
    private MesDvMaintenRecordService maintenRecordService;
    @Resource
    private MesDvRepairService repairService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesDvMachineryFinalSyncRespVO syncFinalSheet(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ServiceExceptionUtil.invalidParamException("\u6700\u7EC8\u7248 Excel \u4E0D\u80FD\u4E3A\u7A7A");
        }

        ParsedSheet parsedSheet = parseSheet(file);
        if (CollUtil.isEmpty(parsedSheet.validRows())) {
            throw ServiceExceptionUtil.invalidParamException("\u6700\u7EC8\u7248 Excel \u4E2D\u6CA1\u6709\u53EF\u5BFC\u5165\u7684\u8BBE\u5907\u884C");
        }
        validateUniqueCapacityByMachineryAndProcess(parsedSheet.validRows());

        MesMdWorkshopDO defaultWorkshop = workshopMapper.selectByCode(DEFAULT_WORKSHOP_CODE);
        if (defaultWorkshop == null) {
            throw ServiceExceptionUtil.invalidParamException("\u9ED8\u8BA4\u8F66\u95F4 {} \u4E0D\u5B58\u5728", DEFAULT_WORKSHOP_CODE);
        }
        MesDvMachineryTypeDO defaultType = resolveOrCreateDefaultType();

        Map<String, DeviceAggregate> targetByCode = aggregateByDeviceCode(parsedSheet.validRows());
        List<MesDvMachineryDO> currentMachineries = machineryMapper.selectList();
        Map<String, MesDvMachineryDO> currentByCode = currentMachineries.stream()
                .collect(Collectors.toMap(MesDvMachineryDO::getCode, item -> item, (a, b) -> a, LinkedHashMap::new));

        List<MesDvMachineryDO> toDelete = currentMachineries.stream()
                .filter(item -> !targetByCode.containsKey(item.getCode()))
                .toList();
        validateMachineriesCanBeDeleted(toDelete);
        deleteObsoleteMachineries(toDelete);

        int createdCount = 0;
        int updatedCount = 0;
        Map<String, Long> machineryIdByCode = new LinkedHashMap<>();
        for (DeviceAggregate aggregate : targetByCode.values()) {
            MesDvMachineryDO existing = currentByCode.get(aggregate.code());
            MesDvMachineryDO syncTarget = buildMachineryTarget(existing == null ? null : existing.getId(),
                    aggregate, defaultType.getId(), defaultWorkshop.getId());
            if (existing == null) {
                machineryMapper.insert(syncTarget);
                createdCount++;
            } else {
                machineryMapper.updateById(syncTarget);
                updatedCount++;
            }
            machineryIdByCode.put(aggregate.code(), syncTarget.getId());
            syncMachineryBarcode(syncTarget.getId(), syncTarget.getCode(), syncTarget.getName());
        }

        machineryProcessMapper.deleteByMachineryIds(machineryIdByCode.values());
        List<MesDvMachineryProcessDO> detailRows = buildProcessRows(parsedSheet.validRows(), machineryIdByCode);
        machineryProcessMapper.insertBatch(detailRows);

        return MesDvMachineryFinalSyncRespVO.builder()
                .excelEffectiveRowCount(parsedSheet.validRows().size())
                .ignoredPlaceholderRowCount(parsedSheet.ignoredPlaceholderRowCount())
                .machineryCount(targetByCode.size())
                .processDetailCount(detailRows.size())
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .deletedCount(toDelete.size())
                .defaultMachineryTypeCode(DEFAULT_TYPE_CODE)
                .defaultWorkshopCode(DEFAULT_WORKSHOP_CODE)
                .build();
    }

    private ParsedSheet parseSheet(MultipartFile file) {
        Sheet1MachineryProcessExcelParser.ParsedSheet parsedSheet = parser.parse(file);
        List<FinalSheetRow> validRows = parsedSheet.deviceRows().stream()
                .map(row -> new FinalSheetRow(row.sourceRowNo(), row.lineName(), row.machineryCode(), row.processName(),
                        row.deviceName(), row.deviceQuantity(), row.tenHalfHourDailyCapacity(),
                        row.standardHourlyCapacity()))
                .toList();
        return new ParsedSheet(validRows, parsedSheet.ignoredPlaceholderRowCount());
    }

    private void validateHeader(Row headerRow, DataFormatter formatter) {
        if (headerRow == null) {
            throw ServiceExceptionUtil.invalidParamException("Excel \u7F3A\u5C11\u8868\u5934");
        }
        for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {
            String actual = readString(headerRow.getCell(i), formatter);
            String expected = EXPECTED_HEADERS.get(i);
            if (!Objects.equals(expected, actual)) {
                throw ServiceExceptionUtil.invalidParamException(
                        "Excel \u8868\u5934\u4E0D\u7B26\u5408\u9884\u671F: \u7B2C {} \u5217\u5E94\u4E3A [{}], \u5B9E\u9645\u4E3A [{}]",
                        i + 1, expected, actual);
            }
        }
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter) {
        for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {
            if (StrUtil.isNotBlank(readString(row.getCell(i), formatter))) {
                return false;
            }
        }
        return true;
    }

    private String readString(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        return StrUtil.trim(formatter.formatCellValue(cell));
    }

    private BigDecimal readPositiveDecimal(Cell cell, DataFormatter formatter, String fieldName, int sourceRowNo) {
        String raw = readString(cell, formatter);
        if (StrUtil.isBlank(raw)) {
            throw ServiceExceptionUtil.invalidParamException("Excel \u7B2C {} \u884C{} \u4E0D\u80FD\u4E3A\u7A7A", sourceRowNo, fieldName);
        }
        try {
            BigDecimal value = new BigDecimal(raw);
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                throw ServiceExceptionUtil.invalidParamException("Excel \u7B2C {} \u884C{} \u5FC5\u987B\u5927\u4E8E 0", sourceRowNo, fieldName);
            }
            return value;
        } catch (NumberFormatException exception) {
            throw ServiceExceptionUtil.invalidParamException("Excel \u7B2C {} \u884C{} \u4E0D\u662F\u6709\u6548\u6570\u503C", sourceRowNo, fieldName);
        }
    }

    private Map<String, DeviceAggregate> aggregateByDeviceCode(List<FinalSheetRow> rows) {
        Map<String, DeviceAggregate> aggregateMap = new LinkedHashMap<>();
        for (FinalSheetRow row : rows) {
            DeviceAggregate aggregate = aggregateMap.computeIfAbsent(row.machineryCode(),
                    code -> new DeviceAggregate(code));
            aggregate.add(row);
        }
        return aggregateMap;
    }

    private void validateUniqueCapacityByMachineryAndProcess(List<FinalSheetRow> rows) {
        Map<MachineryProcessKey, FinalSheetRow> firstRowByKey = new LinkedHashMap<>();
        for (FinalSheetRow row : rows) {
            MachineryProcessKey key = new MachineryProcessKey(row.machineryCode(), row.processName());
            FinalSheetRow existing = firstRowByKey.putIfAbsent(key, row);
            if (existing == null) {
                continue;
            }
            if (existing.standardHourlyCapacity().compareTo(row.standardHourlyCapacity()) == 0) {
                continue;
            }
            throw ServiceExceptionUtil.invalidParamException(
                    "\u540C\u4E00\u8BBE\u5907\u540C\u4E00\u5DE5\u5E8F\u53EA\u80FD\u914D\u7F6E\u4E00\u4E2A\u6807\u51C6\u5C0F\u65F6\u4EA7\u80FD: \u8BBE\u5907\u7F16\u7801 {}, \u5DE5\u5E8F {}, Excel \u884C {} \u7684\u4EA7\u80FD {} \u4E0E Excel \u884C {} \u7684\u4EA7\u80FD {} \u4E0D\u4E00\u81F4",
                    row.machineryCode(), row.processName(),
                    existing.sourceRowNo(), existing.standardHourlyCapacity().toPlainString(),
                    row.sourceRowNo(), row.standardHourlyCapacity().toPlainString());
        }
    }

    private MesDvMachineryTypeDO resolveOrCreateDefaultType() {
        MesDvMachineryTypeDO type = machineryTypeMapper.selectByCode(DEFAULT_TYPE_CODE);
        if (type != null) {
            return type;
        }
        MesDvMachineryTypeDO create = MesDvMachineryTypeDO.builder()
                .code(DEFAULT_TYPE_CODE)
                .name(DEFAULT_TYPE_NAME)
                .parentId(MesDvMachineryTypeDO.PARENT_ID_ROOT)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .sort(1)
                .remark("Final balloon sheet default machinery type")
                .build();
        machineryTypeMapper.insert(create);
        return create;
    }

    private void validateMachineriesCanBeDeleted(List<MesDvMachineryDO> machineries) {
        for (MesDvMachineryDO machinery : machineries) {
            Long machineryId = machinery.getId();
            if (checkPlanMachineryService.getCheckPlanMachineryCountByMachineryId(machineryId) > 0) {
                throw ServiceExceptionUtil.invalidParamException(
                        "\u8BBE\u5907 {} \u5DF2\u5173\u8054\u70B9\u68C0\u8BA1\u5212\uFF0C\u65E0\u6CD5\u4ECE\u6700\u7EC8\u7248\u540C\u6B65\u4E2D\u5220\u9664", machinery.getCode());
            }
            if (checkRecordService.getCheckRecordCountByMachineryId(machineryId) > 0) {
                throw ServiceExceptionUtil.invalidParamException(
                        "\u8BBE\u5907 {} \u5DF2\u5173\u8054\u70B9\u68C0\u8BB0\u5F55\uFF0C\u65E0\u6CD5\u4ECE\u6700\u7EC8\u7248\u540C\u6B65\u4E2D\u5220\u9664", machinery.getCode());
            }
            if (maintenRecordService.getMaintenRecordCountByMachineryId(machineryId) > 0) {
                throw ServiceExceptionUtil.invalidParamException(
                        "\u8BBE\u5907 {} \u5DF2\u5173\u8054\u4FDD\u517B\u8BB0\u5F55\uFF0C\u65E0\u6CD5\u4ECE\u6700\u7EC8\u7248\u540C\u6B65\u4E2D\u5220\u9664", machinery.getCode());
            }
            if (repairService.getRepairCountByMachineryId(machineryId) > 0) {
                throw ServiceExceptionUtil.invalidParamException(
                        "\u8BBE\u5907 {} \u5DF2\u5173\u8054\u7EF4\u4FEE\u5DE5\u5355\uFF0C\u65E0\u6CD5\u4ECE\u6700\u7EC8\u7248\u540C\u6B65\u4E2D\u5220\u9664", machinery.getCode());
            }
        }
    }

    private void deleteObsoleteMachineries(List<MesDvMachineryDO> machineries) {
        if (CollUtil.isEmpty(machineries)) {
            return;
        }
        List<Long> machineryIds = machineries.stream().map(MesDvMachineryDO::getId).toList();
        machineryProcessMapper.deleteByMachineryIds(machineryIds);
        for (MesDvMachineryDO machinery : machineries) {
            MesWmBarcodeDO barcode = barcodeService.getBarcodeByBizTypeAndBizId(
                    BarcodeBizTypeEnum.MACHINERY.getValue(), machinery.getId());
            if (barcode != null) {
                barcodeService.deleteBarcode(barcode.getId());
            }
            machineryMapper.deleteById(machinery.getId());
        }
    }

    private MesDvMachineryDO buildMachineryTarget(Long existingId, DeviceAggregate aggregate,
                                                  Long defaultTypeId, Long defaultWorkshopId) {
        BigDecimal summaryCapacity = aggregate.hasSingleProcessAndSingleCapacity()
                ? aggregate.singleStandardHourlyCapacity() : null;
        String summaryProcessName = aggregate.hasSingleProcessAndSingleCapacity()
                ? aggregate.singleProcessName() : null;
        return MesDvMachineryDO.builder()
                .id(existingId)
                .code(aggregate.code())
                .name(aggregate.deviceName())
                .brand(null)
                .specification(null)
                .machineryTypeId(defaultTypeId)
                .workshopId(defaultWorkshopId)
                .processName(summaryProcessName)
                .standardHourlyCapacity(summaryCapacity)
                .status(MesDvMachineryStatusEnum.PRODUCING.getStatus())
                .remark(null)
                .build();
    }

    private void syncMachineryBarcode(Long machineryId, String code, String name) {
        MesWmBarcodeDO barcode = barcodeService.getBarcodeByBizTypeAndBizId(
                BarcodeBizTypeEnum.MACHINERY.getValue(), machineryId);
        if (barcode == null) {
            barcodeService.autoGenerateBarcode(BarcodeBizTypeEnum.MACHINERY.getValue(), machineryId, code, name);
            return;
        }
        MesWmBarcodeSaveReqVO updateReqVO = new MesWmBarcodeSaveReqVO();
        updateReqVO.setId(barcode.getId());
        updateReqVO.setConfigId(barcode.getConfigId());
        updateReqVO.setBizType(barcode.getBizType());
        updateReqVO.setContent(barcode.getContent());
        updateReqVO.setBizId(machineryId);
        updateReqVO.setBizCode(code);
        updateReqVO.setBizName(name);
        updateReqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        updateReqVO.setRemark(barcode.getRemark());
        barcodeService.updateBarcode(updateReqVO);
    }

    private List<MesDvMachineryProcessDO> buildProcessRows(List<FinalSheetRow> rows,
                                                           Map<String, Long> machineryIdByCode) {
        Map<String, Long> processIdByName = resolveEnabledProcessIdMap(rows);
        List<MesDvMachineryProcessDO> detailRows = new ArrayList<>();
        for (FinalSheetRow row : rows) {
            detailRows.add(MesDvMachineryProcessDO.builder()
                    .machineryId(machineryIdByCode.get(row.machineryCode()))
                    .processId(processIdByName.get(row.processName()))
                    .machineryCode(row.machineryCode())
                    .lineName(row.lineName())
                    .processName(row.processName())
                    .deviceName(row.deviceName())
                    .deviceQuantity(row.deviceQuantity())
                    .tenHalfHourDailyCapacity(row.tenHalfHourDailyCapacity())
                    .standardHourlyCapacity(row.standardHourlyCapacity())
                    .sourceRowNo(row.sourceRowNo())
                    .remark(null)
                    .build());
        }
        return detailRows;
    }

    private Map<String, Long> resolveEnabledProcessIdMap(List<FinalSheetRow> rows) {
        Set<String> processNames = rows.stream().map(FinalSheetRow::processName).collect(Collectors.toCollection(LinkedHashSet::new));
        List<MesProProcessDO> enabledProcesses = processMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
        Map<String, Long> processIdByName = new LinkedHashMap<>();
        for (MesProProcessDO process : enabledProcesses) {
            if (!processNames.contains(process.getName())) {
                continue;
            }
            Long existing = processIdByName.putIfAbsent(process.getName(), process.getId());
            if (existing != null) {
                throw ServiceExceptionUtil.invalidParamException("启用工序中存在重名记录: {}", process.getName());
            }
        }
        if (processIdByName.size() != processNames.size()) {
            Set<String> missing = new LinkedHashSet<>(processNames);
            missing.removeAll(processIdByName.keySet());
            throw ServiceExceptionUtil.invalidParamException("以下工序不存在或未启用: {}", String.join(", ", missing));
        }
        return processIdByName;
    }

    private record ParsedSheet(List<FinalSheetRow> validRows, Integer ignoredPlaceholderRowCount) {
    }

    private record FinalSheetRow(Integer sourceRowNo, String lineName, String machineryCode, String processName, String deviceName,
                                 BigDecimal deviceQuantity, BigDecimal tenHalfHourDailyCapacity,
                                 BigDecimal standardHourlyCapacity) {
    }

    private record MachineryProcessKey(String machineryCode, String processName) {
    }

    private static final class DeviceAggregate {
        private final String code;
        private final List<FinalSheetRow> rows = new ArrayList<>();
        private final Set<String> deviceNames = new LinkedHashSet<>();
        private final Set<String> processNames = new LinkedHashSet<>();
        private final Set<BigDecimal> standardHourlyCapacities = new LinkedHashSet<>();

        private DeviceAggregate(String code) {
            this.code = code;
        }

        private void add(FinalSheetRow row) {
            rows.add(row);
            deviceNames.add(row.deviceName());
            processNames.add(row.processName());
            standardHourlyCapacities.add(row.standardHourlyCapacity());
            if (deviceNames.size() > 1) {
                throw ServiceExceptionUtil.invalidParamException(
                        "\u8BBE\u5907\u7F16\u7801 {} \u5728 Excel \u4E2D\u5BF9\u5E94\u4E86\u591A\u4E2A\u8BBE\u5907\u540D\u79F0: {}",
                        code, String.join(", ", deviceNames));
            }
        }

        private String code() {
            return code;
        }

        private String deviceName() {
            return deviceNames.iterator().next();
        }

        private boolean hasSingleProcessAndSingleCapacity() {
            return processNames.size() == 1 && standardHourlyCapacities.size() == 1;
        }

        private String singleProcessName() {
            return hasSingleProcessAndSingleCapacity() ? processNames.iterator().next() : null;
        }

        private BigDecimal singleStandardHourlyCapacity() {
            return hasSingleProcessAndSingleCapacity() ? standardHourlyCapacities.iterator().next() : null;
        }
    }
}
