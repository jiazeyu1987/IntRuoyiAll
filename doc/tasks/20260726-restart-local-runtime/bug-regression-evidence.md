# Bug Regression Evidence

## Bug Summary

本地重启脚本 `restart-int-ruoyi-local.ps1` 通过 `worktree-port-map.ps1` 解析 `int_main` 前端根目录时，仍使用旧路径 `E:\IntRuoyi\yudao-ui-admin-vue3`，导致当前项目实际前端目录 `E:\IntRuoyi\IntRuoyiFronted` 无法重启。

## Expected Behavior

`int_main` 本地运行态应使用 `E:\IntRuoyi\IntRuoyiFronted`，前端端口保持 `8081`，后端端口保持 `48081`。

## Reproduction

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full
```

实际失败：`Missing int_main frontend path: E:\IntRuoyi\yudao-ui-admin-vue3`。

## Root Cause

`IntRuoyiBackend\script\deploy\worktree-port-map.ps1` 中前端仓库目录名仍硬编码为 `yudao-ui-admin-vue3`，与当前项目规则中的 `IntRuoyiFronted` 不一致。

## Regression Test

更新 `IntRuoyiBackend\script\tests\test-worktree-port-map.ps1`，覆盖 `IntRuoyiFronted` 目录名、`New-IntRuoyiMainPortContext` 的前端路径解析，以及固定端口 `8081/48081`。

## RED:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File IntRuoyiBackend\script\tests\test-worktree-port-map.ps1
```

结果：FAIL，`RepoFolder` 不接受 `IntRuoyiFronted`。

## GREEN:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File IntRuoyiBackend\script\tests\test-worktree-port-map.ps1
```

结果：PASS，输出 `worktree-port-map tests passed`。

## Risk And Scope

影响范围限定在本地运行脚本的工作区前端路径解析；未修改端口矩阵、共享前端环境文件或后端配置。

## Verification

- 回归测试：`powershell -NoProfile -ExecutionPolicy Bypass -File IntRuoyiBackend\script\tests\test-worktree-port-map.ps1` 通过。
- 实际重启：`powershell -NoProfile -ExecutionPolicy Bypass -File IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full` 退出码为 0。
- 运行态验证：后端 `/actuator/health` 返回 `UP`，前端入口返回 HTTP `200`。

## Blockers And Follow-Up

无已知代码 blocker；后续仍需通过实际本地重启和 HTTP 健康检查验证运行态。
