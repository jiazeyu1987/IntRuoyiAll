package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkRespVO;

public record DccControlledFileBinary(
        String fileName,
        String contentType,
        byte[] bytes,
        DccControlledPreviewWatermarkRespVO watermark
) {
}
