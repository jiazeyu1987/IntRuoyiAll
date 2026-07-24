package cn.iocoder.yudao.module.srm.enums.tender;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SrmTenderCandidateStatusEnum {

    CANDIDATE("CANDIDATE", "候选"),
    WINNING("WINNING", "中标");

    private final String status;
    private final String label;
}
