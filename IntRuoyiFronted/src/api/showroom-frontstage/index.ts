import request from '@/config/axios'

export interface ShowroomNarrationQuery {
  targetType: 'COMPANY' | 'PRODUCT' | 'HALL'
  targetId: number
  audienceType: 'PUBLIC'
  language: 'ZH' | 'EN'
}

export interface ShowroomWebsiteConfigRequestContext {
  websiteConfig: unknown
  hallId?: number
  productId?: number
  narrationQuery?: ShowroomNarrationQuery
}

export interface ShowroomDisplayNarrationRespVO {
  text: string
  audioUrl: string
}

const buildDisplayUrl = (path: string) =>
  new URL(path, import.meta.env.VITE_BASE_URL || window.location.origin).toString()
const websiteConfigPath = '/showroom/display/website-config'

const createWebsiteConfigContext = (
  websiteConfig: unknown,
  extra: Omit<ShowroomWebsiteConfigRequestContext, 'websiteConfig'> = {}
): ShowroomWebsiteConfigRequestContext => ({
  websiteConfig,
  ...extra
})

export const ShowroomFrontstageApi = {
  getWebsiteConfig: async () => {
    return await request.get({ url: buildDisplayUrl(websiteConfigPath) })
  },
  getDisplayHome: async () => {
    return createWebsiteConfigContext(await ShowroomFrontstageApi.getWebsiteConfig())
  },
  getDisplayCompany: async () => {
    return createWebsiteConfigContext(await ShowroomFrontstageApi.getWebsiteConfig())
  },
  getDisplayHall: async (hallId: number) => {
    return createWebsiteConfigContext(await ShowroomFrontstageApi.getWebsiteConfig(), { hallId })
  },
  getDisplayProduct: async (productId: number) => {
    return createWebsiteConfigContext(await ShowroomFrontstageApi.getWebsiteConfig(), { productId })
  },
  getDisplayNarration: async (params: ShowroomNarrationQuery): Promise<ShowroomDisplayNarrationRespVO> => {
    return await request.get({ url: buildDisplayUrl('/showroom/display/narration'), params })
  }
}
