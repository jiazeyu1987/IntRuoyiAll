<template>
  <Dialog v-model="state.visible" title="设置直属主管" width="560px" @close="close">
    <el-alert v-if="state.error" :title="state.error" type="error" :closable="false" class="mb-12px" />
    <el-form v-loading="state.loading" :disabled="state.saving || !state.ready" label-width="100px">
      <el-form-item label="目标账号">
        <span>{{ state.relation ? `${state.relation.username}（${state.relation.nickname}）` : '正在读取' }}</span>
      </el-form-item>
      <el-form-item label="当前主管">
        <span v-if="state.relation?.supervisorUserId">
          {{ state.relation.supervisorUsername || '账号不存在' }}（{{ state.relation.supervisorNickname || '未记录名称' }}）
          <el-tag v-if="state.relation.supervisorStatus !== 0" type="warning" class="ml-8px">已失效，请重新设置或清空</el-tag>
        </span>
        <span v-else>未设置</span>
      </el-form-item>
      <el-form-item label="选择主管">
        <el-select v-model="state.selectedId" filterable placeholder="请选择启用账号" @change="selected">
          <el-option v-for="user in state.candidates" :key="String(user.id)" :value="String(user.id)" :label="`${user.username}（${user.nickname}）`" />
        </el-select>
        <el-button link type="warning" class="ml-12px" @click="clear">明确清空</el-button>
      </el-form-item>
      <el-alert v-if="state.notice" :title="state.notice" type="info" :closable="false" class="mb-12px" />
      <el-form-item label="变更原因" required>
        <el-input v-model="state.reason" type="textarea" :rows="3" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button v-hasPermi="['system:user:update']" type="primary" :loading="state.saving" :disabled="!state.ready || state.loading" @click="save">保存关系</el-button>
      <el-button @click="close">取消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { reactive } from 'vue'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import { getDirectSupervisor, saveDirectSupervisor, getDirectSupervisorCandidates } from '@/api/system/user/directSupervisor'
import { createSupervisorState, openSupervisor, closeSupervisor, clearSupervisor, submitSupervisor, type SupervisorPort } from './direct-supervisor-state'

const emit = defineEmits<{ saved: [userId: string] }>()
const state = reactive(createSupervisorState())
const port: SupervisorPort = { get: getDirectSupervisor, save: saveDirectSupervisor, candidates: getDirectSupervisorCandidates }
const close = () => closeSupervisor(state)
const clear = () => clearSupervisor(state)
const selected = () => { state.operation = 'SET'; state.notice = '' }
const open = (userId: number | string) => openSupervisor(state, getVisitTenantId() || getTenantId(), userId, port)
const save = async () => {
  const generation = state.sequence, target = state.userId
  const result = await submitSupervisor(state, port)
  if (result && generation === state.sequence && result.userId === target) emit('saved', result.userId)
}
defineExpose({ open })
</script>
