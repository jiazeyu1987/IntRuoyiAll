package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import jakarta.annotation.Resource;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccApprovedProductIdentityMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductIdentityResolver;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

/** Real approved writer, H2 project/catalog/relation and formal readonly workflow preview. */
class G46ApprovedProductIdentityTest extends DccApprovedProjectLeaderOwnerTest {
    @Resource DccApprovedProductIdentityMapper productProof;
    @Resource DccControlledFileMapper controlledFiles;
    @Resource cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper controlledMasters;
    @Resource PlatformTransactionManager transactionManager;

    @Test
    void approvedDccProductMustBeAvailableThroughFormalUploadProductPreview() {
        var completed = writer.writeApprovedRequest(approvedRequest());
        assertEquals("COMPLETED", completed.getStatus());
        assertEquals(1, relations.selectList().size());
        assertEquals(1, products.selectList().size());
        var workflow = new DccControlledFileWorkflowServiceImpl();
        ReflectionTestUtils.setField(workflow, "projectCodeMapper", projects);
        ReflectionTestUtils.setField(workflow, "projectAccessService", access);
        ReflectionTestUtils.setField(workflow, "approvedProductIdentityMapper", productProof);
        var product = workflow.previewProjectProduct(7L, completed.getGeneratedProjectCodeId());
        assertEquals("DCC_CATALOG", product.getSource());
        assertEquals(completed.getProductCode(), product.getProductCode());
        assertEquals(completed.getProductName(), product.getProductName());
        assertNull(product.getProductMasterId());
        assertEquals(completed.getGeneratedProductCatalogId(), product.getProductCatalogId());
        assertEquals(completed.getId(), product.getProductCreateRequestId());
        assertEquals(completed.getRelationId(), product.getProductRelationId());
    }

    @Test
    void modernMissingRelationOrForeignCatalogCannotBecomeUnboundOrUseAnotherIdentity() {
        var completed = writer.writeApprovedRequest(approvedRequest());
        var project = projects.selectById(completed.getGeneratedProjectCodeId());
        jdbc.update("UPDATE dcc_product_catalog SET tenant_id=2 WHERE id=?", completed.getGeneratedProductCatalogId());
        assertThrows(RuntimeException.class, () -> DccProjectProductIdentityResolver.resolve(project,null,productProof));
        jdbc.update("UPDATE dcc_product_catalog SET tenant_id=1 WHERE id=?", completed.getGeneratedProductCatalogId());
        relations.deleteById(completed.getRelationId());
        assertThrows(RuntimeException.class, () -> DccProjectProductIdentityResolver.resolve(project,null,productProof));
    }

    @Test
    void declaredInvalidMdmNeverFallsBackToTheApprovedCatalog() {
        var completed = writer.writeApprovedRequest(approvedRequest());
        var project = projects.selectById(completed.getGeneratedProjectCodeId());
        project.setProductMasterId(999L);
        var mdm = org.mockito.Mockito.mock(cn.iocoder.yudao.module.mdm.api.product.MdmProductApi.class);
        assertThrows(RuntimeException.class, () -> DccProjectProductIdentityResolver.resolve(project,mdm,productProof));
        assertEquals(1, productProof.selectApprovedProduct(1L,project.getId()).size());
    }

    @Test
    void actualCatalogCodeAndNameMustExactlyMatchApprovedFacts() {
        var completed=writer.writeApprovedRequest(approvedRequest());
        var project=projects.selectById(completed.getGeneratedProjectCodeId());
        for(String changed:new String[]{completed.getProductCode().toLowerCase(),completed.getProductCode()+" ","OŴNER-PRODUCT1"}) {
            jdbc.update("UPDATE dcc_product_catalog SET product_code=? WHERE id=?",changed,completed.getGeneratedProductCatalogId());
            assertThrows(RuntimeException.class,()->DccProjectProductIdentityResolver.resolve(project,null,productProof));
        }
        jdbc.update("UPDATE dcc_product_catalog SET product_code=?,product=? WHERE id=?",
                completed.getProductCode(),completed.getProductName()+" ",completed.getGeneratedProductCatalogId());
        assertThrows(RuntimeException.class,()->DccProjectProductIdentityResolver.resolve(project,null,productProof));
        jdbc.update("UPDATE dcc_product_catalog SET product=? WHERE id=?",completed.getProductName(),completed.getGeneratedProductCatalogId());
        assertEquals("DCC_CATALOG",DccProjectProductIdentityResolver.resolve(project,null,productProof).source());
    }

    @Test
    void typedProductProvenancePersistsWithTrueCatalogIdAndRollsBackWithFile() {
        var completed = writer.writeApprovedRequest(approvedRequest());
        var product = DccProjectProductIdentityResolver.resolve(
                projects.selectById(completed.getGeneratedProjectCodeId()),null,productProof);
        var master=cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO.builder()
                .tenantId(1L).categoryId(1L).fileName("product-proof.pdf").fileNumber("PRODUCT-PROOF").status("WORKING").build();
        controlledMasters.insert(master);
        var file = DccControlledFileDO.builder().tenantId(1L).masterId(master.getId()).categoryId(1L).directoryId(1L)
                .sourceFileId(1L).originalFileId(1L).title("Product proof").submitterId(1L).requesterId(1L).fileName("product-proof.pdf")
                .fileNumber("PRODUCT-PROOF").status("WORKING").versionNo("A/1-1")
                .dccProjectCodeId(completed.getGeneratedProjectCodeId()).productSource(product.source())
                .productMasterId(product.masterId()).productCatalogId(product.catalogId())
                .productRelationId(product.relationId()).productCreateRequestId(product.requestId())
                .productCode(product.code()).productName(product.name()).build();
        assertEquals(1,controlledFiles.insert(file));
        var saved = controlledFiles.selectById(file.getId());
        assertEquals("DCC_CATALOG",saved.getProductSource());
        assertNull(saved.getProductMasterId());
        assertEquals(completed.getGeneratedProductCatalogId(),saved.getProductCatalogId());
        assertEquals(completed.getRelationId(),saved.getProductRelationId());
        assertEquals(completed.getId(),saved.getProductCreateRequestId());
        var tx = new TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class,()->tx.execute(status->{
            controlledFiles.updateById(DccControlledFileDO.builder().id(file.getId()).productCode("SHOULD-ROLL-BACK").build());
            throw new IllegalStateException("actual isolated downstream failure");
        }));
        assertEquals(product.code(),controlledFiles.selectById(file.getId()).getProductCode());
        controlledFiles.deleteById(file.getId());
        controlledMasters.deleteById(master.getId());
    }

    @Test
    void productPreviewHttpSerializesExactLongProvenance() throws Exception {
        var workflow = org.mockito.Mockito.mock(cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowService.class);
        org.mockito.Mockito.when(workflow.previewProjectProduct(7L,9007199254740993L)).thenReturn(
                cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccProjectProductRespVO.builder()
                        .projectCodeId(9007199254740993L).source("DCC_CATALOG").productCatalogId(9007199254740994L)
                        .productRelationId(9007199254740995L).productCreateRequestId(9007199254740996L)
                        .productCode("BUSINESS-LONG-CODE").productName("正式产品").build());
        var controller = new cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController();
        ReflectionTestUtils.setField(controller,"workflowService",workflow);
        try(var actor=org.mockito.Mockito.mockStatic(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.class)) {
            actor.when(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils::getLoginUserId).thenReturn(7L);
            var response=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build()
                    .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/dcc/controlled-files/project-product")
                            .param("projectCodeId","9007199254740993")).andReturn();
            assertEquals(200,response.getResponse().getStatus());
            var body=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(response.getResponse().getContentAsString());
            assertTrue(body.get("data").get("productCatalogId").isTextual());
            assertEquals("9007199254740994",body.get("data").get("productCatalogId").asText());
            assertEquals("9007199254740993",body.get("data").get("projectCodeId").asText());
            assertEquals("DCC_CATALOG",body.get("data").get("source").asText());
        }
    }
}
