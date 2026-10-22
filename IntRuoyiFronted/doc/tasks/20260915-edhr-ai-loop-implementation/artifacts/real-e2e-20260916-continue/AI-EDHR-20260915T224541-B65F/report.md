# eDHR AI E2E AI-EDHR-20260915T224541-B65F

- 状态：FAIL
- 失败阶段：PREPARE
- 错误类型：BUSINESS_ASSERTION
- 动作：PREPARE确定性ERP生产订单创建
- 页面：http://127.0.0.1:8081/mes/pro/work-order
- Trace：trace.zip
- 截图：PREPARE.png
- 消息：创建 O01 业务失败：当前生产工单创建 ERP 生产订单缺少必要数据：ERP生产订单模板计量单位编码

1040502016 !== 0


## 期望值

```json
{
  "workOrderCode": "AI-EDHR-20260915T224541-B65F-O01",
  "batchCode": "AI-EDHR-20260915T224541-B65F-B01",
  "quantity": 100
}
```

## 实际值

```json
{
  "message": "创建 O01 业务失败：当前生产工单创建 ERP 生产订单缺少必要数据：ERP生产订单模板计量单位编码\n\n1040502016 !== 0\n"
}
```

## 阶段结果

| 阶段 | 状态 |
|---|---|
| S01 | BLOCKED |
| S02 | BLOCKED |
| S03 | BLOCKED |
| S04 | BLOCKED |
| S05 | BLOCKED |
| S06 | BLOCKED |
| S07 | BLOCKED |
| S08 | BLOCKED |

## 页面请求证据

| 方法 | URL | HTTP | 业务码 |
|---|---|---:|---:|
| GET | /admin-api/system/tenant/get-by-website | 200 | 0 |
| GET | /admin-api/system/tenant/get-by-website | 200 | 0 |
| GET | /admin-api/system/tenant/get-id-by-name | 200 | 0 |
| GET | /admin-api/system/tenant/get-id-by-name | 200 | 0 |
| POST | /admin-api/system/auth/login | 200 | 0 |
| POST | /admin-api/system/auth/login | 200 | 0 |
| GET | /admin-api/system/dict-data/simple-list | 200 | 0 |
| GET | /admin-api/system/dict-data/simple-list | 200 | 0 |
| GET | /admin-api/system/auth/get-permission-info | 200 | 0 |
| GET | /admin-api/system/auth/get-permission-info | 200 | 0 |
| GET | /admin-api/mes/pro/edhr-work-task/my-page | 200 | -- |
| GET | /admin-api/dcc/distribution-tasks/my-page | 200 | -- |
| GET | /admin-api/dcc/training-tasks/my-page | 200 | -- |
| GET | /admin-api/dcc/training-tasks/my-page | 200 | -- |
| GET | /admin-api/mes/pro/work-order/page | 200 | -- |
| GET | /admin-api/approval-center/tasks/page | 200 | -- |
| GET | /admin-api/system/auth/get-permission-info | 200 | 0 |
| GET | /admin-api/mes/pro/edhr-work-task/my-page | 200 | 0 |
| GET | /admin-api/dcc/training-tasks/my-page | 200 | 0 |
| GET | /admin-api/dcc/training-tasks/my-page | 200 | 0 |
| GET | /admin-api/dcc/distribution-tasks/my-page | 200 | 0 |
| GET | /admin-api/mes/pro/work-order/page | 200 | 0 |
| GET | /admin-api/system/user-table-column-config/get | 200 | 0 |
| GET | /admin-api/showroom/assignment/page | 200 | 0 |
| GET | /admin-api/system/notify-message/get-unread-count | 200 | 0 |
| GET | /admin-api/approval-center/tasks/page | 200 | 0 |
| GET | /admin-api/mes/pro/work-order/page | 200 | 0 |
| POST | /admin-api/mes/pro/work-order/create-ai-e2e-production-order | 200 | 1040502016 |

## 候选代码路径

- IntRuoyiFronted/tests/e2e/edhr-ai-loop/runner.cjs
- IntRuoyiFronted/tests/e2e/edhr-ai-loop/reporter.cjs
