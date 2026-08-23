# Verification Report

## Scope

本轮仅验证和提交 `IntRuoyiBackend`、`IntRuoyiFronted` 下归属明确的前后端源码和测试；根目录规则、历史任务记录、资源包和迁移包不在范围内。

## Results

- 前端范围无改动；后端 5 个文件已精确提交，未混入根目录和历史任务改动。
- `TenantServiceImplTest`：23 项，失败 0，跳过 1。
- `MesReleaseAuthoritativeContextConfigurationTest`：2 项，失败 0。
- `git diff --check -- IntRuoyiBackend IntRuoyiFronted`：通过。
- `scripts\preflight\branch-runtime-port-guard.ps1`：通过，`int_main` 为前端 8081、后端 48081。
- 推送前待推送对象无超过 100 MB 的 blob（402 个 blob 扫描）。
- `git push origin int_main`：通过，推送范围 `8fe9228b2..0002767c0`。

## Final Git State

- 本地实现提交：`9b18ee0934746a0356785181963f044f69813f53`。
- 本地收尾记录提交：`5652096e82f6f06d54de4c7baae15e04e0fe5be8`。
- 推送后本地与远端提交：`0002767c0486f11d82bd82666bf8b0f164aee597`。
- 本地相对远端 ahead 0；前后端目录无残余改动；暂存区为空。
- 收尾清理 preview/apply 均通过，三份任务记录保留，无临时产物删除。

## Blockers

- 无。
