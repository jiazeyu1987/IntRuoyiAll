package cn.iocoder.yudao.module.showroom.controller.admin.excel;

public record ShowroomNarrationExcelRow(String targetType,
                                        String targetCode,
                                        String targetName,
                                        String language,
                                        String scriptText,
                                        Long audioFileId,
                                        String audioUrl,
                                        Integer audioDurationSeconds,
                                        String voice) {
}
