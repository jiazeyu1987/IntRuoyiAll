<template>
  <div data-workflow-interaction-log class="mb-8px">
    <el-alert v-if="error || saveError" :title="error || saveError" type="error" :closable="false" />
    <el-button size="small" :loading="loading || saving > 0" @click="open">本人失败与取消日志</el-button>
    <Dialog v-model="visible" title="本人最近失败与取消日志" width="1100px" :fullscreen="false">
      <p>此处显示本人最近100条页面取消、校验拦截和服务器失败记录；成功业务动作在订单操作记录中查看。</p>
      <el-button :loading="loading" @click="load">刷新</el-button>
      <el-table :data="rows" max-height="600">
        <el-table-column prop="actionName" label="中文动作" min-width="300" />
        <el-table-column label="操作人" width="180"><template #default="{row}">{{row.actorName}}（{{row.actorUserId}}）</template></el-table-column>
        <el-table-column label="结果" width="90"><template #default="{row}">{{labels[row.resultStatus]}}</template></el-table-column>
        <el-table-column label="发生时间" width="190"><template #default="{row}">{{formatDate(row.occurredAt)}}</template></el-table-column>
      </el-table>
    </Dialog>
  </div>
</template>
<script setup lang="ts">
import {getOwnWorkflowLogs,type WorkflowLogEntry} from '../workflowInteractionLog'
import {formatDate} from '@/utils/formatTime'
defineProps<{saveError:string;saving:number}>()
const visible=ref(false),loading=ref(false),error=ref(''),rows=ref<WorkflowLogEntry[]>([])
const labels:Record<string,string>={FAILED:'失败',CANCELED:'已取消',BLOCKED:'已拦截'}
const load=async()=>{loading.value=true;error.value='';rows.value=[]
  try{rows.value=await getOwnWorkflowLogs()}catch{error.value='读取本人操作日志失败，请稍后重试。'}finally{loading.value=false}}
const open=async()=>{visible.value=true;await load()}
</script>
