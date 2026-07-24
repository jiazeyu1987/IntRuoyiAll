package cn.iocoder.yudao.module.dcc.service.file;

public record DccOnlyOfficeConversionCommand(String converterUrl,
                                             String jwtSecret,
                                             String fileType,
                                             String key,
                                             String title,
                                             String documentUrl) {
}
