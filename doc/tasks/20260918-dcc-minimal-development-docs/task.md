# DCC 主流程最小实现开发文档

## Goal

根据本会话完整业务需求及前一轮静态差距分析，为全部未实现、部分实现和与需求冲突的内容形成最小开发方案；只编写文档，不实现业务代码。

## Milestones

1. 核对当前代码、仓库规则和旧文档冲突。
2. 编写业务范围、技术设计及 Given/When/Then 验收，覆盖全部缺口。
3. 验证需求编号、路径、链接、方法锚点、UTF-8 和交付边界。
4. 进入 ready_for_closeout，核对任务资产及正式收尾条件。

## Expected Verification

- 文档结构验证：每项缺口均有业务规则、设计归属及验收场景。
- 本地引用文件和代码方法锚点存在，方案与现状分开。
- 明确旧规则被本次设计替代的范围，不把本轮建议写成既往用户已确认。
- 不执行构建、产品测试、E2E、数据库或服务操作。

## Current Status

blocked — 双文件上传设计、review优化和文档结构验证已完成；正式收尾仍因仓库未提供指定task-closeout-cleanup入口、且当轮未授权Git提交/推送而阻塞，未标记completed。文档可直接交给后续开发使用。

## 设计约束检查

- 用户要求：所有缺口采用最小实现，越简单越好，只覆盖主流程。
- 本轮追加要求：可编辑版本非必填、不可编辑版本必填，日常在线浏览使用不可编辑版本；先设计开发文档，再review文档优化。
- 复用现有项目权限、上传票据、文件存储、BPM、电子签名、版本链和站内通知。
- 保留明确权限、必需签名、内容历史和唯一有效版本等主流程约束；不扩展复杂容灾、任意流程编排、批量自动修复。
- BDD/TDD：本次仅文档结构验证；未来生产代码按验收文档先 RED 再 GREEN，当前不声称任何实现或测试已通过。
- 当前实际分支为 int_qms，目录名称不代表分支；基线 HEAD 为 a9bcb6d36d96145ddc1252f111347b644b328deb，存在无关未提交修改，不触碰。
- 当前用户没有授权 Git 提交/推送；根 AGENTS.md 的明确授权要求优先于收尾文件中的通用提交要求。
- 未启用子 Agent；未授权 E2E、数据库写入、服务启停、远程和发布。
- 双文件专项采用真实PDF作为第一版不可编辑浏览版；不做自动转换，不允许缺浏览版时回退可编辑源文件或历史字段。

## Deliverables

- docs/dcc-controlled-file-dual-version-upload.md
- docs/dcc-minimal-main-flow/README.md
- docs/dcc-minimal-main-flow/implementation.md
- docs/dcc-minimal-main-flow/dual-artifact-upload.md
- docs/dcc-minimal-main-flow/dual-artifact-upload-review.md
- docs/dcc-minimal-main-flow/acceptance.md
- docs/changes/20260918-dcc-minimal-main-flow.md

## Cleanup Keep

- doc/tasks/20260918-dcc-minimal-development-docs/task.md
- doc/tasks/20260918-dcc-minimal-development-docs/execution-log.md
- doc/tasks/20260918-dcc-minimal-development-docs/verification-report.md
