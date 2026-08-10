# Execution Log

## User Intent

- 用户要求在新增几个产品时，按压式扩张压力泵和球囊扩张压力泵的活跃订单能够加入并显示。
- 附图显示入口为“生产组长 > 活跃订单池”，当前列表为空，页面存在“球囊扩张压力泵方案”“按压式球囊扩张压力泵方案”和“新增活跃订单”入口。

## Rule Reads

- 已读取 `docs/task-closeout-rules.md`。
- 已读取 `docs/experience-index.md`，命中 MES 活跃订单、压力泵和生产组长负责路线相关门禁。
- 已读取 `docs/frontend-development.md`。
- 已读取 `docs/backend-development.md`。
- 已读取 `frontend-feature-delivery` 和 `backend-api-delivery` 技能及其 evidence contract。

## BDD Scenarios

- BDD: 压力泵生产订单加入活跃订单池 -> Given 生产组长正式负责按压式扩张压力泵和球囊扩张压力泵路线，且相关产品生产订单已确认并有正式路线绑定；When 在活跃订单池新增多个相关产品订单；Then 候选允许加入，列表展示活跃池ID、生产订单ID、路线名称、版本号、ERP生产数量和加入时间。
- BDD: 路线来源缺失必须失败 -> Given 目标产品缺少唯一正式路线绑定或缺少唯一 ACTIVE 发布版本；When 搜索或加入活跃订单；Then 接口返回明确业务错误，不使用默认路线或空成功。

## Work Log

- in_progress: 创建任务记录并开始定位前后端活跃订单链路。
- RED: `node doc/tasks/20260808-pressure-pump-active-orders/add-pressure-pump-active-orders.e2e.cjs` -> FAIL，活跃订单列表接口返回 `1040501000 工艺路线不存在`；只读 DB 诊断发现 activeOrderId `41..45` 为 `CODX-PQC-20260807-SP` 任务残留，指向已删除路线 `980094`。
- GREEN: 正式移除接口 `/mes/pro/process-pool/team-leader/active-order/remove` -> PASS，activeOrderId `41..45` 全部置为 `REMOVED`，列表接口恢复返回 5 条既有按压式球囊扩充压力泵 ACTIVE 活跃订单。
- RED: 真实前端新增脚本 -> FAIL，球囊扩张压力泵候选被 `产品正式工艺路线绑定不唯一`、`缺少已发布QA规程`、`QA规程发布版本缺少末检适用性配置` 等正式校验拦截。
- GREEN: DB 事务修复孤儿产品路线绑定 -> PASS，软删除 `mes_pro_route_product` `922291..922293`，这些绑定均指向已删除 E2E 路线 `922138`；计数 `target=3 affected=3 remaining=0 COMMIT`。
- GREEN: DB 事务补齐球囊产品 `902149` 的正式 QA 前置 -> PASS，从同产品 V21 已发布 QA 规程复制到当前 ACTIVE V27 发布快照，新增 14 条规程、14 条版本、78 条项目、32 条设备绑定，并对齐发布快照 `routeProcessId=980645..980658`。
- GREEN: QA 固定数量修正 -> PASS，V27 FINAL 项固定数量补为 3，混用 FIRST 项按同工序最大固定数量收敛；候选不再因固定数量校验失败。
- GREEN: `node doc/tasks/20260808-pressure-pump-active-orders/add-pressure-pump-active-orders.e2e.cjs` -> PASS，真实页面新增 `PQC-E2E-FS-20260804` activeOrderId `48` 和 `881MO090889` activeOrderId `49`，最终活跃订单池 7 条。
- GREEN: DB verification -> PASS，activeOrderId `48`/`49` 均为 ACTIVE，路线 `922119` / V27；每单 14 条工序快照、56 条 PQC 任务，业务日期均为 `2026-08-08`。
- EXPERIENCE: 已更新 `docs/backend-development.md#零排产活跃订单必须使用发布态正式路线` 与 `docs/experience-index.md`，补充孤儿 route-product 绑定、发布快照 routeProcessId、QA 规程末检适用性和固定数量门禁。
- GREEN: `rg -n "发布快照 routeProcessId|固定检验数量无效|孤儿路线绑定" docs\backend-development.md docs\experience-index.md doc\tasks\20260808-pressure-pump-active-orders` -> PASS。
- GREEN: `git diff --check -- docs\backend-development.md docs\experience-index.md doc\tasks\20260808-pressure-pump-active-orders` -> PASS；仅有 Git CRLF 提示，无 whitespace error。
- CLEANUP PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-pressure-pump-active-orders --mode preview` -> PASS，blocked/warnings 均为 `<none>`。
- CLEANUP APPLY: 同命令 `--mode apply` -> PASS，删除一次性 Playwright 脚本和 artifacts，保留三份核心记录。
- FINAL STATUS: completed；未执行 Git stage、commit、merge 或 push。

## Verification Evidence

- 详见 `verification-report.md`。

## Blockers

- 无当前阻塞。历史工单 `RRM-20260801-PP-MO-001` 仍因排产工序缺少 `plan_date` 不可加入；未推断填充计划日期。
