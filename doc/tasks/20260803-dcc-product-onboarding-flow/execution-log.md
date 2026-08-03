# Execution Log

## User Intent

- 用户要求补全“产品立项/建档闭环”：从一个还不在 DCC 项目代码里的产品发起建档、审批、生成 DCC 项目代码、关联 MDM 产品，并进行开发验证。

## Rule And Skill Bootstrap

- 已读取 `docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/database-rules.md`、`docs/powershell-encoding.md`。
- 已读取 `docs/experience-index.md`，命中 DCC 文控审批、DCC 基础条目/项目代码、数据库 schema 核对、前端静态契约隔离等门禁。
- 已读取技能：`backend-api-delivery`、`frontend-feature-delivery`、`database-schema-delivery`、`behavior-driven-development` 及其 evidence contract。

## BDD

- BDD: 产品建档申请生成待审批单 -> Given 一个产品尚未存在 DCC 项目代码 / When 用户提交包含 MDM 产品信息和目标 DCC 项目代码的建档申请 / Then 系统创建待审批申请 / And 不立即生成正式 DCC 项目代码。
- BDD: 审批通过生成正式 DCC 项目代码并绑定 MDM -> Given 产品建档申请处于待审批状态 / When 审批人审批通过 / Then 系统必须创建或绑定启用状态的 MDM 产品 / And 生成启用的 DCC 项目代码 / And DCC 项目代码记录 `productMasterId`。
- BDD: 重复 DCC 项目代码必须拒绝 -> Given DCC 项目代码已存在 / When 用户提交相同目标项目代码的建档申请 / Then 请求被拒绝 / And 不创建申请、MDM 产品或 DCC 项目代码。
- BDD: 禁用 MDM 产品不能被绑定 -> Given 目标 MDM 产品存在但状态为禁用 / When 用户审批通过建档申请 / Then 审批动作失败并提示 MDM 产品不可用 / And 不生成 DCC 项目代码。
- BDD: 受控文件提交沿用 MDM 产品绑定 -> Given DCC 项目代码已由建档闭环生成并绑定 MDM 产品 / When 用户基于该项目代码提交受控文件 / Then 受控文件必须保存 `productMasterId`、产品编码和产品名称的正式 MDM 来源。
- BDD: 页面入口暴露建档申请失败原因 -> Given 用户在 DCC 项目代码基础数据页发起产品建档 / When 后端因重复编码、缺必填或禁用 MDM 产品拒绝请求 / Then 页面必须展示真实失败原因，不吞掉错误或默认成功。

## Command Log

- Bootstrap: `git status --short --branch` -> 当前 `int_main` 领先 origin 5 个提交，存在多个无关脏改动；本任务将只触碰 `doc/tasks/20260803-dcc-product-onboarding-flow` 和本功能相关代码，提交前按项目策略复核。

## Milestone Status

- M1 任务文档、BDD 和门禁：in_progress。
- M2 现有代码定位：pending。
- M3 RED 测试：pending。
- M4 实现：pending。
- M5 GREEN/回归验证：pending。
- M6 收尾证据：pending。
