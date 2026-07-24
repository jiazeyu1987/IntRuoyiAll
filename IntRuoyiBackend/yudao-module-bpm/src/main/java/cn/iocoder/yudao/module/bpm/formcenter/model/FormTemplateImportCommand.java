package cn.iocoder.yudao.module.bpm.formcenter.model;

import java.util.Arrays;

public class FormTemplateImportCommand {

    private final String templateName;
    private final String versionNo;
    private final String sourceFileName;
    private final byte[] sourceBytes;
    private final String remark;

    private FormTemplateImportCommand(String templateName, String versionNo, String sourceFileName,
            byte[] sourceBytes, String remark) {
        this.templateName = templateName;
        this.versionNo = versionNo;
        this.sourceFileName = sourceFileName;
        this.sourceBytes = Arrays.copyOf(sourceBytes, sourceBytes.length);
        this.remark = remark;
    }

    public static FormTemplateImportCommand of(String templateName, String versionNo, String sourceFileName,
            byte[] sourceBytes, String remark) {
        return new FormTemplateImportCommand(templateName, versionNo, sourceFileName, sourceBytes, remark);
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getVersionNo() {
        return versionNo;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public byte[] getSourceBytes() {
        return Arrays.copyOf(sourceBytes, sourceBytes.length);
    }

    public String getRemark() {
        return remark;
    }

}
