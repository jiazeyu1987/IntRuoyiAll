# eDHR 顶部栏布局统一

## Task Goal

统一 eDHR 批次详情顶部栏：带批记录切换与不带批记录切换时，`工艺流程` 链接和 `同步状态` 按钮位置保持一致。

## Milestones

- [x] 创建任务记录并确认现有并发脏改边界。
- [ ] 增加静态回归测试复现顶部栏位置不一致。
- [ ] 实施最小模板/样式修复。
- [ ] 运行目标静态测试和类型检查。
- [ ] 更新验证记录并进入 closeout。

## Expected Verification

- 顶部栏静态合同覆盖左侧上下文、中间操作组、右侧版本/载体组。
- `node tests\e2e\edhr-batch-context-carrier-header-static.spec.js` 通过。
- `pnpm ts:check` 通过。

## Current Status

in_progress

## Blockers

- 当前工作区存在大量并发未提交改动；本任务只修改批次详情顶部栏与对应静态合同，不执行全量提交/推送。

## Experience Gate

- 已读取 `docs\experience-index.md` 中静态合同/E2E 相关索引，适用 `docs\e2e-rules.md#静态合同与真实-e2e-同步门禁`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，统一顶部栏结构而非按某个截图写偏移。
- `是否存在临时补丁或绕过`：否。