package com.example.projetografo.Repository;

import com.example.projetografo.Model.Produto;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import java.util.List;

public interface ProdutoRepository extends Neo4jRepository<Produto, Long> {

    /**
     * Consulta Cypher customizada: busca produtos por categoria.
     * Spring Data Neo4J converte isso em Cypher por debaixo dos panos.
     */
    @Query("MATCH (p:Produto) WHERE p.categoria = $categoria RETURN p")
    List<Produto> findByCategoria(String categoria);

}
