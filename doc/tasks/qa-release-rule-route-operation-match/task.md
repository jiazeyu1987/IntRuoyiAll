# QA 发布规则正式工序匹配修复

## Task Goal

修复 QA 发布规则中选择产品“球囊扩张压力泵”时，外观检验正式工序“清洗”无法匹配激活路线版本工序而阻断发布的问题。

## Milestones

- [x] 建立缺陷复现与任务证据
- [x] 定位 QA 发布规则正式工序与激活路线工序匹配实现
- [x] 编写并运行 RED 回归测试复现问题
- [x] 实施最小正式修复
- [x] 运行 GREEN 与定向回归验证

## Expected Verification

- 定向前端静态合同覆盖“QA 多工序发布按正式工序匹配激活路线版本工序”。
- 验证不使用 formBindings、表单槽位或默认槽位替代正式工序来源。
- 验证发布规则缺失正式工序匹配时仍按真实异常 fail fast。

## Applicable Experience Gates

- 命中经验索引关键词：QA 多工序发布、routeProcessId 逐工序 payload、激活路线版本工序身份、清洗精洗。
- 待核对门禁：docs/backend-development.md 中 “qa-多工序正式发布与退役夹具唯一键必须隔离”。
- 前端实施门禁：docs/frontend-development.md 中前端静态契约隔离门禁。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；修复项目级显式工序映射，不使用默认匹配或包含关系猜测。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

- 已完成实现、验证、缺陷证据校验与 task-closeout-cleanup preview/apply。

## Cleanup Keep

- doc/tasks/qa-release-rule-route-operation-match/bug-regression-evidence.md
