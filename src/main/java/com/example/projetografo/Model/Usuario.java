package com.example.projetografo.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Node("Usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    private String id;

    private String nome;
    private String username;
    private String email;
    private LocalDateTime criadoEm;

    /**
     * Relacionamento direcionado: (este Usuario)-[:SEGUE]->(outro Usuario).
     * O @JsonIgnoreProperties evita recursão infinita quando dois usuários se seguem.
     */
    @Relationship(type = "SEGUE", direction = Relationship.Direction.OUTGOING)
    @JsonIgnoreProperties({"seguindo", "seguidores"})
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Usuario> seguindo = new HashSet<>();

}
