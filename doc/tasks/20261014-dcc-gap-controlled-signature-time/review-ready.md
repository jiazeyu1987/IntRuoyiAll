# G02 软件范围正式通过并冻结交还

**最新正式联合 M08：23 reactor BUILD SUCCESS，八类55/55 PASS、0F/0E/0skip，G01/G02软件范围Root联合接收。** G02两类仍10+11=21项，四源码0漂移、冻结交还保持。最新证据 m08-formal-joint-evidence.json / g02-software-handback.json；八份Root归档XML hash及原文件mtime均已逐项核对。本轮全仓有3项其它source漂移，不能称最终全工程freeze，也不覆盖custom/附件/真实ports/E2E。

Root 已接收本轮授权软件范围。正式 M07 中 G02 两类 **21/21 PASS、0F/0E/0skip**：DccG02ControlledSignatureTimeTransactionTest 10项，DccG02DelayedFinalizationTransactionTest 11项。fresh XML 修改时间已核在本轮真实M07起止窗口内，摘要/hash/方法清单见 m07-formal-g02-evidence.json；Root稳定归档在协调任务 m07-junit/。

## 已交付

- 延迟 controlledTime 消费 G01 唯一确认 Evidence 的权威微秒 S，publishedTime/处理事件仍 T，所选 E 秒精度独立；同日未来精确到期、未签/伪造/错轮次拒绝、重放不改 S。
- 原编制人公开 ConfirmationService.confirm → 官方 MICROS/scoped intent producer → 统一签名行 → DCC HMAC projection → CAS → 真实 GxP → BpmTaskServiceImpl.approveTask → 官方 Flowable End → 盖章/受控。
- 同步 End 晚期真实 GxP/控制失败回滚统一/DCC/确认/账本/任务，重试生成新 S；预先已提交确认的内域恢复保留原 S；立即方式沿原文控签名语义。
- 必要培训的实际 receive 历史、本版本/本轮原上传人附件与摘要消费；错轮次培训拒绝。此项不冒称培训上传入口验收。

只修改授权 LifecycleService、FinalizationServiceImpl 和两专属测试；G01共同 Evidence/Rule/Bound/Date/Signature/DDL 未并写。精确 G02-only 生产 diff 以改前SHA与Root baseline完全一致验证后保存 a13-production.diff。

## 文件归还与冻结

 g02-software-handback.json 登记四个当前 raw SHA-256、M07 21项及范围。**四源码已冻结交还 Root，G02 不再写入或重复 private14/公开2/guard3/training2；后续修改须 Root 重新分派。** Root 后续最终freeze/必要联合可统一重跑。

## 证据与限制

历史M07整票exit1：signature旧11PASS、DCC44中43PASS/1ERROR，唯一V6图测试Backend/Backend/config路径错误已由G01修，正式M08同八类全过关闭该错误。M07原报告保留，不覆盖历史；当前四G02源与两轮正式执行快照一致。M08八类通过仍不能称全部12项或运行验收完成。

账号/密码及签名图片目录、物理存储、platform/audience/distribution端口仍有明确替身；没有真实服务/E2E/业务库验收。正式受控时间/签名/确认/真实GxP/BPM/End/培训消费软件范围已由当前reactor通过。各私有阶段的失败和SOURCE_DRIFT历史保留在 public-command-verification.md、public-guards-verification.md、public-training-verification.md，不覆盖正式21项结果。

## 尚未实现/验证与工程状态

G02授权软件已实现并正式验证通过；跨模块真实ports/E2E和最终统一freeze按Root整体任务处理。工程Git提交/推送本轮明确未授权，按 task-closeout-rules.md 记工程收尾 blocked，**不是业务故障，也不标 completed**。没有共享文件接入请求、没有待处理本线程验证句柄；不自行跨线程发消息。
