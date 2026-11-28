$ErrorActionPreference="Stop"
$stamp=Get-Date -Format yyyyMMddHHmmss
$out=Join-Path $PWD "output\$stamp"
$report=Join-Path $PWD "report\$stamp.xlsx"
New-Item -ItemType Directory $out -Force|Out-Null
$shots=@{确认="1795773549029";成功="1795773631987";产品="1795774460549";详情="1795774893899";保存="1795776077431";限制="1795776672058";负责人="1795776930813";编码="1795777565494";人员="1795778276035";无匹配="1795779210624";分类="1795779881946";编译="1795781410218";失败="1795782639119";无产品="1795783190006";无项目="1795783580855";空记录="1795783715283";最终="1795784159318"}
foreach($key in @($shots.Keys)){$dest=Join-Path $out "$key.png";Copy-Item -LiteralPath "C:\Users\BJB110\.agent-browser\tmp\screenshots\screenshot-$($shots[$key]).png" -Destination $dest;$shots[$key]=$dest}
$rows=[System.Collections.Generic.List[object]]::new()
function Case($state,$id,$chain,$check,$actual,$steps,$limit,$shot){$rows.Add([pscustomobject]@{state=$state;id=$id;chain=$chain;check=$check;actual=$actual;steps=("本地http://localhost:8081/，测试1，wangyiru。"+$steps);limit=$limit;shot=$shots[$shot]})}
$fix="业务维护人员确认并更新指南与当前无需审核流程；如仍要求审批，恢复正式前端入口后补测。"
$mismatch="指南要求审批，但确认页明确直接创建、无需审核。无待审批样本，未办理审批，未伪造结果。"
Case '已通过' '01-1甲' '创建通过' '项目入口应能创建' '王艺茹、测试1正确；项目入口完成第一模拟样本。' '项目代码，新建项目及产品，填写并核对确认页。' '模拟数据，非真实立项。' '确认'
Case '未通过' '01-1乙' '创建通过' '目录入口应能独立创建' '第二样本最终确认后提示服务器错误，状态码500，未重试。事后项目MC20261127B与产品927452均查不到。' '产品目录，新建项目代码及产品；名称本地主链目录入口模拟测试20261127，项目MC20261127B，产品927452，王艺茹，三类，测试模板01-研发设计，国内市场，注册人是，生产方是，转移否，填模拟原因；确认新建信息，确认创建。' '开发人员处理后补测。截图为失败后表单，提示已消失；500依据当次操作回执。不据空查询断言回滚。' '失败'
Case '已通过' '01-2' '创建通过' '完整参数应传入保存' '项目MC202611271728、产品927451，名称本地主链模拟测试202611271728，王艺茹、三类、模板01；国内、注册人是、生产方是、转移否、备注空；记录68已完成。' '项目代码，新建项目及产品，填写第一样本，核对确认页，确认创建一次。' '直接创建，不是送审；未做重复编码拒绝测试。' '确认'
Case '存疑' '01-3' '创建通过' '待审批数据隔离' $mismatch '联合创建确认页读取无需审核；完成后记录68已完成并已有正式数据。' $fix '成功'
Case '存疑' '01-4' '创建通过' '本人资格审批通过' $mismatch '项目代码，项目申请与审批，对照直接创建说明，未办理审批。' $fix '成功'
Case '已通过' '01-5' '创建通过' '正式数据应一致' '927451在两处各一条，项目代码一致、三类；三个目录已生成；正式负责人王艺茹、启用、空起止时间。初始属性与确认页一致。' '产品目录查927451，点击项目代码，核对详情、目录和项目权限。' '当前可见样本对应唯一，不代表全库审计。' '负责人'
Case '存疑' '01-6' '创建通过' '刷新保留及追溯' '正式数据重进仍在；后来项目申请与审批暂无数据，与先前记录68已完成不一致，显示范围原因未明。审批人时间不适用无需审核。' '先创建并查看记录68；重进两页；项目代码，项目申请与审批，列表为空。' '开发人员核查记录显示范围；未断言后台记录丢失。' '空记录'
Case '存疑' '02-1' '创建未通过' '独立待审批样本' ($mismatch+'第二样本创建失败不能当作驳回。') '产品目录填写第二样本，确认页读取直接创建说明。' $fix '确认'
Case '存疑' '02-2' '创建未通过' '回查待审批内容' '没有待审批状态可测；创建失败后的空查询不是审批不通过。' '项目申请与审批暂无数据；查询第二样本项目与产品。' $fix '空记录'
Case '存疑' '02-3' '创建未通过' '本人真实驳回' $mismatch '确认页无需审核，无本次待审批样本，未办理驳回。' $fix '成功'
Case '存疑' '02-4' '创建未通过' '驳回追溯应保留' '无真实驳回动作，不能验证申请、审批人、结果与时间。' '项目申请与审批，无待审批样本，未产生驳回记录。' $fix '空记录'
Case '存疑' '02-5' '创建未通过' '驳回应无正式数据' '未真实驳回，不能判通过。第二样本失败后两处查不到仅是失败后查询状态。' '第二样本创建失败；项目代码查MC20261127B，产品目录查927452。' $fix '无项目'
Case '已通过' '03-1' '项目查询维护' '准确定位正式项目' '已定位本轮项目MC202611271728，未用旧申请或近似名称替代。' '项目代码按产品编码927451查询并打开详情。' '当前直接创建，无审批步骤。' '编码'
Case '已通过' '03-2' '项目查询维护' '筛选及重置应正确' '按编码和完整名称各一条；按王艺茹三条含本次样本；重置恢复129条，第二页找到样本。' '依次按927451、完整项目名称、王艺茹查询；重置，第二页查找。' '负责人条件可匹配多个本人项目。' '人员'
Case '已通过' '03-3' '项目查询维护' '详情产品交叉一致' '名称、项目代码、927451、王艺茹、三类及空备注一致；初始国内市场和是非值与确认页一致。' '产品目录查927451，点击项目代码，详情与确认页对照。' '文件夹模板与文件模板不同，不将后者为空当失败。' '详情'
Case '已通过' '03-4' '项目查询维护' '修改保存刷新保持' '刷新后国内及欧盟市场、注册人否、生产方否、转移是；接收方主链模拟测试接收方，不作真实文件转移。负责人未变。' '详情，修改默认属性与负责人账号，填写上述模拟值和修改原因，保存，刷新重新打开。' '仅改本次模拟项目，不实际转移文件。' '保存'
Case '存疑' '03-5' '项目查询维护' '绑定及其他对象不变' '样本绑定仍在；未编辑其他项目，但无全部对象前后对照，整体无变化未覆盖。' '维护后重进产品目录未分类，核对927451和MC202611271728。' '样本对应已核实，不扩大为全库审计。' '最终'
Case '已通过' '04-1' '产品分类对应' '样本分类应能找到' '未分类显示九条，含927451。目录树未分类与产品管理分类三类是两个字段。' '产品目录，瑛泰产品的未分类，查看明细。' '未擅自补目录树类别。' '最终'
Case '已通过' '04-2' '产品分类对应' '产品信息应一致' '927451查询一条，产品名称与项目名相同、三类、项目MC202611271728，与确认页相符。' '产品目录按927451查询，横向查看名称编码分类项目。' '当前无需审批，与创建确认信息对照。' '产品'
Case '已通过' '04-3' '产品分类对应' '切分类及无匹配查询' '骨水泥定型模具显示一条所属产品，未分类九条；无匹配条件显示暂无数据，不残留旧产品。' '切换骨水泥定型模具和未分类；全部产品下按编码NO_MATCH_20261127查询。' '只查询，未改已有产品。' '无匹配'
Case '已通过' '04-4' '产品分类对应' '项目产品双向对应' '两页按927451各一条，项目代码一致，产品行可进入对应项目详情。' '产品目录查927451点击项目代码；项目代码按927451查询比较。' '当前可见样本验证，不代表全库无重复。' '编码'
Case '存疑' '04-5' '产品分类对应' '重进保持其他产品不变' '样本重进仍在且对应保持；没有其他产品逐项基线，整体无变化未证明。' '重新进入产品目录，未分类，核对927451；本轮未编辑其他产品。' '样本持久化通过，其他产品整体无影响部分未覆盖。' '最终'
Case '存疑' '05-1' '权限边界' '访问维护审批分别核实' '本人可访问创建维护；正式负责人已核实，独立审批资格未验证。' '两页访问，创建维护样本，查看权限及无需审核说明。' $fix '负责人'
Case '存疑' '05-2' '权限边界' '入口应对应真实授权' '列表详情创建维护可用；审批入口后续为空，未办理审批，未读取角色权限全表。' '两页查看列表详情创建；项目申请与审批。' '正向已测，完整权限对应和审批保留存疑。' '空记录'
Case '存疑' '05-3' '权限边界' '未授权项目产品应拒绝' '无明确未授权的项目产品样本，反向未覆盖。受控文件无查询权限不能替代该测试。' '当前项目详情看到受控文件无查询权限；未对明确禁止的项目产品进行读写验证。' '权限管理员提供明确不可访问样本后补测，不提权改授权。' '限制'
Case '存疑' '05-4' '权限边界' '审批资格允许或拒绝' ($mismatch+'不把无审批步骤说成无资格。') '联合创建确认页，项目申请与审批；未办理审批。' $fix '成功'
Case '存疑' '05-5' '权限边界' '检查应无业务副作用' '权限窗口仅查看取消，未保存；样本绑定仍在。无全量授权前后对照，整体零副作用未证明。' '详情，项目权限，查看正式负责人启用空期限，取消；回产品目录核对。' '未主动保存权限，不等于证明所有访问范围不变。' '负责人'
Case '未通过' '补充-1' '运行异常' '创建页不应编译失败' '填写第二样本期间弹出编译错误，文件夹模板编辑组件第七行表达式解析失败。关闭遮罩后继续，未修改源码；不能判与随后500同因。' '产品目录，新建项目代码及产品，填写第二样本期间出现错误，画面指向FolderTemplateLibraryEditor.vue第7行。' '开发人员处理编译错误并保持测试环境稳定后回归。' '编译'
$cmd=[System.Collections.Generic.List[object]]::new()
$headers=@('用例编号','测试主链','检查内容及预期','实际结果','完整测试路径','处置及覆盖边界','证据截图')
$colors=@{'已通过'='16756B';'未通过'='BA3C3C';'存疑'='98691C'}
$summary="五份指南26节点，拆分双入口并增加运行异常，共$($rows.Count)项。通过$(@($rows|Where-Object state -eq '已通过').Count)，未通过$(@($rows|Where-Object state -eq '未通过').Count)，存疑$(@($rows|Where-Object state -eq '存疑').Count)。未执行步骤列存疑，不是全部通过；全部新样本为模拟数据。"
foreach($sheet in @('已通过','未通过','存疑')) {
 if($sheet -eq '已通过'){$cmd.Add(@{command='set';path='/Sheet1';props=@{name=$sheet}})}else{$cmd.Add(@{command='add';parent='/';type='sheet';props=@{name=$sheet}})}
 $cmd.Add(@{command='set';path="/$sheet";props=@{freeze='D4';zoom='75';gridlines='false';tabColor=$colors[$sheet]}})
 foreach($line in 1,2){$cmd.Add(@{command='set';path="/$sheet/A$($line):G$($line)";props=@{merge='true'}})}
 $cmd.Add(@{command='set';path="/$sheet/A1";props=@{value="本地主链复测  $sheet  2026年11月27日";bold='true';size='18';fill=$colors[$sheet];'font.color'='FFFFFF'}})
 $cmd.Add(@{command='set';path="/$sheet/A2";props=@{value=$summary;'alignment.wrapText'='true';fill='EAF0F4'}})
 $cmd.Add(@{command='set';path="/$sheet/row[1]";props=@{height='32'}})
 $cmd.Add(@{command='set';path="/$sheet/row[2]";props=@{height='45'}})
 $widths=@(15,21,30,65,74,60,100)
 for($col=0;$col -lt 7;$col++){$letter=[char](65+$col);$cmd.Add(@{command='set';path="/$sheet/col[$letter]";props=@{width="$($widths[$col])"}});$cmd.Add(@{command='set';path="/$sheet/$($letter)3";props=@{value=$headers[$col];bold='true';fill=$colors[$sheet];'font.color'='FFFFFF'}})}
 $row=4
 foreach($case in @($rows|Where-Object state -eq $sheet)) {
  $values=@($case.id,$case.chain,$case.check,$case.actual,$case.steps,$case.limit)
  for($col=0;$col -lt 6;$col++){$letter=[char](65+$col);$cmd.Add(@{command='set';path="/$sheet/$letter$row";props=@{value=$values[$col];font='微软雅黑';size='11';'alignment.wrapText'='true';'alignment.vertical'='top';fill=$(if($row%2 -eq 0){'F4F7FA'}else{'FFFFFF'})}})}
  $cmd.Add(@{command='set';path="/$sheet/row[$row]";props=@{height='300'}})
  $cmd.Add(@{command='add';parent="/$sheet";type='picture';props=@{src=$case.shot;x='6';y="$($row-1)";width='7.2in';height='4.05in';name=$case.id;alt="用例$($case.id)浏览器证据。存疑画面仅说明限制或流程，不表示完成未执行动作。"}})
  $row++
 }
 $cmd.Add(@{command='set';path="/$sheet";props=@{autoFilter="A3:G$($row-1)"}})
}
$batch=Join-Path $out '报告生成指令.json'
$cmd|ConvertTo-Json -Depth 8|Set-Content $batch -Encoding utf8
$rows|ConvertTo-Json -Depth 5|Set-Content (Join-Path $out '用例记录.json') -Encoding utf8
New-Item -ItemType Directory (Join-Path $PWD 'report') -Force|Out-Null
officecli create $report
if($LASTEXITCODE -ne 0){throw '报告创建失败'}
officecli batch $report --input $batch --stop-on-error --json | Set-Content (Join-Path $out '生成回执.json') -Encoding utf8
if($LASTEXITCODE -ne 0){throw '报告写入失败'}
officecli close $report
Write-Output "REPORT=$report"
Write-Output "EVIDENCE=$out"
Write-Output $summary
