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
 * Schema-driven renderer for Phileas redaction policies.
 *
 * The whole form is generated from the selected version's JSON Schema (fetched from
 * /api/schemas/{version}). The output policy JSON is assembled directly from the form, so it always
 * conforms to the schema for the selected version. Nothing here is tied to a specific Phileas Java
 * model, so a new schema version requires no code change — only a new bundled schema file.
 */
(function () {
    "use strict";

    // ---- Module state -----------------------------------------------------------------------

    let rootSchema = null;     // the full schema document for the selected version
    let defs = {};             // $defs of rootSchema
    let currentVersion = null;
    let supportedTestVersion = null;
    let rootField = null;      // the live form model { get, set }

    const ACRONYMS = {
        ssn: "SSN", iban: "IBAN", url: "URL", pdf: "PDF", dpi: "DPI", ip: "IP", mac: "MAC",
        vin: "VIN", fpe: "FPE", ups: "UPS", usps: "USPS", fedex: "FedEx", tlds: "TLDs", id: "ID",
        pheye: "PhEye", pheyes: "PhEyes", iv: "IV", aes: "AES"
    };

    // The top-level property that holds the PII filters. It gets the prominent "Filters" section;
    // every other top-level property is rendered under "Advanced Options".
    const FILTERS_KEY = "identifiers";

    // ---- Schema helpers ---------------------------------------------------------------------

    function resolveRef(node) {
        if (node && typeof node === "object" && node.$ref) {
            const name = node.$ref.replace(/^#\/\$defs\//, "");
            return defs[name] || {};
        }
        return node;
    }

    // Returns a normalized view of a property's schema: resolves a top-level $ref and merges any
    // allOf branches into a single object schema, while preserving node-level default/description.
    function normalize(node) {
        if (!node || typeof node !== "object") {
            return {};
        }
        const ownDefault = node.default;
        const ownDescription = node.description;
        let resolved = resolveRef(node);

        // Merge allOf (used by every filter: allOf:[abstractFilterProperties] + local properties).
        if (resolved.allOf) {
            const merged = {
                type: "object",
                properties: {},
                required: [],
                additionalProperties: resolved.additionalProperties
            };
            resolved.allOf.forEach(function (branch) {
                const b = resolveRef(branch);
                Object.assign(merged.properties, b.properties || {});
                if (b.required) {
                    merged.required = merged.required.concat(b.required);
                }
            });
            // Local properties override; a property whose value is `true` keeps the inherited def.
            const localProps = resolved.properties || {};
            Object.keys(localProps).forEach(function (k) {
                if (localProps[k] === true) {
                    if (!merged.properties[k]) {
                        merged.properties[k] = {}; // permissive, no inherited definition found
                    }
                } else {
                    merged.properties[k] = localProps[k];
                }
            });
            if (resolved.required) {
                merged.required = merged.required.concat(resolved.required);
            }
            resolved = merged;
        }

        const out = Object.assign({}, resolved);
        if (ownDefault !== undefined) out.default = ownDefault;
        if (ownDescription !== undefined) out.description = ownDescription;
        return out;
    }

    function isObjectSchema(s) {
        return s && (s.type === "object" || s.properties || s.allOf ||
            (s.additionalProperties && typeof s.additionalProperties === "object" && !s.properties));
    }

    function isMapSchema(s) {
        return s && s.type === "object" && !s.properties &&
            s.additionalProperties && typeof s.additionalProperties === "object";
    }

    function titleCase(key) {
        const spaced = key
            .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
            .replace(/[_-]+/g, " ")
            .trim();
        return spaced.split(" ").map(function (word) {
            const lower = word.toLowerCase();
            if (ACRONYMS[lower]) return ACRONYMS[lower];
            return word.charAt(0).toUpperCase() + word.slice(1);
        }).join(" ");
    }

    // ---- Small DOM helpers ------------------------------------------------------------------

    function el(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined) node.textContent = text;
        return node;
    }

    function helpText(schema) {
        if (schema && schema.description) {
            const small = el("div", "form-text", schema.description);
            return small;
        }
        return null;
    }

    // ---- Field builders ---------------------------------------------------------------------
    // Every builder returns { el, get, set }. get() returns the value or `undefined` (meaning omit).
    // set(v) populates the control and marks it touched so the value is re-emitted on collect.

    function buildBoolean(key, schema) {
        const wrap = el("div", "form-check mb-2");
        const input = el("input", "form-check-input");
        input.type = "checkbox";
        input.id = "f_" + Math.random().toString(36).slice(2);
        if (schema.default === true) input.checked = true;
        const label = el("label", "form-check-label", titleCase(key));
        label.setAttribute("for", input.id);
        let touched = false;
        input.addEventListener("change", function () { touched = true; });
        wrap.appendChild(input);
        wrap.appendChild(label);
        const help = helpText(schema);
        if (help) wrap.appendChild(help);
        return {
            el: wrap,
            get: function () { return touched ? input.checked : undefined; },
            set: function (v) { input.checked = !!v; touched = true; }
        };
    }

    function buildNumber(key, schema) {
        const wrap = el("div", "mb-2");
        wrap.appendChild(el("label", "form-label", titleCase(key)));
        const input = el("input", "form-control");
        input.type = "number";
        if (schema.type === "integer") input.step = "1"; else input.step = "any";
        if (schema.minimum !== undefined) input.min = String(schema.minimum);
        if (schema.maximum !== undefined) input.max = String(schema.maximum);
        if (schema.default !== undefined) input.placeholder = String(schema.default);
        let touched = false;
        input.addEventListener("input", function () { touched = true; });
        wrap.appendChild(input);
        const help = helpText(schema);
        if (help) wrap.appendChild(help);
        return {
            el: wrap,
            get: function () {
                if (!touched || input.value === "") return undefined;
                const n = Number(input.value);
                return isNaN(n) ? undefined : n;
            },
            set: function (v) { input.value = (v === undefined || v === null) ? "" : v; touched = true; }
        };
    }

    function buildEnum(key, schema) {
        const wrap = el("div", "mb-2");
        wrap.appendChild(el("label", "form-label", titleCase(key)));
        const select = el("select", "form-select");
        (schema.enum || []).forEach(function (opt) {
            const o = el("option", null, opt);
            o.value = opt;
            if (opt === schema.default) o.selected = true;
            select.appendChild(o);
        });
        let touched = false;
        select.addEventListener("change", function () { touched = true; });
        wrap.appendChild(select);
        const help = helpText(schema);
        if (help) wrap.appendChild(help);
        return {
            el: wrap,
            get: function () { return touched ? select.value : undefined; },
            set: function (v) { select.value = v; touched = true; }
        };
    }

    function buildString(key, schema) {
        const wrap = el("div", "mb-2");
        wrap.appendChild(el("label", "form-label", titleCase(key)));
        const input = el("input", "form-control");
        input.type = "text";
        if (schema.default !== undefined) input.placeholder = String(schema.default);
        let touched = false;
        input.addEventListener("input", function () { touched = true; });
        wrap.appendChild(input);
        const help = helpText(schema);
        if (help) wrap.appendChild(help);
        return {
            el: wrap,
            get: function () { return (touched && input.value !== "") ? input.value : undefined; },
            set: function (v) { input.value = (v === undefined || v === null) ? "" : v; touched = true; }
        };
    }

    function buildStringArray(key, schema) {
        const wrap = el("div", "mb-2");
        wrap.appendChild(el("label", "form-label", titleCase(key)));
        const ta = el("textarea", "form-control");
        ta.rows = 3;
        ta.placeholder = "One per line";
        wrap.appendChild(ta);
        const help = helpText(schema);
        if (help) wrap.appendChild(help);
        return {
            el: wrap,
            get: function () {
                const items = ta.value.split("\n").map(function (s) { return s.trim(); })
                    .filter(function (s) { return s.length > 0; });
                return items.length ? items : undefined;
            },
            set: function (v) { ta.value = Array.isArray(v) ? v.join("\n") : ""; }
        };
    }

    // A key/value map editor for objects with additionalProperties (e.g. PhEye thresholds).
    function buildMap(key, schema) {
        const valueSchema = normalize(schema.additionalProperties);
        const numeric = valueSchema.type === "number" || valueSchema.type === "integer";
        const card = el("div", "border rounded p-2 mb-2");
        card.appendChild(el("div", "fw-semibold small mb-1", titleCase(key)));
        const rows = el("div");
        card.appendChild(rows);
        const help = helpText(schema);
        if (help) card.appendChild(help);

        function addRow(k, v) {
            const row = el("div", "d-flex gap-2 mb-1 map-row");
            const keyInput = el("input", "form-control form-control-sm");
            keyInput.placeholder = "label";
            keyInput.value = k || "";
            const valInput = el("input", "form-control form-control-sm");
            valInput.type = numeric ? "number" : "text";
            valInput.step = "any";
            valInput.placeholder = "value";
            valInput.value = (v === undefined || v === null) ? "" : v;
            const remove = el("button", "btn btn-outline-danger btn-sm", "×");
            remove.type = "button";
            remove.addEventListener("click", function () { row.remove(); });
            row.appendChild(keyInput);
            row.appendChild(valInput);
            row.appendChild(remove);
            rows.appendChild(row);
        }

        const addBtn = el("button", "btn btn-outline-secondary btn-sm mt-1", "+ Add entry");
        addBtn.type = "button";
        addBtn.addEventListener("click", function () { addRow("", ""); });
        card.appendChild(addBtn);

        return {
            el: card,
            get: function () {
                const obj = {};
                rows.querySelectorAll(".map-row").forEach(function (row) {
                    const inputs = row.querySelectorAll("input");
                    const k = inputs[0].value.trim();
                    if (!k) return;
                    obj[k] = numeric ? Number(inputs[1].value) : inputs[1].value;
                });
                return Object.keys(obj).length ? obj : undefined;
            },
            set: function (v) {
                rows.innerHTML = "";
                if (v && typeof v === "object") {
                    Object.keys(v).forEach(function (k) { addRow(k, v[k]); });
                }
            }
        };
    }

    // A repeatable list of objects (e.g. strategies, dictionaries, bounding boxes).
    function buildObjectArray(key, schema) {
        const itemSchema = normalize(schema.items);
        const itemLabel = titleCase(key).replace(/s$/, "");
        const autoSeed = /strateg/i.test(key); // start filters with one strategy, matching old UX

        const container = el("div", "mb-2");
        container.appendChild(el("div", "fw-semibold mb-1", titleCase(key)));
        const help = helpText(schema);
        if (help) container.appendChild(help);
        const list = el("div");
        container.appendChild(list);
        const items = [];

        function addItem(value) {
            const card = el("div", "border rounded p-2 mb-2 position-relative");
            const remove = el("button", "btn btn-outline-danger btn-sm float-end", "Remove");
            remove.type = "button";
            card.appendChild(remove);
            const field = buildObject(itemSchema, { bag: false });
            card.appendChild(field.el);
            list.appendChild(card);
            const entry = { card: card, field: field };
            items.push(entry);
            remove.addEventListener("click", function () {
                card.remove();
                const i = items.indexOf(entry);
                if (i >= 0) items.splice(i, 1);
            });
            if (value !== undefined) field.set(value);
            return field;
        }

        const addBtn = el("button", "btn btn-outline-secondary btn-sm", "+ Add " + itemLabel);
        addBtn.type = "button";
        addBtn.addEventListener("click", function () { addItem(undefined); });
        container.appendChild(addBtn);

        return {
            el: container,
            _autoSeed: autoSeed,
            addItem: addItem,
            get: function () {
                const out = [];
                items.forEach(function (entry) {
                    const v = entry.field.get();
                    if (v !== undefined) out.push(v);
                });
                return out.length ? out : undefined;
            },
            set: function (v) {
                list.innerHTML = "";
                items.length = 0;
                if (Array.isArray(v)) {
                    v.forEach(function (item) { addItem(item); });
                }
            }
        };
    }

    // Picks the right builder for a single property based on its (normalized) schema.
    function buildField(key, rawSchema) {
        const schema = normalize(rawSchema);
        if (isMapSchema(schema)) {
            return buildMap(key, schema);
        }
        if (schema.type === "array") {
            const items = normalize(schema.items);
            if (isObjectSchema(items)) {
                return buildObjectArray(key, schema);
            }
            return buildStringArray(key, schema);
        }
        if (isObjectSchema(schema)) {
            return buildObjectCard(key, schema);
        }
        if (schema.enum) {
            return buildEnum(key, schema);
        }
        if (schema.type === "boolean") {
            return buildBoolean(key, schema);
        }
        if (schema.type === "integer" || schema.type === "number") {
            return buildNumber(key, schema);
        }
        return buildString(key, schema);
    }

    // Wraps a nested object property in a titled, collapsible card.
    function buildObjectCard(key, schema) {
        const wrap = el("div", "mb-2");
        const header = el("div", "fw-semibold mb-1", titleCase(key));
        wrap.appendChild(header);
        const help = helpText(schema);
        if (help) wrap.appendChild(help);
        const body = el("div", "border-start ps-3");
        wrap.appendChild(body);
        const obj = buildObject(schema, { bag: false });
        body.appendChild(obj.el);
        return { el: wrap, get: obj.get, set: obj.set };
    }

    // Renders an object. With bag=false the properties render inline. With bag=true (used for the
    // large `identifiers` property bag) properties are added/removed on demand via a dropdown.
    function buildObject(schema, opts) {
        opts = opts || {};
        const props = schema.properties || {};
        const keys = Object.keys(props);

        if (opts.bag) {
            return buildPropertyBag(schema, keys);
        }

        const container = el("div");
        const fields = {};
        // Split scalar/simple props from composite (object/array-of-object) props for nicer layout.
        const simpleKeys = [];
        const compositeKeys = [];
        keys.forEach(function (k) {
            const ns = normalize(props[k]);
            const isComposite = isObjectSchema(ns) ||
                (ns.type === "array" && isObjectSchema(normalize(ns.items)));
            if (isComposite && !isMapSchema(ns)) compositeKeys.push(k); else simpleKeys.push(k);
        });

        // If this object has a strategies-style array, keep it primary and tuck the rest into a
        // collapsible "Options" panel to reduce clutter (purely a layout nicety, schema-driven).
        const primaryArrayKey = compositeKeys.find(function (k) { return /strateg/i.test(k); });

        function mount(k, parent) {
            const f = buildField(k, props[k]);
            fields[k] = f;
            parent.appendChild(f.el);
            if (f._autoSeed && f.addItem) {
                f.addItem(undefined);
            }
        }

        if (primaryArrayKey) {
            mount(primaryArrayKey, container);
            const otherKeys = simpleKeys.concat(compositeKeys.filter(function (k) { return k !== primaryArrayKey; }));
            if (otherKeys.length) {
                const id = "opt_" + Math.random().toString(36).slice(2);
                const toggle = el("button", "btn btn-link btn-sm p-0 mb-2", "Options");
                toggle.type = "button";
                toggle.setAttribute("data-bs-toggle", "collapse");
                toggle.setAttribute("data-bs-target", "#" + id);
                container.appendChild(toggle);
                const panel = el("div", "collapse");
                panel.id = id;
                otherKeys.forEach(function (k) { mount(k, panel); });
                container.appendChild(panel);
            }
        } else {
            keys.forEach(function (k) { mount(k, container); });
        }

        return {
            el: container,
            get: function () {
                const out = {};
                Object.keys(fields).forEach(function (k) {
                    const v = fields[k].get();
                    if (v !== undefined) out[k] = v;
                });
                return Object.keys(out).length ? out : undefined;
            },
            set: function (v) {
                if (!v || typeof v !== "object") return;
                Object.keys(fields).forEach(function (k) {
                    if (v[k] !== undefined) fields[k].set(v[k]);
                });
            }
        };
    }

    // Add/remove UI for a large property bag (the `identifiers` object).
    function buildPropertyBag(schema, keys) {
        const props = schema.properties || {};
        const container = el("div");

        const controls = el("div", "d-flex gap-2 mb-3");
        const select = el("select", "form-select");
        select.style.maxWidth = "320px";
        const addBtn = el("button", "btn btn-secondary", "Add Filter");
        addBtn.type = "button";
        controls.appendChild(select);
        controls.appendChild(addBtn);
        container.appendChild(controls);

        const list = el("div");
        container.appendChild(list);

        const added = {}; // key -> { card, field }

        function refreshOptions() {
            select.innerHTML = "";
            keys.forEach(function (k) {
                if (added[k]) return;
                const deprecated = (props[k] && props[k].deprecated) ||
                    (resolveRef(props[k]) && resolveRef(props[k]).deprecated);
                const o = el("option", null, titleCase(k) + (deprecated ? " (deprecated)" : ""));
                o.value = k;
                select.appendChild(o);
            });
            const empty = select.options.length === 0;
            select.disabled = empty;
            addBtn.disabled = empty;
            if (empty) {
                const o = el("option", null, "All filters added");
                select.appendChild(o);
            }
        }

        function addKey(k, value) {
            if (added[k]) {
                if (value !== undefined) added[k].field.set(value);
                return added[k].field;
            }
            const card = el("div", "card card-body mb-3");
            const head = el("div", "d-flex justify-content-between align-items-center mb-2");
            head.appendChild(el("h5", "mb-0", titleCase(k)));
            const remove = el("button", "btn btn-outline-danger btn-sm", "Remove");
            remove.type = "button";
            head.appendChild(remove);
            card.appendChild(head);

            const field = buildField(k, props[k]);
            card.appendChild(field.el);
            list.appendChild(card);
            added[k] = { card: card, field: field };

            remove.addEventListener("click", function () {
                card.remove();
                delete added[k];
                refreshOptions();
            });
            refreshOptions();
            if (value !== undefined) field.set(value);
            return field;
        }

        addBtn.addEventListener("click", function () {
            if (select.value) addKey(select.value, undefined);
        });

        refreshOptions();

        return {
            el: container,
            get: function () {
                const out = {};
                Object.keys(added).forEach(function (k) {
                    const v = added[k].field.get();
                    out[k] = (v === undefined) ? {} : v;
                });
                return Object.keys(out).length ? out : undefined;
            },
            set: function (v) {
                list.innerHTML = "";
                Object.keys(added).forEach(function (k) { delete added[k]; });
                refreshOptions();
                if (v && typeof v === "object") {
                    Object.keys(v).forEach(function (k) {
                        if (props[k]) addKey(k, v[k]);
                    });
                }
            }
        };
    }

    // ---- Root rendering ---------------------------------------------------------------------

    function renderForm() {
        const filtersRoot = document.getElementById("filters-root");
        const advancedRoot = document.getElementById("advanced-root");
        filtersRoot.innerHTML = "";
        advancedRoot.innerHTML = "";

        const props = rootSchema.properties || {};
        const fields = {};

        Object.keys(props).forEach(function (k) {
            if (k === FILTERS_KEY) {
                const f = buildObject(normalize(props[k]), { bag: true });
                fields[k] = f;
                filtersRoot.appendChild(f.el);
            }
        });

        // Everything that is not the filters bag goes under Advanced Options.
        Object.keys(props).forEach(function (k) {
            if (k === FILTERS_KEY) return;
            const f = buildField(k, props[k]);
            fields[k] = f;
            const section = el("div", "mb-3");
            section.appendChild(el("h5", null, titleCase(k)));
            section.appendChild(f.el);
            advancedRoot.appendChild(section);
        });

        rootField = {
            get: function () {
                const out = {};
                Object.keys(fields).forEach(function (k) {
                    const v = fields[k].get();
                    if (v !== undefined) out[k] = v;
                });
                return out;
            },
            set: function (policy) {
                Object.keys(fields).forEach(function (k) {
                    if (policy[k] !== undefined) fields[k].set(policy[k]);
                });
            }
        };
    }

    function buildPolicy() {
        const policy = rootField ? rootField.get() : {};
        if (document.getElementById("includeSchemaLink") &&
            document.getElementById("includeSchemaLink").checked && rootSchema.$id) {
            // $schema is a JSON Schema keyword, not a forbidden additional property, so it is safe
            // to include for editor autocompletion even though the schema sets additionalProperties:false.
            return Object.assign({ "$schema": rootSchema.$id }, policy);
        }
        return policy;
    }

    // ---- Schema/version loading -------------------------------------------------------------

    function loadSchema(version) {
        return fetch("/api/schemas/" + encodeURIComponent(version))
            .then(function (r) {
                if (!r.ok) throw new Error("Failed to load schema " + version);
                return r.json();
            })
            .then(function (schema) {
                rootSchema = schema;
                defs = schema.$defs || {};
                currentVersion = version;
                renderForm();
                updateTestAvailability();
                clearOutput();
            });
    }

    function updateTestAvailability() {
        const testBtn = document.getElementById("testPolicyBtn");
        const note = document.getElementById("testVersionNote");
        const matches = !supportedTestVersion || supportedTestVersion === currentVersion;
        if (testBtn) testBtn.disabled = !matches;
        if (note) {
            note.classList.toggle("d-none", matches);
            note.textContent = matches ? "" :
                "Testing is only available for schema version " + supportedTestVersion +
                " (the version this build's Phileas runtime supports).";
        }
    }

    /** True when the version selector offers the given version. */
    function hasVersionOption(version) {
        const select = document.getElementById("schema-version-select");
        if (!select) return false;
        return Array.prototype.some.call(select.options, function (o) { return o.value === version; });
    }

    // Reflect the selected schema version in the URL's ?version= param so the page is shareable.
    function updateVersionInUrl(version) {
        const url = new URL(window.location.href);
        url.searchParams.set("version", version);
        window.history.replaceState(null, "", url);
    }

    function init() {
        return fetch("/api/schemas")
            .then(function (r) { return r.json(); })
            .then(function (info) {
                supportedTestVersion = info.supportedTestVersion;
                const versions = info.versions || [];

                // Allow ?version=<x> in the URL to choose the schema version on page load.
                // An unknown/missing value falls back to the latest version.
                const requested = new URLSearchParams(window.location.search).get("version");
                const requestedIsValid = requested && versions.indexOf(requested) !== -1;
                const initial = requestedIsValid ? requested : (info.latest || versions[0]);

                const notice = document.getElementById("version-notice");
                if (notice) {
                    if (requested && !requestedIsValid) {
                        notice.textContent = "Schema version \"" + requested +
                            "\" is not available. Using version " + initial +
                            " instead. Available versions: " + versions.join(", ") + ".";
                        notice.classList.remove("d-none");
                    } else {
                        notice.classList.add("d-none");
                    }
                }

                const select = document.getElementById("schema-version-select");
                select.innerHTML = "";
                versions.forEach(function (v) {
                    const o = el("option", null, v + (v === info.supportedTestVersion ? " (testable)" : ""));
                    o.value = v;
                    if (v === initial) o.selected = true;
                    select.appendChild(o);
                });
                select.addEventListener("change", function () {
                    if (notice) notice.classList.add("d-none");
                    updateVersionInUrl(select.value);
                    loadSchema(select.value);
                });
                if (initial) {
                    updateVersionInUrl(initial);
                    return loadSchema(initial);
                }
            })
            .catch(function (err) {
                console.error(err);
                const filtersRoot = document.getElementById("filters-root");
                if (filtersRoot) {
                    filtersRoot.appendChild(el("div", "alert alert-danger",
                        "Failed to load policy schemas: " + err.message));
                }
            });
    }

    // ---- Output / actions -------------------------------------------------------------------

    function clearOutput() {
        const out = document.getElementById("policy-output");
        if (out) out.textContent = "";
        const results = document.getElementById("results-section");
        if (results) results.classList.add("d-none");
        const msgs = document.getElementById("validation-messages");
        if (msgs) { msgs.innerHTML = ""; msgs.classList.add("d-none"); }
    }

    function showValidation(valid, messages) {
        const msgs = document.getElementById("validation-messages");
        if (!msgs) return;
        msgs.innerHTML = "";
        msgs.classList.remove("d-none");
        if (valid) {
            msgs.className = "alert alert-success";
            msgs.textContent = "Policy is valid against schema version " + currentVersion + ".";
        } else {
            msgs.className = "alert alert-danger";
            msgs.appendChild(el("div", "fw-semibold",
                "Policy does not conform to schema version " + currentVersion + ":"));
            const ul = el("ul", "mb-0");
            messages.forEach(function (m) { ul.appendChild(el("li", null, m)); });
            msgs.appendChild(ul);
        }
    }

    function generatePolicy() {
        const policy = buildPolicy();
        const json = JSON.stringify(policy, null, 2);
        document.getElementById("policy-output").textContent = json;
        document.getElementById("results-section").classList.remove("d-none");
        document.getElementById("generate-button-container").classList.add("d-none");
        // Validate against the bundled schema for the selected version.
        fetch("/api/validate/" + encodeURIComponent(currentVersion), {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: json
        }).then(function (r) { return r.json(); })
            .then(function (res) { showValidation(res.valid, res.messages || []); })
            .catch(function (err) { console.error("Validation failed", err); });
    }

    function currentPolicyJson() {
        const out = document.getElementById("policy-output").textContent;
        return out && out.trim() ? out : JSON.stringify(buildPolicy(), null, 2);
    }

    function copyToClipboard() {
        const json = currentPolicyJson();
        navigator.clipboard.writeText(json).then(function () {
            const btn = document.getElementById("copyBtn");
            if (btn) { const t = btn.textContent; btn.textContent = "Copied!"; setTimeout(function () { btn.textContent = t; }, 1500); }
        });
    }

    function downloadPolicy() {
        const json = currentPolicyJson();
        const blob = new Blob([json], { type: "application/json" });
        const a = document.createElement("a");
        a.href = URL.createObjectURL(blob);
        a.download = "policy.json";
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(a.href);
    }

    function resetEditor() {
        renderForm();
        clearOutput();
        document.getElementById("generate-button-container").classList.remove("d-none");
        const testSection = document.getElementById("test-section");
        if (testSection) testSection.classList.add("d-none");
    }

    function loadPolicyObject(policy) {
        if (!policy || typeof policy !== "object") return;
        // Re-render a clean form first so removed/added sections reflect exactly the uploaded policy.
        renderForm();
        rootField.set(policy);
        clearOutput();
        document.getElementById("generate-button-container").classList.remove("d-none");
    }

    function handleFileUpload(event) {
        const file = event.target.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = function (e) {
            try {
                loadPolicyObject(JSON.parse(e.target.result));
            } catch (err) {
                console.error("Failed to parse policy JSON:", err);
                alert("Failed to parse policy JSON.");
            }
        };
        reader.readAsText(file);
        event.target.value = "";
    }

    // Presets are version-specific static policy files under /presets/<version>/<name>.json.
    let pendingPreset = null;

    function showPresetDisclaimer(preset) {
        pendingPreset = preset;
        const modalEl = document.getElementById("presetDisclaimerModal");
        const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
        modal.show();
    }

    function confirmLoadPreset() {
        const modalEl = document.getElementById("presetDisclaimerModal");
        bootstrap.Modal.getOrCreateInstance(modalEl).hide();
        if (!pendingPreset) return;
        fetch("/presets/" + encodeURIComponent(currentVersion) + "/" + pendingPreset + ".json")
            .then(function (r) {
                if (!r.ok) throw new Error("Preset not available for version " + currentVersion);
                return r.json();
            })
            .then(loadPolicyObject)
            .catch(function (err) { alert(err.message); });
    }

    // ---- Test feature -----------------------------------------------------------------------

    function showTestSection() {
        const s = document.getElementById("test-section");
        if (s) s.classList.remove("d-none");
    }

    function redactText() {
        const text = document.getElementById("testText").value;
        const body = JSON.stringify({ version: currentVersion, policy: buildPolicy(), text: text });
        fetch("/test-policy", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: body
        }).then(function (r) { return r.json(); })
            .then(function (res) {
                document.getElementById("redactedText").value = res.filteredText || "";
                const explBtn = document.getElementById("showExplanationBtn");
                if (res.explanation) {
                    document.getElementById("explanationText").value = res.explanation;
                    explBtn.classList.remove("d-none");
                } else {
                    explBtn.classList.add("d-none");
                }
            })
            .catch(function (err) {
                document.getElementById("redactedText").value = "Error: " + err.message;
            });
    }

    function toggleExplanation() {
        const s = document.getElementById("explanationSection");
        const btn = document.getElementById("showExplanationBtn");
        const hidden = s.classList.toggle("d-none");
        btn.textContent = hidden ? "Show Explanation" : "Hide Explanation";
    }

    // ---- Wire up ----------------------------------------------------------------------------

    function togglePhiSqlPanel() {
        const panel = document.getElementById("phisql-panel");
        if (panel) panel.classList.toggle("d-none");
    }

    function showPhiSqlMessages(isError, messages, version) {
        const box = document.getElementById("phisql-messages");
        if (!box) return;
        box.innerHTML = "";
        box.classList.remove("d-none");
        box.className = "mt-3 alert " + (isError ? "alert-danger" : "alert-success");
        if (isError) {
            box.appendChild(el("div", "fw-semibold", "PhiSQL could not be compiled:"));
            const ul = el("ul", "mb-0");
            (messages || []).forEach(function (m) { ul.appendChild(el("li", null, m)); });
            box.appendChild(ul);
        } else {
            box.textContent = "Compiled successfully" + (version ? " to schema version " + version : "") +
                ". The policy has been loaded into the editor below.";
        }
    }

    // Compiles the PhiSQL in the panel to a native policy and loads it into the form, where it can be
    // edited, tested, and downloaded like any other policy.
    function compilePhiSql() {
        const source = document.getElementById("phisql-input").value;
        fetch("/api/compile", {
            method: "POST",
            headers: { "Content-Type": "text/plain" },
            body: source
        }).then(function (r) { return r.json(); })
            .then(function (res) {
                if (!res.success) {
                    showPhiSqlMessages(true, res.errors || ["Unknown compile error."]);
                    return;
                }
                const policy = JSON.parse(res.policy);
                const target = res.schemaVersion;
                // PhiSQL compiles to one specific schema version. If the form is on a different
                // version, switch to the target so the policy renders against its own schema.
                if (target && target !== currentVersion && hasVersionOption(target)) {
                    const select = document.getElementById("schema-version-select");
                    select.value = target;
                    updateVersionInUrl(target);
                    loadSchema(target).then(function () {
                        loadPolicyObject(policy);
                        showPhiSqlMessages(false, [], target);
                    });
                    return;
                }
                loadPolicyObject(policy);
                showPhiSqlMessages(false, [], target);
            })
            .catch(function (err) {
                showPhiSqlMessages(true, [err.message]);
            });
    }

    window.PolicyEditor = {
        generatePolicy: generatePolicy,
        copyToClipboard: copyToClipboard,
        downloadPolicy: downloadPolicy,
        resetEditor: resetEditor,
        handleFileUpload: handleFileUpload,
        showPresetDisclaimer: showPresetDisclaimer,
        confirmLoadPreset: confirmLoadPreset,
        showTestSection: showTestSection,
        redactText: redactText,
        toggleExplanation: toggleExplanation,
        togglePhiSqlPanel: togglePhiSqlPanel,
        compilePhiSql: compilePhiSql
    };

    document.addEventListener("DOMContentLoaded", init);
})();
