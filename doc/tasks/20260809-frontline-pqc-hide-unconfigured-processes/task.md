# 一线 PQC 仅展示正式 QA 工序

## Task Goal

修复球囊扩张压力泵一线 PQC 工序列表显示“粗洗工序”的问题，使列表只来自当前产品、当前路线发布版本中正式 QA 配置覆盖的工序，并清除本地历史任务复制出的不一致规程与待检任务。

## Milestones

- [x] M1：确认页面现象、正式 QA 页面内容和一线 PQC 数据来源。
- [x] M2：补充 BDD 场景与 RED 回归测试，锁定未配置工序不得进入列表。
- [x] M3：实现正式 QA 工序映射与数据修复，不使用名称猜测或测试夹具兜底。
- [x] M4：完成目标测试、相关回归与真实页面验证。
- [x] M5：完成验证记录、经验沉淀和任务清理。

## Expected Verification

- 后端回归测试证明未被正式 QA 配置覆盖的路线工序不会进入一线 PQC 工序列表。
- 球囊扩张压力泵正式 QA 工序数据不包含“粗洗工序”，且一线 PQC 页面不再显示“粗洗工序”。
- 已有正式 QA 工序仍能展示对应待检项目，不由 `formBindings`、默认值或测试夹具补齐。
- `bug-regression-fix-loop` 证据校验、相关 Maven/前端静态检查和任务 cleanup 通过。

## Applicable Experience Gates

- 命中 `docs/backend-development.md#MES PQC 项目级检验快照门禁`：一线 PQC 必须消费发布 QA 规程项目级快照；缺少正式工序/项目映射时必须阻塞，禁止由路线全集、旧版本或测试夹具推断。
- 命中零排产活跃订单 QA 门禁：产品、路线、发布版本、路线工序和 QA 规程身份必须一致；禁止复制旧版本后仅替换外层路线工序 ID。
- 当前工作区存在大量并行改动，本任务只修改明确归属的源码、测试、任务文档和精确目标数据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；统一正式 QA 工序配置与一线 PQC 消费来源，并修复污染数据。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed：源码修复、精确数据退役、目标回归、真实页面验证、经验沉淀和任务清理均已完成。

## Cleanup Result

- `task-closeout-cleanup` preview/apply 通过，仅删除本任务的一次性证据、SQL、Playwright 脚本和截图。
- 保留 `task.md`、`execution-log.md` 和 `verification-report.md`；未清理其他任务或共享工作区产物。
