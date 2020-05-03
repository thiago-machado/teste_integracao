package br.com.caelum.pm73.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;

import br.com.caelum.pm73.dominio.Lance;
import br.com.caelum.pm73.dominio.Leilao;
import br.com.caelum.pm73.dominio.Usuario;

/**
 * 
 * Neste curso, estamos usando HSQLDB, um banco simples escrito em Java. Repare
 * que um arquivo caelum.db foi criado na raiz do seu diretório; ele é o banco
 * de dados.
 * 
 * Você poderia facilmente trocar sua conexão para fazer uso de MySql, Postgres,
 * ou qualquer que seja seu banco de dados. Neste projeto, como estamos usando
 * Hibernate, não há problemas em usar HSQLDB, afinal o Hibernate sempre
 * escreverá a SQL correta para seu banco de dados.
 *
 */
@SuppressWarnings("deprecation")
public class CriadorDeSessao {

	private static AnnotationConfiguration config;
	private static SessionFactory sf;

	public Session getSession() {
		if (sf == null) {
			sf = getConfig().buildSessionFactory();
		}

		return sf.openSession();
	}

	public Configuration getConfig() {
		if (config == null) {
			config = new AnnotationConfiguration().addAnnotatedClass(Lance.class).addAnnotatedClass(Leilao.class)
					.addAnnotatedClass(Usuario.class)
					.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver")
					.setProperty("hibernate.connection.url", "jdbc:hsqldb:caelum.db;shutdown=true")
					.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect")
					.setProperty("hibernate.connection.username", "sa").setProperty("hibernate.connection.password", "")
					.setProperty("hibernate.show_sql", "true");
		}
		return config;
	}
}
