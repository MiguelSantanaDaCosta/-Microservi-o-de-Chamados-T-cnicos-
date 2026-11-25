# Releases e tags — ms-chamados

Este repositório segue o [Versionamento Semântico 2.0.0](https://semver.org/lang/pt-BR/) (`MAJOR.MINOR.PATCH[-PRÉ-RELEASE]`).
Cada versão é uma **tag Git anotada** (`vX.Y.Z[-pré]`) publicada como **Release** do GitHub, sempre marcada como *pre-release*
enquanto for `alpha`, `beta` ou `rc`. As notas vêm do [`CHANGELOG.md`](../CHANGELOG.md).

| Tag | Data | Notas da release |
| --- | ---- | ---------------- |
| `v1.0.0-alpha` | 2026-10-22 | [`docs/releases/v1.0.0-alpha.md`](releases/v1.0.0-alpha.md) |
| `v1.0.0-beta` | 2026-11-25 | [`docs/releases/v1.0.0-beta.md`](releases/v1.0.0-beta.md) |
| `v1.0.0-beta.2` | 2026-11-25 | [`docs/releases/v1.0.0-beta.2.md`](releases/v1.0.0-beta.2.md) |

## 1. Enviar commits e tags

```bash
git push origin main --tags
```

## 2. Publicar as releases no GitHub

**Opção A — script (GitHub CLI):**

```bash
gh auth login                       # uma única vez
bash scripts/publicar-releases.sh   # cria as releases que ainda não existem, todas como pre-release
```

**Opção B — pelo navegador:** em *Releases → Draft a new release*, escolha a tag, use como título `ms-chamados <versão>`, cole o conteúdo de
`docs/releases/<tag>.md` na descrição, marque **Set as a pre-release** e publique (da mais antiga para a mais nova).

## 3. Conferir

- Aba **Releases** com todas as versões acima, a mais nova no topo.
- Aba **Tags** com `v1.0.0-alpha v1.0.0-beta v1.0.0-beta.2`.

## Tags antigas que conflitam com o versionamento

O repositório já tinha a tag/release **`1.0`** no commit `8cc8a6a` — o mesmo da `v1.0.0-alpha`. Ela não segue o formato `MAJOR.MINOR.PATCH` e, como versão
final, antecede as `beta` (contradiz o SemVer). Como a `v1.0.0-alpha` já marca esse commit, remova a antiga:

```bash
gh release delete 1.0 --yes            # apaga a release antiga (ou pelo navegador)
git push origin --delete 1.0           # apaga a tag remota
git tag -d 1.0                         # apaga a tag local, se existir
```

A versão `1.0.0` (sem pré-release) só deve ser criada quando o serviço sair do `beta`.

## Lançar uma nova versão

```bash
# 1) ajustar <version> no pom.xml e registrar a versão no CHANGELOG.md; commitar
# 2) criar a nota da release em docs/releases/ e a tag anotada no commit da versão
git tag -a vX.Y.Z-beta.N -m "ms-chamados X.Y.Z-beta.N"
# 3) publicar
git push origin main --tags && bash scripts/publicar-releases.sh
```

> Uma tag **publicada não deve ser movida**: se algo mudar depois, lance uma nova versão (por exemplo `-beta.N+1`).
