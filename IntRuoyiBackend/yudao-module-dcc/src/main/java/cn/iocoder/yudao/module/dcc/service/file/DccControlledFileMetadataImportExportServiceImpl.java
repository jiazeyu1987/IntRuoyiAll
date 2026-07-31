package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMetadataExportExcelVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMetadataImportPreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMetadataImportRowRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMetadataUpdateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRecognitionMigrationExportExcelVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRecognitionMigrationImportPreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRecognitionMigrationImportRowRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRecognitionRecordExportExcelVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRecognitionRecordDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRecognitionRecordMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED;

@Service
public class DccControlledFileMetadataImportExportServiceImpl implements DccControlledFileMetadataImportExportService {

    private static final List<String> EXPECTED_HEADERS = List.of("受控文件ID", "文件名称", "文件编号");
    private static final List<String> RECOGNITION_RECORD_EXPORT_HEADERS = List.of(
            "目录路径", "文件名称", "受控文件ID", "识别状态", "产品名称", "产品编码",
            "命中别名ID", "命中别名文本", "命中别名来源", "匹配方式", "匹配文本", "失败原因",
            "文件类型1", "文件类型2", "文件类型3", "文件类型4", "文件类型5",
            "识别版本", "批量任务ID", "识别人", "识别时间");
    private static final List<String> RECOGNITION_MIGRATION_HEADERS = List.of(
            "目录路径", "文件名称", "文件编号", "测试服受控文件ID", "识别状态", "产品名称", "产品编码",
            "项目名称", "项目编码", "测试服项目ID", "命中别名ID", "命中别名文本", "命中别名来源",
            "匹配方式", "匹配文本", "失败原因", "文件类型1", "文件类型2", "文件类型3", "文件类型4",
            "文件类型5", "识别版本", "批量任务ID", "识别人", "识别时间");
    private static final String ACTION_UPDATE = "UPDATE";
    private static final String ACTION_UNCHANGED = "UNCHANGED";
    private static final String ACTION_INVALID = "INVALID";
    private static final String ACTION_APPLICABLE = "APPLICABLE";
    private static final String ACTION_BLOCKED = "BLOCKED";

    @Resource
    private DccControlledFileQueryService controlledFileQueryService;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileRecognitionRecordMapper recognitionRecordMapper;
    @Resource
    private DccFileDirectoryMapper directoryMapper;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private DccControlledFileMetadataUpdateService metadataUpdateService;
    @Resource
    private PermissionApi permissionApi;

    @Override
    public List<DccControlledFileMetadataExportExcelVO> getExportList(Long userId, DccControlledFilePageReqVO reqVO) {
        validateDocControlRole(userId);
        return controlledFileQueryService.listControlledFileBrowserCandidates(userId, reqVO).stream()
                .filter(this::hasRecognizedMetadata)
                .map(DccControlledFileMetadataExportExcelVO::from)
                .toList();
    }

    @Override
    public byte[] buildImportTemplate() {
        return writeWorkbook(List.of(DccControlledFileMetadataExportExcelVO.builder()
                .controlledFileId(900L)
                .fileName("SOP-示例")
                .fileNumber("DOC-001")
                .build()));
    }

    @Override
    public byte[] buildExportExcel(Long userId, DccControlledFilePageReqVO reqVO) {
        return writeWorkbook(getExportList(userId, reqVO));
    }

    @Override
    public byte[] buildRecognitionRecordExportExcel(Long userId, DccControlledFilePageReqVO reqVO) {
        validateDocControlRole(userId);
        List<DccControlledFileDO> files = controlledFileQueryService.listControlledFileBrowserCandidates(userId, reqVO);
        String recognitionStatus = StrUtil.trimToNull(reqVO.getRecognitionStatus());
        if (reqVO.getBatchRecognitionTaskId() != null) {
            List<DccControlledFileRecognitionRecordExportExcelVO> taskRows = buildRecognitionRecordExportRows(
                    recognitionRecordMapper.selectListByBatchTaskId(reqVO.getBatchRecognitionTaskId(), recognitionStatus),
                    files);
            return writeRecognitionRecordWorkbook(taskRows);
        }
        List<Long> fileIds = files.stream()
                .map(DccControlledFileDO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (fileIds.isEmpty()) {
            return writeRecognitionRecordWorkbook(List.of());
        }
        Map<Long, DccControlledFileDO> fileById = new LinkedHashMap<>();
        files.forEach(file -> {
            if (file.getId() != null) {
                fileById.putIfAbsent(file.getId(), file);
            }
        });
        Map<Long, String> directoryPathById = buildDirectoryPathById();
        List<DccControlledFileRecognitionRecordExportExcelVO> rows = recognitionRecordMapper.selectListByFileIds(
                        fileIds,
                        recognitionStatus,
                        reqVO.getBatchRecognitionTaskId())
                .stream()
                .filter(record -> fileById.containsKey(record.getControlledFileId()))
                .map(record -> {
                    DccControlledFileDO file = fileById.get(record.getControlledFileId());
                    return DccControlledFileRecognitionRecordExportExcelVO.from(
                            file, directoryPathById.get(file.getDirectoryId()), record);
                })
                .toList();
        return writeRecognitionRecordWorkbook(rows);
    }

    @Override
    public byte[] buildRecognitionMigrationExportExcel(Long userId, DccControlledFilePageReqVO reqVO) {
        validateDocControlRole(userId);
        List<DccControlledFileDO> files = controlledFileQueryService.listControlledFileBrowserCandidates(userId, reqVO);
        String recognitionStatus = StrUtil.trimToNull(reqVO.getRecognitionStatus());
        List<DccControlledFileRecognitionRecordDO> records;
        if (reqVO.getBatchRecognitionTaskId() != null) {
            records = recognitionRecordMapper.selectListByBatchTaskId(reqVO.getBatchRecognitionTaskId(), recognitionStatus);
        } else {
            List<Long> fileIds = files.stream()
                    .map(DccControlledFileDO::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            records = fileIds.isEmpty()
                    ? List.of()
                    : recognitionRecordMapper.selectListByFileIds(fileIds, recognitionStatus, reqVO.getBatchRecognitionTaskId());
        }
        return writeRecognitionMigrationWorkbook(buildRecognitionMigrationExportRows(records, files));
    }

    private List<DccControlledFileRecognitionRecordExportExcelVO> buildRecognitionRecordExportRows(
            List<DccControlledFileRecognitionRecordDO> records,
            List<DccControlledFileDO> browserCandidates) {
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, DccControlledFileDO> fileById = new LinkedHashMap<>();
        browserCandidates.forEach(file -> {
            if (file.getId() != null) {
                fileById.putIfAbsent(file.getId(), file);
            }
        });
        List<Long> missingFileIds = records.stream()
                .map(DccControlledFileRecognitionRecordDO::getControlledFileId)
                .filter(Objects::nonNull)
                .filter(controlledFileId -> !fileById.containsKey(controlledFileId))
                .distinct()
                .toList();
        if (!missingFileIds.isEmpty()) {
            controlledFileMapper.selectBatchIds(missingFileIds).forEach(file -> {
                if (file.getId() != null) {
                    fileById.putIfAbsent(file.getId(), file);
                }
            });
        }
        Map<Long, String> directoryPathById = buildDirectoryPathById();
        return records.stream()
                .filter(record -> fileById.containsKey(record.getControlledFileId()))
                .map(record -> {
                    DccControlledFileDO file = fileById.get(record.getControlledFileId());
                    return DccControlledFileRecognitionRecordExportExcelVO.from(
                            file, directoryPathById.get(file.getDirectoryId()), record);
                })
                .toList();
    }

    private List<DccControlledFileRecognitionMigrationExportExcelVO> buildRecognitionMigrationExportRows(
            List<DccControlledFileRecognitionRecordDO> records,
            List<DccControlledFileDO> browserCandidates) {
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, DccControlledFileDO> fileById = buildFileMapForRecords(records, browserCandidates);
        Map<Long, String> directoryPathById = buildDirectoryPathById();
        Map<Long, DccProjectCodeDO> projectCodeById = buildProjectCodeById(records);
        return records.stream()
                .filter(record -> fileById.containsKey(record.getControlledFileId()))
                .map(record -> {
                    DccControlledFileDO file = fileById.get(record.getControlledFileId());
                    return DccControlledFileRecognitionMigrationExportExcelVO.from(
                            file,
                            directoryPathById.get(file.getDirectoryId()),
                            record,
                            projectCodeById.get(record.getMatchedProjectCodeId()));
                })
                .toList();
    }

    private Map<Long, DccControlledFileDO> buildFileMapForRecords(List<DccControlledFileRecognitionRecordDO> records,
                                                                  List<DccControlledFileDO> browserCandidates) {
        Map<Long, DccControlledFileDO> fileById = new LinkedHashMap<>();
        browserCandidates.forEach(file -> {
            if (file.getId() != null) {
                fileById.putIfAbsent(file.getId(), file);
            }
        });
        List<Long> missingFileIds = records.stream()
                .map(DccControlledFileRecognitionRecordDO::getControlledFileId)
                .filter(Objects::nonNull)
                .filter(controlledFileId -> !fileById.containsKey(controlledFileId))
                .distinct()
                .toList();
        if (!missingFileIds.isEmpty()) {
            controlledFileMapper.selectBatchIds(missingFileIds).forEach(file -> {
                if (file.getId() != null) {
                    fileById.putIfAbsent(file.getId(), file);
                }
            });
        }
        return fileById;
    }

    private Map<Long, DccProjectCodeDO> buildProjectCodeById(List<DccControlledFileRecognitionRecordDO> records) {
        List<Long> projectCodeIds = records.stream()
                .map(DccControlledFileRecognitionRecordDO::getMatchedProjectCodeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (projectCodeIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, DccProjectCodeDO> projectCodeById = new HashMap<>();
        projectCodeMapper.selectBatchIds(projectCodeIds).forEach(projectCode -> {
            if (projectCode.getId() != null) {
                projectCodeById.put(projectCode.getId(), projectCode);
            }
        });
        return projectCodeById;
    }

    @Override
    public DccControlledFileMetadataImportPreviewRespVO previewImport(Long userId, MultipartFile file) {
        validateDocControlRole(userId);
        return evaluateImportRows(file, false, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccControlledFileMetadataImportPreviewRespVO confirmImport(Long userId, MultipartFile file) {
        validateDocControlRole(userId);
        DccControlledFileMetadataImportPreviewRespVO preview = evaluateImportRows(file, true, userId);
        if (preview.getFailureCount() != null && preview.getFailureCount() > 0) {
            throw new IllegalStateException("受控文件基础信息导入存在失败行，请先修正后再确认。");
        }
        return preview;
    }

    @Override
    public DccControlledFileRecognitionMigrationImportPreviewRespVO previewRecognitionMigrationImport(Long userId,
                                                                                                      MultipartFile file) {
        validateDocControlRole(userId);
        return evaluateRecognitionMigrationRows(file, false, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccControlledFileRecognitionMigrationImportPreviewRespVO confirmRecognitionMigrationImport(Long userId,
                                                                                                      MultipartFile file) {
        validateDocControlRole(userId);
        return evaluateRecognitionMigrationRows(file, true, userId);
    }

    private DccControlledFileMetadataImportPreviewRespVO evaluateImportRows(MultipartFile file, boolean applyUpdate,
                                                                            Long userId) {
        List<ImportRowDraft> drafts = parseWorkbook(file);
        List<DccControlledFileMetadataImportRowRespVO> rows = new ArrayList<>();
        int updateCount = 0;
        int unchangedCount = 0;
        int failureCount = 0;
        for (ImportRowDraft draft : drafts) {
            DccControlledFileMetadataImportRowRespVO row = evaluateImportRow(userId, draft, applyUpdate);
            rows.add(row);
            if (ACTION_UPDATE.equals(row.getImportAction())) {
                updateCount++;
            } else if (ACTION_UNCHANGED.equals(row.getImportAction())) {
                unchangedCount++;
            } else if (ACTION_INVALID.equals(row.getImportAction())) {
                failureCount++;
            }
        }
        return DccControlledFileMetadataImportPreviewRespVO.builder()
                .totalCount(rows.size())
                .updateCount(updateCount)
                .unchangedCount(unchangedCount)
                .failureCount(failureCount)
                .rows(rows)
                .build();
    }

    private DccControlledFileRecognitionMigrationImportPreviewRespVO evaluateRecognitionMigrationRows(
            MultipartFile file, boolean applyUpdate, Long userId) {
        List<RecognitionMigrationRowDraft> drafts = parseRecognitionMigrationWorkbook(file);
        List<DccControlledFileRecognitionMigrationImportRowRespVO> rows = new ArrayList<>();
        int applicableCount = 0;
        int blockedCount = 0;
        int failedRecognitionCount = 0;
        int appliedCount = 0;
        for (RecognitionMigrationRowDraft draft : drafts) {
            DccControlledFileRecognitionMigrationImportRowRespVO row =
                    evaluateRecognitionMigrationRow(userId, draft, applyUpdate);
            rows.add(row);
            if (ACTION_APPLICABLE.equals(row.getImportAction())) {
                applicableCount++;
                if (applyUpdate) {
                    appliedCount++;
                }
            } else {
                blockedCount++;
            }
            if (!isRecognitionSuccess(draft.recognitionStatus())) {
                failedRecognitionCount++;
            }
        }
        return DccControlledFileRecognitionMigrationImportPreviewRespVO.builder()
                .totalCount(rows.size())
                .applicableCount(applicableCount)
                .blockedCount(blockedCount)
                .failedRecognitionCount(failedRecognitionCount)
                .appliedCount(appliedCount)
                .rows(rows)
                .build();
    }

    private DccControlledFileRecognitionMigrationImportRowRespVO evaluateRecognitionMigrationRow(
            Long userId, RecognitionMigrationRowDraft draft, boolean applyUpdate) {
        String directoryPath = normalizePath(draft.directoryPath());
        String fileName = StrUtil.trim(draft.fileName());
        String fileNumber = StrUtil.trim(draft.fileNumber());
        if (StrUtil.isBlank(directoryPath)) {
            return blockedMigrationRow(draft, null, "目录路径不能为空");
        }
        if (StrUtil.isBlank(fileName)) {
            return blockedMigrationRow(draft, null, "文件名称不能为空");
        }
        DccFileDirectoryDO directory = findDirectoryByPath(directoryPath);
        if (directory == null) {
            return blockedMigrationRow(draft, null, "正式服目录不存在: " + directoryPath);
        }
        List<DccControlledFileDO> matchedFiles = findMigrationTargetFiles(directory.getId(), fileName, fileNumber);
        if (matchedFiles.isEmpty()) {
            return blockedMigrationRow(draft, null, "正式服文件不存在");
        }
        if (matchedFiles.size() > 1) {
            return blockedMigrationRow(draft, null, "正式服文件匹配到多条");
        }
        DccControlledFileDO targetFile = matchedFiles.get(0);
        if (!isRecognitionSuccess(draft.recognitionStatus())) {
            return blockedMigrationRow(draft, targetFile,
                    "测试服识别失败: " + StrUtil.blankToDefault(StrUtil.trim(draft.failureMessage()), "未提供失败原因"));
        }
        DccProjectCodeDO projectCode = resolveMigrationProjectCode(draft);
        if (projectCode == null) {
            return blockedMigrationRow(draft, targetFile, "正式服项目编码不存在: "
                    + StrUtil.blankToDefault(StrUtil.trim(draft.projectName()), "-") + "/"
                    + StrUtil.blankToDefault(StrUtil.trim(draft.projectCode()), "-"));
        }
        if (applyUpdate) {
            metadataUpdateService.updateMetadata(userId, targetFile.getId(),
                    buildMigrationUpdateReq(targetFile, draft, projectCode));
        }
        return DccControlledFileRecognitionMigrationImportRowRespVO.builder()
                .rowNo(draft.rowNo())
                .directoryPath(directoryPath)
                .fileName(fileName)
                .fileNumber(fileNumber)
                .testControlledFileId(draft.testControlledFileId())
                .targetControlledFileId(targetFile.getId())
                .targetFileName(targetFile.getFileName())
                .targetFileNumber(targetFile.getFileNumber())
                .recognitionStatus(StrUtil.trim(draft.recognitionStatus()))
                .importAction(ACTION_APPLICABLE)
                .productName(projectCode.getProjectName())
                .productCode(projectCode.getProjectCode())
                .productMasterId(null)
                .projectName(projectCode.getProjectName())
                .projectCode(projectCode.getProjectCode())
                .dccProjectCodeId(projectCode.getId())
                .fileTypeLevel1(StrUtil.trimToNull(draft.fileTypeLevel1()))
                .fileTypeLevel2(StrUtil.trimToNull(draft.fileTypeLevel2()))
                .fileTypeLevel3(StrUtil.trimToNull(draft.fileTypeLevel3()))
                .fileTypeLevel4(StrUtil.trimToNull(draft.fileTypeLevel4()))
                .fileTypeLevel5(StrUtil.trimToNull(draft.fileTypeLevel5()))
                .build();
    }

    private DccControlledFileRecognitionMigrationImportRowRespVO blockedMigrationRow(
            RecognitionMigrationRowDraft draft, DccControlledFileDO targetFile, String reason) {
        return DccControlledFileRecognitionMigrationImportRowRespVO.builder()
                .rowNo(draft.rowNo())
                .directoryPath(normalizePath(draft.directoryPath()))
                .fileName(StrUtil.trim(draft.fileName()))
                .fileNumber(StrUtil.trim(draft.fileNumber()))
                .testControlledFileId(draft.testControlledFileId())
                .targetControlledFileId(targetFile == null ? null : targetFile.getId())
                .targetFileName(targetFile == null ? null : targetFile.getFileName())
                .targetFileNumber(targetFile == null ? null : targetFile.getFileNumber())
                .recognitionStatus(StrUtil.trim(draft.recognitionStatus()))
                .importAction(ACTION_BLOCKED)
                .failureReason(reason)
                .productName(StrUtil.trim(draft.productName()))
                .productCode(StrUtil.trim(draft.productCode()))
                .productMasterId(null)
                .projectName(StrUtil.trim(draft.projectName()))
                .projectCode(StrUtil.trim(draft.projectCode()))
                .fileTypeLevel1(StrUtil.trimToNull(draft.fileTypeLevel1()))
                .fileTypeLevel2(StrUtil.trimToNull(draft.fileTypeLevel2()))
                .fileTypeLevel3(StrUtil.trimToNull(draft.fileTypeLevel3()))
                .fileTypeLevel4(StrUtil.trimToNull(draft.fileTypeLevel4()))
                .fileTypeLevel5(StrUtil.trimToNull(draft.fileTypeLevel5()))
                .build();
    }

    private DccControlledFileMetadataImportRowRespVO evaluateImportRow(Long userId, ImportRowDraft draft,
                                                                       boolean applyUpdate) {
        String fileName = StrUtil.trim(draft.fileName());
        String fileNumber = StrUtil.trim(draft.fileNumber());
        if (draft.controlledFileId() == null) {
            return invalidRow(draft, "受控文件ID不能为空");
        }
        if (StrUtil.isBlank(fileName)) {
            return invalidRow(draft, "文件名称不能为空");
        }
        if (StrUtil.isBlank(fileNumber)) {
            return invalidRow(draft, "文件编号不能为空");
        }
        DccControlledFileDO file = controlledFileMapper.selectById(draft.controlledFileId());
        if (file == null) {
            return invalidRow(draft, "受控文件不存在: id=" + draft.controlledFileId());
        }
        if (Objects.equals(StrUtil.trim(file.getFileName()), fileName)
                && Objects.equals(StrUtil.trim(file.getFileNumber()), fileNumber)) {
            return DccControlledFileMetadataImportRowRespVO.builder()
                    .rowNo(draft.rowNo())
                    .controlledFileId(draft.controlledFileId())
                    .fileName(fileName)
                    .fileNumber(fileNumber)
                    .importAction(ACTION_UNCHANGED)
                    .build();
        }
        if (applyUpdate) {
            metadataUpdateService.updateMetadata(userId, file.getId(), buildUpdateReq(file, fileName, fileNumber));
        }
        return DccControlledFileMetadataImportRowRespVO.builder()
                .rowNo(draft.rowNo())
                .controlledFileId(draft.controlledFileId())
                .fileName(fileName)
                .fileNumber(fileNumber)
                .importAction(ACTION_UPDATE)
                .build();
    }

    private DccControlledFileMetadataUpdateReqVO buildUpdateReq(DccControlledFileDO file, String fileName,
                                                                String fileNumber) {
        DccControlledFileMetadataUpdateReqVO reqVO = new DccControlledFileMetadataUpdateReqVO();
        reqVO.setProductMasterId(null);
        reqVO.setProductCode(null);
        reqVO.setProductName(null);
        reqVO.setDccProjectCodeId(file.getDccProjectCodeId());
        reqVO.setNeedTraining(file.getNeedTraining());
        reqVO.setFileTypeLevel1(file.getFileTypeLevel1());
        reqVO.setFileTypeLevel2(file.getFileTypeLevel2());
        reqVO.setFileTypeLevel3(file.getFileTypeLevel3());
        reqVO.setFileTypeLevel4(file.getFileTypeLevel4());
        reqVO.setFileTypeLevel5(file.getFileTypeLevel5());
        reqVO.setFileName(fileName);
        reqVO.setFileNumber(fileNumber);
        reqVO.setCategoryId(file.getCategoryId());
        reqVO.setDirectoryId(file.getDirectoryId());
        return reqVO;
    }

    private DccControlledFileMetadataUpdateReqVO buildMigrationUpdateReq(DccControlledFileDO file,
                                                                         RecognitionMigrationRowDraft draft,
                                                                         DccProjectCodeDO projectCode) {
        DccControlledFileMetadataUpdateReqVO reqVO = new DccControlledFileMetadataUpdateReqVO();
        reqVO.setProductMasterId(null);
        reqVO.setProductCode(projectCode.getProjectCode());
        reqVO.setProductName(projectCode.getProjectName());
        reqVO.setDccProjectCodeId(projectCode.getId());
        reqVO.setNeedTraining(file.getNeedTraining());
        reqVO.setFileTypeLevel1(StrUtil.trimToNull(draft.fileTypeLevel1()));
        reqVO.setFileTypeLevel2(StrUtil.trimToNull(draft.fileTypeLevel2()));
        reqVO.setFileTypeLevel3(StrUtil.trimToNull(draft.fileTypeLevel3()));
        reqVO.setFileTypeLevel4(StrUtil.trimToNull(draft.fileTypeLevel4()));
        reqVO.setFileTypeLevel5(StrUtil.trimToNull(draft.fileTypeLevel5()));
        reqVO.setFileName(file.getFileName());
        reqVO.setFileNumber(file.getFileNumber());
        reqVO.setCategoryId(file.getCategoryId());
        reqVO.setDirectoryId(file.getDirectoryId());
        return reqVO;
    }

    private List<ImportRowDraft> parseWorkbook(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalStateException("受控文件基础信息导入文件不能为空");
        }
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalStateException("受控文件基础信息导入文件不能为空");
            }
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            validateHeader(sheet.getRow(0), formatter);
            List<ImportRowDraft> rows = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isEmptyRow(row, formatter)) {
                    continue;
                }
                rows.add(new ImportRowDraft(
                        rowIndex + 1,
                        parseLong(readCell(row, 0, formatter)),
                        readCell(row, 1, formatter),
                        readCell(row, 2, formatter)));
            }
            return rows;
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("受控文件基础信息导入文件解析失败", ex);
        }
    }

    private List<RecognitionMigrationRowDraft> parseRecognitionMigrationWorkbook(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalStateException("识别结果迁移包不能为空");
        }
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalStateException("识别结果迁移包不能为空");
            }
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            validateRecognitionMigrationHeader(sheet.getRow(0), formatter);
            List<RecognitionMigrationRowDraft> rows = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isEmptyRecognitionMigrationRow(row, formatter)) {
                    continue;
                }
                rows.add(new RecognitionMigrationRowDraft(
                        rowIndex + 1,
                        readCell(row, 0, formatter),
                        readCell(row, 1, formatter),
                        readCell(row, 2, formatter),
                        parseLong(readCell(row, 3, formatter)),
                        readCell(row, 4, formatter),
                        readCell(row, 5, formatter),
                        readCell(row, 6, formatter),
                        readCell(row, 7, formatter),
                        readCell(row, 8, formatter),
                        parseLong(readCell(row, 9, formatter)),
                        parseLong(readCell(row, 10, formatter)),
                        readCell(row, 11, formatter),
                        readCell(row, 12, formatter),
                        readCell(row, 13, formatter),
                        readCell(row, 14, formatter),
                        readCell(row, 15, formatter),
                        readCell(row, 16, formatter),
                        readCell(row, 17, formatter),
                        readCell(row, 18, formatter),
                        readCell(row, 19, formatter),
                        readCell(row, 20, formatter),
                        readCell(row, 21, formatter),
                        parseLong(readCell(row, 22, formatter)),
                        parseLong(readCell(row, 23, formatter)),
                        readCell(row, 24, formatter)));
            }
            return rows;
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("识别结果迁移包解析失败", ex);
        }
    }

    private void validateHeader(Row headerRow, DataFormatter formatter) {
        if (headerRow == null) {
            throw new IllegalStateException("受控文件基础信息导入缺少表头行");
        }
        List<String> actualHeaders = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < EXPECTED_HEADERS.size(); columnIndex++) {
            actualHeaders.add(readCell(headerRow, columnIndex, formatter));
        }
        if (!EXPECTED_HEADERS.equals(actualHeaders)) {
            throw new IllegalStateException("受控文件基础信息导入表头不正确，期望表头=" + EXPECTED_HEADERS
                    + "，实际表头=" + actualHeaders);
        }
    }

    private void validateRecognitionMigrationHeader(Row headerRow, DataFormatter formatter) {
        if (headerRow == null) {
            throw new IllegalStateException("识别结果迁移包缺少表头行");
        }
        List<String> actualHeaders = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < RECOGNITION_MIGRATION_HEADERS.size(); columnIndex++) {
            actualHeaders.add(readCell(headerRow, columnIndex, formatter));
        }
        if (!RECOGNITION_MIGRATION_HEADERS.equals(actualHeaders)) {
            throw new IllegalStateException("识别结果迁移包表头不正确，期望表头=" + RECOGNITION_MIGRATION_HEADERS
                    + "，实际表头=" + actualHeaders);
        }
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (int columnIndex = 0; columnIndex < EXPECTED_HEADERS.size(); columnIndex++) {
            if (StrUtil.isNotBlank(readCell(row, columnIndex, formatter))) {
                return false;
            }
        }
        return true;
    }

    private boolean isEmptyRecognitionMigrationRow(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (int columnIndex = 0; columnIndex < RECOGNITION_MIGRATION_HEADERS.size(); columnIndex++) {
            if (StrUtil.isNotBlank(readCell(row, columnIndex, formatter))) {
                return false;
            }
        }
        return true;
    }

    private String readCell(Row row, int columnIndex, DataFormatter formatter) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell);
    }

    private Long parseLong(String value) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Long.valueOf(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private DccControlledFileMetadataImportRowRespVO invalidRow(ImportRowDraft draft, String reason) {
        return DccControlledFileMetadataImportRowRespVO.builder()
                .rowNo(draft.rowNo())
                .controlledFileId(draft.controlledFileId())
                .fileName(StrUtil.trim(draft.fileName()))
                .fileNumber(StrUtil.trim(draft.fileNumber()))
                .importAction(ACTION_INVALID)
                .failureReason(reason)
                .build();
    }

    private boolean hasRecognizedMetadata(DccControlledFileDO file) {
        return StrUtil.isNotBlank(StrUtil.trim(file.getFileName()))
                && StrUtil.isNotBlank(StrUtil.trim(file.getFileNumber()));
    }

    private boolean isRecognitionSuccess(String status) {
        return "SUCCESS".equalsIgnoreCase(StrUtil.trim(status));
    }

    private String normalizePath(String path) {
        String normalized = StrUtil.trimToNull(path);
        if (normalized == null) {
            return "";
        }
        return normalized.replace("\\", "/")
                .replaceAll("/+", "/")
                .replaceAll("^/", "")
                .replaceAll("/$", "");
    }

    private DccFileDirectoryDO findDirectoryByPath(String directoryPath) {
        String normalizedPath = normalizePath(directoryPath);
        List<DccFileDirectoryDO> directories = directoryMapper.selectList();
        Map<Long, DccFileDirectoryDO> directoryById = new HashMap<>();
        for (DccFileDirectoryDO directory : directories) {
            if (directory.getId() != null) {
                directoryById.put(directory.getId(), directory);
            }
        }
        for (DccFileDirectoryDO directory : directories) {
            if (Objects.equals(normalizedPath, normalizePath(buildDirectoryPath(directory.getId(), directoryById)))) {
                return directory;
            }
        }
        return null;
    }

    private List<DccControlledFileDO> findMigrationTargetFiles(Long directoryId, String fileName, String fileNumber) {
        LambdaQueryWrapperX<DccControlledFileDO> query = new LambdaQueryWrapperX<DccControlledFileDO>()
                .eq(DccControlledFileDO::getDirectoryId, directoryId);
        String normalizedFileNumber = StrUtil.trimToNull(fileNumber);
        if (normalizedFileNumber != null) {
            query.eq(DccControlledFileDO::getFileNumber, normalizedFileNumber);
        } else {
            query.eq(DccControlledFileDO::getFileName, StrUtil.trim(fileName));
        }
        return controlledFileMapper.selectList(query);
    }

    private DccProjectCodeDO resolveMigrationProjectCode(RecognitionMigrationRowDraft draft) {
        String projectName = StrUtil.trimToNull(draft.projectName());
        String projectCode = StrUtil.trimToNull(draft.projectCode());
        if (projectName == null || projectCode == null) {
            return null;
        }
        return projectCodeMapper.selectByProjectNameAndProjectCode(projectName, projectCode);
    }

    private void validateDocControlRole(Long userId) {
        if (!permissionApi.hasAnyRoles(userId, DccControlledFileMetadataUpdateService.DOC_CONTROL_ROLE_CODE)) {
            throw exception(CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED);
        }
    }

    private byte[] writeWorkbook(List<DccControlledFileMetadataExportExcelVO> rows) {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("受控文件基础信息");
            Row header = sheet.createRow(0);
            for (int columnIndex = 0; columnIndex < EXPECTED_HEADERS.size(); columnIndex++) {
                header.createCell(columnIndex).setCellValue(EXPECTED_HEADERS.get(columnIndex));
            }
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                DccControlledFileMetadataExportExcelVO item = rows.get(rowIndex);
                Row row = sheet.createRow(rowIndex + 1);
                row.createCell(0).setCellValue(item.getControlledFileId() == null ? "" : String.valueOf(item.getControlledFileId()));
                row.createCell(1).setCellValue(StrUtil.nullToEmpty(item.getFileName()));
                row.createCell(2).setCellValue(StrUtil.nullToEmpty(item.getFileNumber()));
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("受控文件基础信息 Excel 生成失败", ex);
        }
    }

    private byte[] writeRecognitionRecordWorkbook(List<DccControlledFileRecognitionRecordExportExcelVO> rows) {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("识别记录");
            Row header = sheet.createRow(0);
            for (int columnIndex = 0; columnIndex < RECOGNITION_RECORD_EXPORT_HEADERS.size(); columnIndex++) {
                header.createCell(columnIndex).setCellValue(RECOGNITION_RECORD_EXPORT_HEADERS.get(columnIndex));
            }
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                DccControlledFileRecognitionRecordExportExcelVO item = rows.get(rowIndex);
                Row row = sheet.createRow(rowIndex + 1);
                row.createCell(0).setCellValue(StrUtil.nullToEmpty(item.getDirectoryPath()));
                row.createCell(1).setCellValue(StrUtil.nullToEmpty(item.getFileName()));
                row.createCell(2).setCellValue(item.getControlledFileId() == null ? "" : String.valueOf(item.getControlledFileId()));
                row.createCell(3).setCellValue(StrUtil.nullToEmpty(item.getStatus()));
                row.createCell(4).setCellValue(StrUtil.nullToEmpty(item.getProductName()));
                row.createCell(5).setCellValue(StrUtil.nullToEmpty(item.getProductCode()));
                row.createCell(6).setCellValue(item.getMatchedProjectAliasId() == null ? ""
                        : String.valueOf(item.getMatchedProjectAliasId()));
                row.createCell(7).setCellValue(StrUtil.nullToEmpty(item.getMatchedProjectAliasText()));
                row.createCell(8).setCellValue(StrUtil.nullToEmpty(item.getMatchedProjectAliasSource()));
                row.createCell(9).setCellValue(StrUtil.nullToEmpty(item.getMatchType()));
                row.createCell(10).setCellValue(StrUtil.nullToEmpty(item.getMatchText()));
                row.createCell(11).setCellValue(StrUtil.nullToEmpty(item.getFailureMessage()));
                row.createCell(12).setCellValue(StrUtil.nullToEmpty(item.getFileTypeLevel1()));
                row.createCell(13).setCellValue(StrUtil.nullToEmpty(item.getFileTypeLevel2()));
                row.createCell(14).setCellValue(StrUtil.nullToEmpty(item.getFileTypeLevel3()));
                row.createCell(15).setCellValue(StrUtil.nullToEmpty(item.getFileTypeLevel4()));
                row.createCell(16).setCellValue(StrUtil.nullToEmpty(item.getFileTypeLevel5()));
                row.createCell(17).setCellValue(StrUtil.nullToEmpty(item.getRecognitionVersion()));
                row.createCell(18).setCellValue(item.getBatchTaskId() == null ? "" : String.valueOf(item.getBatchTaskId()));
                row.createCell(19).setCellValue(item.getRecognizedBy() == null ? "" : String.valueOf(item.getRecognizedBy()));
                row.createCell(20).setCellValue(item.getRecognizedTime() == null ? "" : String.valueOf(item.getRecognizedTime()));
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("受控文件识别记录 Excel 生成失败", ex);
        }
    }

    private byte[] writeRecognitionMigrationWorkbook(List<DccControlledFileRecognitionMigrationExportExcelVO> rows) {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("识别迁移包");
            Row header = sheet.createRow(0);
            for (int columnIndex = 0; columnIndex < RECOGNITION_MIGRATION_HEADERS.size(); columnIndex++) {
                header.createCell(columnIndex).setCellValue(RECOGNITION_MIGRATION_HEADERS.get(columnIndex));
            }
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                DccControlledFileRecognitionMigrationExportExcelVO item = rows.get(rowIndex);
                Row row = sheet.createRow(rowIndex + 1);
                row.createCell(0).setCellValue(StrUtil.nullToEmpty(item.getDirectoryPath()));
                row.createCell(1).setCellValue(StrUtil.nullToEmpty(item.getFileName()));
                row.createCell(2).setCellValue(StrUtil.nullToEmpty(item.getFileNumber()));
                row.createCell(3).setCellValue(item.getTestControlledFileId() == null ? ""
                        : String.valueOf(item.getTestControlledFileId()));
                row.createCell(4).setCellValue(StrUtil.nullToEmpty(item.getRecognitionStatus()));
                row.createCell(5).setCellValue(StrUtil.nullToEmpty(item.getProductName()));
                row.createCell(6).setCellValue(StrUtil.nullToEmpty(item.getProductCode()));
                row.createCell(7).setCellValue(StrUtil.nullToEmpty(item.getProjectName()));
                row.createCell(8).setCellValue(StrUtil.nullToEmpty(item.getProjectCode()));
                row.createCell(9).setCellValue(item.getTestProjectCodeId() == null ? ""
                        : String.valueOf(item.getTestProjectCodeId()));
                row.createCell(10).setCellValue(item.getMatchedProjectAliasId() == null ? ""
                        : String.valueOf(item.getMatchedProjectAliasId()));
                row.createCell(11).setCellValue(StrUtil.nullToEmpty(item.getMatchedProjectAliasText()));
                row.createCell(12).setCellValue(StrUtil.nullToEmpty(item.getMatchedProjectAliasSource()));
                row.createCell(13).setCellValue(StrUtil.nullToEmpty(item.getMatchType()));
                row.createCell(14).setCellValue(StrUtil.nullToEmpty(item.getMatchText()));
                row.createCell(15).setCellValue(StrUtil.nullToEmpty(item.getFailureMessage()));
                row.createCell(16).setCellValue(StrUtil.nullToEmpty(item.getFileTypeLevel1()));
                row.createCell(17).setCellValue(StrUtil.nullToEmpty(item.getFileTypeLevel2()));
                row.createCell(18).setCellValue(StrUtil.nullToEmpty(item.getFileTypeLevel3()));
                row.createCell(19).setCellValue(StrUtil.nullToEmpty(item.getFileTypeLevel4()));
                row.createCell(20).setCellValue(StrUtil.nullToEmpty(item.getFileTypeLevel5()));
                row.createCell(21).setCellValue(StrUtil.nullToEmpty(item.getRecognitionVersion()));
                row.createCell(22).setCellValue(item.getBatchTaskId() == null ? "" : String.valueOf(item.getBatchTaskId()));
                row.createCell(23).setCellValue(item.getRecognizedBy() == null ? "" : String.valueOf(item.getRecognizedBy()));
                row.createCell(24).setCellValue(item.getRecognizedTime() == null ? "" : String.valueOf(item.getRecognizedTime()));
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("受控文件识别结果迁移包 Excel 生成失败", ex);
        }
    }

    private Map<Long, String> buildDirectoryPathById() {
        List<DccFileDirectoryDO> directories = directoryMapper.selectList();
        Map<Long, DccFileDirectoryDO> directoryById = new HashMap<>();
        for (DccFileDirectoryDO directory : directories) {
            if (directory.getId() != null) {
                directoryById.put(directory.getId(), directory);
            }
        }
        Map<Long, String> pathById = new HashMap<>();
        for (Long directoryId : directoryById.keySet()) {
            pathById.put(directoryId, buildDirectoryPath(directoryId, directoryById));
        }
        return pathById;
    }

    private String buildDirectoryPath(Long directoryId, Map<Long, DccFileDirectoryDO> directoryById) {
        List<String> segments = new ArrayList<>();
        Long currentId = directoryId;
        while (currentId != null) {
            DccFileDirectoryDO directory = directoryById.get(currentId);
            if (directory == null) {
                break;
            }
            String name = StrUtil.trimToNull(directory.getName());
            if (name != null) {
                segments.add(0, name);
            }
            currentId = directory.getParentId();
        }
        return String.join("/", segments);
    }

    private record ImportRowDraft(int rowNo, Long controlledFileId, String fileName, String fileNumber) {
    }

    private record RecognitionMigrationRowDraft(
            int rowNo,
            String directoryPath,
            String fileName,
            String fileNumber,
            Long testControlledFileId,
            String recognitionStatus,
            String productName,
            String productCode,
            String projectName,
            String projectCode,
            Long testProjectCodeId,
            Long matchedProjectAliasId,
            String matchedProjectAliasText,
            String matchedProjectAliasSource,
            String matchType,
            String matchText,
            String failureMessage,
            String fileTypeLevel1,
            String fileTypeLevel2,
            String fileTypeLevel3,
            String fileTypeLevel4,
            String fileTypeLevel5,
            String recognitionVersion,
            Long batchTaskId,
            Long recognizedBy,
            String recognizedTime) {
    }
}
