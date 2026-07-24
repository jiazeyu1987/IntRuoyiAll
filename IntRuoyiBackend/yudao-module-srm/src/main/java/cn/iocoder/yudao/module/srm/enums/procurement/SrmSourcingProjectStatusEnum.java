package cn.iocoder.yudao.module.srm.enums.procurement;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmSourcingProjectStatusEnum {

    DRAFT("DRAFT", "草稿"),
    PUBLISHED("PUBLISHED", "已发布"),
    COMMITTEE_CONFIRMED("COMMITTEE_CONFIRMED", "评委会已确认"),
    CANDIDATE_CONFIRMED("CANDIDATE_CONFIRMED", "候选已确认"),
    WINNING_CONFIRMED("WINNING_CONFIRMED", "中标已确认"),
    DEAL_CONFIRMED("DEAL_CONFIRMED", "已成交"),
    CONTRACT_CREATED("CONTRACT_CREATED", "已建合同");

    private final String status;
    private final String label;

    public static String getLabel(String status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .map(SrmSourcingProjectStatusEnum::getLabel)
                .findFirst()
                .orElse(status);
    }
}
