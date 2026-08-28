package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jeecg.modules.jmreport.desreport.dao.JimuReportDao;
import org.jeecg.modules.jmreport.desreport.entity.JimuReport;
import org.jeecg.modules.jmreport.desreport.model.TreeModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.jeecgframework.minidao.pojo.MiniDaoPage;

class MesProBatchRecordJimuReportGatewayImplTest {

    private final MesProBatchRecordJimuReportGatewayImpl gateway = new MesProBatchRecordJimuReportGatewayImpl();

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void buildPreviewPath_returnsPureReportPreviewPathWithTenantId() {
        TenantContextHolder.setTenantId(1L);

        String path = gateway.buildPreviewPath("report-123");

        assertEquals("/jmreport/view/report-123?tenantId=1", path);
    }

    @Test
    void buildPreviewPath_withoutTenant_returnsPureReportPreviewPath() {
        String path = gateway.buildPreviewPath("report-123");

        assertEquals("/jmreport/view/report-123", path);
    }

    @Test
    void buildDesignerPath_returnsEditableReportPathWithTenantId() {
        TenantContextHolder.setTenantId(1L);

        String path = gateway.buildDesignerPath("report-123");

        assertEquals("/jmreport/index/report-123?tenantId=1", path);
    }

    @Test
    void buildDesignerPath_withoutTenant_returnsEditableReportPath() {
        String path = gateway.buildDesignerPath("report-123");

        assertEquals("/jmreport/index/report-123", path);
    }

    @Test
    void saveOrUpdateReport_persistsTemplateZeroAndSubmitFormForNewReport() {
        JimuReportDao jimuReportDao = mock(JimuReportDao.class);
        MesProBatchRecordReportJsonBuilder reportJsonBuilder = mock(MesProBatchRecordReportJsonBuilder.class);
        MesProBatchRecordReportLayoutCalibrator reportLayoutCalibrator =
                mock(MesProBatchRecordReportLayoutCalibrator.class);
        MesProBatchRecordReportStyleEnhancer reportStyleEnhancer =
                mock(MesProBatchRecordReportStyleEnhancer.class);
        ReflectionTestUtils.setField(gateway, "jimuReportDao", jimuReportDao);
        ReflectionTestUtils.setField(gateway, "reportJsonBuilder", reportJsonBuilder);
        ReflectionTestUtils.setField(gateway, "reportLayoutCalibrator", reportLayoutCalibrator);
        ReflectionTestUtils.setField(gateway, "reportStyleEnhancer", reportStyleEnhancer);
        when(reportLayoutCalibrator.calibrate(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportJsonBuilder.build(any(), anyString())).thenReturn("{\"rows\":[]}");
        when(reportStyleEnhancer.enhance(anyString(), any())).thenReturn("{\"styled\":true}");
        TenantContextHolder.setTenantId(1L);

        gateway.saveOrUpdateReport(MesProBatchRecordJimuReportSaveReq.builder()
                .categoryId("category-ebrr")
                .reportCode("EBR_IMG_HASH_T01")
                .reportName("Image report")
                .parsedTable(parsedTable(1, "Image report"))
                .build());

        ArgumentCaptor<JimuReport> reportCaptor = ArgumentCaptor.forClass(JimuReport.class);
        verify(jimuReportDao).insert(reportCaptor.capture());
        JimuReport persisted = reportCaptor.getValue();
        assertEquals(Integer.valueOf(0), persisted.getTemplate());
        assertEquals(Integer.valueOf(1), persisted.getSubmitForm());
        assertEquals("1", persisted.getTenantId());
        assertEquals(MesProBatchRecordJimuReportGatewayImpl.FILL_FORM_PREVIEW_CSS, persisted.getCssStr());
    }

    @Test
    void saveOrUpdateReport_persistsAutomaticCellRulesForGeneratedJson() {
        JimuReportDao jimuReportDao = mock(JimuReportDao.class);
        MesProBatchRecordReportJsonBuilder reportJsonBuilder = mock(MesProBatchRecordReportJsonBuilder.class);
        MesProBatchRecordReportLayoutCalibrator reportLayoutCalibrator =
                mock(MesProBatchRecordReportLayoutCalibrator.class);
        MesProBatchRecordReportStyleEnhancer reportStyleEnhancer =
                mock(MesProBatchRecordReportStyleEnhancer.class);
        ReflectionTestUtils.setField(gateway, "jimuReportDao", jimuReportDao);
        ReflectionTestUtils.setField(gateway, "reportJsonBuilder", reportJsonBuilder);
        ReflectionTestUtils.setField(gateway, "reportLayoutCalibrator", reportLayoutCalibrator);
        ReflectionTestUtils.setField(gateway, "reportStyleEnhancer", reportStyleEnhancer);
        when(reportLayoutCalibrator.calibrate(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportJsonBuilder.build(any(), anyString())).thenReturn("""
                {
                  "rows":{
                    "0":{"cells":{
                      "0":{"text":"生产数量（pcs）"},
                      "1":{"text":"","fillForm":{"field":"ebr_r0_c1","component":"Input","componentFlag":"input-text"}}
                    }}
                  },
                  "cols":{},
                  "merges":[]
                }
                """);
        when(reportStyleEnhancer.enhance(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        TenantContextHolder.setTenantId(1L);

        gateway.saveOrUpdateReport(MesProBatchRecordJimuReportSaveReq.builder()
                .categoryId("category-ebrr")
                .reportCode("EBR_AUTO_RULE_T01")
                .reportName("自动规则表")
                .parsedTable(parsedTable(1, "自动规则表"))
                .build());

        ArgumentCaptor<JimuReport> reportCaptor = ArgumentCaptor.forClass(JimuReport.class);
        verify(jimuReportDao).insert(reportCaptor.capture());
        JSONObject root = JSON.parseObject(reportCaptor.getValue().getJsonStr());
        JSONObject quantityCell = root.getJSONObject("rows")
                .getJSONObject("0")
                .getJSONObject("cells")
                .getJSONObject("1");
        JSONObject rule = quantityCell.getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY);
        assertNotNull(rule);
        assertEquals("NUMBER", rule.getString("valueType"));
        assertEquals("input-number", rule.getString("componentFlag"));
        assertEquals(false, rule.getBoolean("reviewed"));
        assertEquals(0, rule.getJSONObject("constraints").getInteger("min"));
        assertEquals(0, rule.getJSONObject("constraints").getInteger("scale"));
        assertEquals("input-number", quantityCell
                .getJSONObject(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY)
                .getString("componentFlag"));
    }

    @Test
    void saveOrUpdateReport_normalizesTemplateZeroAndSubmitFormForExistingReport() {
        JimuReportDao jimuReportDao = mock(JimuReportDao.class);
        MesProBatchRecordReportJsonBuilder reportJsonBuilder = mock(MesProBatchRecordReportJsonBuilder.class);
        MesProBatchRecordReportLayoutCalibrator reportLayoutCalibrator =
                mock(MesProBatchRecordReportLayoutCalibrator.class);
        MesProBatchRecordReportStyleEnhancer reportStyleEnhancer =
                mock(MesProBatchRecordReportStyleEnhancer.class);
        ReflectionTestUtils.setField(gateway, "jimuReportDao", jimuReportDao);
        ReflectionTestUtils.setField(gateway, "reportJsonBuilder", reportJsonBuilder);
        ReflectionTestUtils.setField(gateway, "reportLayoutCalibrator", reportLayoutCalibrator);
        ReflectionTestUtils.setField(gateway, "reportStyleEnhancer", reportStyleEnhancer);
        when(reportLayoutCalibrator.calibrate(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportJsonBuilder.build(any(), anyString())).thenReturn("{\"rows\":[]}");
        when(reportStyleEnhancer.enhance(anyString(), any())).thenReturn("{\"styled\":true}");
        JimuReport existing = new JimuReport();
        existing.setId("report-123");
        existing.setCreateBy("芋道源码");
        existing.setUpdateCount(null);
        when(jimuReportDao.get("report-123")).thenReturn(existing);
        TenantContextHolder.setTenantId(1L);

        gateway.saveOrUpdateReport(MesProBatchRecordJimuReportSaveReq.builder()
                .existingReportId("report-123")
                .categoryId("category-ebrr")
                .reportCode("EBR_IMG_HASH_T01")
                .reportName("Image report")
                .parsedTable(parsedTable(1, "Image report"))
                .build());

        ArgumentCaptor<JimuReport> reportCaptor = ArgumentCaptor.forClass(JimuReport.class);
        verify(jimuReportDao).update(reportCaptor.capture());
        JimuReport persisted = reportCaptor.getValue();
        assertEquals(Integer.valueOf(0), persisted.getTemplate());
        assertEquals(Integer.valueOf(1), persisted.getSubmitForm());
        assertEquals(Integer.valueOf(1), persisted.getUpdateCount());
        assertEquals("1", persisted.getTenantId());
        assertEquals(MesProBatchRecordJimuReportGatewayImpl.FILL_FORM_PREVIEW_CSS, persisted.getCssStr());
    }

    @Test
    void saveOrUpdateReport_recreatesMissingExistingReportWithSameId() {
        JimuReportDao jimuReportDao = mock(JimuReportDao.class);
        MesProBatchRecordReportJsonBuilder reportJsonBuilder = mock(MesProBatchRecordReportJsonBuilder.class);
        MesProBatchRecordReportLayoutCalibrator reportLayoutCalibrator =
                mock(MesProBatchRecordReportLayoutCalibrator.class);
        MesProBatchRecordReportStyleEnhancer reportStyleEnhancer =
                mock(MesProBatchRecordReportStyleEnhancer.class);
        ReflectionTestUtils.setField(gateway, "jimuReportDao", jimuReportDao);
        ReflectionTestUtils.setField(gateway, "reportJsonBuilder", reportJsonBuilder);
        ReflectionTestUtils.setField(gateway, "reportLayoutCalibrator", reportLayoutCalibrator);
        ReflectionTestUtils.setField(gateway, "reportStyleEnhancer", reportStyleEnhancer);
        when(reportLayoutCalibrator.calibrate(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportJsonBuilder.build(any(), anyString())).thenReturn("{\"rows\":[]}");
        when(reportStyleEnhancer.enhance(anyString(), any())).thenReturn("{\"styled\":true}");
        when(jimuReportDao.get("report-missing")).thenReturn(null);
        TenantContextHolder.setTenantId(122L);

        gateway.saveOrUpdateReport(MesProBatchRecordJimuReportSaveReq.builder()
                .existingReportId("report-missing")
                .categoryId("category-ebrr")
                .reportCode("EBR_TN122_A_T13")
                .reportName("单包装工序生产记录")
                .parsedTable(parsedTable(13, "单包装工序生产记录"))
                .build());

        ArgumentCaptor<JimuReport> reportCaptor = ArgumentCaptor.forClass(JimuReport.class);
        verify(jimuReportDao).insert(reportCaptor.capture());
        verify(jimuReportDao, never()).update(any());
        JimuReport recreated = reportCaptor.getValue();
        assertEquals("report-missing", recreated.getId());
        assertEquals("EBR_TN122_A_T13", recreated.getCode());
        assertEquals("单包装工序生产记录", recreated.getName());
        assertEquals("122", recreated.getTenantId());
        assertEquals(Integer.valueOf(0), recreated.getTemplate());
        assertEquals(Integer.valueOf(1), recreated.getSubmitForm());
    }

    @Test
    void saveOrUpdateReport_updatesExistingReportWithSameCodeWhenSnapshotRetryLeavesJimuReport() {
        JimuReportDao jimuReportDao = mock(JimuReportDao.class);
        MesProBatchRecordReportJsonBuilder reportJsonBuilder = mock(MesProBatchRecordReportJsonBuilder.class);
        MesProBatchRecordReportLayoutCalibrator reportLayoutCalibrator =
                mock(MesProBatchRecordReportLayoutCalibrator.class);
        MesProBatchRecordReportStyleEnhancer reportStyleEnhancer =
                mock(MesProBatchRecordReportStyleEnhancer.class);
        ReflectionTestUtils.setField(gateway, "jimuReportDao", jimuReportDao);
        ReflectionTestUtils.setField(gateway, "reportJsonBuilder", reportJsonBuilder);
        ReflectionTestUtils.setField(gateway, "reportLayoutCalibrator", reportLayoutCalibrator);
        ReflectionTestUtils.setField(gateway, "reportStyleEnhancer", reportStyleEnhancer);
        when(reportLayoutCalibrator.calibrate(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportJsonBuilder.build(any(), anyString())).thenReturn("{\"rows\":[]}");
        when(reportStyleEnhancer.enhance(anyString(), any())).thenReturn("{\"styled\":true}");
        JimuReport existing = new JimuReport();
        existing.setId("report-left-from-rollback");
        existing.setCode("EBR_TN122_B_DOC_830a89a2_V71_T01");
        existing.setName("旧产品信息");
        existing.setUpdateCount(2);
        MiniDaoPage<JimuReport> page = new MiniDaoPage<>();
        page.setResults(java.util.List.of(existing));
        when(jimuReportDao.getAll(any(JimuReport.class), eq(1), eq(1))).thenReturn(page);
        TenantContextHolder.setTenantId(122L);

        MesProBatchRecordGeneratedReport result = gateway.saveOrUpdateReport(MesProBatchRecordJimuReportSaveReq.builder()
                .categoryId("category-ebrr")
                .reportCode("EBR_TN122_B_DOC_830a89a2_V71_T01")
                .reportName("产品信息")
                .parsedTable(parsedTable(1, "产品信息"))
                .build());

        ArgumentCaptor<JimuReport> reportCaptor = ArgumentCaptor.forClass(JimuReport.class);
        verify(jimuReportDao, never()).insert(any());
        verify(jimuReportDao).update(reportCaptor.capture());
        JimuReport updated = reportCaptor.getValue();
        assertEquals("report-left-from-rollback", result.reportId());
        assertEquals("report-left-from-rollback", updated.getId());
        assertEquals("EBR_TN122_B_DOC_830a89a2_V71_T01", updated.getCode());
        assertEquals("产品信息", updated.getName());
        assertEquals(Integer.valueOf(3), updated.getUpdateCount());
        assertEquals("122", updated.getTenantId());
    }

    @Test
    void renameReportName_updatesExistingJimuReportName() {
        JimuReportDao jimuReportDao = mock(JimuReportDao.class);
        ReflectionTestUtils.setField(gateway, "jimuReportDao", jimuReportDao);
        JimuReport existing = new JimuReport();
        existing.setId("report-123");
        existing.setName("旧名称");
        when(jimuReportDao.get("report-123")).thenReturn(existing);
        TenantContextHolder.setTenantId(1L);

        gateway.renameReportName("report-123", "新名称");

        ArgumentCaptor<JimuReport> reportCaptor = ArgumentCaptor.forClass(JimuReport.class);
        verify(jimuReportDao).update(reportCaptor.capture());
        assertEquals("新名称", reportCaptor.getValue().getName());
        assertEquals("1", reportCaptor.getValue().getTenantId());
    }

    @Test
    void ensureFormTemplateDesignerReport_createsVirtualDesignerReportFromTemplateVersion() {
        JimuReportDao jimuReportDao = mock(JimuReportDao.class);
        FormTemplateVersionMapper templateVersionMapper = mock(FormTemplateVersionMapper.class);
        org.jeecg.modules.jmreport.desreport.service.IJimuReportCategoryService reportCategoryService =
                mock(org.jeecg.modules.jmreport.desreport.service.IJimuReportCategoryService.class);
        ReflectionTestUtils.setField(gateway, "jimuReportDao", jimuReportDao);
        ReflectionTestUtils.setField(gateway, "templateVersionMapper", templateVersionMapper);
        ReflectionTestUtils.setField(gateway, "reportCategoryService", reportCategoryService);
        TreeModel category = mock(TreeModel.class);
        when(category.getTitle()).thenReturn(MesProBatchRecordReportConstants.CATEGORY_NAME);
        when(category.getId()).thenReturn("category-ebrr");
        when(reportCategoryService.queryList(any())).thenReturn(java.util.List.of(category));
        when(jimuReportDao.get("FORMTPL:123")).thenReturn(null);
        MiniDaoPage<JimuReport> emptyPage = new MiniDaoPage<>();
        emptyPage.setResults(java.util.List.of());
        when(jimuReportDao.getAll(any(JimuReport.class), eq(1), eq(1))).thenReturn(emptyPage);
        when(templateVersionMapper.selectById(123L)).thenReturn(FormTemplateVersionDO.builder()
                .id(123L)
                .templateName("模板A")
                .versionNo("V1.2")
                .jimuSchemaJson("{\"rows\":{}}")
                .build());
        TenantContextHolder.setTenantId(1L);

        gateway.ensureFormTemplateDesignerReport("FORMTPL:123");

        ArgumentCaptor<JimuReport> reportCaptor = ArgumentCaptor.forClass(JimuReport.class);
        verify(jimuReportDao).insert(reportCaptor.capture());
        JimuReport persisted = reportCaptor.getValue();
        assertEquals("FORMTPL:123", persisted.getId());
        assertEquals("FORMTPL:123", persisted.getCode());
        assertEquals("模板A V1.2", persisted.getName());
        assertEquals("category-ebrr", persisted.getType());
        assertEquals("{\"rows\":{}}", persisted.getJsonStr());
        assertEquals(MesProBatchRecordJimuReportGatewayImpl.FILL_FORM_PREVIEW_CSS, persisted.getCssStr());
        assertEquals("1", persisted.getTenantId());
    }

    @Test
    void updateReportJson_syncsVirtualDesignerReportBackToTemplateVersion() {
        JimuReportDao jimuReportDao = mock(JimuReportDao.class);
        FormTemplateVersionMapper templateVersionMapper = mock(FormTemplateVersionMapper.class);
        org.jeecg.modules.jmreport.desreport.service.IJimuReportCategoryService reportCategoryService =
                mock(org.jeecg.modules.jmreport.desreport.service.IJimuReportCategoryService.class);
        ReflectionTestUtils.setField(gateway, "jimuReportDao", jimuReportDao);
        ReflectionTestUtils.setField(gateway, "templateVersionMapper", templateVersionMapper);
        ReflectionTestUtils.setField(gateway, "reportCategoryService", reportCategoryService);
        TreeModel category = mock(TreeModel.class);
        when(category.getTitle()).thenReturn(MesProBatchRecordReportConstants.CATEGORY_NAME);
        when(category.getId()).thenReturn("category-ebrr");
        when(reportCategoryService.queryList(any())).thenReturn(java.util.List.of(category));
        FormTemplateVersionDO templateVersion = FormTemplateVersionDO.builder()
                .id(123L)
                .templateName("模板A")
                .versionNo("V1.2")
                .jimuSchemaJson("{\"rows\":{}}")
                .build();
        when(templateVersionMapper.selectById(123L)).thenReturn(templateVersion);
        JimuReport existing = new JimuReport();
        existing.setId("FORMTPL:123");
        existing.setCode("FORMTPL:123");
        existing.setName("模板A V1.2");
        existing.setUpdateCount(2);
        when(jimuReportDao.get("FORMTPL:123")).thenReturn(existing);
        TenantContextHolder.setTenantId(1L);

        gateway.updateReportJson("FORMTPL:123", "{\"rows\":{\"0\":{}}}");

        ArgumentCaptor<JimuReport> reportCaptor = ArgumentCaptor.forClass(JimuReport.class);
        verify(jimuReportDao).update(reportCaptor.capture());
        assertEquals("{\"rows\":{\"0\":{}}}", reportCaptor.getValue().getJsonStr());
        assertEquals(Integer.valueOf(3), reportCaptor.getValue().getUpdateCount());
        assertEquals("1", reportCaptor.getValue().getTenantId());

        ArgumentCaptor<FormTemplateVersionDO> templateCaptor = ArgumentCaptor.forClass(FormTemplateVersionDO.class);
        verify(templateVersionMapper).updateById(templateCaptor.capture());
        assertEquals("{\"rows\":{\"0\":{}}}", templateCaptor.getValue().getJimuSchemaJson());
    }

    private static MesProBatchRecordParsedTable parsedTable(int index, String title) {
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(index)
                .tableTitle(title)
                .rowCount(1)
                .columnCount(1)
                .rows(java.util.List.of(java.util.List.of(MesProBatchRecordParsedCell.builder()
                        .text(title)
                        .build())))
                .build();
    }
}
