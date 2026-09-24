import { describe, it, expect } from 'vitest';
import {
  DEFAULT_LOCALE,
  SUPPORTED_LOCALES,
  isSupportedLocale,
  mapBrowserLanguage,
  mergeLocaleMessages,
  persistLanguage,
  readLanguage,
  resolveLanguage,
} from '../src/i18n/locale.js';

describe('i18n locale contract', () => {
  it('exposes the six console locales', () => {
    expect(SUPPORTED_LOCALES).toEqual(['zh_CN', 'zh_TW', 'en', 'de', 'vi', 'pt_BR']);
    expect(DEFAULT_LOCALE).toBe('en');
  });

  it('accepts only supported locale codes', () => {
    expect(isSupportedLocale('vi')).toBe(true);
    expect(isSupportedLocale('zh_CN')).toBe(true);
    expect(isSupportedLocale('fr')).toBe(false);
    expect(isSupportedLocale('')).toBe(false);
    expect(isSupportedLocale(null)).toBe(false);
  });

  describe('mapBrowserLanguage', () => {
    it('maps Chinese regional tags', () => {
      expect(mapBrowserLanguage('zh-TW')).toBe('zh_TW');
      expect(mapBrowserLanguage('zh_HK')).toBe('zh_TW');
      expect(mapBrowserLanguage('zh-MO')).toBe('zh_TW');
      expect(mapBrowserLanguage('zh-Hant')).toBe('zh_TW');
      expect(mapBrowserLanguage('zh-CN')).toBe('zh_CN');
      expect(mapBrowserLanguage('zh')).toBe('zh_CN');
    });

    it('maps vi, de, pt and everything else to en', () => {
      expect(mapBrowserLanguage('vi-vn')).toBe('vi');
      expect(mapBrowserLanguage('de-DE')).toBe('de');
      expect(mapBrowserLanguage('pt_br')).toBe('pt_BR');
      expect(mapBrowserLanguage('pt')).toBe('pt_BR');
      expect(mapBrowserLanguage('fr-FR')).toBe('en');
      expect(mapBrowserLanguage('')).toBe('en');
      expect(mapBrowserLanguage(undefined)).toBe('en');
    });
  });

  describe('resolveLanguage', () => {
    it('keeps a valid saved locale', () => {
      expect(resolveLanguage('vi', 'zh-CN')).toBe('vi');
    });

    it('ignores garbage storage and uses the browser', () => {
      expect(resolveLanguage('fr', 'vi-VN')).toBe('vi');
      expect(resolveLanguage('nope', 'en-US')).toBe('en');
    });

    it('falls back to English when storage and browser are unusable', () => {
      expect(resolveLanguage('fr', 'ja')).toBe('en');
      expect(resolveLanguage(undefined, undefined)).toBe('en');
    });
  });

  it('lets the app catalog win over Element UI on colliding keys', () => {
    const elementLocale = { el: { pagination: 'el' }, 'common.cancel': 'element-cancel' };
    const appCatalog = { 'common.cancel': 'app-cancel' };
    expect(mergeLocaleMessages(elementLocale, appCatalog)['common.cancel']).toBe('app-cancel');
    expect(mergeLocaleMessages(elementLocale, appCatalog).el.pagination).toBe('el');
  });

  it('keeps the resolved locale usable when storage writes are blocked', () => {
    const storage = {
      setItem() {
        throw new Error('Storage is unavailable');
      },
    };

    expect(persistLanguage('vi', storage)).toBe('vi');
  });

  it('falls back when storage reads are blocked', () => {
    const storage = {
      getItem() {
        throw new Error('Storage is unavailable');
      },
    };

    expect(readLanguage(storage)).toBe(null);
  });
});
