/*
 * Copyright 2026 Philterd, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Tests what a newly added strategy card starts with.
 *
 * A select that the author never touched reports no value, so a strategy card whose dropdown shows
 * the schema default would serialize to nothing, and a filter whose only strategy serializes to
 * nothing vanishes from the generated policy. The card therefore has to start with a real value.
 */

const fs = require('fs');
const path = require('path');
const assert = require('assert');

const SOURCE = path.join(__dirname, '..', '..', 'main', 'resources', 'static', 'js', 'policy-form.js');

function extract(source, name) {
    const start = source.indexOf('    function ' + name + '(');
    assert.ok(start >= 0, 'function not found in policy-form.js: ' + name);
    const marker = '\n    }\n';
    const end = source.indexOf(marker, start) + marker.length;
    return source.slice(start, end);
}

function load() {
    const source = fs.readFileSync(SOURCE, 'utf8');
    // normalize() resolves $ref against the loaded schema's $defs, which a caller supplies.
    const body = 'const defs = {};' + extract(source, 'resolveRef') + extract(source, 'normalize')
        + extract(source, 'seedFor') + 'module.exports = { seedFor: seedFor };';
    const module = { exports: {} };
    new Function('module', body)(module);
    return module.exports;
}

const lib = load();

// The shape of the real strategy schema: a defaulted strategy enum among other defaulted properties.
const STRATEGY_SCHEMA = {
    type: 'object',
    properties: {
        strategy: { type: 'string', enum: ['REDACT', 'MASK'], default: 'REDACT' },
        redactionFormat: { type: 'string', default: '{{{REDACTED-%t}}}' },
        maskCharacter: { type: 'string', default: '*' },
        salt: { type: 'boolean', default: false }
    }
};

const tests = {
    'seeds a new strategy card with the default strategy': function () {
        assert.deepStrictEqual(lib.seedFor(STRATEGY_SCHEMA), { strategy: 'REDACT' });
    },

    'seeds nothing else, so the policy carries only what was chosen': function () {
        const seed = lib.seedFor(STRATEGY_SCHEMA);
        assert.deepStrictEqual(Object.keys(seed), ['strategy'],
            'only the strategy belongs in the seed: ' + JSON.stringify(seed));
    },

    'seeds nothing when the strategy has no default': function () {
        assert.strictEqual(lib.seedFor({
            type: 'object',
            properties: { strategy: { type: 'string', enum: ['REDACT'] } }
        }), undefined);
    },

    'seeds nothing for an array of something other than strategies': function () {
        assert.strictEqual(lib.seedFor({
            type: 'object',
            properties: { x: { type: 'integer' }, y: { type: 'integer' } }
        }), undefined);
    },

    'reads the default through an allOf wrapper': function () {
        // Strategy schemas reach the form as $ref/allOf compositions, which normalize() flattens.
        assert.deepStrictEqual(lib.seedFor({
            allOf: [{ properties: { strategy: { type: 'string', default: 'MASK' } } }]
        }), { strategy: 'MASK' });
    }
};

module.exports = { name: 'seed', tests: tests };

if (require.main === module) {
    process.exit(require('./run').runSuite(module.exports) === 0 ? 0 : 1);
}
