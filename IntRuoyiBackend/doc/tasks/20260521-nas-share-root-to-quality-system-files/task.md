# 任务：调整 NAS 共享根为质量体系文件

## Goal

将当前本地 NAS 管理页所使用的共享名从 `it共享` 切换为 `质量体系文件`，并用真实接口验证：

- `NAS管理` 页显示的共享根变更为 `\\172.30.30.4\质量体系文件`
- `NAS` 测试连接仍然成功
- `NAS` 目录刷新与懒加载仍可正常工作

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-nas-share-root-to-quality-system-files\**`
- 本地运行态 NAS 配置（`/admin-api/infra/file/nas-config`）

## Non-Scope

- 不修改页面结构。
- 不改动权限。
- 不改动 NAS 管理代码逻辑。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-system-nas-management-local-verify\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 当前本地 NAS 管理页已可用，本任务仅切换共享根配置并做运行态验证。

## Milestones

- [x] M1: 创建任务文档并确认前置任务状态。
- [x] M2: 记录 BDD 与 RED，锁定共享根切换契约。
- [x] M3: 更新本地 NAS 配置为 `质量体系文件`。
- [x] M4: 真实接口验证 `nas-config/test` 与 `nas-files`。
- [x] M5: 运行 closeout preview 并完成收尾。

## Expected Verification

- `GET /admin-api/infra/file/nas-config` 返回的 `share` 已变更。
- `POST /admin-api/infra/file/nas-config/test` 返回 `rootPath=\\172.30.30.4\质量体系文件`。
- `GET /admin-api/infra/file/nas-files?path=` 返回新的共享根目录列表。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-nas-share-root-to-quality-system-files --mode preview`

## Current Status

Completed on 2026-05-21. 已切换当前运行态 NAS 配置到 `质量体系文件`，并完成真实接口与页面验证。

## Blockers And Impact

- Blocker: none.
- Impact:
  - `NAS管理` 页当前共享根已变更为 `\\\\172.30.30.4\\质量体系文件`。
  - `nas-config/test` 和 `nas-files` 均已在真实运行态验证通过。

## Final Verification Result

- `PUT http://127.0.0.1:48081/admin-api/infra/file/nas-config` with `share=质量体系文件` -> PASS
- `GET http://127.0.0.1:48081/admin-api/infra/file/nas-config` -> PASS，返回 `share=质量体系文件`
- `POST http://127.0.0.1:48081/admin-api/infra/file/nas-config/test` -> PASS，返回 `rootPath=\\\\172.30.30.4\\质量体系文件`、`itemCount=9`
- `GET http://127.0.0.1:48081/admin-api/infra/file/nas-files?path=` -> PASS，返回 `rootPath=\\\\172.30.30.4\\质量体系文件`
- 真实页面验证 -> PASS，`NAS管理` 页显示 `共享根：\\\\172.30.30.4\\质量体系文件`
- `python C:\\Users\\BJB110\\.codex\\skills\\task-closeout-cleanup\\scripts\\task_closeout.py --workspace D:\\ProjectPackage\\Int\\IntRuoyi\\ruoyi-vue-pro --task-id 20260521-nas-share-root-to-quality-system-files --mode preview` -> READY
