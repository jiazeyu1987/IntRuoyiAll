# DCC 源文件治理阶段验收报告

## 验收结论

NO-GO。代码和测试合同已完成 T3、T4、T5，并通过 T6 Pass 7 独立复测；授权全租户批次后的事务/范围/状态纠正已完成。全量历史治理仍被大量 blocker 阻塞，不能进入 Revision/Iteration 创建或回填。

## 已通过

- 13 个 DCC 定向测试类最新 `81 tests, 0 failures/errors`（T6 Pass 7 独立复测）。
- 旧 `/source-ownership-migration/run` 已明确拒绝，不再绕过确认清单。
- 执行要求固定 rule/schema 版本、`CONFIRMED` 批次和 manifest/request 摘要。
- 共享组执行冻结全局引用集合，三条记录分别生成独立副本；任一失败清理本组新副本并抛错；批大小不能拆分共享组。
- `COPY_SHARED_SOURCE` 即使只剩一条清单明细也不会降级为单条迁移，必须经过组级冻结引用完整性检查。
- postflight 检查 source 指针、ownership、SHA-256、完成后共享 source 和历史证据摘要漂移。
- database schema validator、backend API validator 和 `git diff --check` 通过。
- 测试库只读盘点使用 `START TRANSACTION READ ONLY`，最终 `ROLLBACK`；治理 smoke 写入均为任务专属数据，验证后清理。
- 已授权的测试维护窗口验证：additive schema 三表部署成功；真实 `CLAIM_SOURCE` 首次完成、postflight 1/1、同摘要重试 0 条；真实 `COPY_SHARED_SOURCE` 三条完成、postflight 3/3、三条物理对象 key 唯一且 SHA-256 一致；清单篡改拒绝；唯一键冲突事务回滚。
- 备份恢复验证：11 张相关表恢复到隔离库，业务表 checksum 一致，访问日志历史行缺失/变更为 0，仅追加本次拒绝审计；隔离库与 smoke 数据已清理。

## 真实只读盘点证据

- 数据库：`ruoyi-vue-pro`，最终冻结时间 2026-09-05 22:37:00 +08:00。
- 最大 Master ID：`2054545668044062921`；最大 controlled file ID：`2054545668044070318`。
- 有效 Master/file/source 引用：18,222 / 18,072 / 18,072。
- ownership 缺失：18,065；软删除 source：7；全局共享源 42 组/290 条；跨租户共享 2 组/26 条。
- 正式指针漂移 16；平台 ACTIVE 漂移 17,864；路线快照孤儿 24；访问日志孤儿 685。
- 当前确定性 AUTO_MAP：0。
- 新治理表存在数：3；授权批次审计记录已保留，tenant 1 已处理 17,607 条，tenant 122 已处理 465 条。
- 详细报告：[windchill-inventory-report.md](windchill-inventory-report.md)。

## 未通过或阻塞

- AC-11：小批真实 MySQL 事务、对象复制、唯一索引冲突回滚和对象删除已验证；全量治理仍未执行。
- AC-15：小批恢复快照和访问日志 append-only 对账通过；全量历史关联前后快照尚未建立。
- AC-18/19/20：治理表已部署且同一查询版本重新冻结，AUTO_MAP=0；18,065 ownership blocker、17,864 平台 ACTIVE 漂移等非零问题使全量门禁仍不满足。

## 后续放行条件

1. 先处理身份、分类叶子、平台 ACTIVE 和 source ownership blocker，不能对 18,065 条记录猜测回填。
2. 每个维护窗口重新备份，并生成带 rule/schema/manifest/request 摘要的有游标清单。
3. 对清单 READY 项按完整共享组执行，持续运行真实对象和事务 postflight。
4. 对非 source 历史关联建立全量前后快照对账。
5. 使用同一查询版本和冻结边界重跑 Windchill 盘点；只有 AUTO_MAP、blocker 和历史证据均可解释且门禁满足时，才评估 Revision/Iteration 回填。

## Git/收尾限制

- 未执行 Git 提交、推送或远程发布，保留现有并行工作区改动。
- 任务不能标记 `completed`；当前状态保持 `in_progress`，因为全量历史治理和最终门禁仍未通过。

## 授权全量批次执行补充（2026-09-05 22:37）

- 已在本机测试库按租户和受控文件 ID 游标执行真实 prepare/confirm/execute；未执行远程服务器、E2E 或 Revision/Iteration 写入。
- tenant 1：有效 source 引用 17,607 条，生成 90 个批次（含 1 个空尾批次），17,600 条明确 `BLOCKED`，7 条 `COMPLETED`；7 条完成项 postflight 为 7/7，无发现。
- tenant 122：有效 source 引用 465 条，生成 3 个批次，465 条明确 `BLOCKED`，0 条 `COMPLETED`，0 条 `FAILED`；游标尾部剩余候选为 0。
- 授权批次执行没有改变原有 blocker 结论：最新只读盘点仍为 ownership/hash blocker 18,065、AUTO_MAP=0、平台 ACTIVE 漂移 17,864、路线快照孤儿 24、访问日志孤儿 685。新增 7 条完成项对应的 ownership/hash 原本已有效，因此盘点总 blocker 未下降。
- 治理批次、明细和全局 claim 审计记录保留；本次真实执行不是“全历史已治理”的证明。全量关联前后快照、身份/分类/平台状态治理和剩余 blocker 解释仍未完成，阶段继续 `NO-GO`。

## 事务与范围纠正补充（2026-09-05 23:22）

- 冻结租户范围现在同时校验 `tenant_scope_json`、其 SHA-256 和 item tenant；执行服务不能通过伪造调用参数扩大范围。真实篡改 scope hash 返回 `1080000309`，任务专属 smoke 已清理。
- 批次编排不再用一个外层事务包围多个共享组；每组独立原子提交，后续组失败不会回滚已完成组并留下对象存储孤儿。
- 共享组技术失败先回滚，再用独立事务把组内明细写为 `FAILED`；所有副本清理都会尝试，单个清理失败不会中断后续清理。
- `COPY_VERIFIED` 重试按 migration 的旧源/副本/状态/hash 恢复来源关系并重读副本；状态或摘要不一致明确 BLOCKED。已有 ownership 的共享源保持原行并转人工复核，不隐式改写。
- global claim、ownership、migration、batch、item 的关键 insert/update 均要求 exactly-one；0 行写入立即失败。调用范围必须与冻结范围完全相等；共享组业务证据问题在回滚后仍按稳定原因记为 BLOCKED，技术异常才记 FAILED。最终定向回归 81 tests 全部通过，三个 evidence validator 与 diff check 通过。

## 运行环境恢复记录

- 81-test 版本第一次标准重建被无关并行 MES 文件 `MesProcessPoolTeamLeaderController.java:1293` 的语法错误阻断；本任务未修改该文件。标准脚本失败时已停止 48081，因此先用上一验证包明确回滚恢复服务。
- 并行任务随后修复该文件，MES 单模块编译通过；再次运行标准脚本后，30 模块 package 成功并启动 `backend-runtime-control-20260906-000036.jar`，健康检查为 UP。
- 最新运行包执行完成批次幂等重试为 processed=0、completed=7，postflight 7/7 无发现；2026-09-06 00:02:45 同口径只读盘点指标不变。
- 00:17 并行任务再次标准重启到 `backend-runtime-control-20260906-001702.jar`；进程切换后健康检查恢复 UP，同一批次再次验证 processed=0、completed=7、postflight 7/7。

## 最终独立门禁

- T6 Pass 7 独立报告已覆盖 81-test 事务、冻结范围和共享组 BLOCKED 状态修正，并复核最新运行态与只读盘点。
- 主 Agent acceptance 与独立 tester 结论一致：T6/M4 完成，任务进入 `ready_for_closeout`。
- 全量业务治理仍为 `NO-GO`；M5 仅负责任务产物清理和状态收尾，不改变历史 blocker，也不启动 Revision/Iteration。
