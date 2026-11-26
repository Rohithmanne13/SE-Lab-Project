---
title: "OpenAPI Specification 3.1.0"
type: research
status: inbox
tags: [openapi, api-docs]
created: 2026-11-26
author: Abhiram P B S
related: [SE-7, CON-4, UI-1, "#15"]
---

# OpenAPI Specification 3.1.0

## Summary

The OpenAPI Specification version 3.1.0 (spec.openapis.org/oas/v3.1.0, Apache License 2.0) defines a standard, language-agnostic interface description for HTTP APIs. Our SRS requires the API to be documented in OpenAPI 3.x (CON-4, SE-7) and cites this version as reference [14].

## Key points

- The OpenAPI Specification (OAS) defines a standard, language-agnostic interface to HTTP APIs which allows both humans and computers to discover and understand the capabilities of the service without access to source code, documentation, or through network traffic inspection.
- An OpenAPI definition can be used by documentation generation tools to display the API, code generation tools to generate servers and clients in various programming languages, testing tools, and many other use cases.
- An OpenAPI document MUST contain at least one paths field, a components field or a webhooks field. It is itself a JSON object, which may be represented either in JSON or YAML format; field names are case sensitive.
- It is RECOMMENDED that the root OpenAPI document be named openapi.json or openapi.yaml; a document may be a single document or divided into multiple connected parts using Reference Objects and Schema Object $ref keywords.
- The root OpenAPI Object requires the openapi field (the OAS version number, not related to the API's own info.version) and the info field (metadata about the API); other fields include servers, paths, webhooks, components, security, tags and externalDocs.
- Path templating uses template expressions delimited by curly braces ({}) to mark a section of a URL path as replaceable by path parameters.
- Data types are based on JSON Schema Specification Draft 2020-12, and the Schema Object is a superset of it; integer is a JSON number without a fraction or exponent part, with formats such as int32 and int64.
- The major.minor part of the version (for example 3.1) designates the OAS feature set; patch versions only address errors or clarifications, and tooling that supports OAS 3.1 SHOULD be compatible with all OAS 3.1.* versions.

## Links

- Our API contract follows this specification as required in [[srs-requirements#SE-7|SE-7]]; the HTTP error format it will describe is in [[rfc-9457-problem-details]].
- Team observation (not from the source): money amounts in our API are JSON integers in minor units (CI-2), which matches the OAS integer type; we expect to describe them as integer with format int64.

## Requirements

- [[srs-requirements#SE-7|SE-7]]
- [[srs-requirements#CON-4|CON-4]]
- [[srs-requirements#UI-1|UI-1]]

Issues / PRs: #15

## Source

- https://spec.openapis.org/oas/v3.1.0
- Ingested 2026-11-26 by Abhiram P B S with the brain-ingest skill.
