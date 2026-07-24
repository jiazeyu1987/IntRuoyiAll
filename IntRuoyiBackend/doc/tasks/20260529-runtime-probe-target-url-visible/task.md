# 任务：探针失败告警显示目标地址

## 任务目标

- 优化运行控制台探针失败告警内容，在每条失败明细中显示真实探针目标地址。
- 复用现有 `RuntimeControlProbeRespVO.url` 字段，不改变 API 结构或告警发送链路。
- 保持 fail-fast：探针失败继续暴露真实错误，不用默认成功、mock 地址或 fallback 掩盖。

## BDD 场景

- BDD: 探针失败告警包含目标地址 -> Given 探针结果中存在 NO_GO 或 BLOCKED 记录且记录包含 `url` / When 后端创建探针失败告警 / Then 告警内容和站内信模板参数必须包含失败探针的真实 URL。
- BDD: 探针目标地址缺失时不伪造地址 -> Given 失败探针没有 `url` / When 后端创建探针失败告警 / Then 告警内容不生成默认 IP 或备用地址，只展示已有错误详情。

## 里程碑

- [x] M1：确认后端旧探针任务已完成，定位探针告警内容拼装点和现有测试。
- [x] M2：补充失败测试，约束告警内容必须包含失败探针目标地址。
- [x] M3：实现告警内容目标地址展示。
- [x] M4：运行后端目标测试和真实探针接口验证。
- [x] M5：记录证据、运行收尾清理预览并提交本任务改动。

## 预期验证

- `mvn -pl yudao-module-infra -Dtest=RuntimeProbeServiceImplTest test`
- 真实 API：`GET http://127.0.0.1:48081/admin-api/infra/runtime-control/probes/latest`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260529-runtime-probe-target-url-visible/backend-api-evidence.md`

## 当前状态

completed

## 当前进展

- 已在探针失败告警明细中追加已有 `RuntimeControlProbeRespVO.url`，格式为 `目标=<url>`。
- 已完成目标单测、关联探针回归和真实 `/probes/latest` 接口只读验证。

## 验证结果

- RED: `mvn -pl yudao-module-infra -Dtest=RuntimeProbeServiceImplTest test` -> FAIL，站内信模板参数缺少 `目标=http://frontend.test/` 与 `目标=http://website.test/`。
- GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeProbeServiceImplTest test` -> PASS。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeProbeServiceImplTest,RuntimeOpsProbeDefaultHttpClientTest" test` -> PASS。
- GREEN: 真实 `GET /admin-api/infra/runtime-control/probes/latest` -> PASS，`local/intruoyi-frontend` URL 为 `http://127.0.0.1:8081/`，`local/website-frontend` URL 为 `http://127.0.0.1:4173/`。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260529-runtime-probe-target-url-visible/backend-api-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260529-runtime-probe-target-url-visible --mode preview` -> PASS，delete/blocked/warnings 均为空。

## Cleanup Keep

- doc/tasks/20260529-runtime-probe-target-url-visible/task.md
- doc/tasks/20260529-runtime-probe-target-url-visible/execution-log.md
- doc/tasks/20260529-runtime-probe-target-url-visible/backend-api-evidence.md
