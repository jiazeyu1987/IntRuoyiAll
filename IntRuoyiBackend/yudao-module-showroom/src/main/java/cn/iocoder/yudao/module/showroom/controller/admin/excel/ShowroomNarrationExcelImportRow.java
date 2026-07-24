package cn.iocoder.yudao.module.showroom.controller.admin.excel;

public record ShowroomNarrationExcelImportRow(int rowNo,
                                              String targetType,
                                              String targetCode,
                                              String targetName,
                                              String language,
                                              String scriptText,
                                              Long audioFileId,
                                              String audioUrl,
                                              Integer audioDurationSeconds,
                                              String voice,
                                              byte[] audioContent) {

    public ShowroomNarrationExcelImportRow(int rowNo,
                                           String targetType,
                                           String targetCode,
                                           String targetName,
                                           String language,
                                           String scriptText,
                                           Long audioFileId,
                                           String audioUrl,
                                           Integer audioDurationSeconds,
                                           String voice) {
        this(rowNo, targetType, targetCode, targetName, language, scriptText, audioFileId, audioUrl,
                audioDurationSeconds, voice, null);
    }
}
