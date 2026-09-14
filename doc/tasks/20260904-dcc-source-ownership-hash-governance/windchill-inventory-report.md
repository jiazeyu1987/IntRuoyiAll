# DCC Windchill 迁移只读盘点报告

## 冻结边界

- 环境：本机测试库容器 `int-ruoyi-mysql`，数据库 `ruoyi-vue-pro`。
- 事务：`@@transaction_read_only = 1`，以 `START TRANSACTION READ ONLY` 执行，最后 `ROLLBACK`。
- 盘点时间：2026-09-06 00:02:45 +08:00（81-test 最新运行包部署、幂等 postflight 后）。
- `MAX(dcc_controlled_file_master.id)`：`2054545668044062921`。
- `MAX(dcc_controlled_file.id)`：`2054545668044070318`。
- 代码基线：`329799964e29104f6599491ef504fd71128a56f4`，工作区包含本任务未提交改动。
- 查询文件：`windchill-readonly-inventory.sql`。
- 查询 SHA-256：`24DE996FE70F4ADDE6502A7473BD473038B99BF2063FBFC7AE6CAFBB8042EAB4`。

## 汇总结果

| 检查项 | 结果 |
|---|---:|
| 有效 Master | 18,222 |
| 有效 controlled file | 18,072 |
| 有效 source 引用 | 18,072 |
| I-01 身份缺失或混合 Master | 17,874 |
| I-02 分类无效或非叶子文件 | 17,917 |
| I-03 目标身份跨 Master 重复组 | 0 |
| I-04 正式指针漂移 Master | 16 |
| I-05 非法或重复版本组 | 11 |
| I-06 非法检出 Master | 4 |
| I-07 ownership/hash 阻塞文件 | 18,065 |
| I-07 全局共享源组/引用 | 42 / 290 |
| I-07 跨租户共享源组/引用 | 2 / 26 |
| I-08 签名孤儿/路线快照孤儿 | 0 / 24 |
| I-09 平台 ACTIVE 漂移 Master | 17,864 |
| I-10 历史关联孤儿 | 685 |
| 新治理表存在数（应为 3） | 3 |
| 当前确定性 AUTO_MAP Master | 0 |

## Source 证据细分

- ownership 缺失：18,065。
- 已有有效 ownership 且 source/hash 对齐：7。
- source 记录物理缺失：0。
- source 记录软删除：7。
- source 定位缺失：0。
- ownership 指针不一致：0。
- 已有 ownership 的 SHA-256 非 64 位：0。

上述分类可能重叠，例如软删除 source 同时可以缺 ownership，不能把细分项直接相加作为总数。

## 历史证据细分

- 签名记录孤儿：0。
- 路线快照孤儿：24。
- 关联文件孤儿：0。
- 分发孤儿：0。
- 培训孤儿：0。
- 打印孤儿：0。
- 访问日志孤儿：685。

本报告只统计关联记录是否仍能定位到冻结范围内的有效 controlled file。治理前后逐记录 hash 对账必须在真实治理执行前后使用同一冻结清单完成；本轮虽已执行 source 治理批次，但治理前未建立覆盖全部关联行的逐记录 hash 清单，因此不能把备份恢复 checksum 或当前 after 盘点替代全量前后快照。

## 与历史数字的关系

- ownership/hash 缺失仍为 18,065；已授权批次对 7 条原本已有有效 ownership 的记录完成全局 claim/迁移审计，未对其余 blocker 做猜测回填。
- 软删除 source 仍为 7，与上一轮一致。
- 本次按 `controlled_file.deleted = 0` 且固定最大 ID 的全局口径得到共享源 42 组；历史记录中的 43 组不能直接复用。本次全量物理行口径为 48 组，说明是否过滤软删除记录会明显改变数字。
- 当前冻结 ID 与上一轮不同，任何数量变化必须以本报告的冻结边界为准。

## 结论

- `AUTO_MAP_CURRENT = 0`，当前没有 Master 同时满足稳定身份、分类叶子、版本链、正式指针、source ownership/hash 和非共享源等确定性条件。
- `GOVERNANCE_SCHEMA_PRESENT = 3`，治理批次、明细和全局 claim 三张表已部署；授权批次审计记录保留在治理表中，当前 tenant 1 已处理 17,607 条（7 条 COMPLETED、17,600 条 BLOCKED），tenant 122 已处理 465 条且全部 BLOCKED，未产生 FAILED。
- 未解决项必须保持 BLOCKED；禁止创建或回填 Revision/Iteration。当前 AUTO_MAP 仍为 0，平台 ACTIVE 漂移、路线快照孤儿、访问日志孤儿等历史 blocker 仍未治理。
- 下一运行态门禁是：先处理身份/分类/平台状态及历史证据 blocker，再按同一查询版本重跑本报告；只有全量历史证据可对账且门禁满足时，才评估 Revision/Iteration。
