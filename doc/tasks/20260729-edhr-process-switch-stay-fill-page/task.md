# eDHR 辅助填写工序切换留在填写页

## Task Goal

修复辅助填写页顶部“工序”切换行为：点击切换当前订单/批次的任意工序后仍保留在辅助填写页/原表填写页上下文，不跳转到批次流程详情页；该查看切换不做填写权限拦截，任何填写人都可以切换查看。

## Milestones

1. 已完成：读取前端、E2E、任务收尾、PowerShell、技能和既有关联经验门禁。
2. 进行中：定位当前跳转到流程页的前端分支和现有静态合同。
3. 待完成：补充 RED 静态合同覆盖“无 execution/workTask 的工序也留在填写页”。
4. 待完成：实现同页只读/查看式工序切换，不放宽保存、提交、openTask 写权限。
5. 待完成：运行目标静态合同、相邻切换合同和类型检查。
6. 待完成：更新证据、收尾状态、提交/推送或记录阻塞。

## Expected Verification

- `node tests/e2e/edhr-assist-process-switch-all-statuses-static.spec.js`
- `node tests/e2e/edhr-assist-fill-mode-static.spec.js`
- `node tests/e2e/edhr-switch-filler-selectability-static.spec.js`
- `node tests/e2e/edhr-switch-filler-formcenter-slot-static.spec.js`
- `pnpm ts:check` 或记录无关阻塞

## Applicable Gates

- eDHR 辅助模式当前工序 assistRows 路由门禁：工序切换列表必须来自当前批次全部普通工序；无执行记录/工作任务时不得报“缺少可查看执行记录或工作任务”。
- eDHR 当前工序运行态展示门禁：状态背景只表示运行态展示，不提升填写权限。
- 前端 Route Query ID 比较门禁：`batchTaskId/workTaskId/assistUserId` 等路由 ID 比较必须统一语义。
- 切换填写人 FormCenter 槽位导航门禁：不得破坏 FormCenter 槽位分支和 `assistUserId` 透传。
- Strict No-Fallback：不伪造 openTask 成功，不吞后端错误；查看切换走正式页面状态而不是默认成功。

## Design Constraints Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是在执行页内建立正式查看式工序上下文切换。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress
