package cn.iocoder.yudao.module.showroom.content.model;

import java.util.Map;

public record ShowroomAwardRevision(Long awardId, Long revisionId, int revisionNo, String status,
                                    String awardCode, String nameCn, String nameEn, boolean incomplete,
                                    Map<String, String> fields) {
}
