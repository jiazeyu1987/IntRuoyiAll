package cn.iocoder.yudao.module.srm.enums.tender;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SrmTenderSubmissionStatusEnum {

    SUBMITTED("SUBMITTED", "已投标");

    private final String status;
    private final String label;
}
