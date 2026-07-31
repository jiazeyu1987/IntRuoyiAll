# 球囊扩张压力泵批记录表单默认填写人角色配置

## Task Goal

在本机 `int_main` 运行态的“芋道源码”租户中，为产品“球囊扩张压力泵”当前批记录表单目录里的每个表单创建对应的填写者角色，并为每个角色随机分配 3 个当前租户正常启用的普通账号；随后把对应批记录表单的默认填写人设置为该角色。

## Milestones

- [x] 建立任务文档、记录既有脏工作区基线与适用经验门禁。
- [x] 只读核对本机运行态、目标租户、目标产品表单清单、角色/用户/填写规则 API 与 schema。
- [x] 先记录 BDD 与 RED 验证，证明当前目标表单默认填写人按角色配置后，后端 `get-by-report` 缺少角色名称回显。
- [x] 通过正式本机接口或核实后的正式数据链路创建角色、分配 3 个账号、保存表单默认填写人角色。
- [x] 复验每个表单、角色、账号分配、登录态 API 与只读页面展示口径，记录证据与阻塞项。

## Expected Verification

- BDD 覆盖：每个表单生成一个填写者角色；每个角色随机绑定 3 个启用普通账号；批记录表单列表填写人显示为角色并可通过小弹窗更换。
- RED/GREEN 记录在 `execution-log.md`，命令不输出密码、token 或连接密钥。
- 使用本机 `http://127.0.0.1:48081` 与“芋道源码/admin”登录态核验真实接口；若运行态、登录、租户、账号数或表单数据缺失则 fail fast。
- 写入后核对角色存在、角色账号数量、填写规则来源为角色、目标产品表单数量与配置数量一致。

## Cleanup Keep

- doc/tasks/20260728-pressure-pump-batch-record-role-fillers/configure_pressure_pump_role_fillers.mjs
- doc/tasks/20260728-pressure-pump-batch-record-role-fillers/verify_pressure_pump_role_fillers.py
- doc/tasks/20260728-pressure-pump-batch-record-role-fillers/pressure_pump_role_filler_ui_readonly.e2e.js
- doc/tasks/20260728-pressure-pump-batch-record-role-fillers/pressure-pump-role-filler-verification.json
- doc/tasks/20260728-pressure-pump-batch-record-role-fillers/pressure-pump-role-filler-ui-e2e.json
- doc/tasks/20260728-pressure-pump-batch-record-role-fillers/bug-regression-evidence.md
- doc/tasks/20260728-pressure-pump-batch-record-role-fillers/backend-api-evidence.md

## Current Status

ready_for_closeout

## Closeout Blockers

- Cleanup preview/apply 已执行，无可删除任务产物。
- 验证已通过，但仓库当前 `int_main` 落后 `origin/int_main` 22 个提交，且存在多个并行任务/用户的未提交改动；本任务未提交、未推送，避免混入无关变更。

## Applicable Gates

- eDHR 填写人配置必须走正式角色、用户角色绑定与批记录表单填写规则链路，不得从当前登录人、创建人、更新人或历史角色 ID 推断。
- 批记录表单与表单槽位不得混用：本任务只处理“批记录表单”目录里的 `MAIN` 批记录表单默认填写人。
- 写入前必须核对真实 schema、目标租户、目标产品版本、目标表单数量、角色/用户范围和回滚方式。
- 本机运行态与登录态必须使用 `127.0.0.1:48081` 和“芋道源码/admin”来源，不切换远端或其它租户。
- PowerShell 和中文数据写入必须使用 UTF-8；不记录密码、token、私钥或连接串密钥。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，使用正式角色与批记录表单填写人配置链路，不使用临时前端展示或 mock 数据。
- `是否存在临时补丁或绕过`：否。
