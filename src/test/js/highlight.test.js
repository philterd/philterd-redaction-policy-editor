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
 * Tests the highlighting in the Test Policy output.
 *
 * Span offsets returned by /test-policy locate each span in the text that was submitted, but the
 * highlights are drawn over the filtered text, where every replacement of a different length has
 * shifted everything after it. That remapping is the part worth testing, and it is easy to get
 * subtly wrong: an error only shows up on the second span onwards, and only when a replacement is
 * not the same length as what it replaced.
 *
 * The functions are read out of the shipped policy-form.js rather than copied here, so this tests
 * the code that runs in the browser instead of a second implementation of the same idea.
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

/** Loads the highlighting functions with just enough of a DOM for them to run. */
function load() {
    const source = fs.readFileSync(SOURCE, 'utf8');
    const shim = `
        const out = { children: [], set innerHTML(v) { this.children = []; },
                      appendChild(c) { this.children.push(c); } };
        const document = {
            createElement: (tag) => ({ tag, className: '', title: '', textContent: '',
                                       appendChild() {} }),
            createTextNode: (text) => ({ tag: '#text', textContent: text }),
            getElementById: () => out
        };
    `;
    const body = shim
        + extract(source, 'el')
        + extract(source, 'parseSpans')
        + extract(source, 'renderRedactedText')
        + extract(source, 'spanTitle')
        + 'module.exports = { parseSpans, renderRedactedText, out };';
    const module = { exports: {} };
    new Function('module', body)(module);
    return module.exports;
}

const lib = load();

/** Renders and returns the marks, plus the text as the reader would see it. */
function render(filteredText, explanation) {
    lib.out.children = [];
    lib.renderRedactedText(filteredText, lib.parseSpans(explanation));
    return {
        text: lib.out.children.map(function (n) { return n.textContent; }).join(''),
        marks: lib.out.children.filter(function (n) { return n.tag === 'span'; })
    };
}

function span(start, end, type, text, replacement, confidence) {
    return {
        characterStart: start, characterEnd: end, filterType: type,
        text: text, replacement: replacement, confidence: confidence === undefined ? 0.9 : confidence
    };
}

function explanationOf(applied, identified) {
    return JSON.stringify({ appliedSpans: applied, identifiedSpans: identified || applied });
}

const tests = {
    'renders plain text when nothing was detected': function () {
        const result = render('Nothing to redact here.', explanationOf([]));
        assert.strictEqual(result.text, 'Nothing to redact here.');
        assert.strictEqual(result.marks.length, 0);
    },

    'highlights a single replacement that is longer than the text it replaced': function () {
        // "SSN 123-45-6789." -> "SSN {{{REDACTED-ssn}}}."
        const result = render('SSN {{{REDACTED-ssn}}}.',
            explanationOf([span(4, 15, 'SSN', '123-45-6789', '{{{REDACTED-ssn}}}')]));
        assert.strictEqual(result.marks.length, 1);
        assert.strictEqual(result.marks[0].textContent, '{{{REDACTED-ssn}}}');
        assert.strictEqual(result.marks[0].className, 'span-applied');
        assert.strictEqual(result.text, 'SSN {{{REDACTED-ssn}}}.');
    },

    'carries the offset shift across several spans': function () {
        // "Patient John Smith, SSN 123-45-6789, seen on 2026-01-15."
        // John  (4 chars)  -> ****                 same length
        // Smith (5 chars)  -> {{{REDACTED-city}}}  grows by 14
        // SSN   (11 chars) -> {{{REDACTED-ssn}}}   grows by 7
        // date  (10 chars) -> {{{REDACTED-date}}}  grows by 9
        const filtered = 'Patient **** {{{REDACTED-city}}}, SSN {{{REDACTED-ssn}}}, seen on {{{REDACTED-date}}}.';
        const result = render(filtered, explanationOf([
            span(8, 12, 'FIRST_NAME', 'John', '****', 1.0),
            span(13, 18, 'LOCATION_CITY', 'Smith', '{{{REDACTED-city}}}', 1.0),
            span(24, 35, 'SSN', '123-45-6789', '{{{REDACTED-ssn}}}'),
            span(45, 55, 'DATE', '2026-01-15', '{{{REDACTED-date}}}', 0.75)
        ]));
        assert.strictEqual(result.marks.length, 4);
        assert.deepStrictEqual(result.marks.map(function (m) { return m.textContent; }),
            ['****', '{{{REDACTED-city}}}', '{{{REDACTED-ssn}}}', '{{{REDACTED-date}}}']);
        // Nothing may be dropped or duplicated: what is rendered is exactly the filtered text.
        assert.strictEqual(result.text, filtered);
    },

    'sorts spans that arrive out of order': function () {
        // The engine returns spans in detection order, not document order.
        // Input: "a 123-45-6789 b 2026-01-15", so the SSN is at 2..13 and the date at 16..26.
        const filtered = 'a {{{REDACTED-ssn}}} b {{{REDACTED-date}}}';
        const result = render(filtered, explanationOf([
            span(16, 26, 'DATE', '2026-01-15', '{{{REDACTED-date}}}', 0.75),
            span(2, 13, 'SSN', '123-45-6789', '{{{REDACTED-ssn}}}')
        ]));
        assert.deepStrictEqual(result.marks.map(function (m) { return m.textContent; }),
            ['{{{REDACTED-ssn}}}', '{{{REDACTED-date}}}']);
        assert.strictEqual(result.text, filtered);
    },

    'marks a span that was identified but not applied': function () {
        // The ignored SSN survives into the output, so it keeps its original length, and the date
        // after it still has to be found at its shifted position.
        const filtered = 'SSN 123-45-6789 on {{{REDACTED-date}}}.';
        const result = render(filtered, explanationOf(
            [span(19, 29, 'DATE', '2026-01-15', '{{{REDACTED-date}}}', 0.75)],
            [span(19, 29, 'DATE', '2026-01-15', '{{{REDACTED-date}}}', 0.75),
             span(4, 15, 'SSN', '123-45-6789', '{{{REDACTED-ssn}}}')]));
        assert.strictEqual(result.marks.length, 2);
        assert.strictEqual(result.marks[0].className, 'span-identified');
        assert.strictEqual(result.marks[0].textContent, '123-45-6789');
        assert.strictEqual(result.marks[1].className, 'span-applied');
        assert.strictEqual(result.marks[1].textContent, '{{{REDACTED-date}}}');
        assert.strictEqual(result.text, filtered);
    },

    'labels each highlight with its filter type and confidence': function () {
        const result = render('SSN {{{REDACTED-ssn}}}.',
            explanationOf([span(4, 15, 'SSN', '123-45-6789', '{{{REDACTED-ssn}}}', 0.9)]));
        assert.strictEqual(result.marks[0].title, 'SSN, confidence 0.90');
    },

    'says so when a span was detected but left in the clear': function () {
        const result = render('SSN 123-45-6789.',
            explanationOf([], [span(4, 15, 'SSN', '123-45-6789', '{{{REDACTED-ssn}}}', 0.9)]));
        assert.strictEqual(result.marks[0].title, 'SSN, confidence 0.90 (detected, not redacted)');
    },

    'skips a span that does not land inside the filtered text': function () {
        // A span past the end cannot be highlighted honestly, so the text is still rendered whole.
        const result = render('short', explanationOf([span(100, 120, 'SSN', 'x', 'y')]));
        assert.strictEqual(result.text, 'short');
        assert.strictEqual(result.marks.length, 0);
    },

    'renders the text when the explanation is missing or unparseable': function () {
        assert.strictEqual(render('Some output.', '').text, 'Some output.');
        assert.strictEqual(render('Some output.', 'not json').text, 'Some output.');
        assert.strictEqual(render('Error: something failed', null).text, 'Error: something failed');
    }
};

let failed = 0;
Object.keys(tests).forEach(function (name) {
    try {
        tests[name]();
        console.log('  ok   ' + name);
    } catch (e) {
        failed++;
        console.log('  FAIL ' + name);
        console.log('       ' + e.message);
    }
});

console.log((Object.keys(tests).length - failed) + '/' + Object.keys(tests).length + ' highlight tests passed');
process.exit(failed === 0 ? 0 : 1);
