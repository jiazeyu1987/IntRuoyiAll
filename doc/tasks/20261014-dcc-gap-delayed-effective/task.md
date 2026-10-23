# G01 延迟生效确认流程

## Goal

延迟申请上传时不要求具体生效时间。文控签名及本轮必要培训完成后生成原编制人的延迟生效确认任务；原编制人选择不早于本次确认的时间并完成电子签名，才能完成任务并进入后续受控处理。立即生效继续取真实文控签名时间。覆盖新上传、草稿恢复、升版及驳回重提。

## Current Status

blocked — G01/G02软件范围已Root M08正式联合接收：23reactor成功、八类55/55PASS、0失败/错误/跳过；Root T03完整pnpm ts:check也已exit0、无诊断，旧类型错误关闭。独立冻结50文件无漂移。任务记录与只读cleanup preview通过、未删除文件；工程Git提交/推送当轮明确禁止，按docs/task-closeout-rules.md不标completed。blocked仅为工程收尾授权边界，不是业务或类型失败；main24/DetailApplicationPanel仍G03/G06，全项目最终冻结验收由Root推进。

## Milestones

1. 核对 int_qms、规则、最新需求和文件归属，建立记录（已完成）。
2. 独立追踪现行上传/草稿/升版/驳回、BPM、培训、电子签名及日期门禁（已完成静态复核，非运行验收）。
3. 提交最小端到端合同、字段与正式定义方案、精确文件/锚点及真实 RED 计划（已交付待 review）。
4. 已按逐文件授权完成生产RED/GREEN/定向回归，Root M08正式联合八类55/55通过并接收软件范围；不以private旧依赖class冒正式结果。
5. main24/独立50文件及正式验收证据已交Root/G02；cleanup preview23 keep/168临时delete候选/0warning、删除0。工程Git收尾未授权，最终blocked、不completed。

## Expected Verification

- 本阶段：当前源码与相邻测试独立复核；允许运行独立 Node/Python 隔离验证；文档 UTF-8/结构/路径/hash 校验。
- 实施阶段：生产行为 RED/GREEN（真实页面模块或 Java 服务调用），正式 BPM 模型执行、签名/权限/轮次、事务回滚及幂等回归；Maven 仅凭 Root 唯一票执行。
- 未授权：E2E、服务、真实业务库、远程、worktree、Git 提交/推送、分支切换、子 Agent。

## BDD

1. Given 延迟新申请未选择生效时间；When 上传提交；Then 保留 DELAYED 与空时间，正常进入主管节点。
2. Given 文控及本轮必要培训已完成；When 原编制人选择未来时间并电子签名；Then 延迟确认完成且证据绑定精确文件/申请/轮次/任务。
3. Given 时间早于本次确认、未选时间、无签名、错误用户中任一情况；When 尝试完成确认；Then 明确拒绝，零签名写入、零推进、零受控。
4. Given 无需培训的延迟申请文控审核通过；When 流程继续；Then 直接生成原编制人的延迟确认任务，不进入已受控/已生效。
5. Given 需要培训的延迟申请；When 文控签名后培训尚未完成/本轮培训完成；Then 分别保持培训门禁/生成延迟确认任务，不能采用其它申请或轮次记录。
6. Given 立即生效申请；When 文控真实签名时间 S 与后续系统时间 T 不同；Then 生效事实使用 S；培训等既有门禁不被绕过。
7. Given DELAYED 草稿/升版/驳回重提；When 保存恢复或重新送审；Then 生效方式保存且上传阶段没有具体时间要求，新轮次不能借旧确认完成。
8. Given 本轮确认已经提交；When 相同载荷重放/更改时间重放/后续处理失败重试；Then 返回既有证据且不改签名时间/冲突拒绝/失败事务保持可重试且无半成功。

## 设计约束检查

- 权威需求：doc/tasks/20261014-dcc-gap-fix-coordination/requirements.md；本轮授权：coordination.md 与本聊天最新分派，覆盖历史 owner 中明确指定文件。
- 上传人=原编制人，文件负责人不能替代；账号/组织/角色/权限/电子签名资格均保持正式校验。
- 租户、具体版本、申请、轮次、真实活动 BPM task 是完整身份；不得根据显示文案或旧签名推断。
- 必须正式模型承载任务，不能只加前端按钮/本地状态/API 平行旁路。
- 精确提示：文件生效时间由编制人根据文件输入、输出及来源之间的衔接关系确定。
- C02 精度修正：用户所选 E 为秒精度，真实签名 S 保留正式服务原精度；最终 E>=真实 S，不能截断/重写 S。资格为原 requester + 启用/租户/精确 task-round/完整前序 + 正式授权/密码/图片/证据，不额外要求文控岗位或批准角色。
- 立即取文控真实签名；延迟受控时间的最终消费属于 G02，双方先冻结签名合同。
- 禁止 fallback、吞异常、模拟成功、默认原因、修改历史定义/历史申请。
- 前述共享文件已按A04/A06/A07等转交实施，A23再次归还Root→G03/G06；当前不再写main24/DetailApplicationPanel/Query/Lifecycle/Finalization/Token/G05指派文件。后续必要复修由Root重新归属。
- 禁止操作并行脏改动；只写当前明确G01的生产及自有任务资产。initial ABSENT、source-baseline/a06/a07记录与main-handback均保留，不把归还前hash冒后续owner源码。

## Cleanup Keep

- doc/tasks/20261014-dcc-gap-delayed-effective/task.md
- doc/tasks/20261014-dcc-gap-delayed-effective/execution-log.md
- doc/tasks/20261014-dcc-gap-delayed-effective/review-ready.md
- doc/tasks/20261014-dcc-gap-delayed-effective/changed-files.md
- doc/tasks/20261014-dcc-gap-delayed-effective/integration-notes.md
- doc/tasks/20261014-dcc-gap-delayed-effective/verification-report.md
- doc/tasks/20261014-dcc-gap-delayed-effective/design.md
- doc/tasks/20261014-dcc-gap-delayed-effective/contract.md
- doc/tasks/20261014-dcc-gap-delayed-effective/source-baseline.json
- doc/tasks/20261014-dcc-gap-delayed-effective/delayed-upload-red.cjs
- doc/tasks/20261014-dcc-gap-delayed-effective/verify-spec.cjs
- doc/tasks/20261014-dcc-gap-delayed-effective/run-private-java.cjs
- doc/tasks/20261014-dcc-gap-delayed-effective/PrivateJUnit.java
- doc/tasks/20261014-dcc-gap-delayed-effective/SelectionProbe.java
- doc/tasks/20261014-dcc-gap-delayed-effective/a06-write-baseline.json
- doc/tasks/20261014-dcc-gap-delayed-effective/a07-backend-write-baseline.json
- doc/tasks/20261014-dcc-gap-delayed-effective/main-handback.json
- doc/tasks/20261014-dcc-gap-delayed-effective/current-source-checkpoint.json
- doc/tasks/20261014-dcc-gap-delayed-effective/a30-write-baseline.json
- doc/tasks/20261014-dcc-gap-delayed-effective/independent-handback.json
- doc/tasks/20261014-dcc-gap-delayed-effective/a31-write-baseline.json
- doc/tasks/20261014-dcc-gap-delayed-effective/A31ModelRootProbe.java
- doc/tasks/20261014-dcc-gap-delayed-effective/official-m08-receipt.json
- doc/tasks/20261014-dcc-gap-delayed-effective/cleanup-preview-summary.json
