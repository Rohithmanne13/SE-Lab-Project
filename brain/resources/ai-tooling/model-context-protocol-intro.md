---
title: "Model Context Protocol introduction"
type: research
status: distilled
tags: [mcp, ai-tools]
created: 2026-11-23
author: Sunnapu Suhith Reddy
related: [SK-0, CON-4, "#13"]
---

# Model Context Protocol introduction

## Summary

"What is the Model Context Protocol (MCP)?" is the introduction page of the official MCP documentation at modelcontextprotocol.io - the page the course links in its SDLC document. It defines MCP as an open-source standard for connecting AI applications to external systems, gives example uses, explains who benefits, and names the clients that support it.

## Key points

- MCP (Model Context Protocol) is an open-source standard for connecting AI applications to external systems.
- Using MCP, AI applications like Claude or ChatGPT can connect to data sources (e.g. local files, databases), tools (e.g. search engines, calculators) and workflows (e.g. specialized prompts), so they can access key information and perform tasks.
- The page compares MCP to a USB-C port: just as USB-C is a standardized way to connect electronic devices, MCP is a standardized way to connect AI applications to external systems.
- Example uses: agents that access Google Calendar and Notion; Claude Code generating an entire web app from a Figma design; enterprise chatbots connecting to multiple databases across an organization; AI models creating 3D designs in Blender and printing them on a 3D printer.
- Developers: MCP reduces development time and complexity when building, or integrating with, an AI application or agent.
- AI applications or agents: MCP gives them access to an ecosystem of data sources, tools and apps. End-users: they get more capable AI applications that can access user data and take actions on the user's behalf when necessary.
- MCP is an open protocol supported across a wide range of clients and servers - AI assistants like Claude and ChatGPT and development tools like Visual Studio Code, Cursor and MCPJam - so an integration is built once and works everywhere.
- The documentation continues with building MCP servers (to expose data and tools), MCP clients (applications that connect to MCP servers) and MCP Apps (interactive apps that run inside AI clients), plus pages on architecture and security.

## Links

- The course lists "What is the Model Context Protocol?" among its tools and references in [[sdlc-and-workflow]] (section 9).
- Team observation (not from the source): MCP and Agent Skills ([[agent-skills-specification]]) are both ways to extend AI assistants. Our SRS requires Agent Skills (SK-0) but not MCP, and our product is a REST API (CON-4), so this note is background on our AI tooling, not a design input.

## Requirements

- [[srs-requirements#SK-0|SK-0]]
- [[srs-requirements#CON-4|CON-4]]

Issues / PRs: #13

## Source

- https://modelcontextprotocol.io/docs/2026-07-28/getting-started/intro
- Ingested 2026-11-23 by Sunnapu Suhith Reddy with the brain-ingest skill.
