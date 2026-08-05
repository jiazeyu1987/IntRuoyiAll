# 岗位需求分解矩阵真实 E2E 前置证据

- Task ID: `20260801-role-requirement-matrix-implementation`
- Generated At: `2026-08-05T03:21:39.774Z`
- Status: `BLOCKED`
- Frontend: `--`
- Backend: `--`
- Tenant: `--`
- Data Prefix: `RRM-20260801-`

## Result

- BLOCKED: 35 blockers remain.
- ENV:RRM_FRONTEND_URL -> 真实前端入口，例如 http://127.0.0.1:8081。
- ENV:RRM_BACKEND_URL -> 真实后端入口，例如 http://127.0.0.1:48081。
- ENV:RRM_TENANT -> 任务专用非生产测试租户。
- ENV:RRM_PRODUCTION_EMPLOYEE_USERNAME -> 生产员工账号标签。
- ENV:RRM_PRODUCTION_EMPLOYEE_PASSWORD -> 生产员工密码，通过环境变量注入，不写入证据。
- ENV:RRM_PRODUCTION_LEADER_USERNAME -> 生产组长账号标签。
- ENV:RRM_PRODUCTION_LEADER_PASSWORD -> 生产组长密码，通过环境变量注入，不写入证据。
- ENV:RRM_QA_USERNAME -> QA 账号标签。
- ENV:RRM_QA_PASSWORD -> QA 密码，通过环境变量注入，不写入证据。
- ENV:RRM_PQC_INSPECTOR_USERNAME -> PQC 检验员账号标签。
- ENV:RRM_PQC_INSPECTOR_PASSWORD -> PQC 检验员密码，通过环境变量注入，不写入证据。
- ENV:RRM_PQC_LEADER_USERNAME -> PQC 组长账号标签。
- ENV:RRM_PQC_LEADER_PASSWORD -> PQC 组长密码，通过环境变量注入，不写入证据。
- ENV:RRM_RELEASE_OWNER_USERNAME -> 放行负责人账号标签。
- ENV:RRM_RELEASE_OWNER_PASSWORD -> 放行负责人密码，通过环境变量注入，不写入证据。
- ENV:RRM_UNAUTHORIZED_USERNAME -> 错误角色账号标签，用于证明活跃订单写入权限隔离。
- ENV:RRM_UNAUTHORIZED_PASSWORD -> 错误角色密码，通过环境变量注入，不写入证据。
- ENV:RRM_SIGNATURE_IDS_JSON -> 六类角色正式电子签名 ID 映射 JSON。
- ENV:RRM_PRODUCTION_ORDER_ID -> 任务专用 ERP/MES 生产订单 ID。
- ENV:RRM_PRODUCTION_ORDER_CODE -> 任务专用生产订单编码，建议以 RRM-20260801- 开头。
- ENV:RRM_ROUTE_ID -> 正式工艺路线 ID。
- ENV:RRM_ROUTE_VERSION_ID -> 正式工艺路线版本 ID。
- ENV:RRM_ROUTE_PROCESS_ID_1 -> 系数 1.0 的正式路线工序 ID。
- ENV:RRM_ROUTE_PROCESS_ID_2 -> 系数 3.0 的正式路线工序 ID。
- ENV:RRM_TRANSFER_IDS -> 任务专用调拨/发货/补料/退料正式 ID 列表。
- ENV:RRM_BATCH_RECORD_REPORT_ID -> 正式逐工序批记录报表 ID。
- ENV:RRM_QA_REGULATION_VERSION_ID -> 已发布 QA 规程版本 ID。
- ENV:RRM_PRODUCTION_ORDER_ID -> RRM_PRODUCTION_ORDER_ID 必须是大于 0 的正式业务 ID。
- ENV:RRM_ROUTE_ID -> RRM_ROUTE_ID 必须是大于 0 的正式业务 ID。
- ENV:RRM_ROUTE_VERSION_ID -> RRM_ROUTE_VERSION_ID 必须是大于 0 的正式业务 ID。
- ENV:RRM_ROUTE_PROCESS_ID_1 -> RRM_ROUTE_PROCESS_ID_1 必须是大于 0 的正式业务 ID。
- ENV:RRM_ROUTE_PROCESS_ID_2 -> RRM_ROUTE_PROCESS_ID_2 必须是大于 0 的正式业务 ID。
- ENV:RRM_FRONTEND_URL/RRM_BACKEND_URL -> 前后端 URL 必须成对使用 int_main 8081/48081，或同一 worktree slot 的 8082-8100/48082-48100。
- ENV:RRM_SIGNATURE_IDS_JSON.pqcInspector -> PQC 正式提交必须从 RRM_SIGNATURE_IDS_JSON.pqcInspector 读取大于 0 的正式电子签名 ID。
- ENV:RRM_TRANSFER_IDS -> RRM_TRANSFER_IDS 必须提供至少一个大于 0 的正式调拨/发货/补料/退料 ID。
