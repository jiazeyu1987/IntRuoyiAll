package cn.iocoder.yudao.module.dcc.service.file;

public record DccProjectCodeRecognitionResult(Long projectCodeId,
                                              DccProjectCodeRecognitionMatchType matchType,
                                              String matchText,
                                              Long matchedProjectAliasId,
                                              String matchedProjectAliasText,
                                              String matchedProjectAliasSource) {

    public DccProjectCodeRecognitionResult(Long projectCodeId,
                                           DccProjectCodeRecognitionMatchType matchType,
                                           String matchText) {
        this(projectCodeId, matchType, matchText, null, null, null);
    }

    public boolean hasAliasEvidence() {
        return matchedProjectAliasId != null;
    }
}
