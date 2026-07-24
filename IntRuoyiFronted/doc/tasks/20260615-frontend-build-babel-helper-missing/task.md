# 前端构建缺失 Babel helper 依赖修复

## 任务目标

- 修复测试服构建流程中前端静态资源构建失败的问题。
- 失败现象：`vite.config.ts` 加载时 `@babel/types` 无法解析 `@babel/helper-validator-identifier`。
- 保持无 fallback：缺失依赖、损坏安装或锁文件不一致时直接失败并输出明确原因。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：无。本轮只处理本机前端依赖安装完整性与构建验证，不涉及登录、真实 E2E、服务器写入/重启/发布、备份恢复、worktree 合并/清理或前端页面样式改造。
- 适用强制门禁：
  - 不执行服务器写入、发布、重启或备份恢复。
  - 不使用 mock、空成功、兼容降级或 fallback 掩盖依赖缺失。
  - 若需要进入服务器或高风险动作，必须先补充命中文档并在 `execution-log.md` 记录 `experience-preflight`。

## 上一任务检查

- 最近前端任务：`yudao-ui-admin-vue3/doc/tasks/20260615-showroom-award-export-import-real-e2e/task.md`
- 状态：已完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。先定位依赖安装/锁文件完整性，再使用包管理器恢复依赖关系，不在 Vite 配置中硬编码绕过。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 前端构建依赖完整时测试模式构建成功 -> Given 前端依赖按 `pnpm-lock.yaml` 完整安装 / When 执行 `vite build --mode test` / Then `vite.config.ts` 能加载 `@vitejs/plugin-vue-jsx` 及 Babel 依赖，构建通过。
- BDD: Babel helper 缺失时构建失败并暴露根因 -> Given `@babel/types` 运行依赖缺少 `@babel/helper-validator-identifier` 可解析入口 / When 执行测试模式构建 / Then 构建失败并报告缺失模块，不返回成功。

## 里程碑

- [ ] M1：记录任务、附件失败日志、上一任务状态与经验门禁。
- [ ] M2：复现并定位缺失依赖的安装完整性问题，记录 RED。
- [ ] M3：用正式依赖安装/锁文件一致性方案修复，记录 GREEN。
- [ ] M4：运行前端测试模式构建或等价发布构建验证。
- [ ] M5：执行缺陷证据校验、收尾清理预览，并按当前任务范围提交。

## 预期验证

- RED：`node node_modules/vite/bin/vite.js build --mode test` 或等价依赖解析命令失败，原因包含 `Cannot find module '@babel/helper-validator-identifier'`。
- GREEN：依赖解析命令通过，`pnpm build:test` 或 `node node_modules/vite/bin/vite.js build --mode test` 通过。
- 证据：`bug-regression-evidence.md` 通过 `validate_bug_regression.py` 校验。

## 当前状态

进行中：已读取附件失败日志，准备复现并定位依赖安装完整性问题。
