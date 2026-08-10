# QA 规程已配置产品下拉排序口径统一

## Task Goal

统一 QA 规程配置页面产品下拉排序口径：所有已配置 QA 规程的产品优先展示，不按具体产品名称硬编码固定第一、第二；草稿状态下只要前端候选数据已经带有 QA 规程配置关系，也应进入已配置优先组。

## Milestones

1. 定位 QA 规程配置页面产品下拉候选映射、排序和已配置状态来源。
2. 用静态回归合同复现“已配置产品未按统一口径优先”的失败。
3. 实现统一排序口径，并保留产品身份、编码、名称和草稿状态展示链路。
4. 运行目标回归、相邻静态合同、类型检查和 diff 检查。
5. 合回 int_main 并完成任务记录、经验沉淀和收尾。

## Expected Verification

- BDD 场景覆盖：多个产品存在 QA 规程配置时，已配置项整体排在未配置项之前；同一组内维持既有稳定排序；DRAFT 不排除已配置判断。
- RED/GREEN 静态合同证明旧逻辑失败、新逻辑通过。
- 前端目标静态合同、相邻 QA 规程静态合同、`pnpm ts:check`、`git diff --check` 通过或记录明确阻塞。

## Current Status

ready_for_closeout

## Applicable Experience Gate

- 命中 docs/backend-development.md#QA 规程配置状态必须来自产品级规程记录：排序和草稿恢复必须以 DCC productMasterId 为产品身份 key，不能把具体项目代码或产品名称固定置顶。
- 本任务按用户确认后的统一口径执行：后台正式 project-statuses 返回的已配置状态 + 当前页面已按产品保存的 QA 规则草稿数据，都进入“已配置优先组”；排序本身不包含产品名特判。

## Completed Work

- 已在任务 worktree 中把 QA 规程配置页下拉候选从单页 50 条改为按 200 条分页拉完整匹配集合，再统一查询产品级 QA 配置状态并排序。
- 已补齐页面当前草稿数据的配置判断口径：同一产品 ID 下已有检验项目草稿时，也按用户要求进入“已配置优先组”。
- 已新增只读真实页面回归，验证本机页面中 IDI / 按压式球囊扩充压力泵 / 1 与 ID / 球囊扩张压力泵 / 112 都进入已配置组，且已配置组整体排在未配置项之前。

## Verification Evidence

- RED: node tests/e2e/qa-regulation-project-configured-dropdown-real.e2e.cjs -> FAIL，失败原因为 ID / 球囊扩张压力泵 / 112 未出现在真实下拉候选中。
- GREEN: node tests/e2e/qa-regulation-project-configured-dropdown-static.spec.cjs -> PASS。
- GREEN: node tests/e2e/qa-regulation-project-configured-dropdown-real.e2e.cjs -> PASS，真实页面候选数 119，已配置组最后位置 1，未配置组起始位置 2，无写请求、无 console error、无 page error。
- GREEN: 相邻回归 qa-regulation-header-project-select-static、qa-regulation-project-last-copy-static、qa-regulation-product-specific-rules-static -> PASS。
- GREEN: pnpm ts:check -> PASS。
- GREEN: pnpm install --frozen-lockfile --reporter append-only -> PASS，补齐任务 worktree 前端依赖，锁文件未改变。
- GREEN: pnpm ts:check -> PASS，基于任务 worktree 本地依赖复跑通过。
- GREEN: git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-project-configured-dropdown-static.spec.cjs IntRuoyiFronted/tests/e2e/qa-regulation-project-configured-dropdown-real.e2e.cjs -> PASS。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，目标是统一已配置状态排序口径，不做产品名特判。
- 是否存在临时补丁或绕过：否。

## Experience Gate Summary

- 已读取 docs/experience-index.md，命中 QA 规程配置状态产品级口径门禁；本任务已把适用摘要记录到 Applicable Experience Gate。
- 已按 project-experience-consolidation 规则合并长期经验到 docs/backend-development.md，并补充 docs/experience-index.md 关键词。

## Cleanup Candidates

- output/playwright/20260810-qa-regulation-configured-sort/

## Closeout Evidence

- 待融合进 int_main 后执行最终 closeout。
