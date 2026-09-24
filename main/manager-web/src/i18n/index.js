import Vue from 'vue';
import VueI18n from 'vue-i18n';
import zhCN from './zh_CN';
import zhTW from './zh_TW';
import en from './en';
import de from './de';
import vi from './vi';
import ptBR from './pt_BR';

import enLocale from 'element-ui/lib/locale/lang/en'
import zhLocale from 'element-ui/lib/locale/lang/zh-CN'
import twLocale from 'element-ui/lib/locale/lang/zh-TW'
import deLocale from 'element-ui/lib/locale/lang/de'
import viLocale from 'element-ui/lib/locale/lang/vi'
import ptBRLocale from 'element-ui/lib/locale/lang/pt-br'
import {
  getLanguageStorage,
  mergeLocaleMessages,
  persistLanguage,
  readLanguage,
  resolveLanguage,
} from './locale';

Vue.use(VueI18n);

const getDefaultLanguage = () => {
  const storage = getLanguageStorage();
  const savedLang = readLanguage(storage);
  const browserLang = typeof navigator === 'undefined' ? undefined : (navigator.language || navigator.userLanguage);
  return resolveLanguage(savedLang, browserLang);
};

const i18n = new VueI18n({
  locale: getDefaultLanguage(),
  fallbackLocale: 'en',
  messages: {
    'zh_CN': mergeLocaleMessages(zhLocale, zhCN),
    'zh_TW': mergeLocaleMessages(twLocale, zhTW),
    'en': mergeLocaleMessages(enLocale, en),
    'de': mergeLocaleMessages(deLocale, de),
    'vi': mergeLocaleMessages(viLocale, vi),
    'pt_BR': mergeLocaleMessages(ptBRLocale, ptBR)
  }
});

export default i18n;

export const changeLanguage = (lang) => {
  i18n.locale = persistLanguage(lang, getLanguageStorage());
};
