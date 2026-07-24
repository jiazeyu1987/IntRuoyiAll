package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;

public interface DccDocumentPdfConversionService {

    DccConvertedPdf convertToPdf(FileDO sourceFile);
}
