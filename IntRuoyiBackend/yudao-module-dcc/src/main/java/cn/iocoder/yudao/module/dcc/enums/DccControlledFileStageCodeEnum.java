package cn.iocoder.yudao.module.dcc.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DccControlledFileStageCodeEnum implements ArrayValuable<String> {

    APPLICANT_REWORK("APPLICANT_REWORK", "Applicant rework"),
    DOC_CONTROL_REVIEW("DOC_CONTROL_REVIEW", "Document control review"),
    MATRIX_REVIEW("MATRIX_REVIEW", "Matrix review"),
    MATRIX_APPROVAL("MATRIX_APPROVAL", "Matrix approval"),
    DOC_CONTROL_APPROVAL("DOC_CONTROL_APPROVAL", "Document control approval");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(DccControlledFileStageCodeEnum::getCode)
            .toArray(String[]::new);

    private final String code;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
