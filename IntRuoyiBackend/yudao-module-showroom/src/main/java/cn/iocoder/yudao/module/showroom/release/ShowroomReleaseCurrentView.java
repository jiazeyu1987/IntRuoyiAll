package cn.iocoder.yudao.module.showroom.release;

import java.time.Instant;

record ShowroomReleaseCurrentView(ShowroomReleaseCurrentPayload payload, String etag, Instant publishedAt) {
}
