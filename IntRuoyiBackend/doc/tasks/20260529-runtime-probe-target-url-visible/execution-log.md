# 执行日志：探针失败告警显示目标地址

BDD: 探针失败告警包含目标地址 -> Given 探针结果中存在 NO_GO 或 BLOCKED 记录且记录包含 `url` / When 后端创建探针失败告警 / Then 告警内容和站内信模板参数必须包含失败探针的真实 URL。

BDD: 探针目标地址缺失时不伪造地址 -> Given 失败探针没有 `url` / When 后端创建探针失败告警 / Then 告警内容不生成默认 IP 或备用地址，只展示已有错误详情。

## 证据

- M1: 已确认 `RuntimeOpsProbeServiceImpl` 的失败告警内容由 `buildFailureContent` 拼装，当前只包含环境、组件、状态和错误详情。
- RED: `mvn -pl yudao-module-infra -Dtest=RuntimeProbeServiceImplTest test` -> FAIL，站内信模板参数缺少 `目标=http://frontend.test/` 与 `目标=http://website.test/`。
- M3: 已在失败告警明细中追加已有探针 URL，格式为 `目标=<url>`；`url` 为空时不生成默认地址。
- GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeProbeServiceImplTest test` -> PASS。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeProbeServiceImplTest,RuntimeOpsProbeDefaultHttpClientTest" test` -> PASS。
- GREEN: 真实 `GET /admin-api/infra/runtime-control/probes/latest` -> PASS，`local/intruoyi-frontend` URL 为 `http://127.0.0.1:8081/`，`local/website-frontend` URL 为 `http://127.0.0.1:4173/`。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260529-runtime-probe-target-url-visible/backend-api-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260529-runtime-probe-target-url-visible --mode preview` -> PASS，delete/blocked/warnings 均为空。
