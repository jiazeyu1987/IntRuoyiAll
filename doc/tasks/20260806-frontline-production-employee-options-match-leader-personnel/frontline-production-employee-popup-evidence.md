# 一线生产员工弹窗真实 E2E 证据

- Task ID: 20260806-frontline-production-employee-options-match-leader-personnel
- Generated At: 2026-08-06T09:22:28.534Z
- Status: PASS
- Frontend: http://127.0.0.1:8081
- Backend: http://127.0.0.1:48081
- Tenant/User: 芋道源码 / admin

## Scope

- Real page path: production leader personnel list -> frontline production fill -> employee popup.
- API usage: page-triggered personnel/runtime responses are parsed; read-only authenticated probes preselect a valid frontline process before opening the real page.
- Write boundary: no create/update/delete target business request is issued by this script.

## PASS

- Enabled personnel count: 8
- Disabled personnel count: 0
- Runtime employee count: 8
- Popup option count: 8
- Enabled personnel hash: a7115b13b7357fb2a3691ec6f3b339a11d45f162c6bc8b81e8f9946ad9378e40
- Runtime employee hash: a7115b13b7357fb2a3691ec6f3b339a11d45f162c6bc8b81e8f9946ad9378e40
- Popup option hash: a7115b13b7357fb2a3691ec6f3b339a11d45f162c6bc8b81e8f9946ad9378e40
- Selected process: 922119/928609/922985 粗洗工序
- Personnel screenshot: E:\IntRuoyi\output\playwright\20260806-frontline-production-employee-options-match-leader-personnel\production-personnel-list.png
- Popup screenshot: E:\IntRuoyi\output\playwright\20260806-frontline-production-employee-options-match-leader-personnel\frontline-production-employee-popup.png
- JSON result: E:\IntRuoyi\output\playwright\20260806-frontline-production-employee-options-match-leader-personnel\frontline-production-employee-popup-result.json
