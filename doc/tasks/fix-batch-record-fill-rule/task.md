# 修复批记录模板填写规则误报

## Task Goal

修复批次执行中点击“打开填写”时，已在规则配置页确认并保存的批记录模板可填单元格被误判为“未确认填写规则”的问题。自动识别出的规则仍是建议态，必须经用户确认或后续显式历史修复后才允许进入执行快照。

## Milestones

- [x] 建立缺陷复现与预期行为记录
- [x] 定位模板填写规则校验的根因
- [x] 补充 BDD/TDD 根治设计，明确复用现有规则保存、Jimu JSON 和执行校验链路
- [x] 新增保存边界回归用例，覆盖 `source=AUTO && reviewed=true` 的确认态归一化
- [x] 新增保存输出到执行规则识别的组合回归用例
- [x] 实施最小正式修复
- [x] 运行关键 Maven GREEN 验证并记录结果
- [x] 用户授权纳入范围外编译前置：修复 `MesProRouteFlowConfigServiceImpl.resolveRecordbookEnabled(Boolean,String)` 缺失
- [x] 修复损耗报告 Word 解析回归：期望保留 `□报废`
- [x] 获得或发现任务专用测试账号后完成真实前端 E2E
- [ ] 如需修复历史模板，另行授权 dry run/apply 受控修复任务
- [ ] 完成收尾与经验沉淀

## Expected Verification

- 目标测试先复现 `source=AUTO && reviewed=true` 保存后仍被执行层视为未确认的误报。
- 修复后目标测试通过，证明已确认并保存的可填单元格不会误报。
- 自动建议态规则仍保持未确认，打开填写前继续被执行层 fail fast 拦截。
- 运行受影响模块的相关回归验证。
- `docs/experience-index.md` 当前不存在；本任务不涉及发布、远程服务、生产数据或破坏性操作，按低风险缺陷修复继续推进。

## Current Status

ready_for_closeout；用户授权纳入的编译前置、损耗报告 Word 解析回归、真实前端 E2E PASS 证据和后续 134 个后端全类回归均已完成。当前 shell 再次复跑真实 E2E 缺少任务专用环境变量而 fail fast，未使用受保护租户替代；剩余为任务清理、经验沉淀和提交/推送门禁。


## 经验门禁

### 任务专用 E2E 环境变量与证据文件门禁

- Trigger: 运行 `edhr-batch-execution-real-flow.e2e.js` 或复跑真实写入型 E2E。
- Preflight check: 显式设置当前任务 `EDHR_BATCH_E2E_TASK_ID` 或 `EDHR_BATCH_E2E_EVIDENCE_FILE`，并确认账号、工单、批次、路线、签名密码环境变量来自已授权测试租户。
- Blocker: 任一必需环境变量缺失、目标租户受保护、或证据路径会覆盖非当前任务历史 PASS 证据时停止。
- Verification: 记录 E2E 命令、证据路径、入口 URL、测试租户/账号标签、目标业务数据标识和 PASS/BLOCKED 结果。
- Forbidden action: 禁止用默认任务 ID 覆盖历史证据，禁止用受保护租户或默认生产/admin 数据替代任务专用测试数据。
- Evidence: `docs/e2e-rules.md#任务专用-e2e-环境变量与证据文件门禁`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是
- `是否存在临时补丁或绕过`：否
