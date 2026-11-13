<template>
  <div v-loading="loading" class="release-owners" data-flow-panel="release-owner-detail">
    <div class="release-owners__toolbar">
      <span>放行责任人</span>
      <el-button link type="primary" size="small" @click="loadOwners">刷新</el-button>
    </div>
    <div v-for="owner in owners" :key="owner.code" class="release-owners__card" :data-release-owner="owner.code">
      <strong>{{ owner.label }}</strong>
      <el-select model-value="ROLE_GROUP" disabled size="small" aria-label="负责人来源">
        <el-option label="权限角色" value="ROLE_GROUP" />
      </el-select>
      <el-select :model-value="owner.role?.id" disabled size="small" :aria-label="`${owner.label}权限角色`" :placeholder="loading ? '加载中' : owner.error ? '加载失败' : '默认角色未配置'">
        <el-option v-if="owner.role" :label="owner.role.name" :value="owner.role.id" />
      </el-select>
      <el-alert v-if="owner.error" :title="owner.error" type="error" :closable="false" />
      <span v-else-if="owner.users.length" class="release-owners__users">
        已授权：{{ owner.users.map(user => `${user.nickname}（${user.username}）${user.status === 0 ? '' : '【已停用】'}`).join('、') }}
      </span>
      <span v-else-if="!loading">尚未授权人员，请在权限角色中分配。</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ProRouteApi, type RouteReleaseOwnerVO } from '@/api/mes/pro/route'

const loading = ref(false)
const owners = ref([
  { label: '生产放行', code: 'MES_PQC_RELEASE_OWNER', role: undefined as RouteReleaseOwnerVO['role'] | undefined, users: [] as RouteReleaseOwnerVO['users'], error: '' },
  { label: '上市放行', code: 'MES_MANAGEMENT_REPRESENTATIVE', role: undefined as RouteReleaseOwnerVO['role'] | undefined, users: [] as RouteReleaseOwnerVO['users'], error: '' }
])
const loadOwners = async () => {
  if (loading.value) return
  loading.value = true
  try {
    await Promise.all(owners.value.map(async owner => {
      owner.role = undefined
      owner.users = []
      owner.error = ''
      try {
        const result = await ProRouteApi.getReleaseOwner(owner.code)
        owner.role = result.role
        owner.users = result.users
      } catch (error) {
        owner.error = error instanceof Error ? error.message : `${owner.label}角色加载失败`
      }
    }))
  } finally {
    loading.value = false
  }
}
onMounted(loadOwners)
</script>

<style scoped>
.release-owners { display: grid; gap: 8px; }
.release-owners__toolbar { display: flex; align-items: center; justify-content: space-between; }
.release-owners__card { display: grid; gap: 8px; padding: 10px; border: 1px solid #d9e5f7; border-radius: 6px; background: #fafcff; }
.release-owners__card .el-select { width: 100%; min-width: 0; }
.release-owners__users { color: #45618a; overflow-wrap: anywhere; }
</style>
