package br.com.brazuca.brazucaapi.util;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import br.com.brazuca.brazucaapi.dto.PartidaGoogleDTO;

public class ScrapingUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScrapingUtil.class);
	private static final String BASE_URL_GOOGLE = "https://www.google.com/search?q=";
	private static final String COMPLEMENTO_URL_GOOGLE = "&hl=pt-BR";

	public static void main(String[] args) {

		String url = BASE_URL_GOOGLE + "criciuma x ponte preta 15/07/2022" + COMPLEMENTO_URL_GOOGLE;

		ScrapingUtil scraping = new ScrapingUtil();

		scraping.obtemInformacoesPartida(url);

	}

	public PartidaGoogleDTO obtemInformacoesPartida(String url) {

		PartidaGoogleDTO partidaGoogleDTO = new PartidaGoogleDTO();

		Document document = null;

		try {
			document = Jsoup.connect(url).get();
			
			String title = document.title();
			LOGGER.info("Título da Página -> {}",title);
			

		} catch (IOException e) {

			LOGGER.error("ERRO AO TENTAR CONECTAR NO GOOGLE COM JSOUP -> {}", e.getMessage());
		}

		return partidaGoogleDTO;
	}

}
