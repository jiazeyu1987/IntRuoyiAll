# 任务：修复 NAS 转移权限快照未就绪前端提示

## 任务目标

修复 NAS 管理转移任务创建后，权限恢复面板自动加载快照状态时把 `DCC NAS permission snapshot is not ready` 显示为红色接口错误的问题。前端应先展示“未采集”状态，并且只有真实快照存在后才请求目录快照明细、未映射主体和恢复预览。

## 前序任务检查

- 上一任务 `doc/tasks/20260601-showroom-structured-network-error/task.md` 当前为 `completed`。
- 当前仓库存在无关未跟踪任务目录，本任务不纳入。

## BDD 场景

BDD: NAS 转移后权限快照未采集时不显示接口错误 -> Given NAS 转移任务已创建但后端返回 `NOT_COLLECTED` 快照状态 / When NAS 管理页渲染转移结果中的权限恢复面板 / Then 页面显示“未采集”状态，不自动请求明细或恢复预览接口。

BDD: 恢复预览只允许在真实快照完成后执行 -> Given 权限快照状态不是 `CAPTURED` / When 管理员打开恢复抽屉 / Then 前端不得调用恢复预览接口，必须提示快照尚未采集完成。

## 里程碑

- [x] M1：建立任务文档和 BDD 场景。
- [x] M2：补充静态回归测试并记录 RED。
- [x] M3：实现快照摘要优先的前端门禁。
- [x] M4：运行静态合同与类型检查验证。
- [x] M5：提交任务改动。

## 预期验证

- RED：`node tests/e2e/dcc-nas-permission-restore-static.spec.js` 在缺少 `NOT_COLLECTED` 和明细门禁时失败。
- GREEN：`node tests/e2e/dcc-nas-permission-restore-static.spec.js` 通过。
- REGRESSION：`pnpm ts:check` 或等价增加 Node heap 的类型检查通过。

## 当前状态

status: completed

## Current Status

completed

## 最终验证

- RED：`node tests/e2e/dcc-nas-permission-restore-static.spec.js` -> FAIL，当前权限恢复面板不识别 `NOT_COLLECTED`，且恢复抽屉并发请求摘要、明细和未映射主体。
- GREEN：`node tests/e2e/dcc-nas-permission-restore-static.spec.js` -> PASS。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。默认 `pnpm ts:check` 先因 Node 约 4GB heap OOM 退出，增加 heap 后同一类型检查通过。
- GREEN：task-closeout-cleanup preview -> PASS，delete `<none>`、blocked `<none>`。
- RED：task-closeout-cleanup apply -> BLOCKED，脚本未识别中文状态段落中的完成状态，已补充 `Current Status: completed`。
- GREEN：task-closeout-cleanup apply -> PASS，delete `<none>`、blocked `<none>`。

## 阻塞

None.
