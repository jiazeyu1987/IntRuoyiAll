# 任务：定位 NAS 中的 1.QMS documents / 2.DHF / 3.DMR 文件夹

## Goal

确认当前机器可访问范围内，是否能够定位截图中的三个文件夹：

- `1.QMS documents`
- `2.DHF`
- `3.DMR`

并给出可复用的实际路径证据；如果当前机器无法访问对应 NAS 或缺少挂载/UNC 路径，则明确报告阻塞。

## Scope

- 当前机器的文件系统映射信息、SMB/NAS 连接信息
- 当前机器可访问的本地盘符与已建立的网络映射
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-nas-folder-locate-qms-dhf-dmr\**`

## Non-Scope

- 不修改后端代码、前端代码、数据库、配置或权限。
- 不伪造 NAS 路径，不用 mock 成功或占位路径替代真实定位结果。
- 不在没有路径前提的情况下声称已定位到真实 NAS 目录。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-infra-nas-directory-read-api\task.md`
- Status before this task: `Completed with blockers on 2026-05-20`
- Impact on this task: 上一任务已补齐 NAS 目录读取接口，但明确记录了“当前机器未发现可直接访问的真实 NAS 挂载盘或 net use 映射”；本次继续做只读定位排查，不修改上一任务产物。

## Milestones

- [x] M1: 确认上一同仓任务状态并创建本任务文档。
- [x] M2: 检查当前机器可用的盘符、网络映射和 SMB 连接线索。
- [x] M3: 按 `1.QMS documents`、`2.DHF`、`3.DMR` 三个目录名搜索当前机器可访问路径。
- [x] M4: 记录定位结果或阻塞结论，并完成收尾。

## Expected Verification

- `Get-PSDrive -PSProvider FileSystem`
- `net use`
- `Get-SmbMapping` / `Get-SmbConnection`（若系统支持）
- 按三个目录名进行精确目录搜索

## Current Status

Blocked on 2026-05-20. 已确认当前机器没有活跃的 NAS 映射盘或 `net use` 连接；在缺少具体 UNC / 共享根路径的前提下，无法可靠定位截图中的三个文件夹。

## Blockers And Impact

- Blocker:
  - `Get-PSDrive -PSProvider FileSystem` 仅发现 `C:`、`D:`。
  - `net use` 无网络映射。
  - `Get-SmbConnection` 被系统权限拒绝，无法补出活动远端共享信息。
  - 注册表 `MountPoints2` 未提供可用的 NAS/UNC 共享线索；其中 `E` 更像历史移动盘，非当前可用 NAS 映射。
  - 对 `C:\` / `D:\` 以及 `C:\Users\BJB110`、`D:\ProjectPackage` 做按目录名精确递归搜索时超时，且由于没有共享根路径，无法把搜索范围缩到真实 NAS。
- Impact:
  - 目前不能基于证据声称已定位到 `1.QMS documents`、`2.DHF`、`3.DMR` 这三个文件夹。
  - 若用户提供 NAS 根路径、UNC 路径或重新挂载网络盘，可继续定向定位。

## Final Verification Result

- `Get-PSDrive -PSProvider FileSystem` -> PASS，结果仅有 `C:`、`D:`。
- `net use` -> PASS，结果为空。
- `Get-SmbMapping` -> PASS，未返回活跃映射。
- `Get-SmbConnection` -> BLOCKED，系统返回 `Access is denied`。
- `reg query "HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer\MountPoints2\E" /s` -> PASS，仅显示历史 `E:\autorun.ico` 与 `64 GB` 标签，像移动盘而非 NAS。
- 对三个目录名在可见盘符上的精确递归搜索 -> BLOCKED，搜索超时，且在缺少共享根路径时不能可靠收敛到真实 NAS。
