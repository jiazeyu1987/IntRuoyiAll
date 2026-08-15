# Execution Log

- USER-INTENT: 用户明确要求提交并推送前后端所有当前代码。
- PRECHECK: Git 根目录为 `E:\IntRuoyi`，当前分支 `int_main`，HEAD=`9f7055e2989291564a215d20725cb4186afc02f7`，上游 `origin/int_main`，远端为 GitHub HTTPS。
- SCOPE: 纳入 `IntRuoyiBackend` 与 `IntRuoyiFronted` 的全部源码、测试、SQL、脚本和项目文档改动；排除凭据、运行日志、PID、构建目录、临时产物和超过远端限制的大文件。根级任务记录按收尾证据单独处理。
- PRECHECK: `git fetch origin int_main` 成功；本地相对远端为 behind=0、ahead=20，不存在非快进前置冲突。
- GATE: `scripts\\preflight\\branch-runtime-port-guard.ps1` -> PASS，`int_main` 端口基线为前端 8081、后端 48081。
- GREEN: `mvn.cmd -pl yudao-server -am "-DskipTests" package` -> PASS，后端全模块编译与打包成功。
- GREEN: `pnpm ts:check` -> PASS，前端 TypeScript 类型检查成功。
- GREEN: `pnpm build:local` -> PASS，前端本地构建成功。
- STAGING-AUDIT: 暂存 164 个前后端文件，其中后端 110、前端 54；未命中凭据、日志、PID、构建目录和危险扩展名，未发现大于等于 10MB 的暂存文件。
- ISOLATION: 根目录其他任务文档、review-fix-loop 产物、设计文件和运行规则改动均未纳入本轮前后端代码暂存区。
