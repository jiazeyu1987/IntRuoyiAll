# 任务：修复测试发布脚本 Website 路径并完成发布

## Goal

修复 `publish-int-ruoyi-to-test.ps1` 中展厅前台 `Website` 仓库定位错误导致的测试服务器发布失败问题，并在修复后完成一次包含数据库与对象存储同步的测试环境发布。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.ps1`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-publish-test-server-website-path-fix\**`

## Non-Scope

- 不修改业务逻辑
- 不新增 fallback 发布路径
- 不改动测试服务器部署架构

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-automation-submit\task.md`
- Status before this task: `Completed`
- Impact: 上一同仓任务已完成，不阻塞本次发布脚本修复。

## Milestones

1. 记录发布失败的真实前置条件问题。
2. 最小修复测试发布脚本中的 `Website` 路径定位。
3. 重新执行测试服务器发布并验证远端状态。
4. 记录结果并为提交/收尾保留证据。

## Expected Verification

- `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q`
- `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\show-int-ruoyi-remote-status.ps1 -ServerHost 172.30.30.58 -RemoteAppDir /opt/intruoyi/runtime`

## Current Status

- `completed`

## Final Result

- 已修复测试发布脚本中的两个真实路径问题：
  - `Website` 仓库路径从错误的 `D:\ProjectPackage\Int\Website` 改为正确的 `D:\ProjectPackage\Website`
  - 删除远端 `website` 目录后，补充重建目录再执行 `scp -r`
- 修复后发布成功，测试服务器正式环境切换到标签 `20260521_184319`
