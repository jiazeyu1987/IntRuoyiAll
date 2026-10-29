<template>
  <el-button link type="primary" :disabled="closed || (!!access?.handoverId && !access.original)" @click="open">
    {{ access?.active && access.original ? '回收' : '交接' }}
  </el-button>
  <Dialog v-model="visible" :title="context?.canRecall ? '回收上市放行授权' : '交接上市放行'" width="580px">
    <div v-loading="loading">
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <el-alert v-if="context?.blocker" :title="context.blocker" type="warning" :closable="false" />
      <el-descriptions v-if="context" :column="1" border>
        <el-descriptions-item label="当前办理人">{{ context.currentUserName || '尚未交接' }}</el-descriptions-item>
      </el-descriptions>
      <ReleaseTaskNotificationEntry v-if="context?.workTaskId" :work-task-id="context.workTaskId">通知记录</ReleaseTaskNotificationEntry>
      <el-form v-if="context?.canHandover || context?.canRecall" label-position="top" class="mt-16px" @submit.prevent>
        <el-form-item v-if="context.canHandover" label="接收人" required>
          <UserSelectV2
            v-model="recipientUserId"
            :user-options="context.candidates.map(user => ({ id: String(user.id), nickname: user.name }))"
            placeholder="选择具备上市放行资格的人员"
            class="w-full"
          />
        </el-form-item>
        <el-form-item :label="context.canRecall ? '回收原因' : '交接原因'" required>
          <el-input v-model="reason" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-alert :title="context.canRecall ? '回收后对方将收到通知，失去该批记录的查看和办理权限。' : '交接后对方收到通知及个人工作台任务；您保留查看和回收权限。'" type="info" :closable="false" />
      </el-form>
    </div>
    <template #footer>
      <el-button :disabled="submitting" @click="visible=false">取消</el-button>
      <el-button v-if="context?.canHandover || context?.canRecall" type="primary" :loading="submitting"
        :disabled="loading || !reason.trim() || (context.canHandover && !recipientUserId)" @click="submit">
        {{ context.canRecall ? '确认回收' : '确认交接' }}
      </el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import { ref, watch } from 'vue'
import ReleaseTaskNotificationEntry from './ReleaseTaskNotificationEntry.vue'
import { getMarketReleaseHandover, changeMarketReleaseHandover, type MarketReleaseAccess, type MarketReleaseHandoverContext } from '@/api/mes/pro/edhr/marketReleaseHandover'
import { generateUUID } from '@/utils'
const props=defineProps<{batchExecutionId:string|number;access?:MarketReleaseAccess;closed:boolean}>()
const emit=defineEmits<{changed:[]}>()
const visible=ref(false),loading=ref(false),submitting=ref(false),error=ref(''),reason=ref(''),recipientUserId=ref<string>()
const context=ref<MarketReleaseHandoverContext>()
let generation=0,commandKey=''
watch(() => props.batchExecutionId,()=>{generation++;visible.value=false;context.value=undefined})
watch(visible,value=>{if(!value){generation++;reason.value='';context.value=undefined;loading.value=false}})
const open=async()=>{
  visible.value=true;loading.value=true;error.value='';reason.value='';recipientUserId.value=undefined
  commandKey=generateUUID();const token=++generation
  try {const value=await getMarketReleaseHandover(props.batchExecutionId);if(token===generation)context.value=value}
  catch(e){if(token===generation)error.value=e instanceof Error?e.message:String(e)}
  finally{if(token===generation)loading.value=false}
}
const submit=async()=>{
  if(submitting.value || !context.value)return
  const token=generation,current=context.value
  submitting.value=true;error.value=''
  try{
    await changeMarketReleaseHandover(current.canRecall?'recall':'assign',{
      batchExecutionId:props.batchExecutionId,recipientUserId:recipientUserId.value,reason:reason.value.trim(),
      expectedVersion:current.version,idempotencyKey:commandKey
    })
    if(token===generation){visible.value=false;emit('changed')}
  }catch(e){if(token===generation)error.value=e instanceof Error?e.message:String(e)}
  finally{submitting.value=false}
}
</script>
