# 20260808 一线生产员工选择即时关闭修复

## Task Goal

修复一线生产全屏后选择员工不如选择工序丝滑的问题：生产模式员工 picker 点击候选后必须立即关闭，再异步执行正式员工上下文切换；PQC 模式继续保留当前登录人校验和校验后关闭逻辑。不引入 fallback、mock 或静默降级。

## Milestones

- [x] M1 定位生产工序选择和员工选择关闭顺序差异。
- [x] M2 新增任务专用静态回归 RED，证明生产员工选择仍在等待接口后关闭。
- [x] M3 实现生产员工选择即时关闭，保留过期请求保护和错误显式暴露。
- [x] M4 执行目标合同、相邻 picker/缓存合同、类型检查和 diff 检查。
- [x] M5 归档 evidence，执行 cleanup preview/apply 后完成收尾。

## Expected Verification

- `node tests/e2e/frontline-production-employee-picker-immediate-close-static.spec.cjs`
- `node tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs`
- `node tests/e2e/frontline-production-picker-initial-loading-static.spec.cjs`
- `node tests/e2e/frontline-production-maximize-runtime-cache-static.spec.cjs`
- `pnpm ts:check`
- `git diff --check -- <本任务文件>`

## Applicable Gates

- 前端选择弹框即时反馈门禁：生产模式点选即关闭，关闭必须发生在耗时员工上下文切换 `await` 前；PQC 校验型场景仍保留校验成功后关闭。
- 前端静态契约隔离门禁：新增任务专用最小静态合同，先 RED 再 GREEN。
- 严格 no-fallback：员工切换失败仍显示正式错误，不用旧员工、默认模板或吞异常冒充成功。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；只调整生产模式 UI 关闭时机，正式员工切换接口失败仍通过现有错误状态暴露。
- `是否从根因和长期维护角度解决`：是；对齐生产工序选择的即时关闭交互，并用静态合同锁定关闭顺序。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

实现、验证、evidence validator 和 cleanup preview/apply 已完成；真实 Playwright 相邻路径因缺少生产组长账号环境变量被正确阻塞，未用 admin/default 账号替代。
