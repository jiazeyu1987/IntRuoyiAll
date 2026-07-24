package cn.iocoder.yudao.module.dcc.service.file;

/**
 * Rendered approval process Word document.
 */
public record DccApprovalPrintRenderedWord(String fileName, String contentType, byte[] bytes) {
}
