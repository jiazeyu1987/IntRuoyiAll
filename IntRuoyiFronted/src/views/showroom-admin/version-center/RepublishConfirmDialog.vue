<template>
  <el-dialog
    :model-value="modelValue"
    destroy-on-close
    title="发布为当前线上版本"
    width="640px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="showroom-version-center-republish-dialog">
      <el-alert
        :closable="false"
        show-icon
        type="warning"
        title="这会复制所选历史版本为新的 published revision，并立即触发全局 showroom release 重建。"
      />

      <div class="showroom-version-center-republish-dialog__meta">
        <span>目标：{{ targetLabel }}</span>
        <span>源版本：{{ selectedVersion ? `V${selectedVersion.revisionNo}` : '未选择' }}</span>
      </div>

      <el-alert
        v-if="permissions.republishDisabledReason"
        :closable="false"
        show-icon
        type="warning"
        :title="permissions.republishDisabledReason"
      />

      <el-alert
        v-if="errorMessage"
        :closable="false"
        show-icon
        type="error"
        :title="errorMessage"
      />

      <el-alert
        v-if="globalReleaseBlockers.length > 0"
        :closable="false"
        show-icon
        type="error"
        title="GLOBAL_RELEASE 阻断"
      >
        <template #default>
          <ul class="showroom-version-center-republish-dialog__blockers">
            <li
              v-for="blocker in globalReleaseBlockers"
              :key="`global-${blocker.blockerCode}-${blocker.message}`"
            >
              [GLOBAL_RELEASE] {{ blocker.message }}
            </li>
          </ul>
        </template>
      </el-alert>

      <div v-if="republishReadiness.blockers.length > 0" class="showroom-version-center-republish-dialog__blockers">
        <h4>阻断项</h4>
        <ul>
          <li v-for="blocker in republishReadiness.blockers" :key="`${blocker.scope}-${blocker.blockerCode}`">
            [{{ blocker.scope }}] {{ blocker.message }}
          </li>
        </ul>
      </div>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button
        type="primary"
        :disabled="!permissions.canRepublish || !republishReadiness.ready"
        :loading="loading"
        @click="emit('confirm')"
      >
        确认发布
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type {
  VersionCenterBlocker,
  VersionCenterPermissionVO,
  VersionCenterRepublishReadiness,
  VersionCenterSnapshotVO
} from './contracts'

defineOptions({ name: 'RepublishConfirmDialog' })

const props = defineProps<{
  modelValue: boolean
  targetLabel: string
  selectedVersion: VersionCenterSnapshotVO | null
  permissions: VersionCenterPermissionVO
  republishReadiness: VersionCenterRepublishReadiness
  errorMessage: string
  errorBlockers: VersionCenterBlocker[]
  loading: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
}>()

const globalReleaseBlockers = computed(() =>
  props.errorBlockers.filter((blocker) => blocker.scope === 'GLOBAL_RELEASE')
)
</script>

<style scoped>
.showroom-version-center-republish-dialog {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.showroom-version-center-republish-dialog__meta,
.showroom-version-center-republish-dialog__blockers {
  color: #4b5563;
  font-size: 0.88rem;
  line-height: 1.6;
}

.showroom-version-center-republish-dialog__meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.showroom-version-center-republish-dialog__blockers h4 {
  margin: 0 0 8px;
  color: #172033;
}

.showroom-version-center-republish-dialog__blockers ul {
  margin: 0;
  padding-left: 18px;
}
</style>
