# Verification Report

## Scope

本轮仅验证和提交 `IntRuoyiBackend`、`IntRuoyiFronted` 下归属明确的前后端源码和测试；根目录规则、历史任务记录、资源包和迁移包不在范围内。

## Results

- 前端范围无改动；后端 5 个文件已精确提交，未混入根目录和历史任务改动。
- `TenantServiceImplTest`：23 项，失败 0，跳过 1。
- `MesReleaseAuthoritativeContextConfigurationTest`：1 项，失败 0。
- `git diff --check -- IntRuoyiBackend IntRuoyiFronted`：通过。
- `scripts\preflight\branch-runtime-port-guard.ps1`：通过，`int_main` 为前端 8081、后端 48081。
- 待推送对象无超过 100 MB 的 blob。

## Final Git State

- 本地实现提交：`9b18ee0934746a0356785181963f044f69813f53`。
- 远端当前提交：`8fe9228b20521d6a6f32a055f0d3d2fc2c9bd4fe`。
- 本地相对远端 ahead 67；前后端目录无残余改动；暂存区为空。
- 推送未完成，原因是 GitHub HTTPS 网络不可达。
- 收尾清理 preview/apply 均通过，三份任务记录保留，无临时产物删除。

## Blockers

- 必须恢复 GitHub HTTPS 网络或可用代理后，重新执行 `git push origin int_main`，再确认本地不再 ahead。
