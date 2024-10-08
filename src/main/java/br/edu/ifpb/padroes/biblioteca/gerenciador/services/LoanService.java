package br.edu.ifpb.padroes.biblioteca.gerenciador.services;

import br.edu.ifpb.padroes.biblioteca.gerenciador.dtos.LoanRequestDTO;
import br.edu.ifpb.padroes.biblioteca.gerenciador.dtos.LoanUpdateRequestDTO;
import br.edu.ifpb.padroes.biblioteca.gerenciador.models.Loan;
import br.edu.ifpb.padroes.biblioteca.gerenciador.models.Book;
import br.edu.ifpb.padroes.biblioteca.gerenciador.models.User;
import br.edu.ifpb.padroes.biblioteca.gerenciador.repositories.LoanRepository;
import br.edu.ifpb.padroes.biblioteca.gerenciador.services.exceptions.BookHasAlreadyBeenReturnedException;
import br.edu.ifpb.padroes.biblioteca.gerenciador.services.exceptions.NotFoundException;
import br.edu.ifpb.padroes.biblioteca.gerenciador.validators.Handler;
import br.edu.ifpb.padroes.biblioteca.gerenciador.validators.loan.ChainBuilder;
import br.edu.ifpb.padroes.biblioteca.gerenciador.validators.loan.handlers.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class LoanService {
    @Autowired
    private LoanRepository repository;

    @Autowired
    private UserService userService;

    @Autowired
    private BookService bookService;

    @Autowired
    private ChainBuilder loanValidatorChain;

    public Loan insertLoan(LoanRequestDTO loanRequestDTO) {
        User user = userService.getUserById(loanRequestDTO.usuarioId());
        Book book = bookService.getBook(loanRequestDTO.livroId());

        validate(loanRequestDTO);

        book.setQuantity(book.getQuantity() - 1);
        bookService.updateQuantityBook(book.getId(), book.getQuantity());

        Loan loan = new Loan(loanRequestDTO, user, book);
        return repository.save(loan);
    }

    private void validate(LoanRequestDTO loanRequestDTO) {
        Handler chain = new ChainBuilder()
                .addHandler(new ExceededLimitLoan(repository))
                .addHandler(new SameBookLoan(repository))
                .addHandler(new InsufficientQuantityBook(bookService))
                .addHandler(new NotPaidLoan(repository))
                .build();

        chain.check(loanRequestDTO);
    }

    public Loan getLoanById(Long id){
        return repository.findById(id).orElseThrow(NotFoundException::new);
    }

    public List<Loan> getAllLoansByUserId(Long id){
        getLoanById(id);
        return repository.findByUserIdOrderByPaidAsc(id);
    }

    public List<Loan> getAllLoans(){
        return repository.findAllByOrderByPaidAsc();
    }

    public void deleteLoan(Long id) {
        Loan loan = getLoanById(id);
        repository.delete(loan);
    }

    public Loan updateLoan(Long id, LoanUpdateRequestDTO loanUpdateDTO) {
        Loan loan = getLoanById(id);

        loan.setEstimatedDate(loanUpdateDTO.dataEntregaPrevista());

        return repository.save(loan);
    }

    public Loan refundBook(Long id, LocalDate refundDate) {
        Loan loan = getLoanById(id);

        if (loan.getRefundDate() != null) {
            throw new BookHasAlreadyBeenReturnedException();
        }

        loan.calculateLateFee();
        loan.setRefundDate(refundDate);

        Book book = loan.getBook();
        bookService.updateQuantityBook(book.getId(), book.getQuantity() + 1);

        return repository.save(loan);
    }

    public void payLateFee(Long id) {
        Loan loan = getLoanById(id);

        loan.hasFee();

        repository.save(loan);
    }

}