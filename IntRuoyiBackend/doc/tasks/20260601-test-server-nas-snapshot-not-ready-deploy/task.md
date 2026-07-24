# 任务：发布 NAS 权限快照未就绪修复到测试服

## 任务目标

将已提交的 NAS 权限快照未就绪修复发布到测试服务器 `172.30.30.58`，让 NAS 管理转移任务在权限快照尚未生成时显示未采集状态，而不是继续提示 `DCC NAS permission snapshot is not ready`。

## 前序任务检查

- 后端修复任务 `doc/tasks/20260601-nas-transfer-permission-snapshot-ready/task.md` 当前为 `completed`，提交 `a1cc2b3948`。
- 前端修复任务 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260601-nas-transfer-permission-snapshot-ready\task.md` 当前为 `completed`，提交 `d8606ffe1`。
- 本任务只操作测试服务器，不操作正式服。

## BDD 场景

BDD: 测试服加载 NAS 权限快照未就绪修复 -> Given 修复已在本机前后端仓库提交 / When 按标准发布流程部署到测试服务器 / Then 测试服后端和前端运行版本包含修复，NAS 权限快照摘要未采集状态不再返回接口错误。

BDD: 测试服发布必须保留 fail-fast 恢复边界 -> Given 权限快照未完成 / When 用户仅查看转移任务权限状态 / Then 页面不得自动调用恢复预览；When 用户显式恢复且缺少真实快照 / Then 后端仍必须返回未就绪错误。

## 里程碑

- [x] M1：建立任务文档并记录发布目标。
- [ ] M2：检查发布脚本、测试服状态和当前本地提交。
- [ ] M3：构建包含前后端修复的测试服发布包。
- [ ] M4：部署发布包到测试服务器并重启相关服务。
- [ ] M5：验证测试服接口或页面不再出现自动快照未就绪错误。
- [ ] M6：记录收尾清理与提交本任务文档。

## 预期验证

- 测试服健康检查 `http://172.30.30.58:48081/actuator/health` 返回 `UP`。
- 测试服 NAS 转移任务权限快照摘要接口对已有任务返回 `NOT_COLLECTED` 或真实快照状态，而不是 `DCC_NAS_PERMISSION_SNAPSHOT_NOT_READY`。
- 前端发布产物包含 `NOT_COLLECTED` 与 `isSnapshotReadyForDetail` 门禁逻辑。

## 当前状态

status: blocked

## Current Status

blocked

## 阻塞

用户当前目标已切换为发布包到 NAS、再由 NAS 自动恢复到测试服/正式服的全流程自动化。本任务未完成测试服发布验证，不应继续与新流水线任务混在一起；如需恢复本任务，需要重新授权并按当前发布流水线执行。
