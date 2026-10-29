<template>
  <Dialog v-model="state.visible" title="设置直属主管" width="560px" @close="close">
    <el-alert
      v-if="state.error"
      :title="state.error"
      type="error"
      :closable="false"
      class="mb-12px"
    />
    <el-form v-loading="state.loading" :disabled="state.saving || !state.ready" label-width="100px">
      <el-form-item label="目标账号">
        <span>{{
          state.relation ? `${state.relation.username}（${state.relation.nickname}）` : '正在读取'
        }}</span>
      </el-form-item>
      <el-form-item label="当前主管">
        <span v-if="state.relation?.supervisorUserId">
          {{ state.relation.supervisorUsername || '账号不存在' }}（{{
            state.relation.supervisorNickname || '未记录名称'
          }}）
          <el-tag v-if="state.relation.supervisorStatus !== 0" type="warning" class="ml-8px"
            >已失效，请重新设置或清空</el-tag
          >
        </span>
        <span v-else>未设置</span>
      </el-form-item>
      <el-form-item label="选择主管">
        <UserSelectV2
          v-model="state.selectedId"
          :user-options="state.candidates"
          :option-mapper="mapSupervisorOption"
          :clearable="false"
          :disabled="state.saving || !state.ready"
          placeholder="请选择启用账号"
          @change="selected"
        />
        <el-button link type="warning" class="ml-12px" @click="clear">明确清空</el-button>
      </el-form-item>
      <el-alert
        v-if="state.notice"
        :title="state.notice"
        type="info"
        :closable="false"
        class="mb-12px"
      />
      <el-form-item label="变更原因" required>
        <el-input
          v-model="state.reason"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button
        v-hasPermi="['system:user:update']"
        type="primary"
        :loading="state.saving"
        :disabled="!state.ready || state.loading"
        @click="save"
        >保存关系</el-button
      >
      <el-button @click="close">取消</el-button>
    </template>
  </Dialog>
  <SupervisorHandoverDialog ref="handoverRef" />
</template>
<script setup lang="ts">
import { reactive } from 'vue'
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import SupervisorHandoverDialog from './SupervisorHandoverDialog.vue'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import {
  getDirectSupervisor,
  saveDirectSupervisor,
  getDirectSupervisorCandidates
} from '@/api/system/user/directSupervisor'
import {
  createSupervisorState,
  openSupervisor,
  closeSupervisor,
  clearSupervisor,
  submitSupervisor,
  type SupervisorPort,
  type SupervisorCandidate
} from './direct-supervisor-state'

const emit = defineEmits<{ saved: [userId: string] }>()
const state = reactive(createSupervisorState())
const handoverRef = ref<InstanceType<typeof SupervisorHandoverDialog>>()
const port: SupervisorPort = {
  get: getDirectSupervisor,
  save: saveDirectSupervisor,
  candidates: getDirectSupervisorCandidates
}
const close = () => closeSupervisor(state)
const clear = () => clearSupervisor(state)
const mapSupervisorOption = (user: SupervisorCandidate) => ({ ...user, id: String(user.id) })
const selected = () => {
  state.operation = 'SET'
  state.notice = ''
}
const open = (userId: number | string) =>
  openSupervisor(state, getVisitTenantId() || getTenantId(), userId, port)
const save = async () => {
  const target = state.userId
  const previous = state.relation
  const reason = state.reason.trim()
  const result = await submitSupervisor(state, port)
  // submitSupervisor 已校验请求代次；保存成功关闭弹窗会递增 sequence，不能再将成功结果作废。
  if (result && result.userId === target && state.userId === target) {
    emit('saved', result.userId)
    if (
      previous?.supervisorUserId &&
      result.supervisorUserId &&
      previous.supervisorUserId !== result.supervisorUserId
    ) {
      await handoverRef.value?.open({
        assigneeUserId: previous.supervisorUserId,
        assigneeName:
          previous.supervisorNickname || previous.supervisorUsername || previous.supervisorUserId,
        applicantUserId: result.userId,
        suggestedAssigneeUserId: result.supervisorUserId,
        reason,
        notice: `直属主管已变更。以下仅查询${result.nickname}发起且仍由原主管处理的待办，主管相关节点可逐项确认交给新主管。其他员工的待办不受影响。`
      })
    }
  }
}
defineExpose({ open })
</script>
