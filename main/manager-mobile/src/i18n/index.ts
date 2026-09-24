import type { Language } from '@/store/lang'
import { ref } from 'vue'
import { useLangStore } from '@/store/lang'
import de from './de'

import en from './en'
import { DEFAULT_LOCALE, interpolate, isSupportedLocale } from './locale'
import pt_BR from './pt_BR'
import vi from './vi'
import zh_CN from './zh_CN'
import zh_TW from './zh_TW'

const messages: Record<Language, Record<string, string>> = {
  zh_CN,
  en,
  zh_TW,
  de,
  vi,
  pt_BR,
}

const currentLang = ref<Language>(DEFAULT_LOCALE)

export function initI18n() {
  const langStore = useLangStore()
  currentLang.value = langStore.currentLang
}

export function changeLanguage(lang: Language) {
  const next = isSupportedLocale(lang) ? lang : DEFAULT_LOCALE
  currentLang.value = next
  const langStore = useLangStore()
  langStore.changeLang(next)
}

function lookup(lang: Language, key: string): string | undefined {
  const pack = messages[lang]
  const value = pack?.[key]
  return typeof value === 'string' ? value : undefined
}

export function t(key: string, params?: Record<string, string | number>): string {
  const value
    = lookup(currentLang.value, key)
      ?? lookup('en', key)
      ?? lookup('zh_CN', key)
      ?? key
  return interpolate(value, params)
}

export function getCurrentLanguage(): Language {
  return currentLang.value
}

export function getSupportedLanguages(): { code: Language, name: string }[] {
  return [
    { code: 'zh_CN', name: '简体中文' },
    { code: 'en', name: 'English' },
    { code: 'zh_TW', name: '繁體中文' },
    { code: 'de', name: 'Deutsch' },
    { code: 'vi', name: 'Tiếng Việt' },
    { code: 'pt_BR', name: 'Português (Brasil)' },
  ]
}
