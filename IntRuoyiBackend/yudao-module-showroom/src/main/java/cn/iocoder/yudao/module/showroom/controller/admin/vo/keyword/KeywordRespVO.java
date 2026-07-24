package cn.iocoder.yudao.module.showroom.controller.admin.vo.keyword;

import java.time.LocalDateTime;

public record KeywordRespVO(Long id, String nameZh, String nameEn, LocalDateTime createTime,
                            LocalDateTime updateTime) {

    public Long getId() {
        return id;
    }

    public String getNameZh() {
        return nameZh;
    }

    public String getNameEn() {
        return nameEn;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
}
