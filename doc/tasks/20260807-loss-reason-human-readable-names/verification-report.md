# Verification Report

## Result

PASS。已通过本机真实页面将 `球囊扩张导管` 23 个工序中的 63 条 `RLR0807M-*` 占位描述全部改为与工序匹配的中文损耗原因；最终占位描述数量为 `0`。

## Scope

- 环境：本机 `int_main`，前端 `http://127.0.0.1:8081`，后端 `http://127.0.0.1:48081`。
- 身份：`芋道源码/admin`。
- 路线：`球囊扩张导管`，`routeId=900025`。
- 工序：`23` 个。
- 最终原因：`65` 条，其中本任务修改 `63` 条，保留非目标“黑点”`2` 条。

## BDD/TDD Evidence

- RED：`node ..\\doc\\tasks\\20260807-loss-reason-human-readable-names\\loss-reason-human-readable.e2e.mjs red` -> FAIL（预期），`仍有 63 条 RLR0807M 占位描述`。
- PLAN：同脚本 `plan` -> PASS，初始 23 个工序、63 条显式中文映射、MES 写请求 `0`。
- GREEN APPLY：同脚本 `apply` -> PASS；断点前完成 `15` 条，恢复运行完成 `48` 条，合计 `63` 条。最终 `placeholderCount=0`，本轮正式 PUT `48` 条，均匹配清单 ID、业务码 `0`、目标描述和原启用状态。
- GREEN VERIFY：同脚本 `verify` -> PASS；新浏览器会话 `routeProcessCount=23`、`routeReasonCount=65`、`changedCount=63`、`placeholderCount=0`、`mesWriteCount=0`。

## Data Integrity

- 63 条目标记录的 ID 集合、`reasonCode`、`routeProcessId` 和 `enabled` 与变更前快照一致。
- 所有目标记录的 `reasonName` 精确等于变更清单中的中文目标值。
- 非目标记录保持不变：初始 `ID=15 / LOSS-926785-001 / 黑点`；并发新增 `ID=566 / LOSS-926786-003 / 黑点` 已纳入保持快照。
- 路线原因总数由并发新增造成 `64 -> 65`；本任务未新增、删除、停用原因，也未修改 schema 或直接执行 SQL。
- 最终目标 page error `0`、console error `0`、目标网络失败 `0`；独立只读核验 MES 写请求 `0`。

## Human-Readable Examples

- 吹球囊成型：`球囊成型不良`。
- 球囊裁剪：`裁剪尺寸超差`、`裁剪切口毛刺`。
- 外管与球囊焊接：`焊接不牢`、`焊接偏位`、`焊口开裂`、`过度熔融`、`焊接处焦黄`。
- 穿显影环：`显影环漏装`、`显影环错装`、`显影环位置偏移`、`管材穿伤`、`显影环变形`。
- 纸塑袋封口（包装）：`封口不牢`、`封口褶皱`、`封口宽度超差`、`包装袋破损`、`标签错误`。

完整逐记录映射已在验证阶段由 `change-manifest.json` 校验；验证结果写入本报告后，临时清单已按清理规则移除。

## Visual Evidence

- 收尾前已检查 `final-process-config.png`：1600x900 真实页面截图中，工序配置表前 12 道工序均显示中文损耗描述；结果已记录后按任务清理规则移除临时截图。
- 截图同时显示与本任务无关的既有提示：`team-device/list` 请求地址不存在。该提示不来自损耗修改或列表接口，本任务未隐藏或扩大范围修复。

## Residual Risk

- 当前共享 `int_main` 仍存在 `team-device/list` 独立运行态错误，会在进入生产组长页面时显示提示；不影响本次 63 条损耗描述的数据结果，但需要另行任务处理设备列表接口契约。

## Closeout

- 经验沉淀结构核验通过：断点恢复门禁已写入 `docs/e2e-rules.md`，索引已写入 `docs/experience-index.md`。
- `task-closeout-cleanup` preview/apply 均通过；8 个任务临时产物已删除，仅保留 3 份核心任务记录，无阻塞和警告。
- 最终状态：`completed`。
