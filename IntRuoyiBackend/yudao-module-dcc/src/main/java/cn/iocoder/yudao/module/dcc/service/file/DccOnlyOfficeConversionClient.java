package cn.iocoder.yudao.module.dcc.service.file;

public interface DccOnlyOfficeConversionClient {

    byte[] convertToPdf(DccOnlyOfficeConversionCommand command);
}
