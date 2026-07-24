package cn.iocoder.yudao.module.showroom.controller.admin.vo.keyword;

import java.time.LocalDateTime;

public record KeywordPageRespVO(Long id, String nameZh, String nameEn, LocalDateTime updateTime) {

    public Long getId() {
        return id;
    }

    public String getNameZh() {
        return nameZh;
    }

    public String getNameEn() {
        return nameEn;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
}
