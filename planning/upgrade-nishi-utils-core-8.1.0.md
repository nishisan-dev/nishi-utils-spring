# Plano: Atualizar nishi-utils-spring para nishi-utils-core 8.1.0 (+ corrigir o starter)

## Context

O `nishi-utils-spring` é um **starter / auto-configuration Spring Boot** enxuto cuja única
responsabilidade é expor o subsistema de **estatísticas (`stats`)** da `nishi-utils-core`
como bean Spring e ligá-lo ao Micrometer. Ele consome apenas 5 tipos do pacote
`dev.nishisan.utils.stats.*` (`StatsUtils`, `IStatsListener`, `HitCounterDTO`,
`SimpleValueDTO`, `FixedSizeList`). Antes deste trabalho fixava
`dev.nishisan:nishi-utils-core:4.0.0`, enquanto a biblioteca já estava em **8.1.0**
(tag `v8.1.0`, pom do monorepo).

**Por que mudar:** manter o starter alinhado com a última linha da lib (diretriz "ir sempre
a frente"). A exploração confirmou que a **superfície consumida (`stats`) é 100% estável de
4.0.0 até 8.1.0** — nenhum tipo ou método consumido desapareceu ou mudou de assinatura. As
quebras existentes no intervalo estão em subsistemas que o spring **não** usa:
- **5.0.0** (BREAKING): `RELAY_STREAM`/`FollowerIngestMode` — NGrid/replicação.
- **6.0.0 / 7.0.0** (BREAKING): formato e geometria do **ngrrd** — módulo `nishi-utils-oss`.

Aproveitando o trabalho, corrigiu-se também um **bug pré-existente do starter**: o arquivo de
registro de auto-configuration estava **vazio (0 bytes)**, então o starter nunca se auto-ativava
em consumidores; e a classe usava `@Configuration` em vez de `@AutoConfiguration`.

**Decisões:**
1. Versão alvo: **8.1.0**.
2. Escopo: **bump + corrigir o starter** (preencher o `AutoConfiguration.imports` + `@AutoConfiguration`).
3. Versão do próprio artefato: **2.0.0 → 2.1.0**.

**Resultado:** `nishi-utils-spring:2.1.0` consumindo `nishi-utils-core:8.1.0`, com o
starter de fato auto-registrável, build e smoke tests verdes.

---

## Mudanças por arquivo

### 0. Pré-requisito — tornar `nishi-utils-core:8.1.0` resolvível
O repo fonte da lib está local (`nishi-utils`) com `main` no nível 8.1.0. Instalar **só o
parent + o core** (sem buildar oss/ngrid-test nem rodar a suíte pesada de IT/Testcontainers):

```bash
mvn -f /caminho/para/nishi-utils/pom.xml -pl nishi-utils-core -am -DskipTests clean install
```

Publica `nishi-utils-parent:8.1.0` e `nishi-utils-core:8.1.0` no `~/.m2`. (Alternativa: se a
8.1.0 estiver publicada no GitHub Packages, o build do consumidor a resolve via `settings.xml`.)

### 1. `pom.xml` (consumidor)
- **Property** em `<properties>`: `<nishi-utils.version>8.1.0</nishi-utils.version>`.
- **Dependência:** `<version>4.0.0</version>` → `<version>${nishi-utils.version}</version>`.
- **Versão do projeto:** `2.0.0` → `2.1.0`.
- **Sem mudança de toolchain:** a lib 8.1.0 é compilada para **Java 21**; o consumidor permanece
  em `maven.compiler.release=25`. CI (`publish.yml`) e `qodana.yaml` ficam **inalterados** (já em JDK 25).

### 2. `NishisanStatsAutoConfiguration.java` — migrar para auto-configuration real
- Import: `org.springframework.context.annotation.Configuration` → `org.springframework.boot.autoconfigure.AutoConfiguration`.
- Anotação: `@Configuration` → `@AutoConfiguration`.
- Corrigir o comentário enganoso do `@ConditionalOnProperty`: desativado por padrão, só ativa
  com `nishi.utils.stats.enabled=true`.
- **Comportamento preservado:** o `@ConditionalOnProperty(matchIfMissing=false)` mantém o
  subsistema desligado até o consumidor optar explicitamente.

### 3. `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Preencher (arquivo antes vazio) com:
  `dev.nishisan.utils.spring.configuration.NishisanStatsAutoConfiguration`

### Sem mudança
- `StatsUtilsMetricBind.java` (genéricos já corretos), `NishisanStatsAutoConfigurationTest.java`
  (registra a classe via `AutoConfigurations.of(...)`, independe do imports file),
  `.github/workflows/publish.yml`, `qodana.yaml`, `<distributionManagement>`, `<repositories>`.

---

## Sequência de commits atômicos

Branch `chore/upgrade-nishi-utils-core-8.1.0`:

1. **`deps: atualiza nishi-utils-core de 4.0.0 para 8.1.0`**
2. **`fix: registra e ativa a auto-configuration de stats`**
3. **`chore: bump de versao para 2.1.0`**
4. **`docs: registra plano de upgrade para nishi-utils-core 8.1.0`**

---

## Verificação end-to-end

```bash
ls ~/.m2/repository/dev/nishisan/nishi-utils-core/8.1.0/   # jar presente
mvn -s settings.xml clean verify                          # compila release 25 + smoke tests
mvn -s settings.xml dependency:tree | grep -i nishi       # nishi-utils-core:8.1.0, sem 4.0.0
cat src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

**Resultado obtido:** `BUILD SUCCESS`; `Tests run: 2, Failures: 0, Errors: 0`; jar
`nishi-utils-spring-2.1.0.jar`; árvore com `nishi-utils-core:8.1.0` (transitivas novas:
`jackson-*:2.19.2` alinhado ao BOM do Boot, `org.lz4:lz4-java:1.8.0`), sem conflitos.

---

## Riscos / pontos de atenção
- **8.1.0 ausente no `.m2`:** mitigado pelo Passo 0 (install local do core a partir do fonte).
- **Deps transitivas do core cresceram** (NGrid/LZ4): conferidas via `dependency:tree`, sem conflito
  com o BOM do Spring Boot.
- **`main` da lib 6 commits à frente da tag `v8.1.0`** (docs + ngrrd, sem tocar `stats`): o install
  gera 8.1.0 funcionalmente equivalente para a superfície consumida.
- **Versão publicada pelo CI:** o pipeline pode aplicar patch automático por cima do 2.1.0.
