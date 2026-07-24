# Execution Log：展厅封面服务构造注入导致后端启动失败

BDD: 展厅封面服务可被 Spring 正常注入 -> Given 后端启动时需要创建 ShowroomProductCoverImageService / When Spring 装配 showroom 相关 bean / Then 不再因缺少默认构造器导致应用启动失败。
BDD: 展厅封面服务既有调用契约保持不变 -> Given 批量封面任务与运行时依赖该服务 / When 后端启动并装配依赖链 / Then 既有业务方法仍可通过定向测试与应用启动路径使用。
RED: `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi-backend.bat` -> FAIL, 后端 JAR 可完成打包，但运行态在 `ShowroomApiRuntime -> ShowroomProductCoverBatchTaskService -> ShowroomProductCoverImageService` 装配链上抛出 `No default constructor found`，导致 `http://localhost:48081/actuator/health` 120 秒内无法返回 200。
GREEN: `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi-backend.bat` -> PASS, 为 `ShowroomProductCoverImageService(FileService, ShowroomNativeImageGenerationService)` 明确注入构造器后，本机后端成功启动；`http://localhost:48081/actuator/health` 返回 200，`/admin-api/system/auth/get-permission-info` 返回 `401 账号未登录`，说明业务接口已正常对外。
