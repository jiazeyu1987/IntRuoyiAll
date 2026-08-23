# 生产放行芋道源码写入夹具授权

## Request Summary

- change_id: `20260817-production-release-yudao-write-fixture-authorization`
- source: 用户于 2026-08-17 明确授权从“芋道源码”用户列表选择合适业务账号，并允许自行创建订单等虚拟数据；用户同时提供了统一测试密码，密码值不写入本变更单、任务日志或测试证据。
- follow-up source: 固定账号真实登录返回“密码已过期”后，用户于 2026-08-17 明确回复“授权”，批准仅将 `zhulijiang`、`xujianhai` 两个固定业务账号重置为用户提供的统一测试密码。
- requested_change: 在此前 `芋道源码/admin` 只读验证基础上，恢复 P11 真实多账号写入型验收，并允许创建带任务标识、可追踪、可清理的任务自有虚拟业务数据。

## Current Baseline Reviewed

- 任务目录：`doc/tasks/20260814-production-release-flow-implementation`；P1-P10 已完成，P11 仍为 `blocked`。
- 当前运行态：本机 `int_main` 的 8081/48081 来源和健康检查已通过，后端运行包包含生产放行核心实现。
- 当前已完成：`芋道源码/admin` 官方登录及生产组长、eDHR 工作任务、表单追溯三个只读入口已由主 Agent 和独立 tester 复核通过。
- 当前缺口：尚未盘点业务账号与角色，尚未创建三组任务自有订单、四附件、签核证据和清理计划，也未证明第二租户隔离账号与本机文件存储可用。

## Classification

- type: 验收数据与账号授权扩展。
- product_scope_change: 否。
- behavior_change: 否。
- data_scope_change: 是；允许在本机“芋道源码”范围创建和清理任务自有虚拟数据。
- credential_scope_change: 是；允许临时使用用户提供的统一测试密码登录已存在的合适业务账号，禁止记录密码明文。

## Impact Analysis

| 范围 | 影响 |
| --- | --- |
| 产品 | AC-01 至 AC-34 不变，不新增或修改业务功能。 |
| 设计 | 无架构、接口和页面设计变更。 |
| 数据 | 只允许新增带 `PRFLOW-T11-20260817` 标识的任务自有虚拟数据，并按预先登记的页面清理计划清理；另允许仅重置 `zhulijiang`、`xujianhai` 两个固定账号密码，不得修改其它用户、角色或无关业务记录。 |
| API | 写动作必须由真实前端页面触发；API/数据库只允许前置只读盘点和最终只读核验，不得推状态。 |
| 测试 | 恢复 TC-13 多账号写入验证和独立 tester 门禁；缺账号、第二租户、正式来源、文件存储或清理入口时 fail fast。 |
| 发布 | 不构成生产、远程服务器或发布授权，仅限当前本机运行态。 |
| 运维 | 不授权启停当前共享服务；如运行态、数据库、Redis、文件存储任一不可用，记录 blocker 后停止。 |

## Decision

- decision: `ACCEPT`
- accepted_scope: 只读盘点“芋道源码”现有业务账号和角色；仅将 `zhulijiang`、`xujianhai` 两个固定业务账号重置为用户提供的统一测试密码；使用该密码执行真实登录；通过正式页面创建、处理和清理任务自有虚拟订单及生产放行链路数据。
- excluded_scope: 修改其它现有账号密码、角色、菜单、租户基线、无关业务记录；通过 SQL 或 API 直接推进业务状态；访问远程或生产环境。
- rationale: 用户已明确补充账号来源与虚拟数据写入授权，能够进入正式 P11 前置准备；仍须按项目 E2E 数据门禁证明每项前置和清理能力。

## Required Approval

- requester_approval: 已获得本机“芋道源码”现有业务账号临时登录和任务自有虚拟数据创建授权。
- password_reset_approval: 已获得仅重置 `zhulijiang`、`xujianhai` 两个固定账号密码的明确授权。
- password_handling: 已获得临时密码来源；仅在进程内传递，不回显、不落盘、不写任务证据。
- additional_approval: 若需修改上述两个账号之外的账号、角色或租户，或清理只能依赖 SQL/API，必须另行阻塞，不得扩权。

## Downstream Actions

1. 将 P11 从 `blocked` 恢复为 `in_progress`，只继续当前阶段。
2. 通过真实用户管理页面仅重置 `zhulijiang`、`xujianhai` 的密码，并用两个固定账号重新登录核验权限。
3. 只读盘点现有账号角色、第二租户隔离账号、三类正式来源、可用于虚拟订单的生产基础数据、文件存储和页面清理入口。
4. 为本轮生成唯一 `PRFLOW-T11-20260817` 业务标识，冻结创建清单、写前快照和清理计划。
5. 前置齐备后配置 30 项进程级输入，运行真实 Playwright 多账号链；页面完成后只读核验并按计划清理。
6. 由独立 tester 复验真实结果、跨角色/跨租户边界、最终状态和零残留，再由主 Agent 更新 P11 状态。

## Blockers And Next Action

- current_blockers: 用户已提供符合真实页面强度合同的新测试密码，但重启后本机 8081/48081 均未监听；第二租户、三组订单 fixture、三类正式来源、四附件、签核、文件存储和页面清理能力仍待确认。
- next_action: 恢复本机 `int_main` 的 8081/48081 标准运行态；随后通过真实用户页面重置两个固定账号并重新登录。
