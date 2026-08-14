# 20260807 一线生产最大化运行态缓存

## Task Goal

在一线生产页面点击“最大化”时预加载当前运行态的轻量工序与员工切换数据，并在后续工序/员工切换中优先命中内存缓存，减少重复请求和切换等待；不缓存批记录表单内容、附件或草稿数据，不引入 fallback、mock 或静默降级。

## Milestones

- [x] M1 定位一线生产最大化、工序 picker、员工 picker 与现有接口链路。
- [x] M2 先补静态合同 RED，锁定最大化预加载缓存、命中读取、过期请求保护和失败显式暴露。
- [x] M3 实现最大化预加载轻量运行态缓存，并保持正式接口失败可见。
- [x] M4 执行目标静态合同、相邻一线生产合同、类型检查或记录明确阻塞。
- [x] M5 更新验证报告与收尾状态。
- [x] M6 使用真实 Playwright 页面验证最大化预加载、工序缓存命中和员工切换缓存命中。

## Expected Verification

- `node tests/e2e/frontline-production-maximize-runtime-cache-static.spec.cjs`
- 相邻静态合同：`node tests/e2e/frontline-production-picker-initial-loading-static.spec.cjs`
- 相邻静态合同：`node tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs`
- `pnpm ts:check`，若被无关历史问题阻塞，记录首个无关 blocker。
- `git diff --check`
- 真实 E2E：验证 `requestFullscreen()` 后的 `runtime-config` GET 数量、工序切换命中缓存、员工首次切换 POST 与重复切换 POST 数量、目标链路错误和页面错误。

## Applicable Gates

- 前端选择弹框即时反馈门禁：工序/员工候选依赖正式异步请求时，必须区分 loading、empty、error、ready；点击切换不能因耗时请求造成旧请求覆盖新状态。
- 静态合同隔离门禁：本轮先新增任务专用静态合同 RED/GREEN，不能用全量历史失败替代当前行为验证。
- 严格 no-fallback：缓存只保存正式请求成功后的轻量运行态数据；预加载失败必须暴露，不能用旧数据或空默认值冒充成功。
- 工艺路线三类配置术语契约：若缓存字段涉及批记录表单、工序开始或表单槽位，只能缓存各自元数据并保持来源边界；不得用 `formBindings` 替代正式逐工序批记录表单绑定。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缓存未命中继续走正式现有加载，预加载失败显式记录错误状态。
- `是否从根因和长期维护角度解决`：是；将最大化入口作为预加载时机，减少重复切换请求并用任务专用合同锁定。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

实现、静态验证、用户追加的真实 Playwright E2E 和 task-closeout-cleanup preview/apply 均已完成。当前工作区存在大量无关脏改动与历史 `target_corrupt` 警告，本任务未清理、未回退、未提交这些无关文件。

## Cleanup Keep

- doc/tasks/20260807-frontline-maximize-runtime-cache/frontline-production-runtime-cache-real-e2e.cjs
- output/playwright/20260807-frontline-maximize-runtime-cache/frontline-production-runtime-cache-result.json
- output/playwright/20260807-frontline-maximize-runtime-cache/frontline-production-runtime-cache.png
