# 任务：修复本机展厅发布读回缺少 origin 配置（后端回归）

## 任务目标

补充 `script/tests/test_restart_ruoyi_script.py` 回归测试，确保根目录 `restart-ruoyi.bat` 启动本机后端时显式传入 `showroom.release.public-website-origin`。

## 前序任务检查

- 已确认上一后端任务 `doc/tasks/20260601-backup-server-publish-260530-001131/task.md` 状态为 blocked，阻塞原因是备用服务器 MySQL 镜像 CPU 指令集不满足，与本机展厅发布读回配置修复无关。
- 当前后端仓库存在无关未跟踪 `runtime/`，本任务不触碰、不提交。

## BDD 场景

- BDD: 根目录重启脚本注入发布读回 origin -> Given 使用 `restart-ruoyi.bat` 启动本机后端 / When 执行展厅发布 / Then 后端具备 `showroom.release.public-website-origin`，不会因缺配置失败。
- BDD: 本机读回使用后端公开发布 API -> Given 本机发布读回在同一机器执行 / When 配置 public readback origin / Then 指向 `http://127.0.0.1:48081` 对应的后端公开发布 API。

## 里程碑

- [x] M1：建立任务文档。
- [x] M2：补充 RED 回归测试。
- [x] M3：配合根目录脚本最小修复。
- [x] M4：运行 GREEN 和证据校验。
- [x] M5：收尾清理并提交本任务相关改动。

## 预期验证

- RED：`python -m pytest script/tests/test_restart_ruoyi_script.py -q` 先失败。
- GREEN：`python -m pytest script/tests/test_restart_ruoyi_script.py -q` 通过。

## Current Status

completed

## 当前状态

status: completed

## 根因

根目录 `restart-ruoyi.bat` 未注入 `showroom.release.public-website-origin`，导致本机展厅发布读回校验缺配置失败。

## 完成工作

- `script/tests/test_restart_ruoyi_script.py` 增加回归测试，要求 `restart-ruoyi.bat` 定义本机读回 origin 并传给后端。
- 根目录脚本修复在根仓库提交。

## 最终验证

- RED：`python -m pytest script/tests/test_restart_ruoyi_script.py -q` -> FAIL，新增断言发现缺少读回 origin。
- GREEN：`python -m pytest script/tests/test_restart_ruoyi_script.py -q` -> PASS，3 passed。
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260601-local-showroom-release-readback-origin/bug-regression-evidence.md` -> PASS。

## Cleanup Candidates

- doc/tasks/20260601-local-showroom-release-readback-origin/bug-regression-evidence.md
