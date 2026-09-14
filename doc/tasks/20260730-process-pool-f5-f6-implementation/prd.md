# PRD

## Goal

实现生产一线报工工序池 F5/F6：审核副本按上下限生成修正值并保留原始值；原始记录允许在未 FIFO 分配前修改，但必须记录字段级日志并重新电子签名。

## Scope

- F5 审核副本 schema、服务、接口/合同、前端入口和时间轴只读追溯。
- F6 原始记录 revision schema、服务、接口/合同、前端入口和时间轴只读追溯。
- 与 F5/F6 直接相关的测试、mapper、VO、API 和文档证据。
- 两个 worktree 独立实现，合并后整体回归。

## Non-Goals

- 不实现完整排产系统。
- 不改变生产工单 FIFO 排序规则本身。
- 不将现有余量池替代为工序池。
- 不把 F1/F2/F3/F4/F7/F8 全量重做，除非它们的缺失直接阻塞 F5/F6 正式实现。

## User or System Scenarios

- 审核人员基于工序池提交事件生成审核副本，系统按正式上下限元数据修正超限值。
- 员工发现未分配原始记录录入错误，提交修改原因和新的电子签名完成修改。
- 系统拒绝审核或修改已 FIFO 分配的数量片段，保证生产工单分配结果不被绕过。
- 管理人员在时间轴/事件详情中只读追溯审核副本状态和修改历史摘要。

## Functional Requirements

- AC-01: F5 必须有正式审核副本主模型和字段明细模型，字段明细保存 rawValue、correctedValue、ruleType、字段映射和来源事件。
- AC-02: F5 生成审核副本时不得更新工序池原始 payload、报工来源或记录本原始条目。
- AC-03: F5 对数值字段只按正式上下限元数据执行 clamp：低于下限修正为下限，高于上限修正为上限，范围内保持原值。
- AC-04: F5 缺少上下限元数据、字段映射、审核权限或审核电子签名时必须失败且不生成有效副本。
- AC-05: F5 对已 FIFO 分配且影响数量、质量状态或可分配状态的字段必须拒绝修正。
- AC-06: F6 必须有正式 revision 主模型和字段级 diff 模型，关联工序池提交事件。
- AC-07: F6 修改未分配原始记录时必须保存修改原因、新电子签名、服务端修改时间、修改前后 payload 和字段级 diff。
- AC-08: F6 缺少新电子签名、复用原提交签名、缺少修改原因或修改原因空白时必须拒绝。
- AC-09: F6 对已 FIFO 分配数量片段或无法确认 FIFO 锁定状态的字段必须拒绝修改。
- AC-10: F5/F6 状态和历史必须进入工序池时间轴/事件详情只读展示，时间轴不得提供写操作。

## Non-Functional Requirements

- 所有写入必须可追溯 tenant、事件、生产工单、工序、员工、账号、设备、签名和服务端时间。
- 错误必须明确暴露缺失前置条件，不允许 silent downgrade。
- 测试应覆盖成功、拒绝、边界值和已分配锁定路径。

## Dependencies and Constraints

- 依赖正式工序池提交事件、payload、字段模板元数据、FIFO 分配锁定查询和电子签名能力。
- 依赖 `docs\acceptance\production-line-process-pool\` 中 F5/F6 现有验收文档。
- 依赖项目 Maven、Node、Playwright 测试命令可执行。

## Acceptance Criteria

- AC-01 至 AC-10 均有 RED/GREEN 证据、实现证据和主 agent review 证据。
- 两个 worktree 的最终实现合并进 `int_main`。
- 合并后 `int_main` 通过 F5/F6 定向测试、静态合同、端口 guard 和必要回归。

