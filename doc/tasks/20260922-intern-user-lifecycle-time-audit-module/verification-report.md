# 验证报告

## 2026-09-22

- PASS：`node IntRuoyiFronted/tests/e2e/intern-user-time-maintenance-module-static.spec.cjs`
  - 覆盖实习用户模块静态契约、权限编码、SQL 角色绑定、上传时间审计入口、DCC 升版/作废时间入口。
- PASS：`mvn -pl yudao-module-infra -am -Dtest=InternUserTimeMaintenanceServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - 覆盖上传时间修改后追加审计记录。
- PASS：`mvn -pl yudao-module-dcc -am -Dtest=DccInternUserTimeMaintenanceServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - 覆盖升版时间、作废时间修改后调用审计服务写入审计记录。
- PASS：`pnpm ts:check`
  - 覆盖前端 TypeScript/Vue 类型校验。
- PASS：`git diff --check`
  - 未发现空白错误；命令仅输出仓库现有 LF/CRLF 转换警告。

## 未执行项

- Git 提交/推送：本轮用户未授权执行提交或推送，且工作区存在大量非本任务脏改动。

## 追加 E2E 授权

- 2026-09-22：用户明确授权重启并进行 E2E 验证，后续补充真实页面验收结果。

## 追加验证结果

- PASS：执行本任务 SQL 到本机库 `ruoyi-vue-pro`。
  - 只读核验：审计表存在；`intern-user:time-maintenance:*` 权限 6 条；`实习用户 / intern_user` 角色存在且绑定 6 条本模块按钮权限。
- PASS：`mvn -pl yudao-server -am -DskipTests package`
  - 构建新 `yudao-server-exec.jar`，并解包确认包含本模块新增 infra/DCC 类。
- PASS：重启当前分支运行时 `int_qms`。
  - 前端 `http://127.0.0.1:8061/` HTTP 200；后端 `48061` 新 jar 启动日志显示 Tomcat started。
  - 备注：标准 `restart-int-ruoyi-local.ps1 -Component full` 被既有迁移预检阻塞，后续采用当前分支端口矩阵直接启动新构建产物。
- PASS：`node tests/e2e/intern-user-time-maintenance-real.e2e.cjs`
  - 使用真实前端、真实租户 `芋道源码`、真实账号 `admin`。
  - 页面完成：修改上传时间、查看上传时间修改审计、修改升版时间、查看升版时间修改审计、修改作废时间、查看作废时间修改审计。
  - E2E 目标响应均为 HTTP 200：上传时间 PUT/审计 GET、DCC 升版时间 PUT/审计 GET、DCC 作废时间 PUT/审计 GET。
- PASS：只读 DB 核验审计结果。
  - `createTime`、`publishedTime`、`obsoletedTime` 三类审计记录均存在；DCC E2E 文件 `2054545668044068400` 已生成升版和作废审计。

## 残留风险

- E2E 页面捕获到既有 DCC 分类接口 `系统异常`；后端日志指向本机库缺少 `action_type` 字段。该异常未阻断本任务实习用户时间维护流程，需归入 DCC 环境/迁移残留问题单独处理。
