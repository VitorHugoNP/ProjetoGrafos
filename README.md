# ProjetoGrafo

API REST de CRUD sobre um grafo **Neo4j**, com o domínio **Usuario**
(relacionamento direcionado `:SEGUE` entre usuários).

> O domínio **Produto** existiu até o commit `1f79f1c`, que removeu `Produto`,
> `ProdutoRepository` e `ProdutoController`. Hoje a API só trabalha com `Usuario`;
> o banco continua se chamando `produtos`.

---

## 1. Stack

| Item | Versão / configuração |
|---|---|
| Java | 27 |
| Spring Boot | 4.1.1 |
| Build | Maven (wrapper `mvnw` / `mvnw.cmd`) |
| Banco | Neo4j 2026.09 Enterprise — banco `produtos` |
| Driver / acesso | `spring-boot-starter-data-neo4j` (Spring Data Neo4j) |
| Web | `spring-boot-starter-webmvc` |
| REST automático | `spring-boot-starter-data-rest` |
| Boilerplate | Lombok **1.18.48** (fixado no `pom.xml` para funcionar no Java 27) |
| Dev | `spring-boot-devtools` (hot restart) |
| Pacote base | `com.example.projetografo` |

Dependências de teste: `spring-boot-starter-data-neo4j-test`,
`spring-boot-starter-data-rest-test`, `spring-boot-starter-neo4j-test`,
`spring-boot-starter-webmvc-test`.

---

## 2. Estrutura

```
ProjetoGrafo/
├── pom.xml
├── mvnw / mvnw.cmd
├── AGENTS.md                  # regras do projeto
├── README.md                  # este arquivo
└── src/
    ├── main/
    │   ├── java/com/example/projetografo/
    │   │   ├── ProjetoGrafoApplication.java
    │   │   ├── Model/
    │   │   │   └── Usuario.java
    │   │   ├── Repository/
    │   │   │   └── UsuarioRepository.java
    │   │   └── Controller/
    │   │       └── UsuarioController.java
    │   └── resources/application.properties
    └── test/java/com/example/projetografo/
        └── ProjetoGrafoApplicationTests.java
```

Camadas: **entidade `@Node` → repository `Neo4jRepository` → controller REST**
(não há camada de service; a lógica fica no controller, seguindo o padrão já existente).

---

## 3. Configuração do Neo4j

`src/main/resources/application.properties`:

```properties
spring.application.name=ProjetoGrafo

spring.neo4j.uri=bolt://localhost:7687
spring.neo4j.authentication.username=neo4j
spring.neo4j.authentication.password=12345678

spring.data.neo4j.database=produtos
```

O banco `produtos` **precisa existir** e estar **online** antes de subir a aplicação.
Sem o Neo4j no ar, o contexto Spring não inicia.

---

## 4. Como rodar

Pré-requisitos:

1. Neo4j local ouvindo em `bolt://localhost:7687`, usuário `neo4j`, senha `12345678`.
2. Banco `produtos` criado (Neo4j **Enterprise**, pois o Community só tem o banco `neo4j`).
3. JDK 27.

```powershell
# compilar
.\mvnw.cmd -DskipTests compile

# subir a aplicação (http://localhost:8080)
.\mvnw.cmd spring-boot:run

# testes
.\mvnw.cmd test
```

No Linux/macOS use `./mvnw`.

---

## 5. Modelo de dados

### Usuario — label `:Usuario`

| Propriedade | Tipo | Observação |
|---|---|---|
| `id` | String | UUID gerado no controller (propriedade persistida no nó) |
| `nome` | String | |
| `username` | String | único — validado na aplicação |
| `email` | String | |
| `criadoEm` | LocalDateTime | preenchido na criação |
| `seguindo` | Set\<Usuario\> | relação `:SEGUE` de saída — **não** é propriedade |

### Relacionamento

```cypher
(:Usuario)-[:SEGUE]->(:Usuario)
```

Direcionado: `A -[:SEGUE]-> B` significa "A segue B".
O campo `seguindo` é serializado no JSON; `@JsonIgnoreProperties({"seguindo", "seguidores"})`
evita recursão infinita quando dois usuários se seguem mutuamente.

---

## 6. API REST

### 6.1 Usuários — `/usuarios`

| Método | Rota | Descrição | Sucesso |
|---|---|---|---|
| POST | `/usuarios/criar` | cria usuário (`username`, `nome` e `email` obrigatórios; `username` único) | 201 / 400 / 409 |
| GET | `/usuarios/listar` | lista todos (com `seguindo`) | 200 |
| GET | `/usuarios/{id}` | busca por id (UUID) | 200 / 404 |
| PUT | `/usuarios/{id}` | atualiza `nome`, `username`, `email` | 200 / 404 / 409 |
| DELETE | `/usuarios/{id}` | remove o usuário | 204 / 404 |
| POST | `/usuarios/{id}/seguir/{alvoId}` | cria `:SEGUE` do usuário para o alvo | 200 / 400 / 404 |
| DELETE | `/usuarios/{id}/seguir/{alvoId}` | remove a relação `:SEGUE` | 204 / 404 |
| GET | `/usuarios/{id}/seguindo` | quem o usuário segue | 200 / 404 |
| GET | `/usuarios/{id}/seguidores` | quem segue o usuário | 200 / 404 |
| GET | `/usuarios/{id}/talvez-conheca` | sugestões de conexões (amigos de amigos que ainda não segue) | 200 / 404 |
| GET | `/usuarios/{id}/seguindo-count` | quantos usuários o usuário segue (`Long`) | 200 / 404 |
| GET | `/usuarios/{id}/seguidores-count` | quantos usuários seguem o usuário (`Long`) | 200 / 404 |

Regras de negócio do `UsuarioController`:

- `username`, `nome` e `email` são obrigatórios no POST (400 se ausentes).
- `username` duplicado retorna **409 Conflict** (POST e PUT).
- Um usuário não pode seguir a si mesmo (**400**).
- Remover uma relação inexistente retorna **404**.
- `seguindo` e `criadoEm` não são aceitos do cliente: são zerados/gerados na criação.

### 6.2 Sugestões e contadores

Três consultas de leitura sobre o relacionamento `:SEGUE`:

| Método | Rota | Método Java | Descrição |
|---|---|---|---|
| GET | `/usuarios/{id}/talvez-conheca` | `talvezConheca` | 404 se o usuário não existe; senão devolve `findTalvezConheca` |
| GET | `/usuarios/{id}/seguindo-count` | `seguindoCount` | 404 se o usuário não existe; senão devolve `findSeguindoCount(id)` |
| GET | `/usuarios/{id}/seguidores-count` | `seguidoresCount` | 404 se o usuário não existe; senão devolve `findSeguidoresCount(id)` |

Queries Cypher no `UsuarioRepository`:

```cypher
-- findTalvezConheca: amigos de amigos que ainda não são seguidos
MATCH (u:Usuario {id: $id})-[:SEGUE]->(:Usuario)-[:SEGUE]->(s:Usuario)
WHERE s.id <> $id AND NOT (u)-[:SEGUE]->(s)
RETURN DISTINCT s

-- findSeguindoCount: quantos eu sigo
MATCH (u:Usuario {id: $id})-[:SEGUE]->(s:Usuario) RETURN COUNT(s)

-- findSeguidoresCount: quantos me seguem
MATCH (u:Usuario {id: $id})<-[:SEGUE]-(s:Usuario) RETURN COUNT(s)
```

As contagens devolvem `Long` (o `COUNT()` do Cypher é inteiro longo) e são serializadas
como número no corpo da resposta. `talvez-conheca` só considera caminhos de **dois saltos**:
usuários a três ou mais conexões de distância não aparecem.

---

## 7. Exemplos de uso

### Criar dois usuários

```powershell
$ana = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/usuarios/criar" `
  -ContentType "application/json" `
  -Body '{"nome":"Ana Silva","username":"ana","email":"ana@exemplo.com"}'

$bruno = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/usuarios/criar" `
  -ContentType "application/json" `
  -Body '{"nome":"Bruno Costa","username":"bruno","email":"bruno@exemplo.com"}'
```

Resposta (201):

```json
{
  "id": "9a33a733-24ac-4cca-a007-8e06f09a3b88",
  "nome": "Ana Silva",
  "username": "ana",
  "email": "ana@exemplo.com",
  "criadoEm": "2026-11-09T15:04:19.9849527",
  "seguindo": []
}
```

### Criar o relacionamento (Ana segue Bruno)

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/usuarios/$($ana.id)/seguir/$($bruno.id)"
```

### Consultar

```powershell
# listar todos
Invoke-RestMethod "http://localhost:8080/usuarios/listar"

# quem Ana segue
Invoke-RestMethod "http://localhost:8080/usuarios/$($ana.id)/seguindo"

# quem segue Bruno
Invoke-RestMethod "http://localhost:8080/usuarios/$($bruno.id)/seguidores"
```

### Atualizar / remover

```powershell
Invoke-RestMethod -Method Put -Uri "http://localhost:8080/usuarios/$($bruno.id)" `
  -ContentType "application/json" `
  -Body '{"nome":"Bruno Costa Atualizado","email":"bruno.novo@exemplo.com"}'

# desfazer o relacionamento
Invoke-WebRequest -Method Delete -Uri "http://localhost:8080/usuarios/$($ana.id)/seguir/$($bruno.id)" -UseBasicParsing

# remover usuário
Invoke-WebRequest -Method Delete -Uri "http://localhost:8080/usuarios/$($bruno.id)" -UseBasicParsing
```

### Sugestões e contadores

```powershell
# sugestões (amigos de amigos que Ana ainda não segue)
Invoke-RestMethod "http://localhost:8080/usuarios/$($ana.id)/talvez-conheca"

# quantos Ana segue / quantos seguem Ana
Invoke-RestMethod "http://localhost:8080/usuarios/$($ana.id)/seguindo-count"
Invoke-RestMethod "http://localhost:8080/usuarios/$($ana.id)/seguidores-count"
```

### Verificação direta no grafo

```cypher
MATCH (u:Usuario) RETURN u
MATCH (a:Usuario)-[:SEGUE]->(b:Usuario) RETURN a.username, b.username
```

---

## 8. Decisões técnicas e observações

1. **Lombok 1.18.48** — a versão gerenciada pelo Spring Boot 4.1.1 não suporta o Java 27
   (erro `java.lang.ExceptionInInitializerError: com.sun.tools.javac.tree.EndPosTable`).
   A versão está fixada em `<lombok.version>` no `pom.xml`.

2. **Id do Usuario é UUID (String)** — gerado no controller e persistido no nó,
   o que permite consultas Cypher por `id` (`MATCH (u:Usuario {id: $id})`).
   O antigo `Produto` usava id Long interno (removido em `1f79f1c`).

3. **Remoção de relacionamento usa Cypher explícito** —
   `save()` com a coleção `seguindo` vazia **não** apaga a aresta no Neo4j.
   Por isso `UsuarioRepository.removerSeguindo()` executa:

   ```cypher
   MATCH (u:Usuario {id: $id})-[r:SEGUE]->(s:Usuario {id: $alvoId}) DELETE r
   ```

4. **Duplicidade de starters Neo4j** — o `pom.xml` contém tanto
   `spring-boot-starter-data-neo4j` quanto `spring-boot-starter-neo4j`.
   Funciona, mas o log mostra o escaneamento de repositories duas vezes.
   O `AGENTS.md` recomenda manter apenas o `data-neo4j`.

5. **Sem camada de service e sem validação Bean Validation** — validações são manuais
   no controller. Não há paginação nem ordenação.

6. **Spring Data REST** — os repositories também são expostos automaticamente pelo
   starter `data-rest`; os controllers têm precedência nas rotas declaradas.

7. **Sugestões e contadores (commit `b191fc4`, corrigido)** — as rotas são separadas
   (`/{id}/seguindo-count` e `/{id}/seguidores-count`) porque o Spring não aceita duas
   mappings no mesmo path (*Ambiguous mapping*). As contagens são `COUNT()` simples — sem
   `List` + `getFirst()` no controller — e retornam `Long`. Em `findTalvezConheca`:
   - `DISTINCT` é obrigatório — sem ele o mesmo usuário aparece uma vez por caminho;
   - `NOT (u)-[:SEGUE]->(s)` remove quem já é seguido;
   - `s.id <> $id` remove o próprio usuário;
   - o padrão `-[:SEGUE]->(:Usuario)-[:SEGUE]->` limita a busca a dois saltos
     (equivalente a `*2`, mas sem bind de relationship, que quebrava o mapeamento
     para `List<Usuario>`).

---

## 9. Testes

Existe apenas o teste de contexto:

```java
@SpringBootTest
class ProjetoGrafoApplicationTests {
    @Test
    void contextLoads() { }
}
```

`.\mvnw.cmd test` **exige o Neo4j no ar** e o banco `produtos` disponível, pois o
contexto completo é carregado.

---

## 10. Dados de exemplo deixados no banco

Criados via API durante a validação do CRUD:

| Username | Nome | Id |
|---|---|---|
| `ana` | Ana Silva | `9a33a733-24ac-4cca-a007-8e06f09a3b88` |
| `bruno` | Bruno Costa Atualizado | `25ac7b45-3e08-4d3d-8582-143a80a9a292` |

Relação existente: `(:Usuario {username:"ana"})-[:SEGUE]->(:Usuario {username:"bruno"})`.

---

## 11. Troubleshooting

| Sintoma | Causa / solução |
|---|---|
| `Fatal error compiling: ... EndPosTable` | Lombok incompatível com o Java 27 — confirme `<lombok.version>1.18.48</lombok.version>`. |
| `Connection refused` / contexto não sobe | Neo4j desligado ou fora da porta `7687`. |
| `database not found: produtos` | Crie o banco (`CREATE DATABASE produtos`) — requer Neo4j Enterprise. |
| `409` ao criar usuário | `username` já cadastrado. |
| `DELETE .../seguir/...` retorna 204 mas a relação continua | Versão antiga do controller usando apenas `save()`; a versão atual usa Cypher explícito. |
| `Ambiguous mapping ... /usuarios/{id}/...` na subida | Dois `@GetMapping` no mesmo path. As contagens já usam rotas distintas (`seguindo-count` × `seguidores-count`). |
| `GET .../talvez-conheca` retorna `[]` com o grafo populado | Esperado quando não há caminho de **dois saltos** (A → B → C) ou quando o candidato já é seguido; a query não olha 3+ saltos. |
| `GET .../seguindo-count` ou `.../seguidores-count` retorna `0` | Usuário sem relações na direção consultada (ou usuário inexistente, que dá 404). |
| Porta `8080` ocupada | Encerre o processo anterior ou use `--server.port=8081`. |

---

## 12. Próximos passos sugeridos

- Extrair lógica para uma camada `Service`.
- Adicionar `spring-boot-starter-validation` (`@NotBlank`, `@Email`, `@Size`).
- Criar constraint de unicidade no Neo4j para `Usuario.username`.
- Testes de integração reais (criar, listar, seguir, deletar) com a infra de teste do SDN.
- Paginação/filtros nas listagens.
- Limitar (`LIMIT 10`) e ordenar por conexões em comum as sugestões de `talvez-conheca`;
  hoje ela devolve todos os candidatos de dois saltos em ordem arbitrária.
- Testes de integração para `talvez-conheca`, `seguindo-count` e `seguidores-count`
  (usuário inexistente → 404, sem relações → `0`, e sugestão não incluindo quem já é seguido).
- Definir se o domínio `Produto` (removido em `1f79f1c`) volta com outro nome de banco,
  já que o banco atual ainda se chama `produtos`.

---

## 13. Coleção Postman

A coleção **ProjetoGrafo API** fica em `postman/collections/ProjetoGrafo API/` (formato
v3, um YAML por request) e aparece automaticamente no **Local View** do Postman, já que
o `postman/` está registrado no `.postman/resources.yaml` deste repositório.

Variáveis da coleção: `baseUrl` (`http://localhost:8080`), `usuarioId` e `alvoId`
(vêm preenchidos com `ana` e `bruno`; os requests de criação sobrescrevem via script).

| Pasta | Requests |
|---|---|
| `Usuarios` | Listar, Buscar, Criar, Criar alvo, Atualizar |
| `Relacionamentos` | Seguir, Deixar de seguir, Quem eu sigo, Quem me segue |
| `Sugestoes e contadores` | Talvez conheca, Quantos eu sigo, Quantos me seguem |
| `Limpeza` | Remover usuario, Remover usuario alvo |

Cada request tem exemplo de resposta e asserção de status. Rodando na ordem das pastas,
`Criar usuario`/`Criar usuario alvo` preenchem os ids, as consultas usam esses ids e
`Limpeza` apaga os dois usuários criados.

```powershell
# validar o YAML
postman collection lint "postman/collections/ProjetoGrafo API"

# executar a coleção inteira contra a aplicação no ar
postman collection run "postman/collections/ProjetoGrafo API"
```
