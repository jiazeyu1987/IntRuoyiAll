# DCC项目代码与产品目录联合新建审批开发文档任务

## Goal

根据用户确认的需求，形成一份可交给后续开发使用的 DCC 联合新建审批开发文档，并对需求完整性、现有代码边界、数据一致性和审批流程进行 review。

本任务只编写和校验文档，不修改生产代码、测试代码、数据库、运行环境或服务。

## Milestones

1. 读取仓库规则及现有 DCC 项目代码、产品目录、产品建档申请实现。
2. 建立需求范围、页面字段、审批状态、数据模型、接口边界、事务与重复校验设计。
3. 对设计进行 review，记录已确认规则、实现风险、待确认事项和禁止行为。
4. 执行文档结构、链接、编码和内容覆盖校验。
5. 进入 `ready_for_closeout`，复核任务资产和交付状态。

## Expected Verification

- 正式开发文档覆盖用户确认的入口、字段、审批、关联、重复校验、回滚和失败状态。
- 开发文档明确区分 DCC 产品目录数据、DCC 项目代码数据、MDM 产品建档申请。
- 所有 Given/When/Then 场景均能映射到设计规则。
- 文档中的本地链接、章节标题和代码参考文件存在。
- Markdown 严格 UTF-8 解码通过，文档空白检查无问题。
- 不执行构建、单元测试、E2E、数据库写入、服务启停、Git 提交或推送。

## Current Status

completed

## 设计约束检查

- 入口必须在 DCC 产品目录；DCC 项目代码页面删除直接新建入口。
- 一次申请只允许一个项目代码和一个产品，并建立一对一关联。
- 新建界面只保留：项目名称、项目代码、项目负责人、产品编码、产品名称、分类、备注。
- 分类只能选择“一类、二类、三类”。
- 审核人和批准人均为 `admin`，保留两个独立审批节点和审批记录。
- 审批通过后才写入正式 DCC 项目代码和 DCC 产品目录。
- 正式写入必须事务化；任一步失败时整体回滚，并把申请标记为写入失败。
- 项目代码、产品编码、产品名称均须防止重复；提交时和批准写入前再次校验，并由数据库唯一约束兜底。
- 不把现有 MDM 产品建档申请直接当作本需求的申请模型。
- 不使用 fallback、默认数据、吞异常或模拟审批成功。
- 当前工作区已有其他任务未提交改动，本任务只新增自身文档资产。

## Deliverables

- docs/dcc-project-product-approval/README.md
- docs/dcc-project-product-approval/review-report.md
- doc/tasks/20260920-dcc-project-product-create-approval-design/task.md
- doc/tasks/20260920-dcc-project-product-create-approval-design/execution-log.md
- doc/tasks/20260920-dcc-project-product-create-approval-design/verification-report.md

## Cleanup Keep

- doc/tasks/20260920-dcc-project-product-create-approval-design/task.md
- doc/tasks/20260920-dcc-project-product-create-approval-design/execution-log.md
- doc/tasks/20260920-dcc-project-product-create-approval-design/verification-report.md
