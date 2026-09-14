# 报工数据参数按设备类型展示

## Request Summary

用户确认批记录单元格链接页“报工数据”的字段目录应显示一线填写参数，而不是按每台物理设备重复展示参数。设备字段只显示实际选用设备编码；同类多台设备只展示一套参数；不同设备类型的参数都要显示并可链接。

## Source

- 2026-08-30 当前对话截图和用户确认。

## Current Baseline Reviewed

- 已有实现把设备参数来源字段按 `deviceId` 展开并在标签中显示设备名称/编码。
- 已有真实路径显示 `超声波清洗机 / B09393`，但用户确认这对设备参数展示过度重复。
- 已有项目门禁要求报工数据必须来自生产组长正式工序设备配置，不能用前端拼接或空列表替代。

## Classification

- Product behavior change.

## Product Impact

- 用户选择来源字段时看到的是业务参数，如清洗次数、清洗介质、清洗功率、室温、清洗时间、烘干温度、烘干时间。
- 同类多台设备不再造成重复参数列表。
- 不同类型设备的参数仍完整展示。

## Design Impact

- 字段目录需要增加“展示分组”和“回填读取”两个概念：展示按设备类型/参数组去重，回填按实际选用设备读取。
- 设备字段的显示值应为实际设备编码，不再把设备名称/编码组合用于设备字段值。

## Data Impact

- 不新增 schema。
- 不修改正式业务数据。
- 仍使用生产组长正式工序设备绑定和设备参数规则作为来源。

## API Impact

- `PROCESS_POOL_REPORT` 来源目录需要支持设备参数展示去重，同时保留足够的正式身份用于回填解析。
- 参数规则缺正式设备、参数或分组身份时必须明确失败。

## Test Impact

- 需要新增/更新后端 RED/GREEN，覆盖同类设备去重、不同类型设备保留、实际设备编码回填。
- 需要新增/更新前端静态合同和真实只读 E2E 断言。

## Release Impact

- 当前仅在隔离 worktree 设计开发验证；不做主线提交、推送或发布。

## Operations Impact

- 需要按 worktree 端口矩阵预留独立 slot；不得占用 `8081/48081`。

## Decision

ACCEPT.

## Required Approvals

- 用户已在当前对话确认此业务口径，并要求先在 worktree 进行设计开发验证。

## Downstream Skill Reruns

- `backend-api-delivery`
- `frontend-feature-delivery`
- `behavior-driven-development`
- `playwright`
- `task-closeout-cleanup`

## Blockers And Next Action

- 当前无外部阻塞。
- 下一步：创建隔离 worktree，补 RED 测试并实现最小正式方案。
