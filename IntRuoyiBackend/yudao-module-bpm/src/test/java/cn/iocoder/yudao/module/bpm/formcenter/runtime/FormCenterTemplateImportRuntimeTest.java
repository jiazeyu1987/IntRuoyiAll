package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalEffectResult;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalOrchestrator;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormCenterTemplateImportReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormCenterTemplateImportRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormCenterTemplatePoolPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormCenterTemplateRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormRecognizedField;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateRecognition;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateStatus;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormTemplateRecognizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormCenterTemplateImportRuntimeTest extends BaseMockitoUnitTest {

    @Mock
    private FormTemplateVersionMapper templateVersionMapper;
    @Mock
    private FormTemplateRecognizer templateRecognizer;
    @Mock
    private BusinessApprovalOrchestrator businessApprovalOrchestrator;

    @InjectMocks
    private FormCenterRuntimeServiceImpl runtimeService;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void importDocCreatesNewTemplateWithSystemGeneratedFirstVersion() {
        TenantContextHolder.setTenantId(122L);
        when(templateVersionMapper.selectLatestByTemplateName(122L, "损耗单")).thenReturn(null);
        mockRecognizer();
        mockInsertId(300L);

        FormCenterTemplateImportRespVO result = runtimeService.importDoc(req("损耗单", null), 101L);

        ArgumentCaptor<FormTemplateVersionDO> captor = ArgumentCaptor.forClass(FormTemplateVersionDO.class);
        verify(templateVersionMapper).insert(captor.capture());
        FormTemplateVersionDO inserted = captor.getValue();
        assertEquals("损耗单", inserted.getTemplateName());
        assertEquals("V1.0", inserted.getVersionNo());
        assertEquals(FormTemplateStatus.DRAFT.name(), inserted.getStatus());
        assertEquals(300L, result.getTemplateId());
        assertEquals("V1.0", result.getVersionNo());
        assertEquals("CREATE", result.getImportAction());
        assertNull(result.getApprovalProcessInstanceId());
        verify(businessApprovalOrchestrator, never()).submit(any());
    }

    @Test
    void importDocStartsUpgradeApprovalWhenExistingTemplateIsSelected() {
        TenantContextHolder.setTenantId(122L);
        when(templateVersionMapper.selectLatestByTemplateId(122L, 200L)).thenReturn(FormTemplateVersionDO.builder()
                .id(201L)
                .tenantId(122L)
                .templateId(200L)
                .templateName("损耗单")
                .versionNo("V1.0")
                .status(FormTemplateStatus.PUBLISHED.name())
                .build());
        mockRecognizer();
        mockInsertId(301L);
        when(businessApprovalOrchestrator.submit(any())).thenAnswer(invocation -> {
            BusinessApprovalContext context = invocation.getArgument(0);
            return BusinessApprovalRequest.builder()
                    .requestId(9001L)
                    .tenantId(context.getTenantId())
                    .policyId(88L)
                    .policyMode(BusinessApprovalPolicyMode.BPM_REQUIRED)
                    .processDefinitionKey("form-template-upgrade-v1")
                    .effectExecutorCode("FORM_TEMPLATE_UPGRADE")
                    .status(BusinessApprovalRequestStatus.PENDING_BPM)
                    .context(context)
                    .processInstanceId("PI-FORM-TPL-301")
                    .resultState(FormTemplateStatus.PENDING_APPROVAL.name())
                    .build();
        });

        FormCenterTemplateImportRespVO result = runtimeService.importDoc(req("损耗单", 200L), 101L);

        ArgumentCaptor<FormTemplateVersionDO> versionCaptor = ArgumentCaptor.forClass(FormTemplateVersionDO.class);
        verify(templateVersionMapper).insert(versionCaptor.capture());
        assertEquals(200L, versionCaptor.getValue().getTemplateId());
        assertEquals("V2.0", versionCaptor.getValue().getVersionNo());

        ArgumentCaptor<BusinessApprovalContext> contextCaptor = ArgumentCaptor.forClass(BusinessApprovalContext.class);
        verify(businessApprovalOrchestrator).submit(contextCaptor.capture());
        BusinessApprovalContext context = contextCaptor.getValue();
        assertEquals(122L, context.getTenantId());
        assertEquals("FORM_CENTER", context.getDataDomain());
        assertEquals("FORM_CENTER", context.getSystemCode());
        assertEquals("FORM_TEMPLATE", context.getObjectType());
        assertEquals("301", context.getObjectId());
        assertEquals("V2.0", context.getObjectVersion());
        assertEquals("UPGRADE", context.getActionCode());
        assertEquals(FormTemplateStatus.DRAFT.name(), context.getObjectState());
        assertEquals(101L, context.getApplicantUserId());

        assertEquals(200L, result.getTemplateId());
        assertEquals("V2.0", result.getVersionNo());
        assertEquals("UPGRADE", result.getImportAction());
        assertEquals(9001L, result.getApprovalRequestId());
        assertEquals("PI-FORM-TPL-301", result.getApprovalProcessInstanceId());
        assertEquals(FormTemplateStatus.PENDING_APPROVAL.name(), result.getStatus());
    }

    @Test
    void importDocStartsUpgradeApprovalWhenExistingTemplateNameIsTypedWithoutSelection() {
        TenantContextHolder.setTenantId(122L);
        when(templateVersionMapper.selectLatestByTemplateName(122L, "损耗单")).thenReturn(FormTemplateVersionDO.builder()
                .id(201L)
                .tenantId(122L)
                .templateId(200L)
                .templateName("损耗单")
                .versionNo("V1.0")
                .status(FormTemplateStatus.PUBLISHED.name())
                .build());
        mockRecognizer();
        mockInsertId(301L);
        mockPendingApproval();

        FormCenterTemplateImportRespVO result = runtimeService.importDoc(req(" 损耗单 ", null), 101L);

        ArgumentCaptor<FormTemplateVersionDO> versionCaptor = ArgumentCaptor.forClass(FormTemplateVersionDO.class);
        verify(templateVersionMapper).insert(versionCaptor.capture());
        assertEquals("损耗单", versionCaptor.getValue().getTemplateName());
        assertEquals(200L, versionCaptor.getValue().getTemplateId());
        assertEquals("V2.0", versionCaptor.getValue().getVersionNo());
        assertEquals("UPGRADE", result.getImportAction());
        assertEquals(FormTemplateStatus.PENDING_APPROVAL.name(), result.getStatus());
    }

    @Test
    void templatePoolExposesRecognizedFieldsForInlinePreview() {
        TenantContextHolder.setTenantId(122L);
        FormTemplateVersionDO version = FormTemplateVersionDO.builder()
                .id(401L)
                .tenantId(122L)
                .templateId(400L)
                .templateName("损耗单")
                .versionNo("V2.0")
                .status(FormTemplateStatus.PUBLISHED.name())
                .jimuSchemaJson("{\"cellRules\":[{\"rowIndex\":3,\"columnIndex\":1,\"valueType\":\"STRING\"}]}")
                .sourceFileName("loss.doc")
                .recognizedSchemaJson("[{\"fieldCode\":\"lossReason\",\"label\":\"损耗原因\",\"fieldType\":\"textarea\",\"required\":true}]")
                .build();
        when(templateVersionMapper.selectPage(any(FormCenterTemplatePoolPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(version), 1L));

        PageResult<FormCenterTemplateRespVO> result = runtimeService.getTemplatePool(new FormCenterTemplatePoolPageReqVO());

        FormCenterTemplateRespVO row = result.getList().get(0);
        assertEquals(400L, row.getTemplateId());
        assertEquals("损耗原因", row.getRecognizedFields().get(0).getLabel());
        assertEquals("textarea", row.getRecognizedFields().get(0).getFieldType());
        assertEquals("{\"cellRules\":[{\"rowIndex\":3,\"columnIndex\":1,\"valueType\":\"STRING\"}]}",
                row.getJimuSchemaJson());
        assertEquals("loss.doc", row.getSourceFileName());
    }

    private void mockRecognizer() {
        when(templateRecognizer.recognize(any())).thenReturn(FormTemplateRecognition.success(List.of(
                FormRecognizedField.required("lossReason", "损耗原因", "textarea"))));
    }

    private void mockPendingApproval() {
        when(businessApprovalOrchestrator.submit(any())).thenAnswer(invocation -> {
            BusinessApprovalContext context = invocation.getArgument(0);
            return BusinessApprovalRequest.builder()
                    .requestId(9001L)
                    .tenantId(context.getTenantId())
                    .policyId(88L)
                    .policyMode(BusinessApprovalPolicyMode.BPM_REQUIRED)
                    .processDefinitionKey("form-template-upgrade-v1")
                    .effectExecutorCode("FORM_TEMPLATE_UPGRADE")
                    .status(BusinessApprovalRequestStatus.PENDING_BPM)
                    .context(context)
                    .processInstanceId("PI-FORM-TPL-301")
                    .resultState(FormTemplateStatus.PENDING_APPROVAL.name())
                    .build();
        });
    }

    private void mockInsertId(Long id) {
        doAnswer(invocation -> {
            FormTemplateVersionDO version = invocation.getArgument(0);
            version.setId(id);
            return 1;
        }).when(templateVersionMapper).insert(any(FormTemplateVersionDO.class));
    }

    private FormCenterTemplateImportReqVO req(String templateName, Long selectedTemplateId) {
        FormCenterTemplateImportReqVO reqVO = new FormCenterTemplateImportReqVO();
        reqVO.setTemplateName(templateName);
        reqVO.setSelectedTemplateId(selectedTemplateId);
        reqVO.setRemark("form remark");
        reqVO.setFile(new MockMultipartFile("file", "loss.doc", "application/msword", new byte[]{1, 2, 3}));
        return reqVO;
    }

}
