# 20260808 班组长 FIFO 分配数量快捷按钮

## Task Goal

在班组长工序池 FIFO 分配弹框中，将分配数量限制为整数，并在数量输入后增加“最大”和“一半”两个快捷按钮：

- “最大”：按当前订单可分配数量与当前剩余数量计算最大可分配整数数量。
- “一半”：按当前订单全部数量的一半与当前剩余数量计算最大可分配整数数量。

## Milestones

- [x] 定位 FIFO 分配弹框、数量输入和保存校验逻辑。
- [x] 先补静态合同，覆盖整数输入、按钮文案、稳定锚点和点击行为。
- [x] 实现整数数量输入和快捷分配逻辑。
- [x] 运行目标静态合同、类型检查或记录阻塞，完成回归核对。
- [x] 执行真实页面只读 E2E，验证“最大 / 一半”按钮在分配弹框中填充正整数且不触发确认分配写请求。

## Expected Verification

- 目标静态合同先 RED 后 GREEN。
- 相关前端静态合同通过。
- `pnpm ts:check` 如可运行则通过；若存在非当前任务阻塞，记录首个阻塞和影响。
- `git diff --check` 通过。
- 真实 Playwright 只读 E2E 使用本机 `8081/48081`，通过页面筛选真实待复核报工，点击按钮验证正整数，不提交分配确认。

## Applicable Gates

- 前端按钮文案与行为一致性门禁：按钮必须有稳定 `data-*` 锚点、可见文案、正式点击处理器和静态合同。
- 复合输入控件交互保留门禁：新增按钮不得替代或破坏原数量输入控件的正式输入职责。
- PowerShell/UTF-8 门禁：任务文档和中文内容使用 UTF-8，命令不使用 `&&`。
- No-fallback 门禁：不引入默认成功、吞异常、兼容降级或 mock 数据。

## Current Status

completed

真实页面只读 E2E 已通过，cleanup preview/apply 已完成且保留 E2E 证据文件。

## Cleanup Keep

- doc/tasks/20260808-team-leader-fifo-allocation-buttons/e2e-allocation-shortcuts-readonly.cjs
- doc/tasks/20260808-team-leader-fifo-allocation-buttons/e2e-allocation-shortcuts-readonly-result.json
- doc/tasks/20260808-team-leader-fifo-allocation-buttons/e2e-allocation-shortcuts-readonly.png

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接在 FIFO 分配弹框的正式输入与分配逻辑中实现。
- `是否存在临时补丁或绕过`：否。
