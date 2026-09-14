# 一线 PQC 检验项目标签文字居中

## 任务目标

将一线 PQC 填写页底部检验项目标签（截图中的“外观”）文字改为水平、垂直居中，不改变标签数据、切换行为、尺寸和选中态。

## 里程碑

- [x] M1：按截图定位正式组件、选择器和相邻静态合同。
- [x] M2：先补充居中行为静态合同并取得预期 RED。
- [x] M3：实施最小 CSS 修复并取得 GREEN。
- [x] M4：完成相邻回归、技能证据校验与任务收尾。

## 预期验证

- `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js`
- `node tests/e2e/frontline-pqc-tab-description-redbox-only-static.spec.cjs`
- `node tests/e2e/pqc-tab-item-name-display-static.spec.cjs`
- `git diff --check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260809-frontline-pqc-tab-text-center/frontend-feature-evidence.md`

## 经验门禁

- `docs/experience-index.md` 存在。
- 本任务命中 `docs/frontend-development.md#前端截图样式块静态契约门禁`：测试必须先抽取 `.pqc-item-tab` 目标样式块，再断言居中 token，避免跨块误命中。
- 本任务只调整截图中检验项目标签文字对齐，不扩大到整页或其它按钮。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；直接修正目标按钮自身对齐样式，并由聚焦合同锁定。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed：实现、聚焦合同、相邻回归、差异检查、技能证据校验及 cleanup preview/apply 均已通过。
