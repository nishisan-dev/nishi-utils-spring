# Plano: Migrar nishi-utils-spring para nishi-utils-core 4.0.0 (+ Java 25)

## Context

O `nishi-utils-spring` hoje depende de `dev.nishisan:nishi-utils:3.2.0`. O objetivo é
evoluí-lo para consumir a última linha da biblioteca, `dev.nishisan:nishi-utils-core:4.0.0`.
Isso envolve **dois** movimentos simultâneos: uma **renomeação de artefato**
(`nishi-utils` → `nishi-utils-core`, onde as classes de `stats` agora residem) e um
**major bump** da lib (3.x → 4.0.0).

Diagnóstico levantado (verificado via `.m2` local — JAR + sources da 4.0.0):

- **API 100% compatível.** Os fontes de `IStatsListener`, `StatsUtils`, `HitCounterDTO`,
  `SimpleValueDTO` e `FixedSizeList` são **byte a byte idênticos** entre 3.2.0 e 4.0.0.
  `IStatsListener<E extends Number>` já era genérica na 3.2.0; `StatsUtils.registerListener(IStatsListener<Long>)`
  e `startStatsThread()` inalterados. → **O código de produção não precisa mudar** para a migração funcionar.
- **Java 25 é obrigatório.** O `nishi-utils-core-4.0.0.jar` é bytecode **major 69 = Java 25**
  (confirmado via `javap`; o `nishi-utils-parent:4.0.0` define `compiler.source/target=25`).
  Um JDK 21 não compila nem carrega essas classes (`UnsupportedClassVersionError`). O projeto
  está em Java 21 → precisa subir para **Java 25** (build, CI e runtime).
- **Bloqueador técnico:** o build atual usa `maven-compiler-plugin` **3.1** (herdado do
  super-POM do Maven 3.6.3), que não conhece o parâmetro `release` nem target 25. É
  **obrigatório fixar** uma versão moderna do plugin (3.13.0+).
- **Sem novidades transitivas.** A 4.0.0 traz as mesmas deps que a 3.2.0 (`jackson-databind`
  e `jackson-dataformat-yaml` 2.15.2). A resolução no GitHub Packages já funciona via
  `settings.xml` org-level (`https://maven.pkg.github.com/nishisan-dev`).

Decisões do usuário: **elevar para Java 25**; **major bump 1.0.3 → 2.0.0** (o CI aplica o
patch automático por cima e publica **2.0.1** como 1ª release da era Java 25); incluir os
extras (smoke test, higiene de genéricos, alinhar URL do repositório).

---

## Mudanças por arquivo

### 1. `pom.xml`
- **Versão (linha 9):** `1.0.3` → `2.0.0`.
- **Dependência (linhas 37-41):** `artifactId` `nishi-utils` → `nishi-utils-core`;
  `version` `3.2.0` → `4.0.0` (groupId `dev.nishisan` permanece).
- **Java (linhas 54-55):** remover o par `maven.compiler.source/target=21` e usar uma única
  property **`<maven.compiler.release>25</maven.compiler.release>`** (melhor prática: garante
  consistência entre API usada e bytecode-alvo).
- **Novo `<build>`:** fixar `org.apache.maven.plugins:maven-compiler-plugin` **3.13.0**
  (ou mais recente). É o que destrava `release 25`. Não há `<build>` hoje — seção nova.
- **`<repositories>` (linhas 17-23):** alinhar a URL legada
  `.../nishisan-utils` → `.../nishi-utils` (onde o core 4.0.0 reside). **Manter o
  `<id>github-utils`** (casa com o `<server>` do `settings.xml`); trocar **somente a URL**.
- **Dependência de teste (para o smoke test):** adicionar
  `org.springframework.boot:spring-boot-starter-test` com `<scope>test</scope>`
  (sem versão — vem do BOM já importado).
- `<distributionManagement>` (publica em `nishi-utils-spring`) permanece inalterado.

### 2. CI — `.github/workflows/publish.yml`
- Linhas 18-22: `name: Set up JDK 21` → `JDK 25`; `java-version: '21'` → `'25'`.
  `distribution: temurin` mantém. (Validar no merge que Temurin 25 GA está no `setup-java@v4`.)

### 3. Qodana — `qodana.yaml`
- `projectJDK: "21"` → `"25"`. Se a imagem `qodana-jvm-community:2025.2` não suportar JDK 25,
  isso afeta apenas a análise estática (não o build Maven) — item de baixa prioridade.

### 4. Higiene de genéricos — `src/main/java/dev/nishisan/utils/spring/stats/StatsUtilsMetricBind.java`
- Linha 15: `implements IStatsListener` → `implements IStatsListener<Long>`
  (tem de ser `Long`, pois `StatsUtils.registerListener` exige `IStatsListener<Long>`).
- Linhas 26 e 33: parâmetro `FixedSizeList fixedSizeList` → `FixedSizeList<Long> fixedSizeList`.
  A referência `FixedSizeList::getAverage` (linha 27) permanece válida.
- Os DTOs (`SimpleValueDTO`, `HitCounterDTO`) não são genéricos — nada a fazer. Resultado:
  zero unchecked warnings. **Sem mudança de comportamento.**

### 5. Smoke test (novo) — `src/test/java/dev/nishisan/utils/spring/configuration/NishisanStatsAutoConfigurationTest.java`
- A auto-config é `@ConditionalOnWebApplication(SERVLET)` + `@ConditionalOnProperty(nishi.utils.stats.enabled=true)`.
  Usar **`WebApplicationContextRunner`** (satisfaz a condição SERVLET), com
  `.withPropertyValues("nishi.utils.stats.enabled=true")` e
  `.withConfiguration(AutoConfigurations.of(NishisanStatsAutoConfiguration.class))`
  (registrar como auto-configuration garante que o `@ConditionalOnBean(MeterRegistry)`
  enxergue o registry de usuário).
- **Caso coberto:** com um `SimpleMeterRegistry` registrado via `.withBean(...)`, asseverar que
  o bean `statsUtils` existe (caminho `statsUtilsWithMetrics`). `SimpleMeterRegistry` vem do
  micrometer-core (disponível em teste via actuator `provided`).
- **Observação realista:** o caminho `statsUtilsNoMetrics` (`@ConditionalOnMissingClass(MeterRegistry)`)
  é inviável de simular sem classloader filtrante — cobrir apenas o caminho com `MeterRegistry`.
- O `WebApplicationContextRunner` fecha o contexto automaticamente, evitando vazar a thread iniciada
  por `startStatsThread()`.

---

## Sequência de commits atômicos

1. **`build: eleva toolchain para Java 25`** — fixa `maven-compiler-plugin 3.13.0`, troca
   `source/target` por `release=25`, atualiza `publish.yml` e `qodana.yaml` para 25.
   *(Precede a troca de dependência: a 4.0.0 só compila em Java 25.)*
2. **`deps: substitui nishi-utils 3.2.0 por nishi-utils-core 4.0.0`** — troca de artefato/versão
   + alinhamento da URL do `<repositories>`. Núcleo da migração.
3. **`chore: bump de versão para 2.0.0`** — major bump isolado.
4. **`refactor: parametriza IStatsListener<Long> e FixedSizeList<Long>`** — higiene de raw types.
5. **`test: adiciona smoke test de auto-configuração`** — `spring-boot-starter-test` (test) + o teste.

Mensagens na voz do time, sem referências a agente.

---

## Verificação end-to-end

JDK 25 já é o default do `mvn` neste ambiente (`Java version: 25.0.3`).

```bash
# a partir da raiz do repositório
mvn -v                                  # confirmar Java 25.0.3
mvn clean verify                        # compila em release 25 + roda o smoke test
mvn dependency:tree | grep -i nishi     # deve mostrar nishi-utils-core:4.0.0 e NÃO nishi-utils:3.2.0
javap -classpath target/classes -v dev.nishisan.utils.spring.stats.StatsUtilsMetricBind \
  | grep 'major version'                # esperado: 69 (Java 25)
```

Critérios de aceite:
- `mvn clean verify` verde (compilação + smoke test passando).
- `dependency:tree` sem `nishi-utils:3.2.0`, com `nishi-utils-core:4.0.0`.
- Classes do projeto em bytecode 69.
- Em CI: `mvn deploy` publica em `nishi-utils-spring` (GitHub Packages) rodando sob JDK 25.

Este plano já está versionado em `planning/`, conforme diretriz do projeto.

---

## Riscos / pontos de atenção
- **Plugin 3.1 → 3.13.0:** sem essa fixação o build com Java 25 falha. Roda no Maven 3.6.3;
  se houver atrito, considerar subir o Maven do CI (não obrigatório).
- **Temurin 25 GA no `setup-java@v4`** e **suporte a JDK 25 no Qodana 2025.2:** validar no merge
  (Java 25 é LTS de set/2025; em jun/2026 deve estar OK).
- **Versão publicada será 2.0.1** (pom em 2.0.0 + patch automático do CI). Aceito pelo usuário.
- **Impacto em consumidores:** quem usa `nishi-utils-spring` passará a exigir Java 25 — o major
  bump 2.0.0 sinaliza esse breaking change via semver.
