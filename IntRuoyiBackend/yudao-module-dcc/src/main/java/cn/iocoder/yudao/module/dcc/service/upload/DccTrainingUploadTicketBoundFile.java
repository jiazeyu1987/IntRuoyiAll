package cn.iocoder.yudao.module.dcc.service.upload;

/** Training binding facts from one validated temporary upload ticket. */
public record DccTrainingUploadTicketBoundFile(DccUploadTicketBoundFile file, String fileSha256) {
}
