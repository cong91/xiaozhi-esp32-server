export const SUPPORTED_LOCALES = ['zh_CN', 'zh_TW', 'en', 'de', 'vi', 'pt_BR'];
export const DEFAULT_LOCALE = 'en';
export const LANGUAGE_STORAGE_KEY = 'userLanguage';

export function isSupportedLocale(lang) {
  return SUPPORTED_LOCALES.includes(lang);
}

export function mapBrowserLanguage(browserLang) {
  if (!browserLang || typeof browserLang !== 'string') {
    return DEFAULT_LOCALE;
  }

  const normalized = browserLang.replace('_', '-').toLowerCase();
  if (normalized.indexOf('zh-hant') === 0) {
    return 'zh_TW';
  }
  if (normalized.indexOf('zh') === 0) {
    if (normalized === 'zh-tw' || normalized === 'zh-hk' || normalized === 'zh-mo') {
      return 'zh_TW';
    }
    return 'zh_CN';
  }
  if (normalized.indexOf('de') === 0) {
    return 'de';
  }
  if (normalized.indexOf('vi') === 0) {
    return 'vi';
  }
  if (normalized === 'pt-br' || normalized === 'pt') {
    return 'pt_BR';
  }
  return DEFAULT_LOCALE;
}

export function resolveLanguage(savedLang, browserLang) {
  if (isSupportedLocale(savedLang)) {
    return savedLang;
  }
  return mapBrowserLanguage(browserLang);
}

export function getLanguageStorage() {
  try {
    return typeof localStorage === 'undefined' ? undefined : localStorage;
  } catch {
    return undefined;
  }
}

export function readLanguage(storage) {
  if (storage && typeof storage.getItem === 'function') {
    try {
      return storage.getItem(LANGUAGE_STORAGE_KEY);
    } catch {
      return null;
    }
  }
  return null;
}

export function mergeLocaleMessages(elementLocale, appCatalog) {
  return { ...elementLocale, ...appCatalog };
}

export function persistLanguage(lang, storage) {
  const next = isSupportedLocale(lang) ? lang : DEFAULT_LOCALE;
  if (storage && typeof storage.setItem === 'function') {
    try {
      storage.setItem(LANGUAGE_STORAGE_KEY, next);
    } catch {
      // Storage can be unavailable in private or quota-limited browsers.
    }
  }
  return next;
}
