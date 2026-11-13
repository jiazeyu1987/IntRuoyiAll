<template>
  <el-alert v-if="!canReadLogs" title="仅管理员和审核员可以查看操作日志。" type="error" :closable="false" />
  <template v-else>
  <ContentWrap>
    <el-form inline>
      <el-form-item label="日志来源">
        <el-select :model-value="selectedId" class="!w-360px" filterable @change="selectSource">
          <el-option v-for="source in choices" :key="source.id" :value="source.id" :label="source.label" />
        </el-select>
      </el-form-item>
    </el-form>
    <el-alert v-if="!selected" title="该日志来源不存在或当前账号没有查看权限。" type="error" :closable="false" />
  </ContentWrap>
  <component v-if="selected?.component" :is="selected.component" :key="panelKey" />
  <BusinessLogPanel v-else-if="selected" :key="panelKey" :source="selected" />
  </template>
</template>
<script setup lang="ts">
import { hasPermission } from '@/directives/permission/hasPermi'
import BusinessLogPanel from './BusinessLogPanel.vue'
import { logSources } from './logSources'
import { useUserStore } from '@/store/modules/user'
import { canAccessOperationLogs } from '@/utils/operationLogAccess'
defineOptions({ name: 'SystemOperateLog' })
const route = useRoute(), router = useRouter()
const userStore = useUserStore()
const canReadLogs = computed(() => canAccessOperationLogs(userStore.getRoles))
const allowed = computed(() => logSources.filter(source => !source.permissions.length || hasPermission(source.permissions)))
const selectedId = computed(() => typeof route.query.logSource === 'string' ? route.query.logSource : allowed.value[0]?.id)
const selected = computed(() => allowed.value.find(source => source.id === selectedId.value))
const choices = computed(() => allowed.value.filter(source => !source.internal || source.id === selectedId.value))
const panelKey = computed(() => `${selectedId.value}:${route.fullPath}`)
const selectSource = (logSource: string) => router.replace({ path: route.path, query: { logSource } })
</script>
