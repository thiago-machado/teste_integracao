package br.com.caelum.pm73.dominio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import br.com.caelum.pm73.dao.CriadorDeSessao;
import br.com.caelum.pm73.dao.LeilaoDao;
import br.com.caelum.pm73.dao.UsuarioDao;

public class LeilaoDaoTeste {

	private Session session;
	private LeilaoDao leilaoDao;
	private UsuarioDao usuarioDao;

	@Before
	public void antes() {
		session = new CriadorDeSessao().getSession();
		leilaoDao = new LeilaoDao(session);
		usuarioDao = new UsuarioDao(session);

		session.beginTransaction();
	}

	@After
	public void depois() {
		session.getTransaction().rollback();
		session.close();
	}

	@Test
	public void deveContarLeiloesNaoEncerrados() {
		// criamos um usuario
		Usuario mauricio = new Usuario("Mauricio Aniche", "mauricio@aniche.com.br");

		// criamos os dois leiloes
		Leilao ativo = new Leilao("Geladeira", 1500.0, mauricio, false);
		Leilao encerrado = new Leilao("XBox", 700.0, mauricio, false);
		encerrado.encerra();

		// persistimos todos no banco
		usuarioDao.salvar(mauricio);
		leilaoDao.salvar(ativo);
		leilaoDao.salvar(encerrado);

		// invocamos a acao que queremos testar
		// pedimos o total para o DAO
		long total = leilaoDao.total();

		assertEquals(1L, total);
	}

	@Test
	public void deveRetornarZeroSeNaoHaLeiloesNovos() {
		Usuario mauricio = new Usuario("Mauricio Aniche", "mauricio@aniche.com.br");

		Leilao encerrado = new Leilao("XBox", 700.0, mauricio, false);
		Leilao tambemEncerrado = new Leilao("Geladeira", 1500.0, mauricio, false);
		encerrado.encerra();
		tambemEncerrado.encerra();

		usuarioDao.salvar(mauricio);
		leilaoDao.salvar(encerrado);
		leilaoDao.salvar(tambemEncerrado);

		long total = leilaoDao.total();

		assertEquals(0L, total);
	}

	@Test
	public void deveRetornarLeiloesDeProdutosNovos() {
		Usuario mauricio = new Usuario("Mauricio Aniche", "mauricio@aniche.com.br");

		Leilao produtoNovo = new Leilao("XBox", 700.0, mauricio, false);
		Leilao produtoUsado = new Leilao("Geladeira", 1500.0, mauricio, true);

		usuarioDao.salvar(mauricio);
		leilaoDao.salvar(produtoNovo);
		leilaoDao.salvar(produtoUsado);

		List<Leilao> novos = leilaoDao.novos();

		assertEquals(1, novos.size());
		assertEquals("XBox", novos.get(0).getNome());
	}

	@Test
	public void deveTrazerSomenteLeiloesAntigos() {
		Usuario mauricio = new Usuario("Mauricio Aniche", "mauricio@aniche.com.br");

		// Leilão recente
		Leilao recente = new Leilao("XBox", 700.0, mauricio, false);
		recente.setDataAbertura(Calendar.getInstance());

		// Leilão antigo
		Leilao antigo = new Leilao("Geladeira", 1500.0, mauricio, true);

		Calendar dataAntiga = Calendar.getInstance();
		dataAntiga.add(Calendar.DAY_OF_MONTH, -10);
		antigo.setDataAbertura(dataAntiga);

		usuarioDao.salvar(mauricio);
		leilaoDao.salvar(recente);
		leilaoDao.salvar(antigo);

		List<Leilao> antigos = leilaoDao.antigos();

		assertEquals(1, antigos.size());
		assertEquals("Geladeira", antigos.get(0).getNome());
	}

	/*
	 * Sempre que temos um "<", "<=", ">", ">=" ou qualquer outro comparador, é
	 * sempre bom testar o limite. Ou seja, esse teste garante que leilão criado há
	 * exatamente 7 dias aparececerá na lista.
	 */
	@Test
	public void deveTrazerSomenteLeiloesAntigosHaMaisDe7Dias() {
		Usuario mauricio = new Usuario("Mauricio Aniche", "mauricio@aniche.com.br");

		Leilao noLimite = new Leilao("XBox", 700.0, mauricio, false);

		Calendar dataAntiga = Calendar.getInstance();
		dataAntiga.add(Calendar.DAY_OF_MONTH, -7);

		noLimite.setDataAbertura(dataAntiga);

		usuarioDao.salvar(mauricio);
		leilaoDao.salvar(noLimite);

		List<Leilao> antigos = leilaoDao.antigos();

		assertEquals(1, antigos.size());
	}

	@Test
	public void deveTrazerLeiloesNaoEncerradosNoPeriodo() {

		// Criando as datas para filtro
		Calendar comecoDoIntervalo = Calendar.getInstance();
		comecoDoIntervalo.add(Calendar.DAY_OF_MONTH, -10); // Hoje menos 10 dias
		Calendar fimDoIntervalo = Calendar.getInstance(); // Hoje

		Usuario mauricio = new Usuario("Mauricio Aniche", "mauricio@aniche.com.br");

		// criando os leiloes, cada um com uma data
		Calendar dataDoLeilao1 = Calendar.getInstance();
		dataDoLeilao1.add(Calendar.DAY_OF_MONTH, -2);
		Calendar dataDoLeilao2 = Calendar.getInstance();
		dataDoLeilao2.add(Calendar.DAY_OF_MONTH, -20);

		Leilao leilao1 = new Leilao("XBox", 700.0, mauricio, false);
		leilao1.setDataAbertura(dataDoLeilao1); // Está dentro da faixa de datas
		Leilao leilao2 = new Leilao("Geladeira", 1700.0, mauricio, false);
		leilao2.setDataAbertura(dataDoLeilao2); // Está fora da faixa de datas

		// persistindo os objetos no banco
		usuarioDao.salvar(mauricio);
		leilaoDao.salvar(leilao1);
		leilaoDao.salvar(leilao2);

		// invocando o metodo para testar
		List<Leilao> leiloes = leilaoDao.porPeriodo(comecoDoIntervalo, fimDoIntervalo);

		// garantindo que a query funcionou
		assertEquals(1, leiloes.size());
		assertEquals("XBox", leiloes.get(0).getNome());
	}

	/*
	 * Não deve trazer leilões encerrados que estejam dentro do período
	 */
	@Test
	public void naoDeveTrazerLeiloesEncerradosNoPeriodo() {

		// criando as datas
		Calendar comecoDoIntervalo = Calendar.getInstance();
		comecoDoIntervalo.add(Calendar.DAY_OF_MONTH, -10);
		Calendar fimDoIntervalo = Calendar.getInstance();
		Calendar dataDoLeilao1 = Calendar.getInstance();
		dataDoLeilao1.add(Calendar.DAY_OF_MONTH, -2);

		Usuario mauricio = new Usuario("Mauricio Aniche", "mauricio@aniche.com.br");

		// Criando leilão
		Leilao leilao1 = new Leilao("XBox", 700.0, mauricio, false);
		leilao1.setDataAbertura(dataDoLeilao1);
		leilao1.encerra();

		// persistindo os objetos no banco
		usuarioDao.salvar(mauricio);
		leilaoDao.salvar(leilao1);

		// invocando o metodo para testar
		List<Leilao> leiloes = leilaoDao.porPeriodo(comecoDoIntervalo, fimDoIntervalo);

		// garantindo que a query funcionou
		assertEquals(0, leiloes.size());
	}

	@Test
	public void deveRetornarLeiloesDisputados() {
		Usuario mauricio = new Usuario("Mauricio", "mauricio@aniche.com.br");
		Usuario marcelo = new Usuario("Marcelo", "marcelo@aniche.com.br");

		Leilao leilao1 = new LeilaoBuilder().comDono(marcelo).comValor(3000.0)
				.comLance(Calendar.getInstance(), mauricio, 3000.0).comLance(Calendar.getInstance(), marcelo, 3100.0)
				.constroi();

		Leilao leilao2 = new LeilaoBuilder().comDono(mauricio).comValor(3200.0)
				.comLance(Calendar.getInstance(), mauricio, 3000.0).comLance(Calendar.getInstance(), marcelo, 3100.0)
				.comLance(Calendar.getInstance(), mauricio, 3200.0).comLance(Calendar.getInstance(), marcelo, 3300.0)
				.comLance(Calendar.getInstance(), mauricio, 3400.0).comLance(Calendar.getInstance(), marcelo, 3500.0)
				.constroi();

		usuarioDao.salvar(marcelo);
		usuarioDao.salvar(mauricio);
		leilaoDao.salvar(leilao1);
		leilaoDao.salvar(leilao2);

		List<Leilao> leiloes = leilaoDao.disputadosEntre(2500, 3500);

		assertEquals(1, leiloes.size());
		assertEquals(3200.0, leiloes.get(0).getValorInicial(), 0.00001);
	}

	@Test
	public void listaSomenteOsLeiloesDoUsuario() throws Exception {

		Usuario dono = new Usuario("Mauricio", "m@a.com");
		Usuario comprador = new Usuario("Victor", "v@v.com");
		Usuario comprador2 = new Usuario("Guilherme", "g@g.com");

		Leilao leilao = new LeilaoBuilder().comDono(dono).comValor(50.0)
				.comLance(Calendar.getInstance(), comprador, 100.0).comLance(Calendar.getInstance(), comprador2, 200.0)
				.constroi();

		Leilao leilao2 = new LeilaoBuilder().comDono(dono).comValor(250.0)
				.comLance(Calendar.getInstance(), comprador2, 100.0).constroi();

		usuarioDao.salvar(dono);
		usuarioDao.salvar(comprador);
		usuarioDao.salvar(comprador2);
		leilaoDao.salvar(leilao);
		leilaoDao.salvar(leilao2);

		List<Leilao> leiloes = leilaoDao.listaLeiloesDoUsuario(comprador);
		assertEquals(1, leiloes.size());
		assertEquals(leilao, leiloes.get(0));
	}

	@Test
	public void listaDeLeiloesDeUmUsuarioNaoTemRepeticao() throws Exception {
		Usuario dono = new Usuario("Mauricio", "m@a.com");
		Usuario comprador = new Usuario("Victor", "v@v.com");

		Leilao leilao = new LeilaoBuilder().comDono(dono).comLance(Calendar.getInstance(), comprador, 100.0)
				.comLance(Calendar.getInstance(), comprador, 200.0).constroi();

		usuarioDao.salvar(dono);
		usuarioDao.salvar(comprador);
		leilaoDao.salvar(leilao);

		List<Leilao> leiloes = leilaoDao.listaLeiloesDoUsuario(comprador);
		assertEquals(1, leiloes.size());
		assertEquals(leilao, leiloes.get(0));
	}

	@Test
	public void devolveAMediaDoValorInicialDosLeiloesQueOUsuarioParticipou() {

		Usuario dono = new Usuario("Mauricio", "m@a.com");
		Usuario comprador = new Usuario("Victor", "v@v.com");

		Leilao leilao = new LeilaoBuilder().comDono(dono).comValor(50.0)
				.comLance(Calendar.getInstance(), comprador, 100.0).comLance(Calendar.getInstance(), comprador, 200.0)
				.constroi();

		Leilao leilao2 = new LeilaoBuilder().comDono(dono).comValor(250.0)
				.comLance(Calendar.getInstance(), comprador, 100.0).constroi();

		usuarioDao.salvar(dono);
		usuarioDao.salvar(comprador);
		leilaoDao.salvar(leilao);
		leilaoDao.salvar(leilao2);

		assertEquals(116.66, leilaoDao.getValorInicialMedioDoUsuario(comprador), 0.01);
	}

	@Test
	public void deveDeletarUmLeilao() {

		Usuario dono = new Usuario("Mauricio", "m@a.com");
		Usuario comprador = new Usuario("Victor", "v@v.com");

		Leilao leilao = new LeilaoBuilder().comDono(dono).comValor(50.0)
				.comLance(Calendar.getInstance(), comprador, 100.0).comLance(Calendar.getInstance(), comprador, 200.0)
				.constroi();

		usuarioDao.salvar(dono);
		usuarioDao.salvar(comprador);
		leilaoDao.salvar(leilao);

		session.flush();

		/*
		 * Para garantir que o leilão foi deletado, faça a busca por id. No momento que
		 * você invocar o salvar(), o Hibernate preencherá o ID gerado pelo banco na
		 * instância de leilão automaticamente.
		 */
		leilaoDao.deleta(leilao);

		assertNull(leilaoDao.porId(leilao.getId()));

	}

}
