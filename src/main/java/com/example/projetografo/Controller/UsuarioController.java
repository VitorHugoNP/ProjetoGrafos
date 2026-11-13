package com.example.projetografo.Controller;

import com.example.projetografo.Model.Usuario;
import com.example.projetografo.Repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioRepository repository;

    public UsuarioController(UsuarioRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/criar")
    public ResponseEntity<?> criar(@RequestBody Usuario usuario) {

        // tratamento de erros básicos
        if (usuario.getUsername() == null || usuario.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body("username é obrigatório");
        }
        if (usuario.getNome() == null || usuario.getNome().isBlank()) {
            return ResponseEntity.badRequest().body("nome do Usuário é obrigatório");
        }
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body("email é obrigatório");
        }

        if (repository.findByUsername(usuario.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("username já existe: " + usuario.getUsername());
        }

        usuario.setId(UUID.randomUUID().toString());
        usuario.setCriadoEm(LocalDateTime.now());
        usuario.setSeguindo(new HashSet<>());

        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(usuario));
    }

    // READ ALL
    @GetMapping("listar")
    public List<Usuario> listar() {
        return repository.findAll();
    }

    // READ ONE
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> buscarPorId(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable String id, @RequestBody Usuario dados) {
        Usuario existente = repository.findById(id).orElse(null);
        if (existente == null) {
            return ResponseEntity.notFound().build();
        }

        if (dados.getUsername() != null && !dados.getUsername().equals(existente.getUsername())
                && repository.findByUsername(dados.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("username já existe: " + dados.getUsername());
        }

        if (dados.getNome() != null) {
            existente.setNome(dados.getNome());
        }
        if (dados.getUsername() != null) {
            existente.setUsername(dados.getUsername());
        }
        if (dados.getEmail() != null) {
            existente.setEmail(dados.getEmail());
        }

        return ResponseEntity.ok(repository.save(existente));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable String id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // RELACIONAMENTO: este usuário passa a seguir o alvo
    @PostMapping("/{id}/seguir/{alvoId}")
    public ResponseEntity<?> seguir(@PathVariable String id, @PathVariable String alvoId) {
        if (id.equals(alvoId)) {
            return ResponseEntity.badRequest().body("um usuário não pode seguir a si mesmo");
        }

        Usuario usuario = repository.findById(id).orElse(null);
        Usuario alvo = repository.findById(alvoId).orElse(null);
        if (usuario == null || alvo == null) {
            return ResponseEntity.notFound().build();
        }

        usuario.getSeguindo().add(alvo);
        return ResponseEntity.ok(repository.save(usuario));
    }

    // RELACIONAMENTO: este usuário deixa de seguir o alvo
    @DeleteMapping("/{id}/seguir/{alvoId}")
    public ResponseEntity<?> deixarDeSeguir(@PathVariable String id, @PathVariable String alvoId) {
        Usuario usuario = repository.findById(id).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        boolean removeu = usuario.getSeguindo().removeIf(u -> alvoId.equals(u.getId()));
        if (!removeu) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("não seguia este usuário");
        }

        repository.removerSeguindo(id, alvoId);
        return ResponseEntity.noContent().build();
    }

    // RELACIONAMENTO: quem este usuário segue
    @GetMapping("/{id}/seguindo")
    public ResponseEntity<List<Usuario>> seguindo(@PathVariable String id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(repository.findSeguindo(id));
    }

    // RELACIONAMENTO: quem segue este usuário
    @GetMapping("/{id}/seguidores")
    public ResponseEntity<List<Usuario>> seguidores(@PathVariable String id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(repository.findSeguidores(id));
    }

    @GetMapping("/{id}/talvez-conheca")
    public ResponseEntity<List<Usuario>> talvezConheca(@PathVariable String id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(repository.findTalvezConheca(id));
    }

    @GetMapping("/{id}/seguidores-count")
    public ResponseEntity<Integer> findSeguindoCount(@PathVariable String id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(repository.findSeguindoCount(id).getFirst());
    }

    @GetMapping("/{id}/seguidores-count")
    public ResponseEntity<Integer> findSeguidoresCount(@PathVariable String id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(repository.findSeguidoresCount(id));
    }

}