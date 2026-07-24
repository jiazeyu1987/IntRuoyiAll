import request from '@/config/axios'

export interface TtsGenerateReqVO {
  text: string
  provider?: string
  voice?: string
}

export interface AliyunNlsVoiceOption {
  value: string
  label: string
}

export interface AliyunNlsTokenStatusVO {
  saved: boolean
  configured: boolean
  source: 'saved' | 'runtime' | 'missing' | string
  maskedAccessToken?: string
}

export interface AliyunNlsDefaultsVO {
  defaultVoice: string
  voiceSaved: boolean
  voiceConfigured: boolean
  voiceSource: 'saved' | 'runtime' | 'missing' | string
  appKeySaved: boolean
  appKeyConfigured: boolean
  appKeySource: 'saved' | 'runtime' | 'missing' | string
  maskedAppKey?: string
  tokenSaved: boolean
  tokenConfigured: boolean
  tokenSource: 'saved' | 'runtime' | 'missing' | string
  maskedAccessToken?: string
}

export interface AliyunNlsTokenSaveReqVO {
  accessToken: string
}

export interface AliyunNlsDefaultVoiceSaveReqVO {
  voice: string
}

export interface AliyunNlsAppKeySaveReqVO {
  appKey: string
}

export const ALIYUN_NLS_VOICE_OPTIONS: AliyunNlsVoiceOption[] = [
  { value: 'xiaoyun', label: 'xiaoyun 女声' },
  { value: 'xiaogang', label: 'xiaogang 男声' },
  { value: 'ruoxi', label: 'ruoxi 女声' },
  { value: 'siqi', label: 'siqi 女声' }
]

export const TtsTestApi = {
  generateAudio: async (data: TtsGenerateReqVO): Promise<Blob> => {
    return (await request.postOriginal({
      url: '/ai/tts-test/generate',
      data,
      responseType: 'blob'
    })) as unknown as Blob
  },
  getAliyunNlsDefaults: async (): Promise<AliyunNlsDefaultsVO> => {
    return await request.get({ url: '/ai/tts-test/aliyun-nls-defaults' })
  },
  getAliyunNlsTokenStatus: async (): Promise<AliyunNlsTokenStatusVO> => {
    return await request.get({ url: '/ai/tts-test/aliyun-nls-token' })
  },
  saveAliyunNlsDefaultVoice: async (data: AliyunNlsDefaultVoiceSaveReqVO): Promise<boolean> => {
    return await request.put({ url: '/ai/tts-test/aliyun-nls-default-voice', data })
  },
  saveAliyunNlsAppKey: async (data: AliyunNlsAppKeySaveReqVO): Promise<boolean> => {
    return await request.put({ url: '/ai/tts-test/aliyun-nls-appkey', data })
  },
  saveAliyunNlsToken: async (data: AliyunNlsTokenSaveReqVO): Promise<boolean> => {
    return await request.put({ url: '/ai/tts-test/aliyun-nls-token', data })
  }
}
