package br.com.caelum.pm73.dominio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import br.com.caelum.pm73.dao.CriadorDeSessao;
import br.com.caelum.pm73.dao.UsuarioDao;

/**
 * Qual a diferença entre um teste de integração e um teste de unidade?
 * 
 * Um teste de unidade isola a classe de suas dependências, e a testa
 * independente delas. Testes de unidade fazem sentido quando nossas classes
 * contém regras de negócio, mas dependem de infra-estrutura. Nesses casos, fica
 * fácil isolar a infra-estrutura.
 * 
 * Já testes de integração testam a classe de maneira integrada ao serviço que
 * usam. Um teste de DAO, por exemplo, que bate em um banco de dados de verdade,
 * é considerado um teste de integração. Testes como esses são especialmente
 * úteis para testar classes cuja responsabilidade é se comunicar com outros
 * serviços.
 */
public class UsuarioDAOTeste {

	private Session session;
	private UsuarioDao dao;

	@Before
	public void configuracaoInicial() {
		session = new CriadorDeSessao().getSession();
		dao = new UsuarioDao(session);

		/*
		 * Iniciando uma transação.
		 */
		session.beginTransaction();
	}

	@After
	public void executarAposFimDoTeste() {

		/*
		 * Desfazendo qualquer edição realizada na base de dados através da transação
		 * que foi aberta em @Before.
		 * 
		 */
		session.getTransaction().rollback();
		/*
		 * Precisamos fechar a sessão para que o banco de dados libere essa porta para
		 * um próximo teste. Não fechar a sessão pode implicar em problemas futuros,
		 * como testes que falham ou travam. Lembre-se sempre de fechar a conexão.
		 * 
		 */
		session.close();
	}

	/*
	 * Chamamos esses testes de testes de integração, afinal estamos testando o
	 * comportamento da nossa classe integrada com um serviço externo real. Testes
	 * como esse são úteis para classes como nossos DAOs, cuja tarefa é justamente
	 * se comunicar com outro serviço.
	 * 
	 * Escrever um teste para um DAO é parecido com escrever qualquer outro teste:
	 * (i) precisamos montar um cenário, (ii) executar uma ação e (iii) validar o
	 * resultado esperado.
	 * 
	 * Veja então que escrever um teste para um DAO não é tão diferente; é só mais
	 * trabalhoso, afinal precisamos nos comunicar com o software externo o tempo
	 * todo, para montar cenário, para validar se a operação foi efetuada com
	 * sucesso e etc. Em nosso caso, criamos uma "Session" (uma conexão com o
	 * banco), inserimos um usuário no banco (um INSERT, da SQL), e depois uma busca
	 * (um SELECT).
	 * 
	 * 
	 * Quais são os problemas de se usar mocks (e simular a conexão com o banco de
	 * dados) para testar DAOs?
	 * 
	 * Ao usar Mocks, estamos "enganando" nosso teste. Um bom teste de DAO é aquele
	 * que garante que sua consulta SQL realmente funciona quando enviada para o
	 * banco de dados; e a melhor maneira de garantir isso é enviando-a para o
	 * banco!
	 * 
	 */
	@Test
	public void deveEncontrarPeloNomeEEmail() {

		/*
		 * Criando um usuario e salvando antes de invocar o porNomeEEmail
		 */
		Usuario novoUsuario = new Usuario("João da Silva", "joao@dasilva.com.br");
		dao.salvar(novoUsuario);

		// Agora buscamos no banco
		Usuario usuarioDoBanco = dao.porNomeEEmail("João da Silva", "joao@dasilva.com.br");

		assertEquals("João da Silva", usuarioDoBanco.getNome());
		assertEquals("joao@dasilva.com.br", usuarioDoBanco.getEmail());
	}

	@Test
	public void deveRetornarNuloSeNaoEncontrarUsuario() {

		Usuario usuarioDoBanco = dao.porNomeEEmail("João Joaquim", "joao@joaquim.com.br");

		/*
		 * Veriricando que o retorno deve ser NULL já que não existe um usuário
		 * "João Joaquim"
		 */
		assertNull(usuarioDoBanco);
	}

	@Test
	public void deveDeletarUmUsuario() {

		Usuario usuario = new Usuario("Mauricio Aniche", "mauricio@aniche.com.br");

		dao.salvar(usuario);
		dao.deletar(usuario);

		/*
		 * Muitas vezes nossa camada de acesso à dados não envia as consultas SQL para o
		 * banco até que você finalize a transação. É o que está acontecendo aqui.
		 * Precisamos então forçar o envio do INSERT e do DELETE para o banco, para que
		 * depois o SELECT não traga o objeto! Essa operação é chamada de "flush"!
		 * 
		 * É muito comum fazer uso de "flush" sempre que fazemos testes com banco de
		 * dados. Dessa forma, garantimos que a consulta realmente chegou ao banco de
		 * dados, e que as futuras consultas levarão mesmo em consideração as consultas
		 * anteriores!
		 * 
		 * Veja que problemas como esses aparecerão sempre em testes de integração.
		 * Precisamos entender bem do sistema que está do outro lado para conseguirmos
		 * escrever um bom teste!
		 */
		session.flush();

		session.clear();

		Usuario usuarioNoBanco = dao.porNomeEEmail("Mauricio Aniche", "mauricio@aniche.com.br");

		assertNull(usuarioNoBanco);

	}

	@Test
	public void deveAlterarUmUsuario() {
		Usuario usuario = new Usuario("Mauricio Aniche", "mauricio@aniche.com.br");

		dao.salvar(usuario);

		usuario.setNome("João da Silva");
		usuario.setEmail("joao@silva.com.br");

		dao.atualizar(usuario);

		session.flush();

		// Deve localizar João, já que Maurício foi substituído por João
		Usuario novoUsuario = dao.porNomeEEmail("João da Silva", "joao@silva.com.br");
		assertNotNull(novoUsuario);

		// Não deve mais localizar Maurício
		Usuario usuarioInexistente = dao.porNomeEEmail("Mauricio Aniche", "mauricio@aniche.com.br");
		assertNull(usuarioInexistente);

	}
}
