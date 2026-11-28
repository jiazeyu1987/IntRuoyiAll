$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$screens = Join-Path $root 'screenshots'
$cases = [System.Collections.Generic.List[object]]::new()
$objectName = '拟新建编号DCC-R1127P-01-IDI；初版A/1；未提交，未取得文件ID和申请ID'
$basePath = '环境http://localhost:8081/；租户测试1；陈佩佩chenpeipei登录；文控中心→文件上传→项目→IDI→项目文件夹DCC0-IDI三部门会签测试→文件类型技术文档／设计和开发验证／DCC0流程验证文件；文件编号DCC-R1127P-01-IDI，初始版本A/1；选择正文A1-body.txt及附件A1-attachment-one.txt、A1-attachment-two.txt；会签部门选择质量体系中心、新品开发部、生产制造中心；选择文控审核后立即生效、不需要培训；提交备注为TEST ONLY软件模拟测试，非真实质量批准。'
$block = '本轮新文件正文上传显示上传大小策略缺失或无效，两个附件上传失败；审批路线显示系统异常，创建受控文件按钮禁用。未提交，无文件ID、申请ID、岗位签名和生效时间。未复用旧文件。'
function Add-Case($id,$chain,$title,$account,$expected,$actual,$status,$steps,$pictures,$attempted) {
    $cases.Add([ordered]@{
        caseId=$id; chain=$chain; title=$title; account=$account; object=$objectName
        expected=$expected; actual=$actual; status=$status
        reproduction=($basePath + $steps + '；取证日期2026年11月27日，关键截图时间15:25至15:56，北京时间。')
        screenshotPaths=@($pictures | ForEach-Object {Join-Path $screens $_})
        execution=$attempted
        evidenceNote=$(if($attempted -eq '依赖跳过'){'所附为本轮上游阻塞截图，不是该下游动作已执行的证据。'}else{'本轮真实前端截图。'})
    })
}
Add-Case '01-01' '01' '选择IDI、目录、文件类型和查看申请属性' 'chenpeipei' '可选择本轮项目和目录，显示该申请属性及自动匹配类别。' '已选择IDI、DCC0-IDI三部门会签测试目录、DCC0流程验证文件类型；显示目标市场NMPA国内、注册人是、生产方是、文件转移否，类别自动匹配。仅证明页面值，未证明矩阵后台配置。' '已通过' '查看申请属性和文件类别。' @('03-project-and-properties.png') '已执行'
Add-Case '01-02' '01' '本轮正文上传' 'chenpeipei' '上传标明软件模拟的初版正文并可预览。' '真实选择121字节的正文后显示DCC upload size policy is missing or invalid，即上传大小策略缺失或无效；正文未上传成功，不能创建文件。' '未通过' '选择文件A1-body.txt，点击查看校验详情查看主文件上传错误。' @('01-upload-and-route-errors.png','03-project-and-properties.png') '已执行'
Add-Case '01-03' '01' '两个不同附件上传' 'chenpeipei' '同一版两个可区分附件均保存成功。' '真实选择两个附件，页面列出附件名，但同时明确提示附件上传失败，请移除失败附件后再提交；不能将列出文件名当作上传成功。' '未通过' '在附件选择中一次选择A1-attachment-one.txt与A1-attachment-two.txt；向下滚动文件信息区查看失败提示。' @('04-two-attachments-failed.png') '已执行'
Add-Case '01-04' '01' '三个会签部门与正式审批路线预检' 'chenpeipei' '选择三个会签部门后，系统识别主管、负责人和批准人，并完成正式路线预检。' '三个会签部门可选；每次调整后路线均回到系统异常，批准人区域仍提示等待正式审批路线预检；未取得可核验的正式候选人链。' '未通过' '依次选择质量体系中心、新品开发部、生产制造中心；点击审批路线查看详情和提交前校验详情。' @('02-route-and-distribution-blocked.png','01-upload-and-route-errors.png') '已执行'
Add-Case '01-05' '01' '编号初版及生效培训选项填写' 'chenpeipei' '可填写新编号A/1，选择文控审核后立即生效、不需要培训。' '已填写新编号与A/1并选定两个选项；校验详情显示编号版本可提交、生效方式已填写。该结果不代表整张申请可提交。' '已通过' '填写编号，保留A/1；选择文控审核后立即生效及不需要培训；查看校验详情。' @('01-upload-and-route-errors.png','02-route-and-distribution-blocked.png') '已执行'
Add-Case '01-06' '01' '查看矩阵和正文权限核对' 'chenpeipei' '取得本次申请对应正式查看矩阵并核对人员权限。' '校验详情仅显示浏览权限范围按正式权限校验；未取得可据以核对每位人员的本次查看矩阵，不能认定矩阵正确。' '存疑' '查看校验详情；审批路线详情受系统异常阻塞；没有以旧轮次权限结论替代。' @('01-upload-and-route-errors.png') '未覆盖'
Add-Case '01-07' '01' '确认框完整信息及取消不创建任务' 'chenpeipei' '打开确认框逐项核对后取消，不生成申请和任务。' ($block+'因此未到达确认框，没有执行取消确认测试。') '存疑' '填写后创建受控文件按钮仍禁用；不得绕过校验打开提交确认。' @('01-upload-and-route-errors.png','05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-08' '01' '初版九次岗位签名审批' 'panxin、shanglei、xujianhai、zhouhongxin、weijiahao、zhulijiang、zhaohaichen' '主管一签，部门指派三签、实际会签三签，批准与文控各一签；批准人只批准。' ($block+'没有本轮任务，九签全部未执行。') '存疑' '上游成功后应依次进入各指定账号审批任务，主管审核、三部门指派与实际会签、批准、文控标准化审核；本轮在创建前跳过。' @('01-upload-and-route-errors.png','05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-09' '01' '初版生效、受控、盖章及下发时间' 'zhaohaichen' '文控签名记录生效时间，受控记录受控时间，盖章后下发。' ($block+'没有本轮生效、受控或下发记录。') '存疑' '初版完整审批后核对时间及处理顺序；本轮无初版。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-10' '01' '初版有效期间搜索查看正文和两个附件' 'zhangxinyue' 'A/1仍为当前有效版时按编号查阅正文和两个附件。' ($block+'本轮没有当前有效A/1，未切换张鑫悦执行阅读。') '存疑' '成功后由张鑫悦→文控中心→受控浏览→搜索本编号→浏览A/1，核对FIRST-A1和两个FIRST附件；本轮直接依赖跳过。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-11' '01' '初版正文附件下载打开及留痕' 'zhangxinyue' '有效A/1正文与两个附件逐个下载打开，核对内容及下载记录。' ($block+'没有本轮下载或打开内容证据，不能用最终版或旧轮次文件代替。') '存疑' 'A/1有效期间从受控浏览分别下载正文、附件一、附件二并打开核对，再查下载留痕；本轮跳过。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-12' '01' '首次检出及锁记录' 'chenpeipei' '检出成功，记录持有人及时间，原有效版仍可授权阅读。' ($block+'无本轮有效文件，未执行检出或阅读验证。') '存疑' '有效A/1→操作面板→检出→核对锁和时间，再授权账号阅读原版。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-13' '01' '他人不能检出检入或撤销锁' 'yuanhongwei' '陈佩佩持锁时袁宏伟三种操作均被拒绝。' ($block+'未建立本轮锁，三种拒绝行为未实测。') '存疑' '陈佩佩检出后切换袁宏伟→本编号操作面板→尝试检出、检入、撤销他人检出；无锁前置而跳过。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-14' '01' '本人撤销检出并保留原正文' 'chenpeipei' '本人撤销后释放锁、不生成版本、不改正文，并留人时间原因。' ($block+'无检出前置，未执行撤销。') '存疑' '陈佩佩检出后在检入前撤销，填写软件模拟原因，核对版本和正文不变。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-15' '01' '重新检出并下载本人未作废版本与附件' 'chenpeipei' '重新检出成功；从维护入口下载本人检出文件的未作废版本及附件。' ($block+'未建立本轮正式版、草稿或待生效版，无法验证各版本维护下载资格。') '存疑' '撤销后再次检出；维护入口逐个核对正式、草稿和待生效版本正文附件下载。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-16' '01' '检入保存正文附件与修改说明后释放锁' 'chenpeipei' '全部保存成功才释放锁，工作稿不覆盖旧正式附件。' ($block+'未进入本轮检入，未上传A/2工作材料。') '存疑' '重新检出→修改正文与两个附件→填写说明→检入→核对锁和历史附件。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-17' '01' '草稿只显示草稿且不替换日常有效版' 'chenpeipei、zhangxinyue' '不显示内部小版本号；草稿不替换A/1正式版。' ($block+'无本轮草稿，未核验草稿标签和日常版本。') '存疑' '检入后从维护与日常入口分别查看同编号，比较工作稿标签和当前有效A/1。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-18' '01' '真实上传保存失败时不报成功不释放锁' 'chenpeipei' '检入真实失败应保留锁且不宣称成功。' '本轮确实出现初次上传失败，但不是持锁检入失败，不能替代本异常分支证据；未建立检出锁，也未破坏旧文件来强造故障。' '存疑' '只有本轮检出后出现真实上传或保存失败才核验本分支；当前只有新建上传失败。' @('04-two-attachments-failed.png','05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-19' '01' '局部变更选择小版本升版并完整重审' 'chenpeipei及九签指定账号' '草稿发起局部变更，按适用规则A/1升A/2，选择立即生效和培训并完成九签。' ($block+'没有草稿与本轮版本关系，未执行局部变更或重审。') '存疑' '草稿→局部变更→选择小版本升版→立即生效→需要培训→完整九签；不得只凭显示A/2判通过。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-20' '01' '文控已签但培训未完成时不受控不切版' 'zhaohaichen、zhangxinyue' '文控签名后有实际生效时间，但培训完成前日常仍为旧版且新版本未受控。' ($block+'无本轮培训申请和文控签名，未取得中间状态。') '存疑' '局部变更文控签名后、上传培训记录前核对实际生效时间、受控状态和日常A/1。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-21' '01' '仅编制人上传本轮模拟培训记录' 'chenpeipei' '陈佩佩成功上传保存标明本申请A/2的模拟培训记录，不增加培训审批或签名。' ($block+'没有本轮培训任务，未上传培训材料，也未增加审批。') '存疑' '本轮A/2培训任务→陈佩佩上传注明TEST ONLY和本编号版本的记录→保存→核对培训完成。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-22' '01' '培训完成后受控盖章切版下发及旧版归档' 'chenpeipei、zhaohaichen' '培训记录保存成功后完成受控盖章，切换A/2，不等待全员签收，旧版转历史。' ($block+'无培训完成事件和切版，本轮均未核实。') '存疑' '培训保存后分别核对新版本受控和盖章时间、日常版、下发任务及旧A/1归档。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-23' '01' '局部版有效期间查看下载正文附件和留痕' 'zhangxinyue' 'A/2有效期间搜索阅读并下载打开正文和两个附件，核对本版内容及留痕。' ($block+'本轮A/2未产生；未用B/1或历史下载代替。') '存疑' '必须在A/2有效时从受控浏览逐个查看下载正文和两个附件并核对留痕，完成后才可升B/1。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-24' '01' '再次检出检入换版且无需培训完整重审' 'chenpeipei及九签指定账号' '同编号再次检出检入，按换版规则A/2升B/1，立即生效、不培训并完整重审。' ($block+'本轮没有有效A/2，换版及九签未执行。') '存疑' 'A/2查看下载完成→检出修改正文两个附件→检入→换版→立即生效不培训→九签。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-25' '01' '换版有效期间查看下载正文附件和留痕' 'zhangxinyue' 'B/1有效期间核对正文两个附件及下载留痕，版本内容不混用。' ($block+'B/1未产生，没有下载证据。') '存疑' 'B/1受控生效后受控浏览搜索编号，逐项查看、下载并打开正文和两个附件，核对本版标记与留痕。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-26' '01' '作废旧版仅文控下载及历史审批保留' 'zhaohaichen及普通账号' '赵海辰通过作废归档核验旧版，普通账号不得下载作废旧版；版本审批历史保留。' ($block+'无本轮历史版或作废旧版，未以其他既有文件替代。') '存疑' '同编号完成升版后由赵海辰查看作废归档和审批历史，再核对普通账号的旧版下载边界。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '01-27' '01' '他人检出通知日常入口不授予维护下载权' 'yuanhongwei及普通账号' '普通账号不能因别人检出或通知获得草稿待生效版维护下载权限。' ($block+'没有本轮检出文件及通知，未实测权限拒绝。') '存疑' '本轮陈佩佩持锁并存在工作稿后，由其他账号分别经通知、日常浏览和维护入口验证边界。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '06-01' '06' '独立核对三个下发部门名单' 'chenpeipei' '下发部门可独立核对为生产制造中心、质量体系中心、新品开发部，不能由会签名单推定。' '真实页面中会签已选三个部门，但下发部门仍为空且禁用，审批路线系统异常；未把会签选择当作下发名单正确，未认定永久缺少该入口。' '存疑' '在本次申请中分别观察会签部门与下发部门；下发字段禁用，不能完成独立名单选择核对。' @('02-route-and-distribution-blocked.png') '已执行但阻塞'
Add-Case '06-02' '06' '受控盖章生效后仅下发三位负责人' 'zhaohaichen' '仅尚磊、徐建海、周红欣对应负责人收到签收任务，非全员或七人。' ($block+'没有下发结果或签收任务，未核验人数和负责人。') '存疑' '文件完整受控盖章生效→查看下发结果→逐部门核对三位负责人及任务数量。' @('02-route-and-distribution-blocked.png','05-all-versions-no-object.png') '依赖跳过'
Add-Case '06-03' '06' '待生效期间不得生成签收任务' 'zhaohaichen' '未生效的版本不创建签收任务。' ($block+'没有进入本轮待生效状态；无对象不等于已验证待生效约束。') '存疑' '新版本已提交但未生效时查看签收任务；本轮无新版本而跳过。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '06-04' '06' '尚磊与徐建海本人签名签收' 'shanglei、xujianhai' '两位负责人各自办理本版签收并记录真实签名时间。' ($block+'没有本轮签收任务，二人均未签收。') '存疑' '各本人账号→个人工作台→本编号A/1签收任务→核对版本→签名签收→核对结果。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '06-05' '06' '周红欣在签收任务单独指定危家豪' 'zhouhongxin' '在本版签收任务另行指定危家豪，保存指定人时间版本；不能沿用会签指派。' ($block+'没有本轮签收任务或签收指定关系。') '存疑' '周红欣→本版签收任务→单独指定危家豪→核对指定人、时间、文件版本。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '06-06' '06' '仅指定未签名前仍待签收' 'zhouhongxin、weijiahao' '指定完成但危家豪尚未签名时，任务仍待签收。' ($block+'没有本轮指定前后状态，未验证。') '存疑' '指定危家豪后暂不签名，查看本版签收状态。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '06-07' '06' '危家豪本人签名完成签收' 'weijiahao' '本人办理后才完成，结果显示实际签名人和时间。' ($block+'危家豪未执行本轮签收，不沿用上轮签名。') '存疑' '危家豪→本编号签收任务→核对版本和指定关系→签名→核对签收人时间及完成状态。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '06-08' '06' '签收指定不绕过正文权限' 'zhouhongxin、weijiahao及无正文权限者' '有权限者核对本版正文；无权限者不能因通知签收或指定获得正文权限。' ($block+'没有本轮通知签收入口或有效正文，权限边界未执行。') '存疑' '按实际查看矩阵，从通知和签收打开本版文件，分别验证有权限和无权限身份；不得临时修改权限造前置。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '06-09' '06' '新版不等全部签收即切版' 'zhaohaichen、zhangxinyue' '新版受控且生效即可切换，未签收如实保留，无七人全签要求。' ($block+'没有本轮新版受控或签收记录，切换约束未验证。') '存疑' 'A/2或B/1受控生效后，保持部分签收未完成并查看日常版本；本轮依赖跳过。' @('05-all-versions-no-object.png') '依赖跳过'
Add-Case '06-10' '06' '新版独立任务旧指定旧签名不冒充完成' 'zhaohaichen及三部门签收账号' '新版本生成独立签收任务；旧指定与旧签名保留但新版未签名仍待签收。' ($block+'无本轮初版签收或新版，未取得跨版对照证据。') '存疑' '固定编号，先记录A/1三部门签收，再合法升版；比较新版任务、指定关系及未签状态和旧版历史。' @('05-all-versions-no-object.png') '依赖跳过'
$cases | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $root 'cases.json') -Encoding utf8
$counts = $cases.ToArray() | Group-Object -Property { $_['status'] } | ForEach-Object { "$($_.Name)：$($_.Count)" }
$report = @"
# 本地01及06本轮测试交接

日期：2026年11月27日下午，北京时间。环境：http://localhost:8081/，测试1。

结论：本轮没有完成整条主链，不能报通过。已执行当前可到达的真实前端步骤；正文上传、两个附件上传与审批路线预检失败，创建按钮禁用。所有直接依赖新文件的后续场景分别记录为存疑、依赖跳过，不计通过。

拟新建编号：DCC-R1127P-01-IDI。版本A/1仅为表单输入，未生成正式文件。没有申请ID、文件ID、签名、受控、生效或签收时间。全部版本检索该编号无数据。没有复用旧轮次对象或旧截图。

$($counts -join '；')。总计$($cases.Count)项。

## 真实失败与恢复条件

1. 新建正文上传显示上传大小策略缺失或无效，两个附件也失败。恢复责任：本地系统配置或后端维护方；本分片没有修改配置、权限、源码或数据库，也没有重启服务。恢复条件：本环境上传策略恢复有效，重新从前端上传本轮材料成功。
2. 已选正式类型与三个会签部门，但审批路线显示系统异常，批准人未识别。恢复责任：本地系统维护方；须恢复正式审批路线预检后才可提交。未把系统异常解释成账号密码错误，没有追加密码重试。
3. 下发部门为空且禁用，不能独立核对三部门名单。这是当前路线异常下的阻塞证据，不宣称永久缺少入口，也不由会签名单推定下发名单正确。

## 独立步骤及证据

已核对当前用户陈佩佩和测试1；完成项目、目录、类型及申请属性选择；填写独立编号A/1、立即生效及不培训；真实选择正文和两个附件；逐次选择三个会签部门；查看审批详情及提交前校验。随后通过新的页面进入受控浏览，全局文件按本编号查询，并切换全部版本，结果暂无数据。其他旧文件只出现在列表中，未打开、修改或代替本轮对象。

每条case都引用本轮截图。依赖跳过case所附为上游阻塞证据，不是下游实际操作证据。截图时间分别为15:25、15:30、15:34、15:35、15:56左右，以文件名原始毫秒时间对应为准。没有密码或PIN截图。未触发确认框，所以没有把未提交当作取消确认验证通过。

## 输出

cases.json是可机器合并的UTF8数组，含规定字段及执行情况、证据说明。screenshots是五张原始页面截图；materials是本轮初版正文和两个区分附件。后续A/2、B/1和培训因直接前置失败未上传。证据包仅包含本分片，不含其他分片或旧轮次。

当前恢复点：新建表单未提交。恢复前先查询本编号正式状态；若已由后续工作成功提交，只核验，不重复创建或签名。
"@
$report | Set-Content -LiteralPath (Join-Path $root 'report.md') -Encoding utf8
foreach($item in $cases){foreach($picture in $item.screenshotPaths){if(-not(Test-Path -LiteralPath $picture)){throw "缺失截图：$picture"}}}
$parsed = Get-Content -LiteralPath (Join-Path $root 'cases.json') -Raw | ConvertFrom-Json
if($parsed.Count -ne 37){throw "条目数量异常：$($parsed.Count)"}
if(($parsed | Where-Object status -eq '已通过').Count -ne 2){throw '通过条目数量异常'}
if(($parsed | Where-Object status -eq '未通过').Count -ne 3){throw '失败条目数量异常'}
if(($parsed | Where-Object status -eq '存疑').Count -ne 32){throw '存疑条目数量异常'}
Write-Output ($counts -join '；')
Write-Output "已核验37条记录、全部截图路径存在，内部清单可解析。"
