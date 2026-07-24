# 20260611 生产工单一键创建金蝶生产订单前端执行日志

## BDD 场景

BDD: 行操作创建 ERP 订单 -> Given 用户拥有 `mes:pro-work-order:create-erp` 权限且生产工单满足后端校验 / When 用户点击行内“创建ERP订单”并确认 / Then 前端调用 `/mes/pro/work-order/{id}/create-kingdee-production-order`，展示 ERP 单号成功提示并刷新列表。

BDD: 后端失败直接暴露 -> Given 后端因重复、缺配置或金蝶错误返回失败 / When 用户点击“创建ERP订单” / Then 前端不吞异常，不显示默认成功。

## 执行证据

- 2026-06-11：读取生产工单列表现状，确认已有用户改动移除了本地新增/编辑/删除/冻结/完成/取消等行操作，本次只增量加入“创建ERP订单”。
- RED: `node tests/e2e/workorder-create-erp-order-static.spec.js` -> FAIL，生产工单 API 与列表尚未暴露 `createKingdeeProductionOrder` 和“创建ERP订单”按钮。
- GREEN: `.\restart-ruoyi-frontend.bat` -> PASS，Vite 前端启动到 `http://localhost:8081`。
- RED: `curl.exe --max-time 20 --head http://127.0.0.1:8081/` -> FAIL，首次启动后的 8081 端口监听但 HTTP 无响应，请求超时。
- GREEN: 第二次执行 `.\restart-ruoyi-frontend.bat` -> PASS，清理卡住的 Vite 进程并重新启动。
- GREEN: `curl.exe --fail --silent --show-error --max-time 15 --head http://127.0.0.1:8081/` -> PASS，返回 `HTTP/1.1 200 OK`。
- GREEN: `curl.exe --fail --silent --show-error --max-time 15 http://127.0.0.1:8081/` -> PASS，返回首页 HTML，标题为“瑛泰管理系统”。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260611-kingdee-production-order-create --mode preview` -> PASS，前端 `keep` 包含 task、execution-log，`delete/blocked/warnings` 均为 `<none>`。
- GREEN: `node tests/e2e/workorder-create-erp-order-static.spec.js` -> PASS，生产工单 API、按钮、权限、ERP 配置模板字段和错误暴露契约均通过。
- GREEN: `node tests/e2e/workorder-erp-code-static.spec.js` -> PASS，生产工单列表仍保持 Kingdee 同步入口且不恢复本地新增入口。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS，前端类型检查通过。
- RED: Playwright 配置页临时脚本使用 `/erp/config` -> FAIL，动态路由 404；数据库菜单确认真实路径为 `/erp/kingdee-config`。
- GREEN: Playwright 配置页真实路径 `/erp/kingdee-config` -> PASS，测试租户 `aoteman` 在“生产同步”页签填写模板单号 `881MO090756` 并保存，`/erp/kingdee-config/save` 返回 `code=0`。
- GREEN: Playwright 生产工单列表真实路径 -> PASS，行操作“创建ERP订单”可见，点击并确认后接口返回 `erpBillNo=CODexERP20260610E`、`erpFid=310120`、`saved=true`、`submitted=true`。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260611-kingdee-production-order-create --mode preview` -> PASS，前端任务目录 `delete/blocked/warnings` 均为 `<none>`。
