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

/* Runs the JavaScript test suites. Maven calls this during the test phase. */

function runSuite(suite) {
    let failed = 0;
    const names = Object.keys(suite.tests);
    names.forEach(function (name) {
        try {
            suite.tests[name]();
            console.log('  ok   ' + name);
        } catch (e) {
            failed++;
            console.log('  FAIL ' + name);
            console.log('       ' + e.message);
        }
    });
    console.log((names.length - failed) + '/' + names.length + ' ' + suite.name + ' tests passed');
    return failed;
}

module.exports = { runSuite: runSuite };

if (require.main === module) {
    const suites = [require('./highlight.test'), require('./seed.test')];
    const failed = suites.reduce(function (total, suite) { return total + runSuite(suite); }, 0);
    process.exit(failed === 0 ? 0 : 1);
}
