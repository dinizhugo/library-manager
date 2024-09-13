package br.edu.ifpb.padroes.biblioteca.gerenciador.validators.emprestimo.handlers;

import br.edu.ifpb.padroes.biblioteca.gerenciador.dtos.EmprestimoDTO;
import br.edu.ifpb.padroes.biblioteca.gerenciador.models.Livro;
import br.edu.ifpb.padroes.biblioteca.gerenciador.services.LivroService;
import br.edu.ifpb.padroes.biblioteca.gerenciador.validators.Handler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LivroExiste extends Handler {
    private final LivroService livroService;

    @Autowired
    public LivroExiste(LivroService livroService) {
        this.livroService = livroService;
    }

    @Override
    public void check(EmprestimoDTO data) {
        Livro livro = livroService.getLivro(data.livroId());

        if (getNextHandler() != null) {
            getNextHandler().check(data);
        }
    }
}

