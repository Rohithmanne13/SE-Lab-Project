---
title: "Obsidian internal links"
type: research
status: inbox
tags: [obsidian, links, second-brain]
created: 2026-11-20
author: Manne Rohith Sai
related: [SB-1, SK-2, PR-5]
---

# Obsidian internal links

## Summary

Obsidian Help, "Internal links": how Obsidian links notes, headings, blocks and other files. Our vault uses these wikilinks to connect notes to each other and to requirement IDs.

## Key points

- Two equivalent link formats: wikilinks `[[Three laws of motion]]` (Obsidian's default) and Markdown links `[Three laws of motion](Three%20laws%20of%20motion.md)` (destination must be URL-encoded).
- Folder paths start at the vault root and use forward slashes, even on Windows: `[[Projects/Three laws of motion]]`. Linking to a note that does not exist yet creates it at that path when clicked.
- Characters `# | ^ : %% [[ ]]` may not work in link targets; safe filenames are recommended.
- Links to non-Markdown files must include the extension, e.g. `[[Figure 1.png]]`; prefixing `!` embeds the target.
- Heading links: `[[Note#Heading]]`; subheadings chain with more `#`; `[[#Heading]]` links within the same note.
- Block links: `[[Note#^block-id]]`, identifiers of letters, digits and dashes; block references are Obsidian-specific and do not work outside Obsidian.
- Display text: `[[Note|Custom name]]` or `[[Note#Heading|Section name]]`. Obsidian can update internal links automatically when a file is renamed.

## Links

- The vault structure these links live in: [[para-method]].
- Team observation (not from the source): we link requirements as heading links into the index note, e.g. `[[srs-requirements#FR-2.4|FR-2.4]]`, which our CI checks for broken links (PR-5).

## Requirements

- [[srs-requirements#SB-1|SB-1]]
- [[srs-requirements#SK-2|SK-2]]
- [[srs-requirements#PR-5|PR-5]]

## Source

- https://help.obsidian.md/links
- Ingested 2026-11-20 by Manne Rohith Sai with the brain-ingest skill.
