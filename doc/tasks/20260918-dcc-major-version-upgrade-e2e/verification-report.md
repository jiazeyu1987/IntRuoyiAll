# 20260918 DCC 大版本升级完整 E2E Verification Report

## Result

`PASS - E2E business flow`

真实 Playwright 已完成任务自有 DCC 文件从 `A/1` 到 `B/1` 的大版本升级闭环。最终页面和只读数据库均确认：

- 新版 `B/1` 为 `ACTIVE`，且为当前有效版本。
- 旧版 `A/1` 为 `SUPERSEDED`，`superseded_by_file_id` 指向 B/1。
- 受控浏览默认落在 B/1，发布文件和盖章文件均已生成。
- 详情页版本历史显示 A/1、B/1、`已替代` 和大版本变更说明。
- 当前发布策略下 DCC 最终审批后自动完成发布后续，详情页“发布后续”显示“已完成”；页面未出现额外生效申请待办。

## Scope

- 分支：`int_qms`
- 前端：`http://127.0.0.1:8061`
- 后端：`http://127.0.0.1:48061`
- 测试租户：`芋道源码`
- 测试用户：`admin`
- 浏览器：本机 Chrome + Playwright
- 目标文件编号：`E2E-DCC-20260918234556`
- 初始文件 ID：`2054545668044083983`
- 大版本文件 ID：`2054545668044083984`
- 初始版本：`A/1`
- 大版本：`B/1`

## Real Page Evidence

1. 真实页面完成产品/项目、模板、类别目录绑定、临时上传权限、审批路线和临时 admin 签名配置。
2. 真实页面上传初始源文件，提交 DCC 审批并完成三次审批中心审核和最终文控签名。
3. 真实页面检出 A/1，选择“大版本”，上传新版源文件，填写修改说明和检入备注，生成 B/1。
4. 真实页面提交 B/1 审批，三个审核节点返回 HTTP 200/业务码 0；最终文控节点上传盖章 PDF 并签名返回 HTTP 200/业务码 0。
5. 最终详情页头部显示 `当前有效版 / ACTIVE / B/1`，版本历史显示：
   - `B/1`、`DCC-MAJOR-20260919002601 大版本升级正文变更`
   - `A/1`、`已替代`
6. 最终受控浏览页显示：
   - 当前有效版本 `B/1`
   - 发布文件：已生成
   - 盖章文件：已生成
7. 页面截图：
   - `artifacts/initial-approval-step-1.png`
   - `artifacts/99-final-detail.png`

## Request Evidence

| 页面动作 | 请求 | HTTP | 业务码 |
| --- | --- | ---: | ---: |
| 登录 | `POST /admin-api/system/auth/login` | 200 | 0 |
| 初始版本最终签名 | `POST /admin-api/dcc/controlled-files/2054545668044083983/approve-task` | 200 | 0 |
| 大版本检出 | `POST /admin-api/dcc/controlled-files/2054545668044083983/checkout` | 200 | 0 |
| 大版本源文件预览上传 | `POST /admin-api/dcc/controlled-files/upload-preview` | 200 | 0 |
| 大版本检入 | `POST /admin-api/dcc/controlled-files/2054545668044083983/checkin` | 200 | 0 |
| 提交 DCC 审批 | `POST /admin-api/dcc/controlled-files/2054545668044083984/submit` | 200 | 0 |
| 审批中心审核 | `POST /admin-api/approval-center/tasks/review` | 200 | 0 |
| 大版本盖章 PDF 上传 | `POST /admin-api/dcc/controlled-files/upload-preview` | 200 | 0 |
| 大版本最终签名 | `POST /admin-api/dcc/controlled-files/2054545668044083984/approve-task` | 200 | 0 |

## Read-Only Database Verification

```text
2054545668044083983 | SUPERSEDED | A/1 | superseded_by=2054545668044083984
2054545668044083984 | ACTIVE     | B/1 | predecessor=2054545668044083983
B/1 approved_time=2026-09-19 08:26:45
B/1 published_time=2026-09-19 08:26:44
B/1 stamped_file_id=9198354931072
```

临时 `APPROVAL_PDF` 策略只读核验：

```text
policy_code=CODEX_DCC_APPROVAL_PDF_TEMP_V1
purpose=APPROVAL_PDF
max_bytes=10485760
enabled=1
tenant_id=1
deleted=0
```

## Schema And Configuration Evidence

- 正式 DCC 生命周期、签名、GxP、关联文件和发布后续迁移已执行并重复执行通过。
- GxP coverage gate：`PASS operations=8 annotations=7`。
- EDHR issuer/signing secret 仅存在于 `48061` 任务进程环境，未持久化。
- 任务专用临时配置均有明确 `CODEX` 标识，未写入生产租户。
- 迁移前备份：`output/runtime/int_qms/db-backups/dcc-major-version-upgrade-pre-20260919-021704.sql`。
- DCC 定向快照：`artifacts/dcc-file-directory-pre-unclassified-20260919-052100.sql`。
- 未把被取消的全库 dump 作为成功证据。

## Known Non-Product Errors

- Playwright 记录的 `GET /hm.gif` 失败为页面外部埋点请求，不影响目标业务请求。
- 早期真实复验暴露的缺表、缺策略均已按正式迁移或任务授权种子修复并复验通过。
- 任务脚本最后一次复验从已完成 B/1 状态进入详情页，以避免重复检出产生无关 C/1；该复验结果为 `PASS`。

## Closeout Status

`ready_for_closeout`

业务验证已完成；cleanup preview/apply 已通过并仅清理本任务临时产物。工作区存在大量并行改动，本轮未执行 Git commit/push，任务按规则保持 `ready_for_closeout`。

## 2026-09-19 Rerun Result

`PASS - E2E business flow`

按用户当轮要求已重新完整跑一遍大版本升级真实页面流程。本轮从既有当前有效版 `B/1` 开始，经页面检出、选择“大版本”检入、上传新版源文件、提交审批、三次审批中心审核、最终文控签名和盖章 PDF 上传，生成并生效 `C/1`。

关键结果：

- 文件编号：`E2E-DCC-20260918234556`
- 来源版本：`B/1`
- 新大版本：`C/1`
- 新文件 ID：`2054545668044083987`
- 运行标识：`DCC-MAJOR-20260919073101`
- 前端/后端：`http://127.0.0.1:8061` / `48061`

最终页面证据：

- 详情页显示 `当前有效版 / ACTIVE / C/1`。
- 版本历史显示 `C/1` 当前有效，`B/1` 和 `A/1` 均为已替代。
- 版本历史显示本轮变更说明 `DCC-MAJOR-20260919073101 大版本升级正文变更`。
- 生命周期显示 `C/1` 受控副本已生成、正式生效、审批流程已完成。
- 受控浏览默认当前有效版本为 `C/1`，发布文件和盖章文件均为已生成。

请求证据见 `artifacts/real-e2e-result.json`，这些请求均为真实前端页面操作时由浏览器自然发起的被动监听结果，不是脚本直接调用 API。最终截图见 `artifacts/99-final-detail.png`。自动发布完成后已无额外生效申请待办；脚本中间记录的第二次待办轮询 `BLOCKED processed=0` 表示无待办可处理，最终 `final-verification` 与 `complete` 均为 `PASS`。

本轮未使用 `fetch`、Playwright `APIRequestContext`、后端 HTTP 客户端、SQL/数据库写入或任何脚本直调 API 代替页面操作；所有业务动作均在前端页面空间实际完成。
