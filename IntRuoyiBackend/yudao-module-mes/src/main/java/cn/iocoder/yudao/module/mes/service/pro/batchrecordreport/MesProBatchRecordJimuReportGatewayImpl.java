package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import org.jeecgframework.minidao.pojo.MiniDaoPage;
import org.jeecg.modules.jmreport.desreport.dao.JimuReportDao;
import org.jeecg.modules.jmreport.desreport.entity.JimuReport;
import org.jeecg.modules.jmreport.desreport.entity.JimuReportCategory;
import org.jeecg.modules.jmreport.desreport.model.TreeModel;
import org.jeecg.modules.jmreport.desreport.service.IJimuReportCategoryService;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Component
public class MesProBatchRecordJimuReportGatewayImpl implements MesProBatchRecordJimuReportGateway {

    private static final String FORM_TEMPLATE_REPORT_PREFIX = "FORMTPL:";

    static final String FILL_FORM_PREVIEW_CSS = """
            .fillForm-box,
            .fillForm-box .ivu-form-item,
            .fillForm-box .ivu-form-item-content {
              background: transparent !important;
            }
            .fillForm-box .inputText,
            .fillForm-box textarea,
            .fillForm-box .ivu-input {
              border: 0 !important;
              background: transparent !important;
              box-shadow: none !important;
              border-radius: 0 !important;
              padding: 0 2px !important;
            }
            .fillForm-box .ivu-input-wrapper,
            .fillForm-box .ivu-input-type-text,
            .fillForm-box .ivu-input-type-textarea {
              border: 0 !important;
              background: transparent !important;
              box-shadow: none !important;
            }
            """;

    @Resource
    private IJimuReportCategoryService reportCategoryService;
    @Resource
    private JimuReportDao jimuReportDao;
    @Resource
    private FormTemplateVersionMapper templateVersionMapper;
    @Resource
    private MesProBatchRecordReportJsonBuilder reportJsonBuilder;
    @Resource
    private MesProBatchRecordReportLayoutCalibrator reportLayoutCalibrator;
    @Resource
    private MesProBatchRecordReportStyleEnhancer reportStyleEnhancer;

    @Override
    public String ensureElectronicBatchRecordCategoryId() {
        String existingCategoryId = findElectronicBatchRecordCategoryId();
        if (StrUtil.isNotBlank(existingCategoryId)) {
            return existingCategoryId;
        }

        String categoryId = IdUtil.fastSimpleUUID();
        Date now = new Date();
        JimuReportCategory category = new JimuReportCategory();
        category.setId(categoryId);
        category.setName(MesProBatchRecordReportConstants.CATEGORY_NAME);
        category.setParentId("0");
        category.setIzLeaf(1);
        category.setSourceType("report");
        category.setCreateBy(resolveActor());
        category.setCreateTime(now);
        category.setUpdateBy(resolveActor());
        category.setUpdateTime(now);
        category.setTenantId(resolveTenantId());
        category.setSortNo(resolveNextSortNo());
        reportCategoryService.save(category);
        return categoryId;
    }

    @Override
    public String findElectronicBatchRecordCategoryId() {
        JimuReportCategory query = new JimuReportCategory();
        query.setSourceType("report");
        List<TreeModel> categories = reportCategoryService.queryList(query);
        for (TreeModel category : categories) {
            if (MesProBatchRecordReportConstants.CATEGORY_NAME.equals(category.getTitle())) {
                return category.getId();
            }
        }
        return null;
    }

    @Override
    public MesProBatchRecordGeneratedReport saveOrUpdateReport(MesProBatchRecordJimuReportSaveReq saveReq) {
        MesProBatchRecordParsedTable calibratedTable = reportLayoutCalibrator.calibrate(saveReq.parsedTable());
        String jsonStr = reportStyleEnhancer.enhance(reportJsonBuilder.build(calibratedTable, saveReq.reportCode()), calibratedTable);
        JSONObject root = JSON.parseObject(jsonStr);
        MesProBatchRecordCellRuleSupport.applyAutomaticSuggestions(root, saveReq.reportCode());
        jsonStr = root.toJSONString();
        logChecklistSnapshot(saveReq.reportCode(), calibratedTable, jsonStr);
        Date now = new Date();
        if (StrUtil.isBlank(saveReq.existingReportId())) {
            JimuReport reportByCode = findJimuReportByCode(saveReq.reportCode());
            if (reportByCode != null) {
                return updateReport(reportByCode, saveReq, jsonStr, now);
            }
            JimuReport report = new JimuReport();
            report.setId(IdUtil.fastSimpleUUID());
            report.setCode(saveReq.reportCode());
            report.setName(saveReq.reportName());
            report.setType(saveReq.categoryId());
            report.setJsonStr(jsonStr);
            report.setCreateBy(resolveActor());
            report.setCreateTime(now);
            report.setUpdateBy(resolveActor());
            report.setUpdateTime(now);
            report.setDelFlag(0);
            report.setTemplate(0);
            report.setViewCount(0L);
            report.setTenantId(resolveTenantId());
            report.setUpdateCount(0);
            report.setSubmitForm(1);
            report.setIsMultiSheet(0);
            report.setCssStr(FILL_FORM_PREVIEW_CSS);
            jimuReportDao.insert(report);
            return new MesProBatchRecordGeneratedReport(report.getId(), report.getCode(), report.getName());
        }

        JimuReport report = jimuReportDao.get(saveReq.existingReportId());
        if (report == null) {
            JimuReport reportByCode = findJimuReportByCode(saveReq.reportCode());
            if (reportByCode != null) {
                return updateReport(reportByCode, saveReq, jsonStr, now);
            }
            JimuReport recreated = new JimuReport();
            recreated.setId(saveReq.existingReportId());
            recreated.setCode(saveReq.reportCode());
            recreated.setName(saveReq.reportName());
            recreated.setType(saveReq.categoryId());
            recreated.setJsonStr(jsonStr);
            recreated.setCreateBy(resolveActor());
            recreated.setCreateTime(now);
            recreated.setUpdateBy(resolveActor());
            recreated.setUpdateTime(now);
            recreated.setDelFlag(0);
            recreated.setTemplate(0);
            recreated.setViewCount(0L);
            recreated.setTenantId(resolveTenantId());
            recreated.setUpdateCount(0);
            recreated.setSubmitForm(1);
            recreated.setIsMultiSheet(0);
            recreated.setCssStr(FILL_FORM_PREVIEW_CSS);
            jimuReportDao.insert(recreated);
            return new MesProBatchRecordGeneratedReport(recreated.getId(), recreated.getCode(), recreated.getName());
        }
        return updateReport(report, saveReq, jsonStr, now);
    }

    private MesProBatchRecordGeneratedReport updateReport(JimuReport report,
                                                          MesProBatchRecordJimuReportSaveReq saveReq,
                                                          String jsonStr,
                                                          Date now) {
        report.setCode(saveReq.reportCode());
        report.setName(saveReq.reportName());
        report.setType(saveReq.categoryId());
        report.setJsonStr(jsonStr);
        report.setTemplate(0);
        report.setSubmitForm(1);
        report.setUpdateBy(resolveActor());
        report.setUpdateTime(now);
        report.setTenantId(resolveTenantId());
        report.setUpdateCount(report.getUpdateCount() == null ? 1 : report.getUpdateCount() + 1);
        report.setCssStr(FILL_FORM_PREVIEW_CSS);
        jimuReportDao.update(report);
        return new MesProBatchRecordGeneratedReport(report.getId(), report.getCode(), report.getName());
    }

    private JimuReport findJimuReportByCode(String reportCode) {
        if (StrUtil.isBlank(reportCode)) {
            return null;
        }
        JimuReport query = new JimuReport();
        query.setCode(reportCode);
        query.setTenantId(resolveTenantId());
        MiniDaoPage<JimuReport> page = jimuReportDao.getAll(query, 1, 1);
        if (page == null || page.getResults() == null || page.getResults().isEmpty()) {
            return null;
        }
        return page.getResults().get(0);
    }

    private void logChecklistSnapshot(String reportCode,
                                      MesProBatchRecordParsedTable calibratedTable,
                                      String jsonStr) {
        if (!StrUtil.contains(reportCode, "_T06")) {
            return;
        }
        StringBuilder rowSummary = new StringBuilder();
        int lastRowIndex = Math.min(calibratedTable.getRows().size() - 1, 18);
        for (int rowIndex = 5; rowIndex <= lastRowIndex; rowIndex++) {
            if (rowSummary.length() > 0) {
                rowSummary.append(" || ");
            }
            rowSummary.append("row").append(rowIndex).append('=');
            List<MesProBatchRecordParsedCell> row = calibratedTable.getRows().get(rowIndex);
            for (int cellIndex = 0; cellIndex < row.size(); cellIndex++) {
                if (cellIndex > 0) {
                    rowSummary.append(" | ");
                }
                MesProBatchRecordParsedCell cell = row.get(cellIndex);
                rowSummary.append('[')
                        .append(Math.max(1, cell.getRowSpan()))
                        .append('x')
                        .append(Math.max(1, cell.getColSpan()))
                        .append(' ')
                        .append(compactText(cell.getText()))
                        .append(']');
            }
        }
        System.out.println("[EDHR-T06-CALIBRATED] reportCode=" + reportCode + " rows=" + rowSummary);
        int anchor = jsonStr.indexOf("检查要求");
        if (anchor >= 0) {
            int start = Math.max(0, anchor - 220);
            int end = Math.min(jsonStr.length(), anchor + 2400);
            System.out.println("[EDHR-T06-JSON] reportCode=" + reportCode + " snippet="
                    + jsonStr.substring(start, end));
        }
    }

    private String compactText(String text) {
        return text == null ? "" : text.replace("\r", "").replace("\n", "/").replaceAll("\\s+", "");
    }

    @Override
    public MesProBatchRecordReportInfo getReportInfoByCode(String reportCode) {
        if (StrUtil.isBlank(reportCode)) {
            return null;
        }
        JimuReport query = new JimuReport();
        query.setCode(reportCode);
        query.setTenantId(resolveTenantId());
        MiniDaoPage<JimuReport> page = jimuReportDao.getAll(query, 1, 1);
        if (page == null || page.getResults() == null || page.getResults().isEmpty()) {
            return null;
        }
        return toReportInfo(page.getResults().get(0));
    }

    @Override
    public MesProBatchRecordReportInfo getReportInfo(String reportId) {
        return toReportInfo(jimuReportDao.get(reportId));
    }

    @Override
    public void ensureFormTemplateDesignerReport(String reportId) {
        if (!isFormTemplateReportId(reportId)) {
            return;
        }
        FormTemplateVersionDO templateVersion = requireFormTemplateVersion(parseFormTemplateReportId(reportId));
        ensureFormTemplateDesignerReport(reportId, templateVersion);
    }

    private MesProBatchRecordReportInfo toReportInfo(JimuReport report) {
        if (report == null) {
            return null;
        }
        LocalDateTime updateTime = report.getUpdateTime() == null
                ? null
                : LocalDateTime.ofInstant(report.getUpdateTime().toInstant(), ZoneId.systemDefault());
        return new MesProBatchRecordReportInfo(report.getId(), report.getCode(), report.getName(), updateTime);
    }

    @Override
    public String getReportJson(String reportId) {
        if (isFormTemplateReportId(reportId)) {
            ensureFormTemplateDesignerReport(reportId);
        }
        JimuReport report = jimuReportDao.get(reportId);
        return report == null ? null : report.getJsonStr();
    }

    @Override
    public void updateReportJson(String reportId, String jsonStr) {
        if (isFormTemplateReportId(reportId) && jimuReportDao.get(reportId) == null) {
            ensureFormTemplateDesignerReport(reportId);
        }
        JimuReport report = jimuReportDao.get(reportId);
        if (report == null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_LINKED_REPORT_MISSING,
                    reportId);
        }
        report.setJsonStr(jsonStr);
        report.setUpdateBy(resolveActor());
        report.setUpdateTime(new Date());
        report.setTenantId(resolveTenantId());
        report.setUpdateCount(report.getUpdateCount() == null ? 1 : report.getUpdateCount() + 1);
        jimuReportDao.update(report);
        if (isFormTemplateReportId(reportId)) {
            FormTemplateVersionDO templateVersion = requireFormTemplateVersion(parseFormTemplateReportId(reportId));
            templateVersion.setJimuSchemaJson(buildFormTemplateSchemaWithDesignerJson(
                    templateVersion.getJimuSchemaJson(), jsonStr, reportId));
            templateVersionMapper.updateById(templateVersion);
        }
    }

    @Override
    public void renameReportName(String reportId, String reportName) {
        if (isFormTemplateReportId(reportId)) {
            ensureFormTemplateDesignerReport(reportId);
        }
        JimuReport report = jimuReportDao.get(reportId);
        if (report == null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_LINKED_REPORT_MISSING,
                    reportId);
        }
        report.setName(reportName);
        report.setUpdateBy(resolveActor());
        report.setUpdateTime(new Date());
        report.setTenantId(resolveTenantId());
        jimuReportDao.update(report);
    }

    @Override
    public void deleteReport(String reportId) {
        jimuReportDao.deleteById(reportId);
    }

    @Override
    public int deleteReportsByCategoryId(String categoryId) {
        Long reportCount = jimuReportDao.selectCountByCategoryId(categoryId);
        if (reportCount == null || reportCount <= 0) {
            return 0;
        }

        JimuReport query = new JimuReport();
        query.setType(categoryId);
        query.setTenantId(resolveTenantId());
        MiniDaoPage<JimuReport> page = jimuReportDao.getAll(query, 1, Math.toIntExact(Math.max(reportCount, 1L)));
        List<String> reportIds = page.getResults().stream()
                .map(JimuReport::getId)
                .filter(StrUtil::isNotBlank)
                .toList();
        if (reportIds.isEmpty()) {
            return 0;
        }
        jimuReportDao.deleteByReportIds(reportIds);
        return reportIds.size();
    }

    @Override
    public String buildDesignerPath(String reportId) {
        String tenantId = resolveTenantId();
        return StrUtil.isBlank(tenantId)
                ? "/jmreport/index/" + reportId
                : "/jmreport/index/" + reportId + "?tenantId=" + tenantId;
    }

    @Override
    public String buildPreviewPath(String reportId) {
        String tenantId = resolveTenantId();
        return StrUtil.isBlank(tenantId)
                ? "/jmreport/view/" + reportId
                : "/jmreport/view/" + reportId + "?tenantId=" + tenantId;
    }

    private void ensureFormTemplateDesignerReport(String reportId, FormTemplateVersionDO templateVersion) {
        String reportName = formTemplateReportName(templateVersion);
        String jsonStr = extractFormTemplateDesignerJson(templateVersion.getJimuSchemaJson(), reportId);
        Date now = new Date();
        JimuReport report = jimuReportDao.get(reportId);
        if (report == null) {
            report = findJimuReportByCode(reportId);
            if (report != null && !Objects.equals(report.getId(), reportId)) {
                jimuReportDao.deleteById(report.getId());
                report = null;
            }
        }
        boolean insert = report == null;
        if (insert) {
            report = new JimuReport();
            report.setId(reportId);
            report.setCreateBy(resolveActor());
            report.setCreateTime(now);
            report.setViewCount(0L);
            report.setDelFlag(0);
            report.setUpdateCount(0);
        } else {
            report.setUpdateCount(report.getUpdateCount() == null ? 1 : report.getUpdateCount() + 1);
        }
        report.setCode(reportId);
        report.setName(reportName);
        report.setType(ensureElectronicBatchRecordCategoryId());
        report.setJsonStr(jsonStr);
        report.setUpdateBy(resolveActor());
        report.setUpdateTime(now);
        report.setTenantId(resolveTenantId());
        report.setTemplate(0);
        report.setSubmitForm(1);
        report.setIsMultiSheet(0);
        report.setCssStr(FILL_FORM_PREVIEW_CSS);
        if (insert) {
            jimuReportDao.insert(report);
        } else {
            jimuReportDao.update(report);
        }
    }

    private String extractFormTemplateDesignerJson(String templateSchemaJson, String reportId) {
        JSONObject templateSchema = parseJsonObject(templateSchemaJson, reportId);
        String sheetLayoutJson = StrUtil.trim(templateSchema.getString("sheetLayoutJson"));
        if (StrUtil.isBlank(sheetLayoutJson)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_JSON_INVALID,
                    reportId + " missing sheetLayoutJson");
        }
        return normalizeFormTemplateDesignerJson(parseJsonObject(sheetLayoutJson, reportId), reportId).toJSONString();
    }

    private String buildFormTemplateSchemaWithDesignerJson(String templateSchemaJson, String designerJson,
                                                           String reportId) {
        JSONObject templateSchema = parseJsonObject(templateSchemaJson, reportId);
        JSONObject designerRoot = normalizeFormTemplateDesignerJson(parseJsonObject(designerJson, reportId), reportId);
        templateSchema.put("sheetLayoutJson", designerRoot.toJSONString());
        return templateSchema.toJSONString();
    }

    private JSONObject normalizeFormTemplateDesignerJson(JSONObject designerRoot, String reportId) {
        if (!(designerRoot.get("rows") instanceof JSONObject)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_JSON_INVALID,
                    reportId + " missing rows");
        }
        if (!(designerRoot.get("cols") instanceof JSONObject)) {
            designerRoot.put("cols", new JSONObject(true));
        }
        return designerRoot;
    }

    private JSONObject parseJsonObject(String jsonStr, String reportId) {
        if (StrUtil.isBlank(jsonStr)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_JSON_INVALID,
                    reportId + " blank json");
        }
        try {
            JSONObject root = JSON.parseObject(jsonStr);
            if (root == null) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_JSON_INVALID,
                        reportId + " empty json");
            }
            return root;
        } catch (RuntimeException ex) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_JSON_INVALID,
                    reportId + " " + ex.getMessage());
        }
    }

    private String formTemplateReportName(FormTemplateVersionDO templateVersion) {
        String templateName = StrUtil.blankToDefault(templateVersion.getTemplateName(),
                reportIdOf(templateVersion));
        return StrUtil.isBlank(templateVersion.getVersionNo())
                ? templateName
                : templateName + " " + StrUtil.trim(templateVersion.getVersionNo());
    }

    private String reportIdOf(FormTemplateVersionDO templateVersion) {
        return FORM_TEMPLATE_REPORT_PREFIX + templateVersion.getId();
    }

    private boolean isFormTemplateReportId(String reportId) {
        return StrUtil.startWith(reportId, FORM_TEMPLATE_REPORT_PREFIX);
    }

    private Long parseFormTemplateReportId(String reportId) {
        String templateVersionId = StrUtil.subAfter(reportId, FORM_TEMPLATE_REPORT_PREFIX, false);
        if (StrUtil.isBlank(templateVersionId)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_LINKED_REPORT_MISSING,
                    reportId);
        }
        try {
            return Long.valueOf(templateVersionId);
        } catch (NumberFormatException ex) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_LINKED_REPORT_MISSING,
                    reportId);
        }
    }

    private FormTemplateVersionDO requireFormTemplateVersion(Long templateVersionId) {
        FormTemplateVersionDO templateVersion = templateVersionMapper.selectById(templateVersionId);
        if (templateVersion == null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_LINKED_REPORT_MISSING,
                    FORM_TEMPLATE_REPORT_PREFIX + templateVersionId);
        }
        return templateVersion;
    }

    private String resolveActor() {
        String nickname = SecurityFrameworkUtils.getLoginUserNickname();
        if (StrUtil.isNotBlank(nickname)) {
            return nickname;
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return userId == null ? "system" : String.valueOf(userId);
    }

    private String resolveTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? null : String.valueOf(tenantId);
    }

    private Integer resolveNextSortNo() {
        Integer current = reportCategoryService.getMinSortByParentId("0", "report");
        return current == null ? 99 : current + 1;
    }
}
