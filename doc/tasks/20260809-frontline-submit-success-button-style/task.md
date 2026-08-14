# 一线生产提交成功按钮样式与提示调整

## 任务目标

- 一线生产正式提交成功后，提交按钮保持提交前的视觉样式，不切换为整块成功态样式。
- 成功提示仅表达实际提交人提交成功，不再在按钮中展示报工编号和工序池编号。
- 保留提交成功后的防重复提交约束。

## 里程碑

- [x] M1：定位正式提交按钮、成功态和现有静态合同。
- [x] M2：先补充失败的聚焦静态合同。
- [x] M3：实现最小前端调整并通过聚焦合同。
- [x] M4：完成相邻回归、证据校验与任务收尾。

## 预期验证

- 聚焦静态合同先 RED 后 GREEN，证明成功态按钮不再切换专用成功样式，且显示实际提交人成功提示。
- 现有正式提交相邻静态合同通过。
- `pnpm ts:check` 或记录与本任务无关的既有阻塞。
- `git diff --check` 通过。
- `frontend-feature-delivery` 证据校验通过。

## 经验门禁

- 已读取 `docs/experience-index.md`，命中前端静态合同隔离与一线生产正式提交相关经验。
- Preflight：以提交按钮的 `data-formal-feedback-id` 稳定锚点抽取目标块；成功提示读取当前已选实际员工标签，回执编号只保留为机器可读 metadata。
- Blocker：若移除成功视觉会解除防重复提交、提示改用报工/工序池编号替代人员姓名，或聚焦合同无法稳定 RED/GREEN，则停止实现。
- Verification：聚焦合同锁定正常按钮配色、实际员工成功文案、回执 metadata 和禁用态；再运行现有正式提交合同、类型检查与 `git diff --check`。
- Forbidden：不得隐藏成功态错误、移除正式回执、开放重复提交，或以编号 fallback 冒充提交人。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；直接移除提交成功时对按钮视觉和编号文案的状态分支，继续使用正式提交回执与防重复提交状态。
- 是否存在临时补丁或绕过：否。

## Current Status

completed：实现、定向回归、技能证据校验和 cleanup preview/apply 均已通过。

## 最终验证结果

- 聚焦合同、3 个相邻静态合同、`pnpm ts:check`、任务范围 `git diff --check` 和技能证据 validator 全部通过。
- cleanup preview/apply 无 blocked、无 warning，只删除任务临时 `frontend-feature-evidence.md`，保留三份核心任务记录。
