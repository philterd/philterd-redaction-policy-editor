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
 * Tests what a newly added filter card serializes to, using the bundled schema.
 *
 * Schema 1.3.0 renamed zipCodeFilterStrategy to zipCodeFilterStrategies and kept the old name as a
 * deprecated property. Both are strategy arrays, so both used to be seeded, and the policy carried
 * two lists. Phileas applies whichever comes last, which was the hidden deprecated one.
 */

const fs = require('fs');
const path = require('path');
const assert = require('assert');

const MAIN = path.join(__dirname, '..', '..', 'main', 'resources');
const SOURCE = path.join(MAIN, 'static', 'js', 'policy-form.js');
const SCHEMAS = path.join(MAIN, 'schemas');

// The form builders only create elements, append them, and read a few properties back. This is
// enough of the DOM for that; nothing here is rendered.
function element() {
    return {
        value: '', checked: false, options: [], children: [], style: {},
        classList: { add: function () {}, remove: function () {} },
        appendChild: function (c) { this.children.push(c); return c; },
        setAttribute: function () {}, addEventListener: function () {}, remove: function () {},
        querySelectorAll: function () { return []; },
        set innerHTML(v) { this.children = []; }
    };
}

function load() {
    const lines = fs.readFileSync(SOURCE, 'utf8').split('\n');
    const start = lines.findIndex(function (l) { return l.startsWith('    function resolveRef('); });
    const end = lines.findIndex(function (l) { return l.startsWith('    function buildPropertyBag('); });
    assert.ok(start >= 0 && end > start, 'form builders not found in policy-form.js');

    const version = fs.readdirSync(SCHEMAS)[0];
    const schema = JSON.parse(fs.readFileSync(
        path.join(SCHEMAS, version, 'redaction-policy-schema.json'), 'utf8'));

    const body = 'const defs = schema.$defs; const ACRONYMS = {};' + lines.slice(start, end).join('\n')
        + '\nreturn function (def) { return buildObject(normalize(defs[def]), { bag: false }).get(); };';
    return new Function('document', 'schema', body)({ createElement: element }, schema);
}

const newFilter = load();

const tests = {
    'a new filter starts with one strategy': function () {
        assert.deepStrictEqual(newFilter('filterSsn'), { ssnFilterStrategies: [{ strategy: 'REDACT' }] });
    },

    'a new filter does not seed a deprecated strategies array': function () {
        assert.deepStrictEqual(newFilter('filterZipCode'),
            { zipCodeFilterStrategies: [{ strategy: 'REDACT' }] });
    }
};

module.exports = { name: 'form', tests: tests };

if (require.main === module) {
    process.exit(require('./run').runSuite(module.exports) === 0 ? 0 : 1);
}
