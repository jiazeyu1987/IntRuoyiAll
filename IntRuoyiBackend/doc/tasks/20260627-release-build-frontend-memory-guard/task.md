# 任务：修复发布脚本前端构建内存保护缺失

## 任务目标

- 修复 `publish-int-ruoyi.ps1` 在发布链路中直接调用 `node vite.js build --mode test` 导致绕过前端 `build:test` 内存保护的问题。
- 为发布脚本补充契约测试，确保前端构建继续沿正式 `test` 模式入口执行，并显式保留 `NODE_OPTIONS=--max-old-space-size=8192`。
- 恢复维护控制台 `build-release` 真实链路可继续推进。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260627-backend-closeout-commit\task.md`
- 状态：`COMPLETED`
- 处理说明：已核对前序任务完成，本次作为新的发布脚本回归修复单独记录。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\experience-index.md`
- 命中文档：无
- 适用强制门禁：
  - 本次仅修改本机发布脚本与契约测试，不直接执行服务器写入、发布、恢复或 worktree 清理。
  - 修复必须围绕正式发布脚本入口完成，不得通过降低前端构建检查、跳过构建或手工改包替代。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。根因是发布脚本封装绕开前端正式构建脚本，导致内存保护失效；本次将改回统一入口并补契约测试。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 发布脚本沿正式 test 构建入口执行 -> Given 维护控制台 build-release 需要在前端仓构建 test 静态资源 / When 发布脚本触发前端构建 / Then 必须沿用前端仓正式 build:test 入口与 8GB Node heap 保护，不得再直接以默认 heap 调用 vite CLI。`

## 里程碑

1. M1：记录脚本根因与现状。`COMPLETED`
2. M2：补充脚本契约回归并取得 RED 证据。`COMPLETED`
3. M3：完成脚本修复并跑通契约与真实前端构建回归。`COMPLETED`
4. M4：记录结果并提交后端修复。`COMPLETED`

## 预期验证

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- `pnpm build:test`

## 最终验证结果

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS
- `pnpm build:test` -> PASS
