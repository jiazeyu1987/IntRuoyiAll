# Verification Report

## Result

`BLOCKED`

融合后目标 E2E 未执行到业务写入阶段。测试租户真实登录、批记录表单页面入口、前后端端口和服务健康均通过，但当前 `48081` 运行包没有加载融合后的候选发布修复，因此不能把该运行态作为融合版本进行验收。

## Passed Preflight

- `int_main` 前端 `8081` HTTP `200`，后端 `48081` health `UP`，进程均归属 `E:\IntRuoyi`。
- Playwright 使用本机 Chrome，以测试租户/测试账号真实登录，并进入批记录表单页面看到“导入”。
- 候选生成关键类与本地编译结果哈希一致。
- 未发送目标业务写请求，未产生需要清理的批记录、路线候选或发布数据。

## Blocking Evidence

- 当前运行 Jar：`backend-runtime-control-20260815-032520-dcc-residual-fix.jar`，SHA-256 `77247FBD5EDD3265A4FBF634722DE3B766465D28BDF8243BE323881D38060E97`。
- 候选发布投影关键类：运行态 SHA-256 `17F5B50EEA16BB9ECE9C89501EFC5D8D4C6C18C047E0A2B2230A87362071B814`，本地融合编译类 SHA-256 `04B71DCFF2DDE4C04CD2F4AC1DB98E39E4B875B2E23F689C2C19B886D2F11B10`，不一致。
- 融合源文件修改时间晚于运行 Jar；当前工作区存在 384 项未提交变更，禁止直接从脏主工作区打包冒充可追溯融合运行态。

## Required Next Step

先将本次融合代码形成可追溯的干净版本，再构建并重启 `int_main` 后端。运行 Jar 关键类哈希与该版本一致后，重新执行：未勾选工艺流程负向导入、勾选后候选升版、发布前 ACTIVE 不变、真实页面发布、旧绑定/工序开始/表单槽位保留、新工序正式权限范围及无 END 绑定，并通过真实页面完成任务数据清理。
