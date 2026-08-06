# 一线生产员工弹窗真实 E2E 证据

- Task ID: 20260806-frontline-production-employee-options-match-leader-personnel
- Generated At: 2026-08-06T09:04:52.482Z
- Status: FAIL
- Frontend: http://127.0.0.1:8081
- Backend: http://127.0.0.1:48081
- Tenant/User: 芋道源码 / admin

## Scope

- Real page path: production leader personnel list -> frontline production fill -> employee popup.
- API usage: page-triggered personnel/runtime responses are parsed; read-only authenticated probes preselect a valid frontline process before opening the real page.
- Write boundary: no create/update/delete target business request is issued by this script.

## FAIL

- Reason: frontline runtime-config employees do not equal enabled production personnel list
- Missing in runtime: 112, 113, 114, 陈丽, 方王魏, 李业辉, 李之音, 王一林
- Extra in runtime: 刘悦悦
- Missing in popup: --
- Extra in popup: --
- Personnel screenshot: E:\IntRuoyi\output\playwright\20260806-frontline-production-employee-options-match-leader-personnel\production-personnel-list.png
- Popup screenshot: E:\IntRuoyi\output\playwright\20260806-frontline-production-employee-options-match-leader-personnel\frontline-production-employee-popup.png
- Selected process: 922119/928609/922985 粗洗工序
