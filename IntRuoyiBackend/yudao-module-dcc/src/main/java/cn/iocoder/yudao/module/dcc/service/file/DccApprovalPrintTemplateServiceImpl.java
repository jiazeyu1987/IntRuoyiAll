package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalPrintHtmlRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalPrintTemplateRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalPrintTemplateSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccApprovalPrintTemplateDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccApprovalPrintTemplateMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_PRINT_TEMPLATE_FILE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_PRINT_TEMPLATE_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_PRINT_TEMPLATE_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_PRINT_TEMPLATE_PLACEHOLDER_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_PRINT_TEMPLATE_PLACEHOLDER_UNSUPPORTED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_PRINT_TEMPLATE_RENDER_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;

/**
 * DCC approval print template service implementation.
 */
@Service
@Validated
public class DccApprovalPrintTemplateServiceImpl implements DccApprovalPrintTemplateService {

    public static final String DOCX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private static final List<String> REQUIRED_PLACEHOLDERS = List.of(
            "fileNumber", "fileName", "versionNo", "approvalRecords");
    private static final List<String> SUPPORTED_PLACEHOLDERS = List.of(
            "fileNumber", "fileName", "title", "versionNo", "productCode", "effectiveDate",
            "processInstanceId", "processDefinitionKey", "submittedTime", "approvedTime",
            "publishedTime", "approvalRecords", "processContent");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z][A-Za-z0-9_]*)\\s*}}");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private DccApprovalPrintTemplateMapper templateMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Resource
    private DccControlledFileSignatureMapper signatureMapper;
    @Resource
    private DccControlledFileQueryService queryService;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileService fileService;

    @Override
    public DccApprovalPrintTemplateRespVO saveActiveTemplate(Long userId, DccApprovalPrintTemplateSaveReqVO reqVO) {
        FileDO templateFile = requireTemplateFile(reqVO);
        validateTemplateFileRecord(templateFile);
        validateDocxTemplate(readTemplateContent(templateFile, APPROVAL_PRINT_TEMPLATE_FILE_INVALID));

        DccApprovalPrintTemplateDO activeTemplate = templateMapper.selectActive();
        DccApprovalPrintTemplateDO savedTemplate = DccApprovalPrintTemplateDO.builder()
                .templateFileId(templateFile.getId())
                .templateFileName(templateFile.getName())
                .templateFileContentType(templateFile.getType())
                .active(Boolean.TRUE)
                .remark(reqVO.getRemark())
                .build();
        if (activeTemplate == null) {
            templateMapper.insert(savedTemplate);
        } else {
            savedTemplate.setId(activeTemplate.getId());
            templateMapper.updateById(savedTemplate);
        }
        return toRespVO(savedTemplate);
    }

    @Override
    public DccApprovalPrintTemplateRespVO getActiveTemplate() {
        DccApprovalPrintTemplateDO template = templateMapper.selectActive();
        return template == null ? null : toRespVO(template);
    }

    @Override
    public DccApprovalPrintRenderedWord exportApprovalWord(Long userId, Long controlledFileId) {
        DccControlledFileDO file = requireAccessibleControlledFile(userId, controlledFileId);
        DccApprovalPrintTemplateDO template = requireActiveTemplate();
        FileDO templateFile = requireTemplateFile(template.getTemplateFileId());
        validateTemplateFileRecord(templateFile);
        byte[] templateContent = readTemplateContent(templateFile, APPROVAL_PRINT_TEMPLATE_RENDER_FAILED);
        DocxTemplate docxTemplate = validateDocxTemplate(templateContent);
        Map<String, String> values = buildTemplateValues(file);
        byte[] rendered = renderDocx(docxTemplate, values);
        return new DccApprovalPrintRenderedWord(buildExportFileName(file), DOCX_CONTENT_TYPE, rendered);
    }

    @Override
    public DccApprovalPrintHtmlRespVO getApprovalPrintHtml(Long userId, Long controlledFileId) {
        DccControlledFileDO file = requireAccessibleControlledFile(userId, controlledFileId);
        DccApprovalPrintTemplateDO template = requireActiveTemplate();
        FileDO templateFile = requireTemplateFile(template.getTemplateFileId());
        validateTemplateFileRecord(templateFile);
        validateDocxTemplate(readTemplateContent(templateFile, APPROVAL_PRINT_TEMPLATE_RENDER_FAILED));

        DccApprovalPrintHtmlRespVO respVO = new DccApprovalPrintHtmlRespVO();
        respVO.setTemplateId(template.getId());
        respVO.setTemplateFileName(template.getTemplateFileName());
        respVO.setRequiredPlaceholders(toPlaceholderTokens(REQUIRED_PLACEHOLDERS));
        respVO.setHtml(buildPrintHtml(template, buildTemplateValues(file)));
        return respVO;
    }

    private DccControlledFileDO requireAccessibleControlledFile(Long userId, Long controlledFileId) {
        queryService.getControlledFile(userId, controlledFileId);
        DccControlledFileDO file = controlledFileMapper.selectById(controlledFileId);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        return file;
    }

    private DccApprovalPrintTemplateDO requireActiveTemplate() {
        DccApprovalPrintTemplateDO template = templateMapper.selectActive();
        if (template == null) {
            throw exception(APPROVAL_PRINT_TEMPLATE_NOT_CONFIGURED);
        }
        return template;
    }

    private FileDO requireTemplateFile(Long fileId) {
        if (fileId == null) {
            throw exception(APPROVAL_PRINT_TEMPLATE_FILE_NOT_EXISTS);
        }
        FileDO templateFile = fileMapper.selectById(fileId);
        if (templateFile == null) {
            throw exception(APPROVAL_PRINT_TEMPLATE_FILE_NOT_EXISTS);
        }
        return templateFile;
    }

    private FileDO requireTemplateFile(DccApprovalPrintTemplateSaveReqVO reqVO) {
        if (reqVO.getTemplateFileId() != null) {
            return requireTemplateFile(reqVO.getTemplateFileId());
        }
        return requireTemplateFileByUrl(reqVO.getTemplateFileUrl());
    }

    private FileDO requireTemplateFileByUrl(String fileUrl) {
        String value = StrUtil.trimToEmpty(fileUrl);
        if (StrUtil.isBlank(value)) {
            throw exception(APPROVAL_PRINT_TEMPLATE_FILE_NOT_EXISTS);
        }
        var matcher = Pattern.compile("/infra/file/(\\d+)/get/(.+)$").matcher(value);
        if (!matcher.find()) {
            throw exception(APPROVAL_PRINT_TEMPLATE_FILE_NOT_EXISTS);
        }
        Long configId = Long.valueOf(matcher.group(1));
        String path = URLDecoder.decode(matcher.group(2), StandardCharsets.UTF_8);
        FileDO templateFile = fileMapper.selectOne(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<FileDO>()
                .eq(FileDO::getConfigId, configId)
                .eq(FileDO::getPath, path)
                .orderByDesc(FileDO::getId)
                .last("LIMIT 1"));
        if (templateFile == null) {
            throw exception(APPROVAL_PRINT_TEMPLATE_FILE_NOT_EXISTS);
        }
        return templateFile;
    }

    private void validateTemplateFileRecord(FileDO templateFile) {
        String fileName = StrUtil.blankToDefault(templateFile.getName(), templateFile.getPath());
        boolean hasDocxName = StrUtil.endWithIgnoreCase(fileName, ".docx");
        boolean hasDocxContentType = StrUtil.equalsIgnoreCase(templateFile.getType(), DOCX_CONTENT_TYPE);
        if (!hasDocxName && !hasDocxContentType) {
            throw exception(APPROVAL_PRINT_TEMPLATE_FILE_INVALID);
        }
        if (templateFile.getConfigId() == null || StrUtil.isBlank(templateFile.getPath())) {
            throw exception(APPROVAL_PRINT_TEMPLATE_FILE_INVALID);
        }
    }

    private byte[] readTemplateContent(FileDO templateFile, ErrorCode errorCode) {
        try {
            byte[] content = fileService.getFileContent(templateFile.getConfigId(), templateFile.getPath());
            if (content == null || content.length == 0) {
                throw exception(errorCode);
            }
            return content;
        } catch (cn.iocoder.yudao.framework.common.exception.ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(errorCode);
        }
    }

    private DocxTemplate validateDocxTemplate(byte[] content) {
        DocxTemplate template = readDocxTemplate(content, APPROVAL_PRINT_TEMPLATE_FILE_INVALID);
        if (!template.hasEntry("[Content_Types].xml") || !template.hasEntry("word/document.xml")) {
            throw exception(APPROVAL_PRINT_TEMPLATE_FILE_INVALID);
        }
        String xml = template.joinXmlEntries();
        List<String> missing = REQUIRED_PLACEHOLDERS.stream()
                .filter(placeholder -> !xml.contains("{{" + placeholder + "}}"))
                .toList();
        if (!missing.isEmpty()) {
            throw exception(APPROVAL_PRINT_TEMPLATE_PLACEHOLDER_MISSING);
        }
        List<String> unsupported = findUnsupportedPlaceholders(xml);
        if (!unsupported.isEmpty()) {
            throw exception(APPROVAL_PRINT_TEMPLATE_PLACEHOLDER_UNSUPPORTED);
        }
        return template;
    }

    private DocxTemplate readDocxTemplate(byte[] content, ErrorCode errorCode) {
        List<DocxEntry> entries = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.add(new DocxEntry(entry.getName(), entry.isDirectory(), zip.readAllBytes()));
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw exception(errorCode);
        }
        if (entries.isEmpty()) {
            throw exception(errorCode);
        }
        return new DocxTemplate(entries);
    }

    private List<String> findUnsupportedPlaceholders(String xml) {
        List<String> unsupported = new ArrayList<>();
        var matcher = PLACEHOLDER_PATTERN.matcher(xml);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!SUPPORTED_PLACEHOLDERS.contains(name)) {
                unsupported.add("{{" + name + "}}");
            }
        }
        return unsupported.stream().distinct().toList();
    }

    private byte[] renderDocx(DocxTemplate template, Map<String, String> values) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
                for (DocxEntry entry : template.entries()) {
                    ZipEntry outputEntry = new ZipEntry(entry.name());
                    zip.putNextEntry(outputEntry);
                    if (!entry.directory()) {
                        zip.write(renderEntry(entry, values));
                    }
                    zip.closeEntry();
                }
            }
            return output.toByteArray();
        } catch (IOException ex) {
            throw exception(APPROVAL_PRINT_TEMPLATE_RENDER_FAILED);
        }
    }

    private byte[] renderEntry(DocxEntry entry, Map<String, String> values) {
        if (!entry.name().endsWith(".xml")) {
            return entry.content();
        }
        String xml = new String(entry.content(), StandardCharsets.UTF_8);
        for (Map.Entry<String, String> value : values.entrySet()) {
            xml = xml.replace("{{" + value.getKey() + "}}", escapeXml(value.getValue()));
        }
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    private Map<String, String> buildTemplateValues(DccControlledFileDO file) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("fileNumber", text(file.getFileNumber()));
        values.put("fileName", text(StrUtil.blankToDefault(file.getFileName(), file.getTitle())));
        values.put("title", text(file.getTitle()));
        values.put("versionNo", text(file.getVersionNo()));
        values.put("productCode", text(file.getProductCode()));
        values.put("effectiveDate", file.getEffectiveDate() == null ? "" : DATE_FORMATTER.format(file.getEffectiveDate()));
        values.put("processInstanceId", text(file.getProcessInstanceId()));
        values.put("processDefinitionKey", text(file.getProcessDefinitionKey()));
        values.put("submittedTime", formatDateTime(file.getSubmittedTime()));
        values.put("approvedTime", formatDateTime(file.getApprovedTime()));
        values.put("publishedTime", formatDateTime(file.getPublishedTime()));
        String approvalRecords = buildApprovalRecords(file.getId());
        values.put("approvalRecords", approvalRecords);
        values.put("processContent", approvalRecords);
        return values;
    }

    private String buildApprovalRecords(Long controlledFileId) {
        List<String> lines = new ArrayList<>();
        routeSnapshotMapper.selectListByControlledFileId(controlledFileId).stream()
                .sorted(Comparator.comparing(DccControlledFileRouteSnapshotDO::getStageNo,
                        Comparator.nullsLast(Integer::compareTo)))
                .map(this::formatRouteSnapshot)
                .forEach(lines::add);
        signatureMapper.selectListByControlledFileId(controlledFileId).stream()
                .sorted(Comparator.comparing(DccControlledFileSignatureDO::getSignedAt,
                        Comparator.nullsLast(java.time.LocalDateTime::compareTo)))
                .map(this::formatSignature)
                .forEach(lines::add);
        return lines.isEmpty() ? "暂无审批记录" : String.join("\n", lines);
    }

    private String formatRouteSnapshot(DccControlledFileRouteSnapshotDO snapshot) {
        return "阶段" + text(snapshot.getStageNo()) + " "
                + text(snapshot.getStageName()) + " [" + text(snapshot.getStageCode()) + "] "
                + "审批人:" + text(snapshot.getResolvedUserIds());
    }

    private String formatSignature(DccControlledFileSignatureDO signature) {
        return "签核 " + text(signature.getActionType())
                + " 用户#" + text(signature.getActorId())
                + " " + formatDateTime(signature.getSignedAt())
                + " " + text(signature.getComment());
    }

    private String buildExportFileName(DccControlledFileDO file) {
        String fileNumber = StrUtil.blankToDefault(file.getFileNumber(), String.valueOf(file.getId()));
        return "DCC流程-" + fileNumber + ".docx";
    }

    private String buildPrintHtml(DccApprovalPrintTemplateDO template, Map<String, String> values) {
        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset="utf-8" />
                  <title>DCC流程打印</title>
                  <style>
                    body { font-family: Arial, "Microsoft YaHei", sans-serif; color: #1f2329; margin: 24px; }
                    h1 { font-size: 20px; margin: 0 0 16px; }
                    table { border-collapse: collapse; width: 100%%; font-size: 13px; margin-bottom: 18px; }
                    th, td { border: 1px solid #333; padding: 7px 9px; text-align: left; vertical-align: top; }
                    th { width: 130px; background: #f3f4f6; }
                    pre { white-space: pre-wrap; word-break: break-word; border: 1px solid #d8dde8; padding: 12px; }
                  </style>
                </head>
                <body>
                  <h1>DCC流程打印</h1>
                  <table>
                    <tbody>
                      <tr><th>模板</th><td>%s</td></tr>
                      <tr><th>文件编号</th><td>%s</td></tr>
                      <tr><th>文件名称</th><td>%s</td></tr>
                      <tr><th>版本</th><td>%s</td></tr>
                      <tr><th>产品编号</th><td>%s</td></tr>
                      <tr><th>流程实例</th><td>%s</td></tr>
                    </tbody>
                  </table>
                  <h2>审批记录</h2>
                  <pre>%s</pre>
                </body>
                </html>
                """.formatted(
                escapeHtml(template.getTemplateFileName()),
                escapeHtml(values.get("fileNumber")),
                escapeHtml(values.get("fileName")),
                escapeHtml(values.get("versionNo")),
                escapeHtml(values.get("productCode")),
                escapeHtml(values.get("processInstanceId")),
                escapeHtml(values.get("approvalRecords")));
    }

    private DccApprovalPrintTemplateRespVO toRespVO(DccApprovalPrintTemplateDO template) {
        DccApprovalPrintTemplateRespVO respVO = new DccApprovalPrintTemplateRespVO();
        respVO.setId(template.getId());
        respVO.setTemplateFileId(template.getTemplateFileId());
        respVO.setTemplateFileName(template.getTemplateFileName());
        respVO.setTemplateFileContentType(template.getTemplateFileContentType());
        respVO.setActive(template.getActive());
        respVO.setRemark(template.getRemark());
        respVO.setRequiredPlaceholders(toPlaceholderTokens(REQUIRED_PLACEHOLDERS));
        respVO.setSupportedPlaceholders(toPlaceholderTokens(SUPPORTED_PLACEHOLDERS));
        respVO.setUpdateTime(template.getUpdateTime());
        return respVO;
    }

    private static List<String> toPlaceholderTokens(List<String> placeholders) {
        return placeholders.stream().map(placeholder -> "{{" + placeholder + "}}").toList();
    }

    private String formatDateTime(java.time.LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String escapeXml(String value) {
        return text(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String escapeHtml(String value) {
        return text(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private record DocxEntry(String name, boolean directory, byte[] content) {
    }

    private record DocxTemplate(List<DocxEntry> entries) {

        private boolean hasEntry(String name) {
            return entries.stream().anyMatch(entry -> name.equals(entry.name()));
        }

        private String joinXmlEntries() {
            return entries.stream()
                    .filter(entry -> !entry.directory())
                    .filter(entry -> entry.name().endsWith(".xml"))
                    .map(entry -> new String(entry.content(), StandardCharsets.UTF_8))
                    .reduce("", (left, right) -> left + "\n" + right);
        }
    }
}
