<template>
 <ContentWrap v-loading="loading" class="mt-16px">
  <div class="mb-12px font-600">注册证办理任务</div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" />
  <template v-if="detail">
   <el-descriptions :column="2" border>
    <el-descriptions-item label="任务" :span="2">{{detail.task.title}}</el-descriptions-item>
    <el-descriptions-item label="原负责人">{{detail.task.responsibleName}}</el-descriptions-item>
    <el-descriptions-item label="当前办理人">{{detail.task.assignedName}}</el-descriptions-item>
    <el-descriptions-item label="办理期限">{{formatDateTimeValue(detail.task.dueAt)}}</el-descriptions-item>
    <el-descriptions-item label="状态">{{statuses[detail.task.status]}}</el-descriptions-item>
    <el-descriptions-item label="实际提交人">{{detail.task.actualHandlerName||'尚未提交'}}</el-descriptions-item>
    <el-descriptions-item label="审批结果">{{detail.task.result==='APPROVED'?'审批通过':detail.task.result==='REJECTED'?'审批驳回':detail.task.result==='WITHDRAWN'?'申请撤回':'尚无结果'}}</el-descriptions-item>
   </el-descriptions>
   <div class="mt-12px mb-12px">
    <el-button v-if="detail.canHandle" type="primary" @click="handle">{{detail.task.taskKind==='UPLOAD'?'上传注册证并提交审批':'上传新证并提交延续审批'}}</el-button>
    <el-button v-if="detail.canDelegate" @click="delegateVisible=!delegateVisible">授权办理 / 收回授权</el-button>
    <el-button @click="load">刷新任务</el-button>
   </div>
   <el-form v-if="delegateVisible&&detail.canDelegate" label-width="110px">
    <el-form-item label="指定办理人" required><el-select v-model="assignment.assignedUserId" remote filterable :remote-method="search" placeholder="输入姓名查询（本人可收回授权）"><el-option v-for="user in candidates" :key="user.id" :value="user.id" :label="user.name" /></el-select></el-form-item>
    <el-form-item label="授权截止" required><el-date-picker v-model="assignment.dueAt" type="datetime" /></el-form-item>
    <el-form-item label="授权说明" required><el-input v-model="assignment.reason" maxlength="1024" type="textarea" /></el-form-item>
    <el-button type="primary" :loading="saving" @click="delegate">保存授权并通知</el-button>
   </el-form>
   <el-table :data="detail.events" class="mt-16px" size="small">
    <el-table-column label="业务节点" width="105"><template #default="{row}">{{events[row.eventType]||row.eventType}}</template></el-table-column>
    <el-table-column prop="reason" label="授权说明 / 处理结果" min-width="180" />
    <el-table-column prop="actorName" label="操作人 / 授权人" width="130" />
    <el-table-column prop="assignedName" label="指定 / 实际办理人" width="135" />
    <el-table-column label="时间" width="155"><template #default="{row}">{{formatDateTimeValue(row.createdAt)}}</template></el-table-column>
   </el-table>
  </template>
  <UploadDialog v-model="uploadVisible" :task-project="detail?.project" :request-id="detail?.restartRequestId" @saved="load" />
  <RenewalDialog v-model="renewalVisible" :certificate="detail?.certificate" @saved="load" />
 </ContentWrap>
</template>
<script setup lang="ts">
import {formatDateTimeValue} from '@/utils/formatTime'
import {getHandlingTask,getHandlingCandidates,delegateHandlingTask,type HandlingDetail} from '@/api/dcc/registrationCertificate/handlingTasks'
import UploadDialog from '../upload/UploadDialog.vue'
import RenewalDialog from '../renewal/RenewalDialog.vue'
const props=defineProps<{taskId:string}>()
const detail=ref<HandlingDetail>(),loading=ref(false),saving=ref(false),error=ref(''),uploadVisible=ref(false),renewalVisible=ref(false),delegateVisible=ref(false)
const assignment=reactive({assignedUserId:'',dueAt:undefined as Date|undefined,reason:''})
const candidates=ref<Array<{id:string;name:string}>>([])
const statuses:Record<string,string>={OPEN:'待办理',SUBMITTED:'审批中',COMPLETED:'已完成'}
const events:Record<string,string>={CREATED:'创建任务',DELEGATED:'授权办理',SUBMITTED:'提交审批',APPROVED:'审批通过',REJECTED:'审批驳回',WITHDRAWN:'申请撤回'}
let generation=0
const load=async()=>{const token=++generation;loading.value=true;error.value='';try{const value=await getHandlingTask(props.taskId);if(token!==generation)return;detail.value=value;assignment.assignedUserId=value.task.assignedUserId;assignment.dueAt=new Date(value.task.dueAt);assignment.reason='';candidates.value=[{id:value.task.responsibleUserId,name:value.task.responsibleName},{id:value.task.assignedUserId,name:value.task.assignedName}]}catch(e){if(token===generation)error.value=e instanceof Error?e.message:String(e)}finally{if(token===generation)loading.value=false}}
const search=async(keyword:string)=>{candidates.value=await getHandlingCandidates(keyword,props.taskId)}
const delegate=async()=>{error.value='';if(!assignment.assignedUserId||!assignment.dueAt||!Number.isFinite(assignment.dueAt.getTime())||!assignment.reason.trim()){error.value='请填写办理人、授权截止时间和说明';return}saving.value=true;try{await delegateHandlingTask(props.taskId,{...assignment,dueAt:assignment.dueAt.getTime(),reason:assignment.reason.trim()});delegateVisible.value=false;await load()}catch(e){error.value=e instanceof Error?e.message:String(e)}finally{saving.value=false}}
const handle=()=>{if(!detail.value?.canHandle)return;if(detail.value.task.taskKind==='UPLOAD')uploadVisible.value=true;else renewalVisible.value=true}
watch(()=>props.taskId,()=>{detail.value=undefined;uploadVisible.value=false;renewalVisible.value=false;delegateVisible.value=false;void load()},{immediate:true})
onBeforeUnmount(()=>generation++)
</script>
