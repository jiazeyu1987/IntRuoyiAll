# 活跃订单分配下拉重叠修复

## Task Goal

修复生产组长“活跃订单分配”下拉候选内容重叠问题，并明确“未返回订单编号”的来源：订单编号必须来自活跃订单列表正式 `workOrderCode` 字段，前端不得用内部 ID 伪装成订单编号。

## Milestones

- [x] 定位截图对应的分配弹框、下拉候选模板和订单编号格式化逻辑。
- [x] 用 BDD + RED 静态合同锁定下拉弹层高度、选项布局和正式订单编号展示边界。
- [x] 最小修改前端下拉弹层样式，不改变活跃订单提交身份字段。
- [x] 运行目标静态合同、相邻合同、类型/差异检查并记录结果。
- [x] 完成收尾记录和最终状态。

## Expected Verification

- `node tests/e2e/team-leader-active-order-option-label-static.spec.js`
- `node tests/e2e/team-leader-report-allocation-static.spec.cjs`
- `node tests/e2e/team-leader-workbench-static.spec.cjs`
- `pnpm ts:check`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/team-leader-active-order-option-label-static.spec.js doc/tasks/20260808-active-order-allocation-select-overlap`

## Current Status

completed

实现、验证和 cleanup preview/apply 均已完成；未执行 Git commit/push，因为本项目规则规定未获用户明确要求时不提交。

已定位到 Element Plus `el-option` 默认固定行高与三行自定义候选内容冲突；“未返回订单编号”来自前端对缺失 `workOrderCode` 的显式暴露，不是活跃订单 ID。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；订单编号缺失仍按正式字段缺失暴露，不用 `workOrderId` 或活跃订单 `id` 替代。
- `是否从根因和长期维护角度解决`：是，修复下拉弹层选项高度/行高根因，并保留正式数据合同。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 用户可见描述与内部编码隔离：可见订单编号必须来自正式 `workOrderCode`，不得用内部 ID 占位掩盖。
- 复合输入控件交互保留：仅调整 `el-select` 下拉展示，不替换选择控件、不破坏选择和提交身份。
- 前端截图样式块静态契约：静态合同锁定目标下拉弹层选择器和选项高度，避免跨块误判。
