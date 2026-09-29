import { describe, it, expect } from 'vitest';
import { parseComponent, compileToFunctions } from 'vue-template-compiler';
import { shallowMount } from '@vue/test-utils';
import sfcSource from '../src/components/FunctionDialog.vue?raw';

// The bug under test: params typed into the right panel were saved as the
// dictionary defaults. v-model writes land on f.params (the live object),
// @change writes land on tempFunctions/modifiedFunctions — saveSelection
// trusted modifiedFunctions alone, so any path that missed it saved defaults.
// The fix merges both sources field by field.

function buildComponent() {
  const parsed = parseComponent(sfcSource);
  expect(parsed.script).toBeTruthy();
  const script = parsed.script.content
    .replace(/^import .*$/gm, '')
    .replace('export default', 'return');
  // The script imports Api, i18n and featureManager; none are touched by
  // saveSelection, so stubs are enough here.
  const apiStub = {
    agent: { getAgentMcpAccessAddress: () => {}, getAgentMcpToolsList: () => {} },
  };
  const options = new Function('Api', 'i18n', 'featureManager', script)(
    apiStub,
    {},
    { waitForInitialization: async () => {}, getConfig: () => ({}) },
  );
  const compiled = compileToFunctions(parsed.template.content);
  return { options, compiled };
}

function mountDialog(allFunctions) {
  const { options, compiled } = buildComponent();
  options.render = compiled.render;
  options.staticRenderFns = compiled.staticRenderFns;
  return shallowMount(options, {
    propsData: {
      value: false,
      functions: [],
      allFunctions,
      agentId: 'agent-test',
    },
    mocks: {
      $t: key => key,
      $message: { error: () => {}, success: () => {} },
    },
    stubs: {
      'el-drawer': true,
      'el-checkbox': true,
      'el-button': true,
      'el-empty': true,
      'el-input': true,
      'el-input-number': true,
      'el-switch': true,
      'el-form': true,
      'el-form-item': true,
      'el-tooltip': true,
    },
  });
}

function makeFunction(id, name, defaults) {
  return {
    id,
    name,
    providerCode: name,
    fieldsMeta: Object.keys(defaults).map(key => ({ key, type: 'string', label: key })),
    params: { ...defaults },
  };
}

function findSaved(wrapper, id) {
  const emitted = wrapper.emitted('update-functions');
  expect(emitted).toBeTruthy();
  return emitted[0][0].find(f => f.id === id);
}

describe('FunctionDialog saveSelection', () => {
  it('keeps edits recorded via handleParamChange even when f.params is stale', async () => {
    const webSearch = makeFunction('SYSTEM_PLUGIN_WEB_SEARCH', 'Web Search', {
      provider: 'metaso',
      api_key: 'mk-XXXX',
    });
    const wrapper = mountDialog([webSearch]);
    wrapper.vm.selectedNames = ['Web Search'];
    wrapper.vm.currentFunction = webSearch;

    // @change path only: the temp copy carries the edits while the live
    // f.params object stays at its defaults (stale-copy scenario).
    await wrapper.vm.handleParamChange(webSearch, 'provider', 'tavily');
    await wrapper.vm.handleParamChange(webSearch, 'api_key', 'tvly-dev-key');
    wrapper.vm.saveSelection();

    const saved = findSaved(wrapper, 'SYSTEM_PLUGIN_WEB_SEARCH');
    expect(saved.params.provider).toBe('tavily');
    expect(saved.params.api_key).toBe('tvly-dev-key');
  });

  it('keeps v-model edits that landed directly on f.params', async () => {
    const webSearch = makeFunction('SYSTEM_PLUGIN_WEB_SEARCH', 'Web Search', {
      provider: 'metaso',
      api_key: 'mk-XXXX',
    });
    const wrapper = mountDialog([webSearch]);
    wrapper.vm.selectedNames = ['Web Search'];
    wrapper.vm.currentFunction = webSearch;

    // v-model path: the panel mutates the live object and the @change
    // handler never records it (the historical data-loss path).
    webSearch.params.provider = 'tavily';
    webSearch.params.api_key = 'tvly-dev-key';
    wrapper.vm.saveSelection();

    const saved = findSaved(wrapper, 'SYSTEM_PLUGIN_WEB_SEARCH');
    expect(saved.params.provider).toBe('tavily');
    expect(saved.params.api_key).toBe('tvly-dev-key');
  });

  it('merges both edit sources field by field, modified wins on conflict', async () => {
    const webSearch = makeFunction('SYSTEM_PLUGIN_WEB_SEARCH', 'Web Search', {
      provider: 'metaso',
      api_key: 'mk-XXXX',
      max_results: '5',
    });
    const wrapper = mountDialog([webSearch]);
    wrapper.vm.selectedNames = ['Web Search'];
    wrapper.vm.currentFunction = webSearch;

    // v-model edited one field on the live object, @change recorded another.
    webSearch.params.provider = 'tavily';
    await wrapper.vm.handleParamChange(webSearch, 'api_key', 'tvly-dev-key');
    wrapper.vm.saveSelection();

    const saved = findSaved(wrapper, 'SYSTEM_PLUGIN_WEB_SEARCH');
    expect(saved.params.provider).toBe('tavily');
    expect(saved.params.api_key).toBe('tvly-dev-key');
    expect(saved.params.max_results).toBe('5');
  });

  it('emits one entry per selected function, not per default', () => {
    const webSearch = makeFunction('SYSTEM_PLUGIN_WEB_SEARCH', 'Web Search', {
      provider: 'metaso',
    });
    const weather = makeFunction('SYSTEM_PLUGIN_WEATHER', 'Weather Query', {
      api_key: 'wkey',
    });
    const wrapper = mountDialog([webSearch, weather]);
    wrapper.vm.selectedNames = ['Web Search', 'Weather Query'];
    wrapper.vm.saveSelection();

    const emitted = wrapper.emitted('update-functions');
    expect(emitted[0][0].map(f => f.id)).toEqual([
      'SYSTEM_PLUGIN_WEB_SEARCH',
      'SYSTEM_PLUGIN_WEATHER',
    ]);
  });
});
