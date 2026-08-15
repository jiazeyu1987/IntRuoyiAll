package cn.iocoder.yudao.module.dcc.signature.governance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.governance.vo.SignatureGovernanceRecordPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.governance.vo.SignatureGovernanceRecordRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.signature.governance.SignatureGovernanceRecordMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureImageService;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureImageSnapshot;
import cn.iocoder.yudao.module.dcc.signature.service.records.SignatureGovernanceRecordPdfArtifact;
import cn.iocoder.yudao.module.dcc.signature.service.records.SignatureGovernanceRecordServiceImpl;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class SignatureGovernanceRecordServiceTest extends BaseMockitoUnitTest {

    @Mock
    private SignatureGovernanceRecordMapper recordMapper;
    @Mock
    private DccElectronicSignatureImageService signatureImageService;
    @Mock
    private DccControlledFileSignatureMapper dccSignatureMapper;

    @Test
    void pageDelegatesToUnionMapperAndKeepsGlobalPaginationMetadata() {
        SignatureGovernanceRecordPageReqVO reqVO = new SignatureGovernanceRecordPageReqVO();
        reqVO.setPageNo(2);
        reqVO.setPageSize(20);
        reqVO.setSourceCodes(List.of("FILE", "BATCH_RECORD", "SHOWROOM"));
        reqVO.setKeyword("BR-20260714");
        reqVO.setSignerUserId(101L);
        reqVO.setSignerKeyword("张三");
        reqVO.setActionCode("APPROVE");
        reqVO.setEvidenceHash("hash-abc");
        reqVO.setSignedAt(new LocalDateTime[] {
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 7, 14, 23, 59)
        });

        SignatureGovernanceRecordRespVO row = new SignatureGovernanceRecordRespVO();
        row.setGlobalId("FILE-1001");
        row.setSourceCode("FILE");
        row.setSourceLabel("文件");
        row.setSourceTable("dcc_controlled_file_signature");
        row.setSourceRecordId(1001L);
        row.setBusinessRecordCode("DCC-001");
        row.setBusinessRecordName("受控文件");
        row.setSignerName("张三");
        row.setActionLabel("批准");
        row.setSignedAt(LocalDateTime.of(2026, 7, 14, 10, 30));

        Page<SignatureGovernanceRecordRespVO> mapperPage = new Page<>(2, 20, 3);
        mapperPage.setRecords(List.of(row));
        when(recordMapper.selectSignatureRecordPage(any(), same(reqVO))).thenReturn(mapperPage);

        SignatureGovernanceRecordServiceImpl service = newService();
        PageResult<SignatureGovernanceRecordRespVO> result = service.getPage(reqVO);

        assertEquals(3L, result.getTotal());
        assertEquals("FILE-1001", result.getList().get(0).getGlobalId());
        assertEquals("文件", result.getList().get(0).getSourceLabel());
        ArgumentCaptor<IPage<SignatureGovernanceRecordRespVO>> pageCaptor = ArgumentCaptor.forClass(IPage.class);
        verify(recordMapper).selectSignatureRecordPage(pageCaptor.capture(), same(reqVO));
        assertEquals(2L, pageCaptor.getValue().getCurrent());
        assertEquals(20L, pageCaptor.getValue().getSize());
    }

    @Test
    void mapperXmlUsesUnionAllForOnlyRealPersistedSignatureSources() throws Exception {
        String xml = Files.readString(Path.of(
                "src/main/resources/mapper/signature/governance/SignatureGovernanceRecordMapper.xml"),
                StandardCharsets.UTF_8);

        assertTrue(xml.contains("UNION ALL"));
        assertTrue(xml.contains("dcc_controlled_file_signature"));
        assertTrue(xml.contains("mes_pro_batch_record_execution_signature"));
        assertTrue(xml.contains("showroom_change_request_signature"));
        assertTrue(xml.contains("bpm_approval_signature_record"));
        assertTrue(xml.contains("CONVERT('FILE' USING utf8mb4) AS source_code"));
        assertTrue(xml.contains("CONVERT('BATCH_RECORD' USING utf8mb4) AS source_code"));
        assertTrue(xml.contains("CONVERT('SHOWROOM' USING utf8mb4) AS source_code"));
        assertTrue(xml.contains("CONVERT(sig.module_code USING utf8mb4) AS source_code"));
        assertTrue(xml.contains("CONVERT('文件' USING utf8mb4) AS source_label"));
        assertTrue(xml.contains("CONVERT('批记录' USING utf8mb4) AS source_label"));
        assertTrue(xml.contains("CONVERT('展厅' USING utf8mb4) AS source_label"));
        assertTrue(xml.contains("WHEN sig.module_code = 'BPM' THEN 'BPM审批'"));
        assertTrue(xml.contains("WHEN sig.module_code = 'MES_FEEDBACK' THEN '报工审批'"));
        assertFalse(xml.contains("COLLATE"), "JSQLParser-backed tenant interceptor cannot parse COLLATE in UNION mapper SQL");
        assertTrue(xml.contains("ORDER BY merged.signed_at DESC, merged.source_record_id DESC"));
        assertTrue(xml.contains("sourceCodes"));
        assertTrue(xml.contains("keyword"));
        assertTrue(xml.contains("signerUserId"));
        assertTrue(xml.contains("merged.signer_user_id = #{reqVO.signerUserId}"));
        assertTrue(xml.contains("signerKeyword"));
        assertTrue(xml.contains("evidenceHash"));
        assertTrue(xml.contains("signedAt"));
        assertTrue(xml.contains("selectSignatureRecordByGlobalId"));
        assertTrue(xml.contains("merged.global_id = #{globalId}"));
        assertFalse(xml.contains("'SCHEDULING' AS source_code"));
        assertFalse(xml.contains("'DOCUMENT_CONTROL' AS source_code"));
        assertFalse(xml.toLowerCase().contains("mock"));
        assertFalse(xml.toLowerCase().contains("fallback"));
    }

    @Test
    void exportRecordPdfLoadsExactGlobalRecordAndRendersPdfArtifact() throws Exception {
        SignatureGovernanceRecordRespVO row = new SignatureGovernanceRecordRespVO();
        row.setGlobalId("BPM-1784126214000");
        row.setSourceCode("BPM");
        row.setSourceLabel("BPM审批");
        row.setSourceTable("bpm_approval_signature_record");
        row.setSourceRecordId(1784126214000L);
        row.setBusinessRecordCode("e102a571-804d-11f1-be22");
        row.setBusinessRecordName("BPM审批 e102a571-804d-11f1-be22");
        row.setSignerUserId(101L);
        row.setSignerName("玻璃管理员");
        row.setActionCode("APPROVE");
        row.setActionLabel("审批通过");
        row.setMeaningLabel("电子签名密码已验证");
        row.setComment("APPROVE");
        row.setSignedAt(LocalDateTime.of(2026, 7, 15, 17, 30, 14));
        row.setEvidenceStatus("PASSWORD_VERIFIED");
        when(recordMapper.selectSignatureRecordByGlobalId("BPM-1784126214000")).thenReturn(row);
        when(signatureImageService.requireActiveSnapshot(101L)).thenReturn(DccElectronicSignatureImageSnapshot.builder()
                .imageId(9101L)
                .versionNo(1)
                .fileId(8101L)
                .contentType("image/png")
                .sha256("image-sha256")
                .imageStatus("ACTIVE")
                .verifiedStatus("VALID")
                .content(tinyPng())
                .build());

        SignatureGovernanceRecordServiceImpl service = newService();
        SignatureGovernanceRecordPdfArtifact artifact = service.exportRecordPdf("BPM-1784126214000");

        assertEquals("electronic-signature-BPM-1784126214000.pdf", artifact.fileName());
        assertEquals("application/pdf", artifact.contentType());
        assertTrue(new String(artifact.content(), 0, 5, StandardCharsets.UTF_8).startsWith("%PDF"));
        assertTrue(artifact.content().length > 1000);
        verify(recordMapper).selectSignatureRecordByGlobalId("BPM-1784126214000");
    }

    @Test
    void exportRecordPdfEmbedsSignerUploadedSignatureImage() throws Exception {
        SignatureGovernanceRecordRespVO row = new SignatureGovernanceRecordRespVO();
        row.setGlobalId("BPM-3");
        row.setSourceCode("BPM");
        row.setSourceLabel("BPM审批");
        row.setSourceTable("bpm_approval_signature_record");
        row.setSourceRecordId(3L);
        row.setBusinessRecordCode("e102a571-804d-11f1-be22");
        row.setBusinessRecordName("BPM审批 e102a571-804d-11f1-be22");
        row.setSignerUserId(101L);
        row.setSignerName("玻璃管理员");
        row.setActionCode("APPROVE");
        row.setActionLabel("审批通过");
        row.setMeaningLabel("电子签名密码已验证");
        row.setComment("APPROVE");
        row.setSignedAt(LocalDateTime.of(2026, 7, 15, 17, 30, 14));
        row.setEvidenceStatus("PASSWORD_VERIFIED");
        row.setSignatureImageId(9101L);
        row.setSignatureImageVersionNo(2);
        row.setSignatureImageFileId(8101L);
        row.setSignatureImageFileUrl("/admin-api/infra/file/28/get/dcc/signature-images/signature.png");
        row.setSignatureImageSha256("image-sha256");
        row.setSignatureImageContentType("image/png");
        row.setSignatureImageFileSize(2048L);
        row.setSignatureImageStatusSnapshot("ACTIVE");
        row.setSignatureImageVerifiedStatus("VALID");
        byte[] imageContent = tinyPng();
        when(recordMapper.selectSignatureRecordByGlobalId("BPM-3")).thenReturn(row);
        when(signatureImageService.verifySignatureSnapshot(any(DccElectronicSignatureImageSnapshot.class)))
                .thenReturn(DccElectronicSignatureImageSnapshot.builder()
                .imageId(9101L)
                .versionNo(2)
                .fileId(8101L)
                .contentType("image/png")
                .sha256("image-sha256")
                .imageStatus("ACTIVE")
                .verifiedStatus("VALID")
                .content(imageContent)
                .build());

        SignatureGovernanceRecordPdfArtifact artifact = newService().exportRecordPdf("BPM-3");

        assertPdfContainsImage(artifact.content());
        ArgumentCaptor<DccElectronicSignatureImageSnapshot> snapshotCaptor =
                ArgumentCaptor.forClass(DccElectronicSignatureImageSnapshot.class);
        verify(signatureImageService).verifySignatureSnapshot(snapshotCaptor.capture());
        assertEquals(9101L, snapshotCaptor.getValue().getImageId());
        assertEquals(2, snapshotCaptor.getValue().getVersionNo());
        assertEquals(8101L, snapshotCaptor.getValue().getFileId());
        assertEquals("image-sha256", snapshotCaptor.getValue().getSha256());
        verify(signatureImageService, never()).requireActiveSnapshot(101L);
    }

    @Test
    void exportRecordPdfMarksUndecodableHistoricalImageInsteadOfFailing() throws Exception {
        SignatureGovernanceRecordRespVO row = new SignatureGovernanceRecordRespVO();
        row.setGlobalId("FILE-982");
        row.setSourceCode("FILE");
        row.setSourceLabel("文件");
        row.setSourceTable("dcc_controlled_file_signature");
        row.setSourceRecordId(982L);
        row.setBusinessRecordCode("DCC-HISTORICAL-001");
        row.setBusinessRecordName("历史签名归档文件");
        row.setSignerUserId(101L);
        row.setSignerName("历史签名人");
        row.setActionCode("APPROVE");
        row.setActionLabel("审批通过");
        row.setSignedAt(LocalDateTime.of(2026, 8, 2, 22, 28, 37));
        row.setEvidenceStatus("VALID");
        row.setEvidenceHash("evidence-hash");
        DccControlledFileSignatureDO signature = new DccControlledFileSignatureDO();
        signature.setId(982L);
        when(recordMapper.selectSignatureRecordByGlobalId("FILE-982")).thenReturn(row);
        when(dccSignatureMapper.selectById(982L)).thenReturn(signature);
        when(signatureImageService.verifySignatureSnapshot(signature))
                .thenReturn(DccElectronicSignatureImageSnapshot.builder()
                        .imageId(9101L)
                        .versionNo(1)
                        .fileId(8101L)
                        .contentType("image/png")
                        .sha256("historical-image-sha256")
                        .imageStatus("ACTIVE")
                        .verifiedStatus("VALID")
                        .content("not-a-decodable-image".getBytes(StandardCharsets.UTF_8))
                        .build());

        SignatureGovernanceRecordPdfArtifact artifact = newService().exportRecordPdf("FILE-982");

        String text = extractPdfText(artifact.content());
        assertTrue(text.contains("历史签名图片不可渲染"));
        assertTrue(text.contains("图片文件编号：8101"));
        assertTrue(text.contains("historical-image-sha256"));
        assertTrue(text.contains("evidence-hash"));
    }

    @Test
    void exportRecordPdfRendersChineseSignatureBlockAndWrapsLongHash() throws Exception {
        SignatureGovernanceRecordRespVO row = new SignatureGovernanceRecordRespVO();
        row.setGlobalId("BPM-3");
        row.setSourceCode("BPM");
        row.setSourceLabel("BPM审批");
        row.setSourceTable("bpm_approval_signature_record");
        row.setSourceRecordId(3L);
        row.setBusinessRecordCode("e102a571-804d-11f1-be22");
        row.setBusinessRecordName("BPM审批 e102a571-804d-11f1-be22");
        row.setSignerUserId(101L);
        row.setSignerName("玻璃管理员");
        row.setActorDeptNameSnapshot("质量部");
        row.setActorPostNamesSnapshot("管理员");
        row.setActionCode("APPROVE");
        row.setActionLabel("审批通过");
        row.setMeaningCode("APPROVE");
        row.setMeaningLabel("电子签名密码已验证");
        row.setComment("I approve this document");
        row.setSignedAt(LocalDateTime.of(2026, 7, 15, 17, 30, 14));
        row.setEvidenceStatus("PASSWORD_VERIFIED");
        String longHash = "87b335f7e9429e37ff0df4c0c966681a86932139eade14bf1957d1fda2a19430"
                + "bc5d1e8c90433ef7b113a8360fa8da9ab3174dd260bf92c146fca7af550aab20";
        when(recordMapper.selectSignatureRecordByGlobalId("BPM-3")).thenReturn(row);
        when(signatureImageService.requireActiveSnapshot(101L)).thenReturn(DccElectronicSignatureImageSnapshot.builder()
                .imageId(9101L)
                .versionNo(2)
                .fileId(8101L)
                .contentType("image/png")
                .sha256(longHash)
                .imageStatus("ACTIVE")
                .verifiedStatus("VALID")
                .content(tinyPng())
                .build());

        SignatureGovernanceRecordPdfArtifact artifact = newService().exportRecordPdf("BPM-3");
        writeSamplePdfIfRequested(artifact.content());

        String text = extractPdfText(artifact.content());
        String normalizedText = text.replace("\r\n", "\n");
        assertTrue(text.contains("电子签名"));
        assertTrue(text.contains("签署人："));
        assertTrue(text.contains("签名人：玻璃管理员"));
        assertTrue(text.contains("签名原因：审批通过"));
        assertTrue(text.contains("签名时间：2026-07-15 17:30:14"));
        assertTrue(text.contains("签名采用方式：上传签名图片"));
        assertTrue(text.contains("签名编号：BPM-3"));
        assertTrue(text.contains("签名审计信息"));
        assertTrue(text.contains("来源：审批"));
        assertTrue(text.contains("来源表：审批签名记录"));
        assertTrue(text.contains("业务名称：审批 e102a571-804d-11f1-be22"));
        assertTrue(text.contains("签名图片校验：有效"));
        assertTrue(text.contains("证据状态：签名密码已验证"));
        assertTrue(text.contains("签名图片哈希："));
        assertTrue(normalizedText.contains("签名图片哈希：\n"), "长 Hash 应在标签后换行显示");
        String renderedHashBlock = signatureBlockHashText(normalizedText);
        assertEquals(longHash, renderedHashBlock.replace("\n", ""));
        assertTrue(renderedHashBlock.contains("\n"), "过长 Hash 应按可用宽度拆成多行");
        assertFalse(text.contains("Signed by:"));
        assertFalse(text.contains("Signer Name:"));
        assertFalse(text.contains("Signing Reason:"));
        assertFalse(text.contains("Signing Time:"));
        assertFalse(text.contains("Signature Adoption:"));
        assertFalse(text.contains("Signature ID:"));
        assertFalse(text.contains("Signature Image Hash:"));
        assertFalse(text.contains("签名图片 Hash"));
        assertFalse(text.contains("证据 Hash"));
        assertFalse(text.contains("Hash"));
        assertFalse(text.contains("用户ID"));
        assertFalse(text.contains("APPROVE"));
        assertFalse(text.contains("PASSWORD_VERIFIED"));
        assertFalse(text.contains("VALID"));
        assertFalse(text.contains("bpm_approval_signature_record"));
        assertFalse(text.contains("BPM审批"));
        String auditSection = auditSectionText(normalizedText);
        assertTrue(auditSection.contains("来源记录编号：3"));
        assertTrue(auditSection.contains("部门/岗位：质量部 / 管理员"));
        assertTrue(auditSection.contains("角色：-"));
        assertTrue(auditSection.contains("签名图片文件编号：8101"));
        assertTrue(auditSection.contains("签名图片版本：2"));
        assertTrue(auditSection.contains("签名图片校验：有效"));
        assertFalse(auditSection.contains("全局记录编号："), "全局记录编号与上方签名编号一致，审计区不应重复");
        assertFalse(auditSection.contains("签名人："), "签名人已在上方签名块展示，审计区不应重复");
        assertFalse(auditSection.contains("签名动作："), "签名动作已作为签名原因展示，审计区不应重复");
        assertFalse(auditSection.contains("签名含义："), "签名含义已由证据状态补充表达，审计区不应重复");
        assertFalse(auditSection.contains("签名意见："), "签名意见已作为签名原因展示，审计区不应重复");
        assertFalse(auditSection.contains("签名时间："), "签名时间已在上方签名块展示，审计区不应重复");
        assertFalse(auditSection.contains("签名采用方式："), "签名采用方式已在签名块下方展示，审计区不应重复");
        assertFalse(auditSection.contains("签名编号："), "签名编号已在签名块下方展示，审计区不应重复");
        assertFalse(auditSection.contains("签名图片哈希："), "签名图片哈希已在上方签名块展示，审计区不应重复");
        assertPdfContainsImage(artifact.content());
    }

    private SignatureGovernanceRecordServiceImpl newService() {
        return new SignatureGovernanceRecordServiceImpl(recordMapper, signatureImageService, dccSignatureMapper);
    }

    private static void assertPdfContainsImage(byte[] pdf) throws Exception {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdf))) {
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                Iterator<COSName> xObjectNames = document.getPage(pageIndex).getResources().getXObjectNames().iterator();
                while (xObjectNames.hasNext()) {
                    PDXObject xObject = document.getPage(pageIndex).getResources().getXObject(xObjectNames.next());
                    if (xObject instanceof PDImageXObject) {
                        return;
                    }
                }
            }
        }
        throw new AssertionError("PDF should contain uploaded signature image XObject");
    }

    private static String extractPdfText(byte[] pdf) throws Exception {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdf))) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static String signatureBlockHashText(String normalizedPdfText) {
        String hashLabel = "签名图片哈希：\n";
        int hashStart = normalizedPdfText.indexOf(hashLabel);
        assertTrue(hashStart >= 0, "签名块应包含签名图片哈希标签");
        String afterHashLabel = normalizedPdfText.substring(hashStart + hashLabel.length());
        int hashEnd = afterHashLabel.indexOf("\n签名采用方式");
        assertTrue(hashEnd > 0, "签名块 Hash 后应继续展示签名采用方式");
        return afterHashLabel.substring(0, hashEnd);
    }

    private static String auditSectionText(String normalizedPdfText) {
        String auditLabel = "签名审计信息\n";
        int auditStart = normalizedPdfText.indexOf(auditLabel);
        assertTrue(auditStart >= 0, "PDF 应包含签名审计信息区域");
        return normalizedPdfText.substring(auditStart + auditLabel.length());
    }

    private static void writeSamplePdfIfRequested(byte[] pdf) throws Exception {
        String samplePath = System.getProperty("signature.pdf.sample");
        if (samplePath == null || samplePath.isBlank()) {
            return;
        }
        Path path = Path.of(samplePath);
        Files.createDirectories(path.getParent());
        Files.write(path, pdf);
    }

    private static byte[] tinyPng() throws Exception {
        BufferedImage image = new BufferedImage(24, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.drawLine(2, 7, 21, 3);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }
}
