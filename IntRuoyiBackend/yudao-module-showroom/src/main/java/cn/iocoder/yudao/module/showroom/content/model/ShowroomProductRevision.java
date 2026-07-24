package cn.iocoder.yudao.module.showroom.content.model;

import java.util.List;
import java.util.Map;

public record ShowroomProductRevision(Long productId, Long revisionId, int revisionNo, String status,
                                      String nameCn, String nameEn, boolean incomplete,
                                      Map<String, String> fields, List<ShowroomProductAttachment> attachments) {

    public ShowroomProductRevision(Long productId, Long revisionId, int revisionNo, String status,
                                   String nameCn, String nameEn, boolean incomplete,
                                   Map<String, String> fields) {
        this(productId, revisionId, revisionNo, status, nameCn, nameEn, incomplete, fields, List.of());
    }
}
