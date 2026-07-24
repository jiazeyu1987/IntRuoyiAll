package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;

public record DccConvertedPdf(String fileName, byte[] content) {

    public DccConvertedPdf {
        if (StrUtil.isBlank(fileName)) {
            throw new IllegalArgumentException("Converted PDF file name is required");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Converted PDF content is required");
        }
    }
}
