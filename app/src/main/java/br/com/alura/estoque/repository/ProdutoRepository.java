package br.com.alura.estoque.repository;

import java.util.List;

import br.com.alura.estoque.asynctask.BaseAsyncTask;
import br.com.alura.estoque.database.dao.ProdutoDAO;
import br.com.alura.estoque.model.Produto;
import br.com.alura.estoque.retrofit.EstoqueRetrofit;
import br.com.alura.estoque.retrofit.callback.BaseCallback;
import br.com.alura.estoque.retrofit.callback.CallbackSemRetorno;
import br.com.alura.estoque.retrofit.service.ProdutoService;
import retrofit2.Call;

public class ProdutoRepository {

    private final ProdutoDAO dao;
    private final ProdutoService service;
    private Call<Produto> salva;
    private Call<Produto> call;

    public ProdutoRepository(ProdutoDAO dao) {
        this.dao = dao;
        service = new EstoqueRetrofit().getProdutoService();
    }

    public void buscaProdutos(DadosCarregadosCallback<List<Produto>> callback) {
        buscaProdutosInternos(callback);
    }

    private void buscaProdutosInternos(DadosCarregadosCallback<List<Produto>> callback) {

        new BaseAsyncTask<>(dao::buscaTodos,
                resultado -> {
                    callback.quandoSucesso(resultado);
                    buscaProdutosNaApi(callback);
                }).execute();
    }

    private void buscaProdutosNaApi(DadosCarregadosCallback<List<Produto>> callback) {
        Call<List<Produto>> call = service.buscaTodos();

        call.enqueue(new BaseCallback<>(new BaseCallback.RespostaCallback<List<Produto>>() {
            @Override
            public void quandoSucesso(List<Produto> produtosNovos) {
                callback.quandoSucesso(produtosNovos);
            }

            @Override
            public void quandoFalha(String erro) {
                callback.quandoFalha(erro);
            }
        }));
    }

    private void atualizaInterno(List<Produto> produtos,
                                 DadosCarregadosCallback<List<Produto>> callback) {

        new BaseAsyncTask<>(() -> {
            dao.salva(produtos);
            return dao.buscaTodos();
        }, callback::quandoSucesso)
            .execute();
    }

    public void salva(Produto produto,
                      DadosCarregadosCallback<Produto> callback) {

        salvaNaAPI(produto, callback);
    }

    public void edita(Produto produto,
                      DadosCarregadosCallback<Produto> callback) {

        Call<Produto> call = service.edita(produto.getId(), produto);
        call.enqueue(new BaseCallback<>(new BaseCallback.RespostaCallback<Produto>() {
            @Override
            public void quandoSucesso(Produto resultado) {
                new BaseAsyncTask<>(() -> {
                    dao.atualiza(produto);
                    return produto;
                }, callback::quandoSucesso)
                        .execute();
            }

            @Override
            public void quandoFalha(String erro) {
                callback.quandoFalha(erro);
            }
        }));
    }

    public void remove(Produto produto,
                       DadosCarregadosCallback<Void> callback) {
        removeNaApi(produto, callback);
    }

    private void removeNaApi(Produto produto, DadosCarregadosCallback<Void> callback) {
        Call<Void> call = service.remove(produto.getId());
        call.enqueue(new CallbackSemRetorno(
                new CallbackSemRetorno.RespostaCallback() {
            @Override
            public void quandoSucesso() {
                removeInterno(produto, callback);
            }

            @Override
            public void quandoFalha(String erro) {

            }
        }));
    }

    private void removeInterno(Produto produto, DadosCarregadosCallback<Void> callback) {
        new BaseAsyncTask<>(() -> {
            dao.remove(produto);
            return null;
        }, callback::quandoSucesso)
                .execute();
    }

    private void salvaNaAPI(Produto produto,
                            DadosCarregadosCallback<Produto> callback) {
        Call<Produto> call = service.salva(produto);
        call.enqueue(new BaseCallback<>(new BaseCallback.RespostaCallback<Produto>() {
            @Override
            public void quandoSucesso(Produto produtosSalvo) {
                salvaInterno(produtosSalvo, callback);
            }

            @Override
            public void quandoFalha(String erro) {
                callback.quandoFalha(erro);
            }
        }));
    }

    private void salvaInterno(Produto produto,
                              DadosCarregadosCallback<Produto> callback) {
        new BaseAsyncTask<>(() -> {
            long id = dao.salva(produto);
            return dao.buscaProduto(id);
        }, callback::quandoSucesso)
                .execute();
    }

    public interface DadosCarregadosCallback<T>{
        void quandoSucesso(T resultado);
        void quandoFalha(String erro);
    }

}
