# 20260803-hide-dcc-controlled-file-buttons

## Task Goal

隐藏截图黄框内的 DCC 受控浏览详情页按钮，确保该只读查看场景不再显示审批、分发、版本、修改或识别基础信息等操作入口。

## Milestones

- [x] 定位按钮所在页面、组件和现有权限/状态控制逻辑。
- [x] 先补充静态回归契约，证明旧按钮仍会在目标只读区域渲染。
- [x] 最小化修改前端渲染逻辑，隐藏目标按钮且不影响其它正式操作链路。
- [x] 运行目标契约、相邻前端验证和必要的类型检查。
- [ ] 记录收尾、验证结果和可复用经验。

## Expected Verification

- 目标静态契约先 RED 后 GREEN。
- 相邻 DCC 受控浏览相关静态验证通过。
- 如改动触及 Vue/TypeScript，运行 `pnpm ts:check` 或记录明确阻塞。

## Experience Gate

- 命中 `docs/frontend-development.md#前端截图按钮统一静态契约门禁`：基于截图改按钮显示时，必须先定位目标组件和同类按钮，新增或更新聚焦静态契约，禁止改路由、权限或扩大重设计。
- 命中 `docs/e2e-rules.md#dcc-受控浏览当前有效版与权限隔离门禁`：DCC 受控浏览只读验证不能改权限，不能用 API-only 或直接详情 URL 冒充页面验收，目标链路应保持只读。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是收敛只读详情页操作入口渲染条件。
- `是否存在临时补丁或绕过`：否。
