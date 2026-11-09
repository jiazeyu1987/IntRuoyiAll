<template>
 <el-card class="mt-16px" shadow="never"><template #header>证件附件与完整审批历史</template>
  <el-alert v-if="error" :title="error" type="error" :closable="false"/>
  <el-table :data="evidence.files" border><el-table-column prop="originalName" label="文件"/><el-table-column label="材料类型"><template #default="{row}">{{fileLabel(row.fileKind)}}</template></el-table-column><el-table-column label="操作"><template #default="{row}">
   <el-button link @click="emit('preview',row.id,row.originalName)">查看</el-button><el-button v-if="downloadable.has(String(row.id))" link type="primary" @click="emit('download',row.id)">下载</el-button><el-button v-else link @click="emit('requestDownload',row.id)">申请下载</el-button>
  </template></el-table-column></el-table>
  <h4>每次批准后的完整记录</h4><el-collapse><el-collapse-item v-for="snapshot in evidence.snapshots" :key="snapshot.id" :title="`第 ${snapshot.revisionNo} 次记录`"><el-form label-width="140px"><FactsFields :model-value="parseFacts(snapshot.factsJson)" readonly/></el-form><el-descriptions :column="2" border><el-descriptions-item v-for="field in supplemental" :key="field.key" :label="field.label">{{display(snapshot[field.key])}}</el-descriptions-item></el-descriptions></el-collapse-item></el-collapse>
  <h4>上传、变更、驳回与重提记录</h4><el-table :data="evidence.applications" border><el-table-column prop="id" label="申请"/><el-table-column prop="requesterName" label="申请人"/><el-table-column prop="requestedAt" label="提交时间" :formatter="dateTimeValueFormatter"/><el-table-column label="审批状态"><template #default="{row}">{{statusLabel(row.status)}}</template></el-table-column><el-table-column prop="completedAt" label="审核时间" :formatter="dateTimeValueFormatter"/><el-table-column prop="rejectReason" label="驳回原因"/><el-table-column label="留档"><template #default="{row}"><el-button link @click="inspect(row)">查看本次申请与审核</el-button></template></el-table-column></el-table>
  <el-dialog v-model="showSubmission" title="本次申请与审核留档" width="960px">
   <el-descriptions :column="2" border><el-descriptions-item label="申请人">{{selected?.requesterName}}</el-descriptions-item><el-descriptions-item label="提交时间">{{formatDateTimeValue(selected?.requestedAt)}}</el-descriptions-item><el-descriptions-item label="审批状态">{{statusLabel(selected?.status)}}</el-descriptions-item><el-descriptions-item label="审核时间">{{formatDateTimeValue(selected?.completedAt)}}</el-descriptions-item><el-descriptions-item label="审批人">{{selected?.reviewerName || approval.reviewerName || (approval.reviewerUserId ? `用户 #${approval.reviewerUserId}` : '见原审批流程记录')}}</el-descriptions-item><el-descriptions-item label="审核意见">{{selected?.rejectReason || approval.reviewComment || '未填写意见'}}</el-descriptions-item></el-descriptions>
   <el-alert v-if="!submissionFacts" class="mt-12px" title="该历史申请未保存完整字段，以下按当次变更明细和原文件留档展示。" type="warning" :closable="false"/>
   <el-form v-if="submissionFacts" label-width="140px" class="mt-12px"><FactsFields :model-value="submissionFacts" readonly/></el-form>
   <el-descriptions v-if="submissionDraft" :column="2" border><el-descriptions-item v-for="field in supplemental" :key="field.key" :label="field.label">{{display(submissionDraft[field.key])}}</el-descriptions-item></el-descriptions>
   <el-descriptions :column="2" border><el-descriptions-item label="变更批件批准日期">{{submission.approvalDate || '未填写'}}</el-descriptions-item><el-descriptions-item label="其他变更说明">{{submission.otherDescription || '未填写'}}</el-descriptions-item></el-descriptions>
   <el-table v-if="selected?.changes?.length" :data="selected.changes" border class="mt-12px"><el-table-column label="变更项目"><template #default="{row}">{{changeLabels[row.itemType] || row.itemType}}</template></el-table-column><el-table-column label="变更前"><template #default="{row}"><div class="whitespace-pre-wrap">{{changeDisplay(row.beforeValueJson)}}</div></template></el-table-column><el-table-column label="申请变更后"><template #default="{row}"><div class="whitespace-pre-wrap">{{changeDisplay(row.afterValueJson)}}</div></template></el-table-column></el-table>
   <el-button v-if="selected?.bpmProcessInstanceId" class="mt-12px" @click="showMaterials=true">查看本次审批材料</el-button>
   <el-table :data="selected?.files || []" border class="mt-12px"><el-table-column prop="originalName" label="本次提交文件"/><el-table-column label="材料类型"><template #default="{row}">{{fileLabel(row.fileKind)}}</template></el-table-column><el-table-column label="本次审核结果"><template #default="{row}">{{statusLabel(row.status)}}</template></el-table-column><el-table-column label="已批准文件"><template #default="{row}"><el-button v-if="row.status==='APPROVED'" link @click="emit('preview',row.id,row.originalName)">查看</el-button></template></el-table-column></el-table>
  </el-dialog>
  <el-dialog v-model="showMaterials" title="本次审批材料" width="1000px" destroy-on-close><RequestDetailPanel v-if="selected?.bpmProcessInstanceId" :request-id="Number(selected.id)" :process-instance-id="selected.bpmProcessInstanceId"/></el-dialog>
 </el-card>
</template>
<script setup lang="ts">
import {ref,watch} from 'vue'
import {dateTimeValueFormatter,formatDateTimeValue} from '@/utils/formatTime'
import {getRegistrationCertificateEvidence,getRegistrationCertificateFileDownloadGrantStatuses} from '@/api/dcc/registrationCertificate'
import FactsFields from '../shared/FactsFields.vue'
import RequestDetailPanel from '../approval/RequestDetailPanel.vue'
import {parseFacts,factLabels,type CertificateFacts} from '../shared/facts'
const props=defineProps<{certificateId:number|string;versionId:number|string}>()
const emit=defineEmits<{preview:[number|string,string];download:[number|string];requestDownload:[number|string]}>()
const evidence=ref<any>({files:[],snapshots:[],applications:[]}),error=ref(''),downloadable=ref(new Set<string>()),showSubmission=ref(false),showMaterials=ref(false),submissionFacts=ref<CertificateFacts>(),submissionDraft=ref<any>(),submission=ref<any>({}),selected=ref<any>(),approval=ref<any>({})
const supplemental=[{key:'productName',label:'产品名称'},{key:'registrantName',label:'注册人'},{key:'modelSpecification',label:'型号规格'},{key:'structureComposition',label:'结构组成'},{key:'intendedUse',label:'适用范围'},{key:'technicalRequirements',label:'技术要求'},{key:'residenceAddress',label:'住所'},{key:'productionAddress',label:'生产地址'},{key:'entrustedProduction',label:'是否委托生产'},{key:'selfProduction',label:'是否自行生产'},{key:'entrustedEnterprisesJson',label:'受托企业'}]
const changeLabels:Record<string,string>={CERTIFICATE_INFORMATION:'证件完整信息',PRODUCTION_RELATION:'生产关系',PRODUCT_NAME:'产品名称',REGISTRANT_NAME:'注册人',MODEL_SPECIFICATION:'型号规格',STRUCTURE_COMPOSITION:'结构组成',INTENDED_USE:'适用范围',TECHNICAL_REQUIREMENTS:'技术要求',RESIDENCE_ADDRESS:'住所',PRODUCTION_ADDRESS:'生产地址',OTHER_CONTENT:'其他内容'}
const fileLabel=(kind:string)=>({REGISTRATION_CERTIFICATE:'注册证／批准信函',CHANGE_APPROVAL:'变更批件',CERTIFICATE_ATTACHMENT:'附件',FDA_PRODUCT_DESCRIPTION:'FDA产品描述'}[kind] || kind)
const statusLabel=(status:string)=>({SUBMITTED:'待发起审核',BPM_BOUND:'审核中',REQUESTED:'待审核',APPROVED:'审核通过',REJECTED:'驳回',WITHDRAWN:'已撤回'}[status] || status)
const display=(value:any):string=>{if(value===undefined||value===null||value==='')return '未填写';if(typeof value==='boolean')return value?'是':'否';if(typeof value==='string'&&value.startsWith('[')){try{return display(JSON.parse(value))}catch{return value}}if(Array.isArray(value))return value.map(v=>typeof v==='object'?(v.enterpriseName || [v.revision,v.certificateNo,v.issueDate].filter(Boolean).join(' / ')):display(v)).join('；')||'无';return String(value)}
const changeDisplay=(json:string):string=>{
 const data=JSON.parse(json);let value=data.value??data
 if(typeof value==='string'&&value.startsWith('{'))value=JSON.parse(value)
 if(value&&typeof value==='object'&&!Array.isArray(value)){
  const values={...value,...(value.factsJson?parseFacts(value.factsJson):{}),...(data.value!==undefined?Object.fromEntries(Object.entries(data).filter(([key])=>key!=='value')):{})}
  return Object.entries(values).filter(([key])=>key in factLabels || key==='ceCertificates' || supplemental.some(f=>f.key===key)).map(([key,v])=>`${factLabels[key]||supplemental.find(f=>f.key===key)?.label||'CE证书记录'}：${display(v)}`).join('\n')
 }
 return display(value)
}
const inspect=(row:any)=>{error.value='';try{selected.value=row;submission.value=JSON.parse(row.detailJson);approval.value=row.approvalDetailJson?JSON.parse(row.approvalDetailJson):{};submissionFacts.value=(submission.value.factsJson || submission.value.draft?.factsJson)?parseFacts(submission.value.factsJson || submission.value.draft?.factsJson):undefined;submissionDraft.value=submission.value.draft;showSubmission.value=true}catch(e){error.value=e instanceof Error?e.message:String(e)}}
let loadSequence=0
watch(()=>[props.certificateId,props.versionId],async()=>{
 const sequence=++loadSequence;error.value='';evidence.value={files:[],snapshots:[],applications:[]};downloadable.value=new Set();showSubmission.value=false;showMaterials.value=false;selected.value=undefined;submissionFacts.value=undefined;submissionDraft.value=undefined;submission.value={};approval.value={}
 try{const data=await getRegistrationCertificateEvidence(props.certificateId,props.versionId);if(sequence!==loadSequence)return
  const ids=data.files.map(f=>f.id),statuses=ids.length?await getRegistrationCertificateFileDownloadGrantStatuses(ids):[];if(sequence!==loadSequence)return
  evidence.value=data;downloadable.value=new Set(statuses.filter(s=>s.canDownload).map(s=>String(s.businessFileId)))
 }catch(e){if(sequence===loadSequence)error.value=e instanceof Error?e.message:String(e)}
},{immediate:true})
</script>
