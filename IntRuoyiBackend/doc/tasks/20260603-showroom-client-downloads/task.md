# 任务：展厅模块提供客户端下载接口

## 任务目标

将最新 Android APK 与 Win7 桌面端 ZIP 复制到 `yudao-module-showroom` 的 classpath 资源目录，并在展厅后端模块提供稳定下载接口，确保测试服/正式服常规打包发布时自动携带安装包。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260603-dcc-delete-nas-transfer-categories/task.md`
- 状态：`completed`
- 当前后端仓库已有 unrelated dirty changes；本任务只触碰展厅客户端下载相关资源、接口、测试与任务文档。

## BDD 场景

- BDD: 下载安卓客户端 -> Given 展厅模块 classpath 中存在版本 1.0 APK / When 用户请求安卓客户端下载接口 / Then 后端返回 `application/vnd.android.package-archive`、正确文件名和非空 APK 内容。
- BDD: 下载电脑桌面端 -> Given 展厅模块 classpath 中存在版本 1.0 Win7 ZIP / When 用户请求桌面端下载接口 / Then 后端返回 `application/zip`、正确文件名和非空 ZIP 内容。
- BDD: 安装包缺失必须失败可见 -> Given classpath 资源不存在 / When 下载接口加载安装包 / Then 后端抛出明确异常，不返回默认成功或占位文件。

## Milestones

- [x] M1：建立后端任务文档并确认上一任务已完成。
- [x] M2：新增 RED 测试覆盖资源路径、文件名、content type 与打包携带。
- [x] M3：复制安装包到 `yudao-module-showroom/src/main/resources`。
- [x] M4：实现展厅下载接口。
- [x] M5：运行 Maven 目标测试和必要回归，记录 GREEN 证据。

## Expected Verification

- RED：`mvn -pl yudao-module-showroom -Dtest=ShowroomClientDownloadControllerTest test` 先失败。
- GREEN：同一命令通过。
- GREEN：资源文件存在于 `target/classes` 或 Maven 测试 classpath，证明会进入后端打包产物。

## 当前状态

completed

## 已完成工作

- 新增 `ShowroomClientDownloadController`、`ShowroomClientDownloadService` 与 `ShowroomClientDownloadFile`。
- 已复制：
  - `YingtaiShowroomClient-Android-v1.0.apk`，29,562 bytes。
  - `YingtaiShowroomClient-Win7-v1.0.zip`，113,939,853 bytes。
- Maven 已将两个资源复制到 `target/classes/showroom/client-downloads/v1.0/`。

## 验证结果

- GREEN：`mvn -pl yudao-module-showroom -Dtest=ShowroomClientDownloadControllerTest test` -> PASS，3 tests passed。
- GREEN：backend evidence validator -> PASS。
- GREEN：收尾清理预览 -> PASS，delete `<none>`、blocked `<none>`、warnings `<none>`。

## 剩余阻塞

- 无。

## Cleanup Keep

- `doc/tasks/20260603-showroom-client-downloads/backend-api-evidence.md`
- `yudao-module-showroom/src/test/java/cn/iocoder/yudao/module/showroom/controller/admin/ShowroomClientDownloadControllerTest.java`
