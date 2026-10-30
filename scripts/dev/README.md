# Codex 本地 Debug 与 HotSwap

仅用于本机开发。使用 JDK 21（与当前运行环境一致）。脚本不停止服务、不自动构建、不自动重启。

## 首次启用

先确认当前同步任务已结束，再按当前任务授权停止原后端。使用现有构建流程生成启动包，保留原环境变量和工作目录，然后运行：

```powershell
./scripts/dev/start-debug-backend.ps1 -JavaHome 'C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot' -Jar '<现有构建流程生成的绝对路径.jar>' -Port 48081
```

48081 仅供主工作区现有后端使用；附加 worktree 必须先预约槽位，并传入该槽位端口及独立 DebugPort。调试默认 5005，仅绑定 127.0.0.1。端口被占用立即失败，不杀进程。

## 修改已有方法后

1. 在 IntRuoyiBackend 目录执行受影响模块的 Maven 编译，例如 `mvn -pl yudao-module-erp -am compile '-DskipTests'`。编译失败必须停止。
2. 检查差异仅涉及可热替换的方法体，并确认编译源码与正在运行的包同源；不要把并行任务的类加载到当前进程。
3. 从模块 target/classes 指定准确的类名、class 文件运行：

```powershell
./scripts/dev/hotswap.ps1 -JavaHome 'C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot' -ClassNames '完整包名.类名' -ClassFiles '<对应 target/classes 下的绝对路径.class>'
```

多个类用等长 ClassNames/ClassFiles 数组一次提交。成功输出每个 REDEFINED 类名；未加载类、多个类加载器匹配、结构不支持、调试连接失败均明确报错。不会自动重启或降级。日志用现有日志框架；不打印认证信息。

## 边界

- 通常仅支持已有方法体修改；增加字段/方法、修改继承关系、注解、Spring 配置及数据库结构不能靠此脚本生效。
- 已在栈中执行的方法继续使用旧版本；之后的新调用使用替换版本。长循环内的当前方法不会自动换成新代码。
- HotSwap 不重新初始化 Bean、不重跑静态初始化、不改变运行中的数据。
- 类文件仍须经过现有打包流程才能在下次重启后保留；内存替换不修改 jar。
- 本次全量日志增加了 Lombok 日志字段，因此首次需要启动包含这些改动的新包。

## 已验证

通过独立 JDK 21 调试进程调用 hotswap.ps1，在不更换进程的情况下将方法输出由 BEFORE 改为 AFTER；测试结束只停止测试进程。业务后端未重启。

## 默认入口
正式本地入口 scripts/runtime/start-branch-backend.ps1 默认开启 Debug；主工作区为 5005，其他分支按后端端口偏移分配，可用 -DebugPort 显式指定。需要普通模式时传 -NoDebug。
