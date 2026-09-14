package cn.iocoder.yudao.module.dcc.signature.service.records;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.governance.vo.SignatureGovernanceRecordPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.governance.vo.SignatureGovernanceRecordRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.signature.governance.SignatureGovernanceRecordMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureImageService;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureImageSnapshot;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_RECORD_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_RECORD_PDF_EXPORT_FAILED;

@Slf4j
@Service
public class SignatureGovernanceRecordServiceImpl implements SignatureGovernanceRecordService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String PDF_FONT_PATH = "C:/Windows/Fonts/simhei.ttf";
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final float PDF_MARGIN = 42F;
    private static final float PDF_BODY_FONT_SIZE = 10.5F;
    private static final float PDF_TITLE_FONT_SIZE = 18F;
    private static final float PDF_LINE_GAP = 6F;
    private static final String DCC_SIGNATURE_SOURCE_TABLE = "dcc_controlled_file_signature";

    private final SignatureGovernanceRecordMapper recordMapper;
    private final DccElectronicSignatureImageService signatureImageService;
    private final DccControlledFileSignatureMapper dccSignatureMapper;

    public SignatureGovernanceRecordServiceImpl(SignatureGovernanceRecordMapper recordMapper,
                                                DccElectronicSignatureImageService signatureImageService,
                                                DccControlledFileSignatureMapper dccSignatureMapper) {
        this.recordMapper = recordMapper;
        this.signatureImageService = signatureImageService;
        this.dccSignatureMapper = dccSignatureMapper;
    }

    @Override
    public PageResult<SignatureGovernanceRecordRespVO> getPage(SignatureGovernanceRecordPageReqVO reqVO) {
        IPage<SignatureGovernanceRecordRespVO> page = new Page<>(reqVO.getPageNo(), reqVO.getPageSize());
        IPage<SignatureGovernanceRecordRespVO> result = recordMapper.selectSignatureRecordPage(page, reqVO);
        List<SignatureGovernanceRecordRespVO> records = result.getRecords();
        return new PageResult<>(records == null ? List.of() : records, result.getTotal());
    }

    @Override
    public SignatureGovernanceRecordPdfArtifact exportRecordPdf(String globalId) {
        String normalizedGlobalId = StrUtil.trim(globalId);
        if (StrUtil.isBlank(normalizedGlobalId)) {
            throw exception(SIGNATURE_GOVERNANCE_RECORD_NOT_EXISTS, globalId);
        }
        SignatureGovernanceRecordRespVO record = recordMapper.selectSignatureRecordByGlobalId(normalizedGlobalId);
        if (record == null) {
            throw exception(SIGNATURE_GOVERNANCE_RECORD_NOT_EXISTS, normalizedGlobalId);
        }
        DccElectronicSignatureImageSnapshot signatureImage = resolveSignatureImage(record);
        return new SignatureGovernanceRecordPdfArtifact(
                "electronic-signature-" + sanitizeFileNameSegment(normalizedGlobalId) + ".pdf",
                PDF_CONTENT_TYPE,
                renderRecordPdf(record, signatureImage));
    }

    private byte[] renderRecordPdf(SignatureGovernanceRecordRespVO record,
                                   DccElectronicSignatureImageSnapshot signatureImage) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            File fontFile = new File(PDF_FONT_PATH);
            if (!fontFile.isFile()) {
                throw exception(SIGNATURE_GOVERNANCE_RECORD_PDF_EXPORT_FAILED, "缺少字体文件 " + PDF_FONT_PATH);
            }
            PDFont font = PDType0Font.load(document, fontFile);
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float y = PDRectangle.A4.getHeight() - PDF_MARGIN;
                y = writeColoredLine(contentStream, font, 26F, PDF_MARGIN, y, "电子签名",
                        new Color(84, 86, 91));
                y = writeColoredLine(contentStream, font, 11.5F, PDF_MARGIN, y - 2F,
                        "电子签名记录", new Color(74, 78, 84));
                y = writeReferenceSignatureBlock(document, contentStream, font, y - 8F, record, signatureImage);
                y = writeColoredLine(contentStream, font, 12F, PDF_MARGIN, y - 2F,
                        "签名审计信息", new Color(74, 78, 84));
                for (PdfField field : buildPdfFields(record, signatureImage)) {
                    y = writeWrappedLine(contentStream, font, PDF_BODY_FONT_SIZE, PDF_MARGIN, y,
                            field.label() + "：" + field.value());
                }
            }
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (ServiceException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw exception(SIGNATURE_GOVERNANCE_RECORD_PDF_EXPORT_FAILED, ex.getMessage());
        }
    }

    private DccElectronicSignatureImageSnapshot resolveSignatureImage(SignatureGovernanceRecordRespVO record) {
        if (record.getSignerUserId() == null) {
            throw exception(SIGNATURE_GOVERNANCE_RECORD_PDF_EXPORT_FAILED, "签名人用户编号缺失，无法读取上传签名图片");
        }
        if (DCC_SIGNATURE_SOURCE_TABLE.equals(record.getSourceTable()) && record.getSourceRecordId() != null) {
            DccControlledFileSignatureDO signature = dccSignatureMapper.selectById(record.getSourceRecordId());
            return signatureImageService.verifySignatureSnapshot(signature);
        }
        if (record.getSignatureImageId() != null || record.getSignatureImageFileId() != null
                || StrUtil.isNotBlank(record.getSignatureImageSha256())) {
            return signatureImageService.verifySignatureSnapshot(DccElectronicSignatureImageSnapshot.builder()
                    .imageId(record.getSignatureImageId())
                    .versionNo(record.getSignatureImageVersionNo())
                    .fileId(record.getSignatureImageFileId())
                    .fileUrl(record.getSignatureImageFileUrl())
                    .contentType(record.getSignatureImageContentType())
                    .fileSize(record.getSignatureImageFileSize())
                    .sha256(record.getSignatureImageSha256())
                    .imageStatus(record.getSignatureImageStatusSnapshot())
                    .verifiedStatus(record.getSignatureImageVerifiedStatus())
                    .build());
        }
        return signatureImageService.requireActiveSnapshot(record.getSignerUserId());
    }

    private List<PdfField> buildPdfFields(SignatureGovernanceRecordRespVO record,
                                          DccElectronicSignatureImageSnapshot signatureImage) {
        return List.of(
                field("来源", displaySource(record)),
                field("来源表", displaySourceTable(record.getSourceTable())),
                field("来源记录编号", record.getSourceRecordId()),
                field("业务记录", value(record.getBusinessRecordCode()) + " / " + value(record.getBusinessRecordId())),
                field("业务名称", displayBusinessRecordName(record.getBusinessRecordName())),
                field("部门/岗位", value(record.getActorDeptNameSnapshot()) + " / " + value(record.getActorPostNamesSnapshot())),
                field("角色", record.getActorRoleNamesSnapshot()),
                field("签名图片文件编号", signatureImage.getFileId()),
                field("签名图片版本", signatureImage.getVersionNo()),
                field("签名图片校验", displayKnownValue(signatureImage.getVerifiedStatus())),
                field("证据状态", displayKnownValue(record.getEvidenceStatus())),
                field("证据哈希", record.getEvidenceHash()),
                field("详情路径", displayDetailPath(record.getDetailPath())),
                field("文件生成时间", LocalDateTime.now().format(DISPLAY_TIME_FORMATTER))
        );
    }

    private float writeReferenceSignatureBlock(PDDocument document, PDPageContentStream contentStream, PDFont font,
                                               float y, SignatureGovernanceRecordRespVO record,
                                               DccElectronicSignatureImageSnapshot signatureImage)
            throws IOException {
        byte[] imageContent = signatureImage == null ? null : signatureImage.getContent();
        if (imageContent == null || imageContent.length == 0) {
            throw exception(SIGNATURE_GOVERNANCE_RECORD_PDF_EXPORT_FAILED, "签名图片内容为空");
        }
        PDImageXObject image = createSignatureImage(document, imageContent, signatureImage);
        float blockX = PDF_MARGIN;
        float blockWidth = PDRectangle.A4.getWidth() - PDF_MARGIN * 2;
        float blockHeight = 238F;
        float blockBottom = y - blockHeight;
        contentStream.setNonStrokingColor(new Color(238, 246, 241));
        contentStream.addRect(blockX, blockBottom, blockWidth, blockHeight);
        contentStream.fill();
        contentStream.setNonStrokingColor(new Color(229, 214, 229));
        contentStream.addRect(blockX, y - 46F, blockWidth, 46F);
        contentStream.fill();
        contentStream.setStrokingColor(new Color(211, 221, 216));
        contentStream.setLineWidth(0.8F);
        contentStream.addRect(blockX, blockBottom, blockWidth, blockHeight);
        contentStream.stroke();

        drawReferenceBracket(contentStream, blockX + 26F, y - 58F, blockBottom + 26F);
        drawReferenceShield(contentStream, blockX + 22F, y - 104F);

        float contentX = blockX + 112F;
        writeColoredLine(contentStream, font, 13F, contentX, y - 58F, "签署人：",
                new Color(39, 42, 45));
        float detailX = contentX + 8F;
        writeColoredLine(contentStream, font, 16F, blockX + 58F, y - 28F, "签名",
                new Color(102, 97, 104));
        if (image != null) {
            drawSignatureImage(contentStream, image, contentX, y - 106F, 160F, 42F);
            writeColoredLine(contentStream, font, 13.5F, detailX, y - 118F,
                    "签名人：" + value(record.getSignerName()), Color.BLACK);
            writeColoredLine(contentStream, font, 12.8F, detailX, y - 138F,
                    "签名原因：" + displaySignatureReason(record), Color.BLACK);
            writeColoredLine(contentStream, font, 12.8F, detailX, y - 158F,
                    "签名时间：" + formatTime(record.getSignedAt()), Color.BLACK);
            float hashY = writeColoredLine(contentStream, font, 10.5F, contentX, y - 178F,
                    "签名图片哈希：", Color.BLACK);
            writeWrappedColoredLines(contentStream, font, 10.5F, contentX, hashY,
                    value(signatureImage.getSha256()), blockX + blockWidth - contentX - 18F, Color.BLACK);
        } else {
            writeColoredLine(contentStream, font, 11.5F, contentX, y - 78F,
                    "历史签名图片不可渲染", new Color(183, 28, 28));
            writeColoredLine(contentStream, font, 10.5F, contentX, y - 98F,
                    "图片文件编号：" + value(signatureImage.getFileId()), Color.BLACK);
            float hashY = writeColoredLine(contentStream, font, 10.5F, contentX, y - 116F,
                    "图片 SHA-256：", Color.BLACK);
            writeWrappedColoredLines(contentStream, font, 10.5F, contentX, hashY,
                    value(signatureImage.getSha256()), blockX + blockWidth - contentX - 18F, Color.BLACK);
            writeColoredLine(contentStream, font, 13.5F, detailX, y - 154F,
                    "签名人：" + value(record.getSignerName()), Color.BLACK);
            writeColoredLine(contentStream, font, 12.8F, detailX, y - 174F,
                    "签名原因：" + displaySignatureReason(record), Color.BLACK);
            writeColoredLine(contentStream, font, 12.8F, detailX, y - 194F,
                    "签名时间：" + formatTime(record.getSignedAt()), Color.BLACK);
        }

        float metaY = blockBottom - 18F;
        metaY = writeLine(contentStream, font, PDF_BODY_FONT_SIZE, PDF_MARGIN, metaY,
                "签名采用方式：上传签名图片");
        metaY = writeLine(contentStream, font, PDF_BODY_FONT_SIZE, PDF_MARGIN, metaY,
                "签名编号：" + value(record.getGlobalId()));
        return metaY - 10F;
    }

    private PDImageXObject createSignatureImage(PDDocument document, byte[] imageContent,
                                                DccElectronicSignatureImageSnapshot signatureImage) {
        try {
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageContent));
            if (bufferedImage == null || bufferedImage.getWidth() <= 0 || bufferedImage.getHeight() <= 0) {
                log.warn("[createSignatureImage][historical signature image cannot be rendered, fileId({}), sha256({})]",
                        signatureImage.getFileId(), signatureImage.getSha256());
                return null;
            }
            return LosslessFactory.createFromImage(document, bufferedImage);
        } catch (IOException | RuntimeException ex) {
            log.warn("[createSignatureImage][historical signature image cannot be rendered, fileId({}), "
                            + "sha256({}), errorType({}), errorMessage({})]",
                    signatureImage.getFileId(), signatureImage.getSha256(),
                    ex.getClass().getSimpleName(), ex.getMessage());
            return null;
        }
    }

    private void drawSignatureImage(PDPageContentStream contentStream, PDImageXObject image,
                                    float x, float y, float maxWidth, float maxHeight) throws IOException {
        float width = Math.max(1F, image.getWidth());
        float height = Math.max(1F, image.getHeight());
        float scale = Math.min(maxWidth / width, maxHeight / height);
        float drawWidth = Math.max(1F, width * scale);
        float drawHeight = Math.max(1F, height * scale);
        contentStream.setNonStrokingColor(Color.WHITE);
        contentStream.addRect(x - 4F, y - 4F, maxWidth + 8F, maxHeight + 8F);
        contentStream.fill();
        contentStream.drawImage(image, x, y + (maxHeight - drawHeight) / 2F, drawWidth, drawHeight);
    }

    private void drawReferenceBracket(PDPageContentStream contentStream, float x, float topY, float bottomY)
            throws IOException {
        contentStream.setStrokingColor(new Color(33, 82, 145));
        contentStream.setLineWidth(1.8F);
        contentStream.moveTo(x + 32F, topY);
        contentStream.lineTo(x + 10F, topY);
        contentStream.curveTo(x + 2F, topY, x, topY - 10F, x, topY - 22F);
        contentStream.lineTo(x, bottomY + 22F);
        contentStream.curveTo(x, bottomY + 10F, x + 2F, bottomY, x + 10F, bottomY);
        contentStream.lineTo(x + 32F, bottomY);
        contentStream.stroke();
    }

    private void drawReferenceShield(PDPageContentStream contentStream, float x, float y) throws IOException {
        contentStream.setStrokingColor(new Color(30, 59, 168));
        contentStream.setNonStrokingColor(new Color(30, 59, 168));
        contentStream.setLineWidth(2F);
        contentStream.moveTo(x, y + 36F);
        contentStream.lineTo(x + 30F, y + 36F);
        contentStream.lineTo(x + 30F, y + 10F);
        contentStream.lineTo(x + 16F, y);
        contentStream.lineTo(x, y + 10F);
        contentStream.closePath();
        contentStream.fill();
        contentStream.setNonStrokingColor(new Color(238, 246, 241));
        contentStream.addRect(x + 8F, y + 12F, 14F, 16F);
        contentStream.fill();
    }

    private float writeLine(PDPageContentStream contentStream, PDFont font, float fontSize,
                            float x, float y, String text) throws IOException {
        contentStream.setNonStrokingColor(Color.BLACK);
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
        return y - fontSize - PDF_LINE_GAP;
    }

    private float writeColoredLine(PDPageContentStream contentStream, PDFont font, float fontSize,
                                   float x, float y, String text, Color color) throws IOException {
        contentStream.setNonStrokingColor(color);
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
        return y - fontSize - PDF_LINE_GAP;
    }

    private float writeWrappedLine(PDPageContentStream contentStream, PDFont font, float fontSize,
                                   float x, float y, String text) throws IOException {
        float width = PDRectangle.A4.getWidth() - PDF_MARGIN * 2;
        List<String> lines = wrapText(text, font, fontSize, width);
        float cursorY = y;
        for (String line : lines) {
            cursorY = writeLine(contentStream, font, fontSize, x, cursorY, line);
        }
        return cursorY - 2F;
    }

    private float writeWrappedColoredLines(PDPageContentStream contentStream, PDFont font, float fontSize,
                                           float x, float y, String text, float maxWidth, Color color)
            throws IOException {
        List<String> lines = wrapText(text, font, fontSize, maxWidth);
        float cursorY = y;
        for (String line : lines) {
            cursorY = writeColoredLine(contentStream, font, fontSize, x, cursorY, line, color);
        }
        return cursorY - 2F;
    }

    private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String normalizedText = value(text);
        StringBuilder line = new StringBuilder();
        for (int index = 0; index < normalizedText.length(); index++) {
            String next = normalizedText.substring(index, index + 1);
            String candidate = line + next;
            if (!line.isEmpty() && stringWidth(font, fontSize, candidate) > maxWidth) {
                lines.add(line.toString());
                line = new StringBuilder(next);
            } else {
                line.append(next);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private float stringWidth(PDFont font, float fontSize, String text) throws IOException {
        return font.getStringWidth(text) / 1000F * fontSize;
    }

    private String sanitizeFileNameSegment(String value) {
        String trimmed = StrUtil.trim(value);
        if (StrUtil.isBlank(trimmed)) {
            throw exception(SIGNATURE_GOVERNANCE_RECORD_NOT_EXISTS, value);
        }
        return trimmed.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private static PdfField field(String label, Object value) {
        return new PdfField(label, value(value));
    }

    private static String formatTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DISPLAY_TIME_FORMATTER);
    }

    private static String displaySource(SignatureGovernanceRecordRespVO record) {
        return displayChinesePreferred(record.getSourceLabel(), record.getSourceCode());
    }

    private static String displaySourceTable(String sourceTable) {
        return switch (value(sourceTable)) {
            case DCC_SIGNATURE_SOURCE_TABLE -> "受控文件签名记录";
            case "mes_pro_batch_record_execution_signature" -> "批记录执行签名记录";
            case "showroom_change_request_signature" -> "展厅变更签名记录";
            case "bpm_approval_signature_record" -> "审批签名记录";
            default -> "未识别来源表";
        };
    }

    private static String displayBusinessRecordName(String businessRecordName) {
        String text = value(businessRecordName);
        if ("-".equals(text)) {
            return text;
        }
        return text.replace("BPM审批", "审批")
                .replace("MES_FEEDBACK 审批", "报工审批")
                .replace("SHOWROOM", "展厅");
    }

    private static String displaySignatureReason(SignatureGovernanceRecordRespVO record) {
        String comment = displayKnownValue(record.getComment());
        String action = displayChinesePreferred(record.getActionLabel(), record.getActionCode());
        if ("-".equals(comment)) {
            return action;
        }
        if (!containsChinese(comment) && containsChinese(action)) {
            return action;
        }
        return comment;
    }

    private static String displayChinesePreferred(String label, String code) {
        String displayLabel = displayKnownValue(label);
        if (!"-".equals(displayLabel) && containsChinese(displayLabel)) {
            return displayLabel;
        }
        return displayKnownValue(code);
    }

    private static String displayDetailPath(String detailPath) {
        return "-".equals(value(detailPath)) ? "-" : "可在系统详情页查看";
    }

    private static String displayKnownValue(Object value) {
        String text = value(value);
        return switch (text) {
            case "FILE" -> "文件";
            case "BATCH_RECORD" -> "批记录";
            case "SHOWROOM" -> "展厅";
            case "BPM" -> "审批";
            case "BPM审批" -> "审批";
            case "MES_FEEDBACK" -> "报工审批";
            case "APPROVE", "APPROVED" -> "审批通过";
            case "REJECT", "REJECTED" -> "审批驳回";
            case "RETURN" -> "退回";
            case "TRANSFER" -> "转办";
            case "ADD_SIGN" -> "加签";
            case "PASSWORD_VERIFIED" -> "签名密码已验证";
            case "PASSWORD_NOT_VERIFIED" -> "签名密码未验证";
            case "VALID" -> "有效";
            case "INVALID" -> "无效";
            case "ACTIVE" -> "已启用";
            case "DISABLED" -> "已停用";
            case "UPLOADED" -> "已上传";
            default -> text;
        };
    }

    private static boolean containsChinese(String text) {
        return value(text).chars().anyMatch(character ->
                Character.UnicodeScript.of(character) == Character.UnicodeScript.HAN);
    }

    private static String value(Object value) {
        String text = Objects.toString(value, "").trim();
        return StrUtil.isBlank(text) ? "-" : text;
    }

    private record PdfField(String label, String value) {
    }
}
