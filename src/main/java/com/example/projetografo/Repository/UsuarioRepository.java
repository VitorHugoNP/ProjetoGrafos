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

    @Query("MATCH (u:Usuario {id: $id}) - [r:SEGUE*1..2]->(s:Usuario {id: $id}) RETURN r ")
    List<Usuario> findTalvezConheca(String id);

    @Query("MATCH (u:Usuario {id: $id}) - [r:SEGUE]->(s:Usuario {id: $id}) RETURN COUNT(s)")
    List<Integer> findSeguindoCount(String id);

    @Query("MATCH (u:Usuario {id: $id})<-[r:SEGUE]-(s:Usuario) RETURN COUNT(r)")
    Integer findSeguidoresCount(String id);

}
