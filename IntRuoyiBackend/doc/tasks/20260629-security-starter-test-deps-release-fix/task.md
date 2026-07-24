# 任务：补齐安全模块测试依赖以放行已提交版本发布

- Task ID: `20260629-security-starter-test-deps-release-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

补齐 `yudao-spring-boot-starter-security` 模块测试所需依赖，使当前“已提交 git HEAD”在干净 release worktree 中能够通过 Maven `testCompile`，从而继续真实 `build-release -> publish-test`。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-commit-current-committable-code-backend\task.md`
- 状态：`in_progress`
- 处理说明：该任务是后端提交收口总任务；当前发布阻塞属于其 M3 候选集中的一个最小缺口，本次在独立子任务中收口后，仅提交与安全模块测试依赖放行直接相关的文件，不混入其他候选改动。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 提交前先按命中路由核对 PowerShell / worktree / 发布相关门禁。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文台账与命令输出保持显式 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
  - 仅提交当前发布阻塞直接相关的最小文件，不把其它进行中任务混入本次提交。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。为已存在的测试类补齐正式测试依赖，而不是绕过 Maven `testCompile` 或手工修改发布脚本跳过模块。
- 是否存在临时补丁或绕过：否。

## Milestones

- M1: 建立任务文档并记录发布阻塞根因。状态：completed。
- M2: 用定向 Maven 命令复现安全模块测试依赖缺失。状态：completed。
- M3: 补齐 `pom.xml` 测试依赖并执行 GREEN 验证。状态：completed。
- M4: 只提交本次发布阻塞修复并回写证据。状态：completed。

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-framework/yudao-spring-boot-starter-security -DskipTests test -q`
- `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro diff --cached --check`

## Current Blockers

- 暂无。

## Final Verification Result

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-framework/yudao-spring-boot-starter-security -DskipTests test -q` -> PASS
- 提交结果：`7e2fa923c86a7a3db2eabcdbe34288e7cf77e27c` `任务: 补齐安全模块测试依赖放行发布构建`
