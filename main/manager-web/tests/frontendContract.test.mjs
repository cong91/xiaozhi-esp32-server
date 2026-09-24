import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const addressBookSource = await readFile(
  new URL('../src/views/AddressBookManagement.vue', import.meta.url),
  'utf8',
);
const correctWordApiSource = await readFile(
  new URL('../src/apis/module/correctWord.js', import.meta.url),
  'utf8',
);
const functionDialogSource = await readFile(
  new URL('../src/components/FunctionDialog.vue', import.meta.url),
  'utf8',
);
const customTableSource = await readFile(
  new URL('../src/components/CustomTable.vue', import.meta.url),
  'utf8',
);
const httpRequestSource = await readFile(
  new URL('../src/apis/httpRequest.js', import.meta.url),
  'utf8',
);
const i18nIndexSource = await readFile(
  new URL('../src/i18n/index.js', import.meta.url),
  'utf8',
);

test('address-book permission state consistently uses the target device MAC', () => {
  assert.match(
    addressBookSource,
    /:value="selectedPermissions\.includes\(device\.deviceId\)"/,
  );
  assert.match(
    addressBookSource,
    /@change="\(checked\) => handlePermissionToggle\(device\.deviceId, checked\)"/,
  );
  assert.match(
    addressBookSource,
    /this\.selectedPermissions = this\.allDevices\.map\(d => d\.deviceId\)/,
  );
  assert.match(
    addressBookSource,
    /this\.originalPermissions\.includes\(device\.deviceId\)/,
  );
  assert.doesNotMatch(
    addressBookSource,
    /selectedPermissions\.includes\(device\.id\)/,
  );
  assert.doesNotMatch(
    addressBookSource,
    /originalPermissions\.includes\(device\.id\)/,
  );
  assert.match(
    addressBookSource,
    /requestId !== this\.permissionRequestSequence/,
  );
  assert.match(
    addressBookSource,
    /this\.selectedDevice\?\.deviceId !== macAddress/,
  );
  assert.match(
    addressBookSource,
    /this\.permissionsLoading = true;\s*this\.selectedPermissions = \[\];\s*this\.originalPermissions = \[\];/,
  );
  assert.match(
    addressBookSource,
    /handleSavePermissions\(\) \{\s*if \(this\.permissionsLoading\) return;/,
  );
});

test('correct-word pagination maps the UI page size to the backend limit query', () => {
  assert.match(
    correctWordApiSource,
    /new URLSearchParams\(\{\s*page: params\.page,\s*limit: params\.pageSize\s*\}\)/,
  );
  assert.doesNotMatch(correctWordApiSource, /pageSize: params\.pageSize/);
});

test('function dialog footer stays above the expanding MCP tools section', () => {
  const mcpLayer = functionDialogSource.match(
    /\.mcp-access-point\s*\{[^}]*z-index:\s*(\d+);/s,
  );
  const footerLayer = functionDialogSource.match(
    /\.drawer-footer\s*\{[^}]*z-index:\s*(\d+);/s,
  );

  assert.ok(mcpLayer, 'MCP section should define its stacking layer');
  assert.ok(footerLayer, 'drawer footer should define its stacking layer');
  assert.ok(Number(footerLayer[1]) > Number(mcpLayer[1]));
});

test('browser locale detection is not persisted as explicit user choice', () => {
  assert.doesNotMatch(i18nIndexSource, /persistLanguage\(resolved/);
});

test('shared table defaults use translated labels', () => {
  assert.match(customTableSource, /:label="resolvedSelectLabel"/);
  assert.match(customTableSource, /resolvedOperationsLabel\(\) \{/);
  assert.match(customTableSource, /this\.\$i18n \? this\.\$t\('common\.operation'\) : 'Actions'/);
  assert.match(customTableSource, /this\.\$i18n \? this\.\$t\('common\.loading'\) : 'Loading'/);
  assert.doesNotMatch(customTableSource, /default: 'Loading'/);
});

test('shared components keep standalone English fallbacks without i18n injection', () => {
  assert.match(customTableSource, /this\.\$i18n \? this\.\$t\('common\.select'\) : 'Select'/);
  assert.match(customTableSource, /this\.\$i18n \? this\.\$t\('common\.operation'\) : 'Actions'/);
  assert.match(customTableSource, /this\.\$i18n \? this\.\$t\('common\.loading'\) : 'Loading'/);
  assert.match(customTableSource, /default: ''/);
});

test('http transport toasts go through i18n instead of Chinese literals', () => {
  assert.match(httpRequestSource, /i18n\.t\('common\.networkRequestError'/);
  assert.match(httpRequestSource, /i18n\.t\('common\.cannotConnectServer'\)/);
  assert.match(httpRequestSource, /i18n\.t\('common\.connectingServer'/);
  assert.doesNotMatch(httpRequestSource, /似乎无法连接服务器/);
  assert.doesNotMatch(httpRequestSource, /正在连接服务器/);
  assert.doesNotMatch(httpRequestSource, /网络请求出现了错误/);
});

test('vue-i18n falls back to English and merges app catalogs last', () => {
  assert.match(i18nIndexSource, /fallbackLocale:\s*'en'/);
  assert.match(i18nIndexSource, /mergeLocaleMessages\(enLocale, en\)/);
  assert.match(i18nIndexSource, /mergeLocaleMessages\(viLocale, vi\)/);
  assert.doesNotMatch(i18nIndexSource, /fallbackLocale:\s*'zh_CN'/);
  assert.doesNotMatch(i18nIndexSource, /\.\.\.en,\s*\.\.\.enLocale/);
});
