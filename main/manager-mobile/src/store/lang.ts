import type { Ref } from 'vue'
import type { SupportedLocale } from '@/i18n/locale'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getSystemLanguage, LANGUAGE_STORAGE_KEY, resolveLanguage } from '@/i18n/locale'

export type Language = SupportedLocale

export interface LangStore {
  currentLang: Ref<Language>
  changeLang: (lang: Language) => void
}

export const useLangStore = defineStore(
  'lang',
  (): LangStore => {
    const savedLang = uni.getStorageSync(LANGUAGE_STORAGE_KEY)
    const currentLang = ref<Language>(resolveLanguage(savedLang, getSystemLanguage()))

    const changeLang = (lang: Language) => {
      const next = resolveLanguage(lang, getSystemLanguage())
      currentLang.value = next
      uni.setStorageSync(LANGUAGE_STORAGE_KEY, next)
    }

    return {
      currentLang,
      changeLang,
    }
  },
  {
    persist: {
      key: 'lang',
      serializer: {
        serialize: state => JSON.stringify(state.currentLang),
        deserialize: (value) => {
          try {
            return { currentLang: resolveLanguage(JSON.parse(value), getSystemLanguage()) }
          }
          catch {
            return { currentLang: resolveLanguage(undefined, getSystemLanguage()) }
          }
        },
      },
    },
  },
)
