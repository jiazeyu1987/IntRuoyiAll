import { defineStore } from 'pinia'
import { store } from '../index'
import type { DictDataVO } from '@/api/system/dict/dict.data'
import { CACHE_KEY, useCache } from '@/hooks/web/useCache'
import { getSimpleDictDataList } from '@/api/system/dict/dict.data'
import {
  authContextRevision,
  getAccessToken,
  getAuthSessionId,
  getTenantId,
  getVisitTenantId
} from '@/utils/auth'

const { wsCache } = useCache('sessionStorage')
// The old cache had no tenant owner and must never be reused.
wsCache.delete(CACHE_KEY.DICT_CACHE)

export interface DictValueType {
  value: any
  label: string
  clorType?: string
  colorType?: string
  cssClass?: string
}
export interface DictTypeType {
  dictType: string
  dictValue: DictValueType[]
}
export interface DictState {
  dictMap: Record<string, DictValueType[]>
  isSetDict: boolean
  owner: string
  requestNumber: number
  loading: boolean
}

function currentScope(): string {
  // Read the observable so computed consumers update immediately on tenant switches.
  void authContextRevision.value
  const tenantId = (getAccessToken() && getVisitTenantId()) || getTenantId()
  if (tenantId == null || tenantId === '') return ''
  return JSON.stringify([String(tenantId), getAuthSessionId()])
}
const cacheKey = (scope: string) => `${CACHE_KEY.DICT_CACHE}:${scope}`

export const useDictStore = defineStore('dict', {
  state: (): DictState => ({
    dictMap: {},
    isSetDict: false,
    owner: '',
    requestNumber: 0,
    loading: false
  }),
  getters: {
    getDictMap(): Record<string, DictValueType[]> {
      return this.owner === currentScope() ? this.dictMap : {}
    },
    getIsSetDict(): boolean {
      return this.owner === currentScope() && this.isSetDict
    }
  },
  actions: {
    async setDictMap(force = false) {
      const scope = currentScope()
      if (!scope) throw new Error('加载字典前必须选择租户')
      if (this.owner !== scope || force) {
        this.owner = scope
        this.dictMap = {}
        this.isSetDict = false
        this.loading = false
        this.requestNumber++
      }
      if (force) wsCache.delete(cacheKey(scope))
      const cached = wsCache.get(cacheKey(scope))
      if (cached) {
        this.dictMap = cached
        this.isSetDict = true
        return
      }
      if (this.loading) return
      const request = ++this.requestNumber
      const revision = authContextRevision.value
      this.loading = true
      try {
        const rows: DictDataVO[] = await getSimpleDictDataList()
        if (
          currentScope() !== scope ||
          authContextRevision.value !== revision ||
          this.requestNumber !== request
        )
          return
        const dictMap: Record<string, DictValueType[]> = Object.create(null)
        rows.forEach((row) => {
          if (!dictMap[row.dictType]) dictMap[row.dictType] = []
          dictMap[row.dictType].push({
            value: row.value,
            label: row.label,
            colorType: row.colorType,
            cssClass: row.cssClass
          })
        })
        this.dictMap = dictMap
        this.isSetDict = true
        wsCache.set(cacheKey(scope), dictMap, { exp: 60 })
      } finally {
        if (this.requestNumber === request) this.loading = false
      }
    },
    getDictByType(type: string) {
      if (!this.getIsSetDict && currentScope()) void this.setDictMap()
      return this.getDictMap[type]
    },
    invalidateDict() {
      const scope = currentScope()
      if (scope) wsCache.delete(cacheKey(scope))
      this.requestNumber++
      this.owner = scope
      this.dictMap = {}
      this.isSetDict = false
      this.loading = false
    },
    async resetDict() {
      await this.setDictMap(true)
    }
  }
})

export const useDictStoreWithOut = () => useDictStore(store)
