import { afterEach, describe, expect, it } from 'vitest';
import i18n from '../src/i18n/index.js';

const originalLocale = i18n.locale;
const originalSilentTranslationWarn = i18n.silentTranslationWarn;

afterEach(() => {
  i18n.locale = originalLocale;
  i18n.silentTranslationWarn = originalSilentTranslationWarn;
});

describe('VueI18n runtime', () => {
  it('uses English for keys missing from the active locale', () => {
    i18n.silentTranslationWarn = true;
    i18n.mergeLocaleMessage('en', {
      'runtime.fallbackProbe': 'English fallback',
    });
    i18n.mergeLocaleMessage('vi', {
      'runtime.fallbackProbe': null,
    });
    i18n.locale = 'vi';

    expect(i18n.t('runtime.fallbackProbe')).toBe('English fallback');
  });
});
