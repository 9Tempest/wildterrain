# GitHub Setup

Target remote:

```bash
https://github.com/9Tempest/wildterrain.git
```

The local repository already has this remote configured as `origin`.

## Current Blocker

`gh repo create 9Tempest/wildterrain --public --source . --remote origin --push` failed with:

```text
HTTP 401: Bad credentials
```

Refresh GitHub CLI authentication, then create and push:

```bash
gh auth login -h github.com
gh repo create 9Tempest/wildterrain --public --source . --remote origin --push --description "Forge 1.20.1 exploration mod framework with creatures, ecology, terrain, and ruins"
```

If the repository already exists after manual creation in the browser, use:

```bash
git push -u origin main
```

## After Push

Confirm:

- The README renders the Mossquill texture image.
- The `Build` GitHub Action appears.
- Creature and bug issue templates appear under new issues.
- `AGENTS.md` is visible at repository root for future AI handoff.
