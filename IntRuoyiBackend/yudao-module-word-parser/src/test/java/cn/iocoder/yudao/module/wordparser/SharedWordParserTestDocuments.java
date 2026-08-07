package cn.iocoder.yudao.module.wordparser;

import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

final class SharedWordParserTestDocuments {

    static final String OUTSIDE_PARAGRAPH = "Outside paragraph";
    static final String HEADER_TEXT = "Canonical header";
    static final String FOOTER_TEXT = "Canonical footer";

    private SharedWordParserTestDocuments() {
    }

    static byte[] canonicalDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            paragraph(document.createParagraph(), OUTSIDE_PARAGRAPH, false, 11, ParagraphAlignment.LEFT);

            XWPFHeaderFooterPolicy policy = document.createHeaderFooterPolicy();
            XWPFHeader header = policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT);
            paragraph(header.createParagraph(), HEADER_TEXT, true, 9, ParagraphAlignment.CENTER);
            XWPFFooter footer = policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT);
            paragraph(footer.createParagraph(), FOOTER_TEXT, false, 9, ParagraphAlignment.RIGHT);

            XWPFTable table = document.createTable(3, 3);
            XWPFTableRow firstRow = table.getRow(0);
            firstRow.setHeight(720);
            XWPFTableCell mergedHeader = firstRow.getCell(0);
            mergedHeader.setWidth("3600");
            mergedHeader.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            paragraph(mergedHeader.getParagraphArray(0), "Merged heading", true, 14, ParagraphAlignment.CENTER);
            tcPr(mergedHeader).addNewGridSpan().setVal(BigInteger.valueOf(2));
            firstRow.removeCell(1);
            setText(firstRow.getCell(1), "Side heading");
            firstRow.getCell(1).setWidth("1800");

            XWPFTableCell verticalStart = table.getRow(1).getCell(0);
            setText(verticalStart, "Vertical value");
            verticalStart.setWidth("1800");
            tcPr(verticalStart).addNewVMerge().setVal(STMerge.RESTART);
            XWPFTableCell verticalFollower = table.getRow(2).getCell(0);
            verticalFollower.setText("");
            verticalFollower.setWidth("1800");
            tcPr(verticalFollower).addNewVMerge().setVal(STMerge.CONTINUE);

            XWPFTableCell diagonal = table.getRow(1).getCell(1);
            setText(diagonal, "Diagonal cell");
            diagonal.setWidth("1800");
            CTTcPr diagonalProperties = tcPr(diagonal);
            CTTcBorders borders = diagonalProperties.isSetTcBorders()
                    ? diagonalProperties.getTcBorders() : diagonalProperties.addNewTcBorders();
            border(borders.addNewTop());
            border(borders.addNewBottom());
            border(borders.addNewLeft());
            border(borders.addNewRight());
            border(borders.addNewTl2Br());
            diagonalProperties.addNewShd().setFill("D9EAF7");

            setText(table.getRow(1).getCell(2), "Normal cell");
            table.getRow(1).getCell(2).setWidth("1800");
            setText(table.getRow(2).getCell(1), "Tail one");
            table.getRow(2).getCell(1).setWidth("1800");
            setText(table.getRow(2).getCell(2), "Tail two");
            table.getRow(2).getCell(2).setWidth("1800");

            return write(document);
        }
    }

    static byte[] emptyDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            return write(document);
        }
    }

    static byte[] invalidTableDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable table = document.createTable(1, 1);
            setText(table.getRow(0).getCell(0), "Invalid span");
            tcPr(table.getRow(0).getCell(0)).addNewGridSpan().setVal(BigInteger.ZERO);
            return write(document);
        }
    }

    private static void setText(XWPFTableCell cell, String text) {
        paragraph(cell.getParagraphArray(0), text, false, 11, ParagraphAlignment.LEFT);
    }

    private static void paragraph(XWPFParagraph paragraph, String text, boolean bold, int fontSize,
                                  ParagraphAlignment alignment) {
        paragraph.setAlignment(alignment);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(fontSize);
    }

    private static CTTcPr tcPr(XWPFTableCell cell) {
        return cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    }

    private static void border(CTBorder border) {
        border.setVal(STBorder.SINGLE);
        border.setSz(BigInteger.valueOf(8));
    }

    private static byte[] write(XWPFDocument document) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        document.write(output);
        return output.toByteArray();
    }
}
