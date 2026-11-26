---
title: "IEEE SRS template (Wiegers, 1999)"
type: research
status: distilled
tags: [course, srs, ieee]
created: 2026-11-20
author: Manne Rohith Sai
related: [SE-1, "#4", "PR #18"]
---

# IEEE SRS template (Wiegers, 1999)

## Summary

The IEEE-style SRS template by Karl E. Wiegers (1999) that the course provided. It fixes the structure our SRS follows: title page, table of contents, revision history, six numbered sections and three appendices, each with guidance on what to write.

## Key points

- Front matter: title page ("Software Requirements Specification for <Project>", version, author, organization, date), Table of Contents and a Revision History table with columns Name, Date, Reason For Changes, Version.
- Section 1 Introduction: 1.1 Purpose, 1.2 Document Conventions (e.g. whether detailed requirements inherit the priority of higher-level ones), 1.3 Intended Audience and Reading Suggestions, 1.4 Product Scope, 1.5 References (enough detail to obtain each reference).
- Section 2 Overall Description: 2.1 Product Perspective, 2.2 Product Functions, 2.3 User Classes and Characteristics, 2.4 Operating Environment, 2.5 Design and Implementation Constraints, 2.6 User Documentation, 2.7 Assumptions and Dependencies.
- Section 3 External Interface Requirements: user, hardware, software and communications interfaces.
- Section 4 System Features: per feature, x.1 Description and Priority (High/Medium/Low, optionally benefit, penalty, cost and risk rated 1-9), x.2 Stimulus/Response Sequences, x.3 Functional Requirements, each uniquely identified and concise, complete, unambiguous, verifiable and necessary.
- Section 5 Other Nonfunctional Requirements (performance, safety, security, software quality attributes, business rules) and Section 6 Other Requirements, where "any new sections that are pertinent to the project" may be added.
- Appendices: A Glossary, B Analysis Models (e.g. data flow, class, state-transition, entity-relationship diagrams), C To Be Determined List.

## Links

- The course's SE-1 requirement for the SRS is described in [[sdlc-and-workflow]]; the resulting requirement IDs are in [[srs-requirements]].

## Requirements

- [[srs-requirements#SE-1|SE-1]]

Issues / PRs: #4, PR #18

## Source

- docs/course/srs_template-ieee.doc
- Ingested 2026-11-20 by Manne Rohith Sai with the brain-ingest skill.
