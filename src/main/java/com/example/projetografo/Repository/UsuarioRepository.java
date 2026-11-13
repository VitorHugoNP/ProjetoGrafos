package com.example.projetografo.Repository;

import com.example.projetografo.Model.Usuario;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends Neo4jRepository<Usuario, String> {

    Optional<Usuario> findByUsername(String username);

    /**
     * Quem este usuário segue: (u)-[:SEGUE]->(s)
     */
    @Query("MATCH (u:Usuario {id: $id})-[:SEGUE]->(s:Usuario) RETURN s")
    List<Usuario> findSeguindo(String id);

    /**
     * Quem segue este usuário: (u)-[:SEGUE]->(s)
     */
    @Query("MATCH (u:Usuario)-[:SEGUE]->(s:Usuario {id: $id}) RETURN u")
    List<Usuario> findSeguidores(String id);

    /**
     * Remove a aresta :SEGUE entre dois usuários de forma explícita.
     * O save() com a coleção vazia não apaga a relação no Neo4J.
     */
    @Query("MATCH (u:Usuario {id: $id})-[r:SEGUE]->(s:Usuario {id: $alvoId}) DELETE r")
    void removerSeguindo(String id, String alvoId);

    /**
     * Sugestões de conexão: amigos de amigos que o usuário ainda não segue.
     * Sem DISTINCT o mesmo usuário apareceria uma vez por caminho.
     */
    @Query("""
            MATCH (u:Usuario {id: $id})-[:SEGUE]->(:Usuario)-[:SEGUE]->(s:Usuario)
            WHERE s.id <> $id
              AND NOT (u)-[:SEGUE]->(s)
            RETURN DISTINCT s
            """)
    List<Usuario> findTalvezConheca(String id);

    /**
     * Quantos usuários este usuário segue.
     */
    @Query("MATCH (u:Usuario {id: $id})-[:SEGUE]->(s:Usuario) RETURN COUNT(s)")
    Long findSeguindoCount(String id);

    /**
     * Quantos usuários seguem este usuário.
     */
    @Query("MATCH (u:Usuario {id: $id})<-[:SEGUE]-(s:Usuario) RETURN COUNT(s)")
    Long findSeguidoresCount(String id);

}
