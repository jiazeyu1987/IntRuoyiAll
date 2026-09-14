const assert = require('assert/strict')
const fs = require('fs')
const path = require('path')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const backendRoot = path.resolve(moduleRoot, '..')
const readModule = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')
const readBackend = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')

const controller = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/productionrelease/MesProductionReleaseController.java'
)
assert.match(controller, /@GetMapping\("\/pqc\/page"\)/)
assert.match(controller, /getPqcReleasePage/)
assert.match(controller, /@ExceptionHandler\(MesReleaseFlowBlockerException\.class\)/)
assert.match(controller, /MesReleaseFlowExceptionAdvice\.toResult\(exception\)/)

const approveReq = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/productionrelease/vo/MesPqcProductionReleaseApproveReqVO.java'
)
assert.match(approveReq, /@NotBlank\(message = "电子签名密码不能为空"\)[\s\S]*private String signaturePassword;/)

const approveCommand = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcProductionReleaseApproveCommand.java'
)
assert.match(approveCommand, /private String signaturePassword;/)

const releaseService = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java'
)
assert.match(releaseService, /ensureWorkOrderNotFrozen\(application\.getWorkOrderId\(\),\s*"PQC放行"\)/)
assert.match(releaseService, /recordPqcReleaseSignature\(/)

const signatureService = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionSignatureService.java'
)
assert.match(signatureService, /ACTION_PQC_RELEASE\s*=\s*"PQC_RELEASE"/)
assert.match(signatureService, /recordPqcReleaseSignature\(/)
assert.match(signatureService, /case ACTION_PQC_RELEASE -> "PQC生产放行"/)

const nonconformanceCreateReq = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProEdhrNonconformanceReviewCreateReqVO.java'
)
assert.doesNotMatch(nonconformanceCreateReq, /@NotNull\(message = "批次执行不能为空"\)/)

const nonconformanceService = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java'
)
assert.match(nonconformanceService, /requirePqcReleaseApplication/)
assert.match(nonconformanceService, /selectLatestBySource/)

const migration = readBackend('sql/mysql/20260831_mes_pqc_release_nonconformance_scope.sql')
assert.match(migration, /MODIFY COLUMN `batch_execution_id` bigint NULL/)
assert.match(migration, /idx_mes_edhr_ncr_source/)

const menuMigration = readBackend('sql/mysql/20260831_mes_pqc_production_release_menu.sql')
assert.match(menuMigration, /`name` = 'PQC生产放行'/)
assert.match(menuMigration, /`path` = '\/mes\/production-release\/pqc'/)
assert.match(menuMigration, /`component` = 'mes\/pro\/production-release\/PqcProductionReleasePage'/)
assert.match(menuMigration, /`component_name` = 'MesPqcProductionRelease'/)
assert.match(menuMigration, /JSON_ARRAY_APPEND\(`menu_ids`, '\$', v_menu_id\)/)

console.log('mes-pqc-production-release-mvp-completion-static: PASS')
