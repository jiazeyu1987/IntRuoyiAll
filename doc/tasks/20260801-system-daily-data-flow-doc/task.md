# 系统一天数据流转文档写入

## Task Goal

将用户确认的“系统未来一天的工作 / 数据流转”整理为 Markdown 文档，并写入 `C:\Users\BJB110\Desktop\文档\职责\系统.md`。

## Milestones

- [x] 创建任务目录。
- [x] 读取任务收尾规则和 PowerShell/UTF-8 编码规则。
- [x] 写入目标 Markdown 文档。
- [x] 执行 UTF-8 读取与关键内容验证。

## Expected Verification

- 使用 `python -X utf8` 读取目标文件，确认中文内容可读。
- 检查目标文件包含 ERP 数据、生产订单池、报工、班组长确认、PQC、过程检验记录、批记录完整性和放行推送。
- 检查任务记录保留在 `doc/tasks/20260801-system-daily-data-flow-doc/`。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本次按已确认业务口径整理系统数据流转，不用默认成功或替代链路掩盖缺失。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 目标文件路径包含中文，必须用 UTF-8 显式写入和读取验证。
- 本次只写用户指定桌面文档和本任务记录，不处理工作区既有 unrelated Git 改动。
