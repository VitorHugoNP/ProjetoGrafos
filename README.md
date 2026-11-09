# ProjetoGrafo

API REST de CRUD sobre um grafo **Neo4j**, com dois domínios: **Produto** e **Usuario**
(com relacionamento direcionado `:SEGUE` entre usuários).

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
    │   │   │   ├── Produto.java
    │   │   │   └── Usuario.java
    │   │   ├── Repository/
    │   │   │   ├── ProdutoRepository.java
    │   │   │   └── UsuarioRepository.java
    │   │   └── Controller/
    │   │       ├── ProdutoController.java
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

### Produto — label `:Produto`

| Propriedade | Tipo | Observação |
|---|---|---|
| `id` | Long | `@Id @GeneratedValue` (id interno do Neo4j, não é propriedade do nó) |
| `nome` | String | |
| `preco` | Double | |
| `categoria` | String | usada na busca por categoria |

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

### 6.1 Produtos — `/produtos`

| Método | Rota | Descrição | Sucesso |
|---|---|---|---|
| POST | `/produtos` | cria produto | 200 |
| GET | `/produtos` | lista todos | 200 |
| GET | `/produtos/{id}` | busca por id (Long) | 200 / 404 |
| PUT | `/produtos/{id}` | atualiza (id do corpo é sobrescrito) | 200 / 404 |
| DELETE | `/produtos/{id}` | remove | 204 / 404 |
| GET | `/produtos/categoria/{categoria}` | busca por categoria (Cypher customizado) | 200 |

### 6.2 Usuários — `/usuarios`

| Método | Rota | Descrição | Sucesso |
|---|---|---|---|
| POST | `/usuarios` | cria usuário (`username` obrigatório e único) | 201 / 400 / 409 |
| GET | `/usuarios` | lista todos (com `seguindo`) | 200 |
| GET | `/usuarios/{id}` | busca por id (UUID) | 200 / 404 |
| PUT | `/usuarios/{id}` | atualiza `nome`, `username`, `email` | 200 / 404 / 409 |
| DELETE | `/usuarios/{id}` | remove o usuário | 204 / 404 |
| POST | `/usuarios/{id}/seguir/{alvoId}` | cria `:SEGUE` do usuário para o alvo | 200 / 400 / 404 |
| DELETE | `/usuarios/{id}/seguir/{alvoId}` | remove a relação `:SEGUE` | 204 / 404 |
| GET | `/usuarios/{id}/seguindo` | quem o usuário segue | 200 / 404 |
| GET | `/usuarios/{id}/seguidores` | quem segue o usuário | 200 / 404 |

Regras de negócio do `UsuarioController`:

- `username` é obrigatório no POST (400 se ausente).
- `username` duplicado retorna **409 Conflict** (POST e PUT).
- Um usuário não pode seguir a si mesmo (**400**).
- Remover uma relação inexistente retorna **404**.
- `seguindo` e `criadoEm` não são aceitos do cliente: são zerados/gerados na criação.

---

## 7. Exemplos de uso

### Criar dois usuários

```powershell
$ana = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/usuarios" `
  -ContentType "application/json" `
  -Body '{"nome":"Ana Silva","username":"ana","email":"ana@exemplo.com"}'

$bruno = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/usuarios" `
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
Invoke-WebRequest -Method Delete -Uri "http://localhost:8080/usuarios/$id" -UseBasicParsing
```

### Produtos

```powershell
$p = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/produtos" `
  -ContentType "application/json" `
  -Body '{"nome":"Notebook","preco":3500.0,"categoria":"Eletronicos"}'

Invoke-RestMethod "http://localhost:8080/produtos"
Invoke-RestMethod "http://localhost:8080/produtos/categoria/Eletronicos"
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

2. **Id do Usuario é UUID (String)** — gerado no controller e persistido no nó.
   Diferente do `Produto` (Long interno), permite consultas Cypher por `id`
   (`MATCH (u:Usuario {id: $id})`).

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

5. **Aviso de id interno do Produto** — o Neo4j registra no log que ids Long internos
   estão deprecated; para novos domínios prefira id externo (UUID), como em `Usuario`.

6. **Sem camada de service e sem validação Bean Validation** — validações são manuais
   no controller. Não há paginação nem ordenação.

7. **Spring Data REST** — os repositories também são expostos automaticamente pelo
   starter `data-rest`; os controllers têm precedência nas rotas declaradas.

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
| Porta `8080` ocupada | Encerre o processo anterior ou use `--server.port=8081`. |

---

## 12. Próximos passos sugeridos

- Extrair lógica para uma camada `Service`.
- Adicionar `spring-boot-starter-validation` (`@NotBlank`, `@Email`, `@Size`).
- Criar constraint de unicidade no Neo4j para `Usuario.username`.
- Testes de integração reais (criar, listar, seguir, deletar) com a infra de teste do SDN.
- Paginação/filtros nas listagens.
