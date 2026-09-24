export const SUPPORTED_LOCALES = ['zh_CN', 'zh_TW', 'en', 'de', 'vi', 'pt_BR'] as const
export type SupportedLocale = typeof SUPPORTED_LOCALES[number]
export const DEFAULT_LOCALE: SupportedLocale = 'en'
export const LANGUAGE_STORAGE_KEY = 'app_language'

export function getSystemLanguage(): string | undefined {
  try {
    const info = uni.getSystemInfoSync()
    return info.language || info.osLanguage
  }
  catch {
    return undefined
  }
}

export function isSupportedLocale(lang: unknown): lang is SupportedLocale {
  return typeof lang === 'string' && (SUPPORTED_LOCALES as readonly string[]).includes(lang)
}

export function mapBrowserLanguage(browserLang?: string): SupportedLocale {
  if (!browserLang) {
    return DEFAULT_LOCALE
  }
  const normalized = browserLang.replace('_', '-').toLowerCase()
  if (normalized.indexOf('zh-hant') === 0) {
    return 'zh_TW'
  }
  if (normalized.indexOf('zh') === 0) {
    if (normalized === 'zh-tw' || normalized === 'zh-hk' || normalized === 'zh-mo') {
      return 'zh_TW'
    }
    return 'zh_CN'
  }
  if (normalized.indexOf('de') === 0) {
    return 'de'
  }
  if (normalized.indexOf('vi') === 0) {
    return 'vi'
  }
  if (normalized === 'pt-br' || normalized === 'pt') {
    return 'pt_BR'
  }
  return DEFAULT_LOCALE
}

export function resolveLanguage(savedLang: unknown, browserLang?: string): SupportedLocale {
  if (isSupportedLocale(savedLang)) {
    return savedLang
  }
  return mapBrowserLanguage(browserLang)
}

export function interpolate(template: string, params?: Record<string, string | number>): string {
  if (!params) {
    return template
  }
  let result = template
  Object.entries(params).forEach(([paramKey, paramValue]) => {
    const token = `{${paramKey}}`
    result = result.split(token).join(String(paramValue))
  })
  return result
}
