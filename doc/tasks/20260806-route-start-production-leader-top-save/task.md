# 20260806 route start production leader top save

## Task Goal

让工艺路线流转关系图顶部“保存”按钮同时保存右侧“工序开始生产组长”字段明细中的变动，避免用户只看到通用“保存成功”但生产组长配置未提交。

## Milestones

- [x] 定位顶部保存与生产组长字段保存的分离链路。
- [x] 补充静态回归合同，先证明顶部保存未调用生产组长专用保存。
- [x] 实现顶部保存联动保存生产组长配置。
- [x] 运行目标静态合同和基础校验。

## Expected Verification

- `node tests/e2e/mes-route-start-production-leaders-static.spec.js`
- `git diff --check`

## Current Status

ready_for_closeout

## Applicable Experience Gate

- `docs/frontend-development.md#前端按钮文案与行为一致性门禁`：保存按钮行为必须与文案一致，静态合同需锁定点击处理器和正式保存链路。
- `docs/frontend-development.md#前端静态契约隔离门禁`：新增当前需求专用最小静态合同，避免扩大到无关历史失败。
- `AGENTS.md#工艺路线三类配置术语契约`：生产组长属于“工序开始”配置，不得用表单槽位或批记录表单替代。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，顶部保存复用正式生产组长保存 API，不新增替代数据源。
- `是否存在临时补丁或绕过`：否。
