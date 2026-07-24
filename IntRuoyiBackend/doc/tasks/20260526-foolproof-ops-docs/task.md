# 任务：傻瓜式运维十项能力开发文档与放行评审

## 任务目标

- 在新的后端、前端成对 worktree 中，为当前系统距离“公司 IT 可傻瓜式运维”的 10 项缺口编写实现前开发文档。
- 第一项告警方案按用户明确要求改为“站内信告警”，不接外部 webhook；系统每天登录，站内信作为第一阶段通知渠道。
- 启动多个子 agent 分别编写分片设计文档，主 agent 只作为 reviewer 汇总审查，不替子 agent 隐式补设计。
- reviewer 放行标准：
  - 根据子 agent 开发的文档可以实现 10 项目标，且不引入发布、备份、恢复、权限、租户、数据安全或告警副作用。
  - 文档按 BDD + 严格 TDD + Subagent-Driven 形式组织。
  - 文档逻辑自洽、接口清晰，worker 可按文档先写 RED 测试再实现。

## Worktree

- 后端 worktree：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-foolproof-ops-docs\ruoyi-vue-pro`
- 前端 worktree：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-foolproof-ops-docs\yudao-ui-admin-vue3`
- 分支名：`task/20260526-foolproof-ops-docs`

## 十项能力范围

1. 站内信告警闭环。
2. 责任人、审批人、升级路径和操作确认矩阵。
3. 运行控制台决策向导。
4. 回滚镜像标签和恢复备份点自动候选选择。
5. 发布前检查、发布后观察和一键巡检。
6. 业务健康监控面板。
7. backend/frontend/website 外部探针或健康检查闭环。
8. 日志轮转、磁盘容量和日志增长防爆。
9. 备份、恢复演练和可恢复点状态可视化。
10. 事故记录、处置动作、验证结果和复盘闭环。

## BDD 场景

- BDD: 站内信告警闭环 -> Given 备份失败、演练失败、磁盘容量超阈值或核心探针失败, When 运维监控任务识别异常, Then 系统必须向配置的站内信接收人发送可追溯告警，记录发送结果，不能静默标记成功。
- BDD: 傻瓜式操作向导 -> Given 公司 IT 打开运行控制台且不理解底层脚本, When 选择“系统坏了”“数据坏了”“发布前检查”等场景, Then 页面按规则推荐操作、阻断危险路径，并展示所需责任人和验证证据。
- BDD: 可恢复候选受控 -> Given 系统存在多个镜像标签和备份点, When IT 进行应用回滚或数据恢复, Then 只能选择已校验、已演练且与当前环境匹配的候选，不能手填未知值绕过校验。
- BDD: 巡检和业务健康可见 -> Given 测试服或正式服运行中, When 运维人员执行一键巡检或查看面板, Then 系统展示应用、数据库、Redis、文件对象、ERP/MES、任务日志、API 错误、磁盘和备份演练的红黄绿结论。
- BDD: 事故闭环可审计 -> Given 任一高危操作、告警或恢复动作发生, When 操作完成或失败, Then 系统必须形成事故/操作记录，包含原因、责任人、日志、验证结果、剩余风险和复盘状态。

## 里程碑

- [x] M1：检查主仓与服务仓状态，创建成对 worktree。
- [x] M2：建立主控任务文档、执行日志和监督式交付骨架。
- [x] M3：启动子 agent 分片编写设计文档。
- [x] M4：主 agent 汇总子 agent 文档并执行 reviewer 放行评审。
- [x] M5：运行文档结构、UTF-8、关键文本和收尾预览验证。
- [x] M6：按验证结果标记完成或阻塞。

## 预期验证

- 子 agent 输出必须落盘到 `subagent-output/`，不能只在聊天中描述。
- `review-report.md` 必须包含 `logic_status`、`bdd_tdd_status`、`interface_status`、`side_effect_status`、`blocking_issues`、`required_changes`、`final_decision`。
- 所有中文 Markdown 文件必须能用 UTF-8 正常读取。
- 使用文本检查确认每份设计文档包含 BDD、RED、GREEN、REGRESSION、Subagent 分工、接口契约和副作用控制。
- 运行 task-closeout-cleanup preview；本任务的正式设计文档和评审报告应保留。

## 当前状态

- 状态：completed
- 已完成：已建立成对 worktree；已建立本任务主控文档；已明确站内信替代 webhook 作为第一阶段告警渠道；已定义 reviewer 三条放行标准；已完成子 agent 分片文档；已完成 reviewer 条件放行报告。
- 当前结论：文档任务完成，可作为后续实现规划输入放行；实现前必须按 `review-report.md` 完成 RC-01 到 RC-03。
- 最终验证：PASS。UTF-8、关键标记、review 字段和 task-closeout-cleanup preview 均通过。
- 阻塞：暂无。

## Cleanup Keep

- `doc/tasks/20260526-foolproof-ops-docs/request-analysis.md`
- `doc/tasks/20260526-foolproof-ops-docs/prd.md`
- `doc/tasks/20260526-foolproof-ops-docs/dev-plan.md`
- `doc/tasks/20260526-foolproof-ops-docs/test-plan.md`
- `doc/tasks/20260526-foolproof-ops-docs/test-report.md`
- `doc/tasks/20260526-foolproof-ops-docs/review-report.md`
- `doc/tasks/20260526-foolproof-ops-docs/task-state.json`
- `doc/tasks/20260526-foolproof-ops-docs/subagent-output/backend-runtime-control-contract.md`
- `doc/tasks/20260526-foolproof-ops-docs/subagent-output/observability-site-message-alerts.md`
- `doc/tasks/20260526-foolproof-ops-docs/subagent-output/bdd-tdd-subagent-plan.md`
