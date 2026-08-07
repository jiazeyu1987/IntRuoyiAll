package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.time.LocalDateTime;

public record MesFrontlineProductionSubmitCandidate(Long eventId,
                                                     LocalDateTime serverSubmitTime) {
}
