# DCC 文控签核追溯 E2E 验证报告

## Status

E2E BLOCKED

## Scope

- 场景：DCC 文控“签核追溯”真实页面验证。
- 环境：本机 `int_main`，前端 `http://127.0.0.1:8081`，后端 `http://127.0.0.1:48081`。
- 边界：只验证本场景；不修其它场景；不使用 admin；不使用 API-only、SQL 改状态或历史结果冒充本轮页面通过。

## Preflight Matrix

| Item | Result | Evidence |
| --- | --- | --- |
| 前端运行态 | PASS | `http://127.0.0.1:8081/` 返回 HTTP 200 |
| 后端运行态 | PASS | `http://127.0.0.1:48081/actuator/health` 返回 `status=UP` |
| 端口归属 | PASS | 8081 属于 `E:\IntRuoyi\IntRuoyiFronted` Vite；48081 属于 `E:\IntRuoyi\output\runtime\int_main` 后端运行 Jar |
| Playwright 浏览器 | PASS | 本机 Chrome 可执行文件存在 |
| 非 admin 账号准备 | PASS | 既有前置记录包含 `pengyunfeng`、`zhaohaichen`、`zhaojie`、`zhaomingyu`、`wangsiyu` 五个非 admin 账号 |
| 可复用任务自有文件 | PASS | 原版文件 `2054545668044070262`、升版发布文件 `2054545668044070261` 可作为候选追溯对象 |
| 非 admin 密码环境变量 | BLOCKED | 当前环境未注入 `DCC_E2E_PASSWORD` 或等价本场景密码变量 |

## Required Traceability Data

| Field | Current Evidence |
| --- | --- |
| 文件 ID | 候选：`2054545668044070262` 原版；`2054545668044070261` 升版 V2 |
| 版本 | 候选：V1.0 原版；V2.0 升版发布 |
| 上传人 | 候选：`pengyunfeng` |
| 审批人列表 | 候选：`zhaohaichen`、`zhaojie`、`zhaomingyu`、`wangsiyu` |
| 签名人列表 | BLOCKED，未登录页面核验 |
| 证据 ID | BLOCKED，未打开页面且未做最终只读核验 |
| 文件 hash / stampedFileId / publishedFileId | BLOCKED，未打开页面且未做最终只读核验 |
| 页面路径 | 预期从受控文件详情页进入签核追溯/审批记录/操作日志/版本历史；本轮未登录，未产生页面路径证据 |

## RED

- RED: password-env precheck -> FAIL，`DCC_E2E_PASSWORD` 或等价本场景非 admin 密码环境变量未注入。

## Impact

- 无法登录上传人账号确认上传记录存在。
- 无法登录审批/签名账号逐节点处理真实审批和电子签名。
- 无法登录 DCC/QA/文控查看账号进入受控文件详情页。
- 无法通过页面证明“谁上传、谁审核、谁签名”可见。
- 无法证明页面签名证据、文件 hash、盖章文件、发布文件与只读后端数据一致。
- 无法验证导出/打印追溯记录入口和内容字段。
- 无法执行缺签名授权或错误签名密码的受控失败诊断。

## Actions Not Taken

- 未使用 admin 账号。
- 未通过 API 或 SQL 制造审批记录、签名记录或文件状态。
- 未用只读 DB/API 结果替代页面追溯验证。
- 未把既有历史 E2E PASS 结果冒充为本轮签核追溯页面 PASS。

## Required Unblock

- 在运行本轮 Playwright 的 shell 中注入 `DCC_E2E_PASSWORD` 或等价本场景非 admin 密码变量。
- 注入后复跑真实页面路径：上传人查看记录、审批/签名人逐节点处理、DCC/QA/文控账号打开受控文件详情和追溯入口，再执行只读 API/DB 一致性核验。

