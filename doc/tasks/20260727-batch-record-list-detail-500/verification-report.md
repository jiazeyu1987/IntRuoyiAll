# 验证报告

## 结论

批记录表单右侧详情 HTTP 500 已解除。根因是运行中的 Maven `target` Jar 被后续构建覆盖，不是批记录业务数据或详情代码缺陷。

## RED

- 页面：`http://127.0.0.1:8081/mes/pro/batch-record-form-list`
- 失败接口：`GET /admin-api/mes/pro/batch-record-report/cell-rules`
- 失败结果：HTTP 500
- 旧运行 PID：`46388`
- 旧运行 Jar：`IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`
- 旧进程启动：`2026-07-27 20:33:12`
- `target` Jar 后续修改：`2026-07-27 20:54:09`

## 根因证据

- 运行 Jar 在进程启动后被 Maven 替换。
- 异常跨越 Jimu 模板、Freemarker、Hutool、Spring 和 Tomcat：
  - `JimuReportDao_update.sql` / `TemplateLoaderUtils`
  - `ExceptionUtil`
  - `ChainedPersistenceExceptionTranslator`
  - `RequestUtil`
- 多个无关核心依赖同时延迟加载失败，符合运行中嵌套 Jar 被覆盖后的归档不一致。

## 修复

- 使用 `restart-int-ruoyi-local.ps1 -Component backend` 重新构建。
- 将可执行 Jar 复制到 `output\runtime\int_main` 后启动。
- 未修改业务代码、数据库、端口或数据源，未引入 fallback。

## GREEN

- 新 PID：`4000`
- 新进程启动：`2026-07-27 21:44:45`
- 稳定运行 Jar：`output\runtime\int_main\backend-runtime-control-20260727-214426.jar`
- 稳定 Jar 修改：`2026-07-27 21:44:23`
- health：`UP`
- Playwright：`cell-rules` 与 `signature-cell-markers` HTTP 200
- 页面：选中“产品信息”，无可见错误，预览正常显示

## 防复发回归

- 后端运行期间执行 `mvn -pl yudao-server -am "-DskipTests" package`，结果 `BUILD SUCCESS`。
- `target` Jar 于 `2026-07-27 21:51:08` 再次生成。
- 运行 PID 与稳定运行 Jar 保持不变，health 仍为 `UP`。
- Playwright 再次复验两个详情接口均为 HTTP 200。

## Git 收尾

当前共享 `int_main` 存在其它并行任务的未提交改动，未执行 commit/push；任务状态保持 `ready_for_closeout`。

