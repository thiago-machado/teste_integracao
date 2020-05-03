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
 * Qual a diferen�a entre um teste de integra��o e um teste de unidade?
 * 
 * Um teste de unidade isola a classe de suas depend�ncias, e a testa
 * independente delas. Testes de unidade fazem sentido quando nossas classes
 * cont�m regras de neg�cio, mas dependem de infra-estrutura. Nesses casos, fica
 * f�cil isolar a infra-estrutura.
 * 
 * J� testes de integra��o testam a classe de maneira integrada ao servi�o que
 * usam. Um teste de DAO, por exemplo, que bate em um banco de dados de verdade,
 * � considerado um teste de integra��o. Testes como esses s�o especialmente
 * �teis para testar classes cuja responsabilidade � se comunicar com outros
 * servi�os.
 */
public class UsuarioDAOTeste {

	private Session session;
	private UsuarioDao dao;

	@Before
	public void configuracaoInicial() {
		session = new CriadorDeSessao().getSession();
		dao = new UsuarioDao(session);

		/*
		 * Iniciando uma transa��o.
		 */
		session.beginTransaction();
	}

	@After
	public void executarAposFimDoTeste() {

		/*
		 * Desfazendo qualquer edi��o realizada na base de dados atrav�s da transa��o
		 * que foi aberta em @Before.
		 * 
		 */
		session.getTransaction().rollback();
		/*
		 * Precisamos fechar a sess�o para que o banco de dados libere essa porta para
		 * um pr�ximo teste. N�o fechar a sess�o pode implicar em problemas futuros,
		 * como testes que falham ou travam. Lembre-se sempre de fechar a conex�o.
		 * 
		 */
		session.close();
	}

	/*
	 * Chamamos esses testes de testes de integra��o, afinal estamos testando o
	 * comportamento da nossa classe integrada com um servi�o externo real. Testes
	 * como esse s�o �teis para classes como nossos DAOs, cuja tarefa � justamente
	 * se comunicar com outro servi�o.
	 * 
	 * Escrever um teste para um DAO � parecido com escrever qualquer outro teste:
	 * (i) precisamos montar um cen�rio, (ii) executar uma a��o e (iii) validar o
	 * resultado esperado.
	 * 
	 * Veja ent�o que escrever um teste para um DAO n�o � t�o diferente; � s� mais
	 * trabalhoso, afinal precisamos nos comunicar com o software externo o tempo
	 * todo, para montar cen�rio, para validar se a opera��o foi efetuada com
	 * sucesso e etc. Em nosso caso, criamos uma "Session" (uma conex�o com o
	 * banco), inserimos um usu�rio no banco (um INSERT, da SQL), e depois uma busca
	 * (um SELECT).
	 * 
	 * 
	 * Quais s�o os problemas de se usar mocks (e simular a conex�o com o banco de
	 * dados) para testar DAOs?
	 * 
	 * Ao usar Mocks, estamos "enganando" nosso teste. Um bom teste de DAO � aquele
	 * que garante que sua consulta SQL realmente funciona quando enviada para o
	 * banco de dados; e a melhor maneira de garantir isso � enviando-a para o
	 * banco!
	 * 
	 */
	@Test
	public void deveEncontrarPeloNomeEEmail() {

		/*
		 * Criando um usuario e salvando antes de invocar o porNomeEEmail
		 */
		Usuario novoUsuario = new Usuario("Jo�o da Silva", "joao@dasilva.com.br");
		dao.salvar(novoUsuario);

		// Agora buscamos no banco
		Usuario usuarioDoBanco = dao.porNomeEEmail("Jo�o da Silva", "joao@dasilva.com.br");

		assertEquals("Jo�o da Silva", usuarioDoBanco.getNome());
		assertEquals("joao@dasilva.com.br", usuarioDoBanco.getEmail());
	}

	@Test
	public void deveRetornarNuloSeNaoEncontrarUsuario() {

		Usuario usuarioDoBanco = dao.porNomeEEmail("Jo�o Joaquim", "joao@joaquim.com.br");

		/*
		 * Veriricando que o retorno deve ser NULL j� que n�o existe um usu�rio
		 * "Jo�o Joaquim"
		 */
		assertNull(usuarioDoBanco);
	}

	@Test
	public void deveDeletarUmUsuario() {

		Usuario usuario = new Usuario("Mauricio Aniche", "mauricio@aniche.com.br");

		dao.salvar(usuario);
		dao.deletar(usuario);

		/*
		 * Muitas vezes nossa camada de acesso � dados n�o envia as consultas SQL para o
		 * banco at� que voc� finalize a transa��o. � o que est� acontecendo aqui.
		 * Precisamos ent�o for�ar o envio do INSERT e do DELETE para o banco, para que
		 * depois o SELECT n�o traga o objeto! Essa opera��o � chamada de "flush"!
		 * 
		 * � muito comum fazer uso de "flush" sempre que fazemos testes com banco de
		 * dados. Dessa forma, garantimos que a consulta realmente chegou ao banco de
		 * dados, e que as futuras consultas levar�o mesmo em considera��o as consultas
		 * anteriores!
		 * 
		 * Veja que problemas como esses aparecer�o sempre em testes de integra��o.
		 * Precisamos entender bem do sistema que est� do outro lado para conseguirmos
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

		usuario.setNome("Jo�o da Silva");
		usuario.setEmail("joao@silva.com.br");

		dao.atualizar(usuario);

		session.flush();

		// Deve localizar Jo�o, j� que Maur�cio foi substitu�do por Jo�o
		Usuario novoUsuario = dao.porNomeEEmail("Jo�o da Silva", "joao@silva.com.br");
		assertNotNull(novoUsuario);

		// N�o deve mais localizar Maur�cio
		Usuario usuarioInexistente = dao.porNomeEEmail("Mauricio Aniche", "mauricio@aniche.com.br");
		assertNull(usuarioInexistente);

	}
}
