package cn.iocoder.yudao.module.showroom.content.model;

import java.util.Optional;

public record ShowroomAwardSnapshot(Long awardId, String awardCode, Optional<Long> currentRevisionId,
                                    boolean incomplete, boolean live) {
}
