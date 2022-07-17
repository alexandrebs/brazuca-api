package br.com.brazuca.brazucaapi.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpRequest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import br.com.brazuca.brazucaapi.dto.PartidaGoogleDTO;
import java.util.Base64;

public class ScrapingUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScrapingUtil.class);
	private static final String BASE_URL_GOOGLE = "https://www.google.com/search?q=";
	private static final String COMPLEMENTO_URL_GOOGLE = "&hl=pt-BR";

	public static void main(String[] args) {

		String url = BASE_URL_GOOGLE + "flamengo x coritiba" + COMPLEMENTO_URL_GOOGLE;
		// criciuma x ponte preta vila nova x csa flamengo x coritiba
		ScrapingUtil scraping = new ScrapingUtil();

		scraping.obtemInformacoesPartida(url);

	}

	public PartidaGoogleDTO obtemInformacoesPartida(String url) {

		PartidaGoogleDTO partidaGoogleDTO = new PartidaGoogleDTO();

		Document document = null;

		try {
			document = Jsoup.connect(url).get();

			String title = document.title();
			LOGGER.info("Título da Página -> {}", title);

			StatusPartida statusPartida = obtemStatusPartida(document);
			LOGGER.info("Status Partida {} ", statusPartida);

			if (statusPartida != StatusPartida.PARTIDA_NAO_INICIADA) {
				String tempoPartida = obtemTempoPartida(document);
				LOGGER.info("Tempo Partida {} ", tempoPartida);

				Integer placarEquipeCasa = recuperaPlacarEquipeCasa(document);
				LOGGER.info("Placar Equipe Casa: {}", placarEquipeCasa);
				
				Integer placarEquipeVisitante = recuperaPlacarEquipeVisitante(document);
				LOGGER.info("Placar Equipe Casa: {}", placarEquipeVisitante);
			}

			String nomeEquipeCasa = obtemNomeEquipeCasa(document);
			LOGGER.info("Nome Equipe Casa: {} ", nomeEquipeCasa);

			String nomeEquipeVisitante = obtemNomeEquipeVisitante(document);
			LOGGER.info("Nome Equipe Visitante: {} ", nomeEquipeVisitante);

			String urlLogoEquipeCasa = obtemImagemEquipeCasa(document);
			LOGGER.info("URL Logo equipe Casa: {}", urlLogoEquipeCasa);

		} catch (IOException e) {

			LOGGER.error("ERRO AO TENTAR CONECTAR NO GOOGLE COM JSOUP -> {}", e.getMessage());
		}

		return partidaGoogleDTO;
	}

	public StatusPartida obtemStatusPartida(Document document) {

		// situações
		// 1 - partida não iniciada
		// 2 - partida iniciada/jogo rolando/ intervalo
		// 3 - partida encerrada
		// 4 - penalidades
		StatusPartida statusPartida = StatusPartida.PARTIDA_NAO_INICIADA;

		boolean isTempoPartida = document.select("div[class=imso_mh__lv-m-stts-cont]").isEmpty();

		if (!isTempoPartida) {
			String tempoPartida = document.select("div[class=imso_mh__lv-m-stts-cont]").first().text();
			statusPartida = StatusPartida.PARTIDA_EM_ENDAMENTO;

			if (tempoPartida.contains("Pênaltis")) {
				statusPartida = StatusPartida.PARTIDA_PENALTIS;
			}

		}

		isTempoPartida = document.select("span[class=imso_mh__ft-mtch imso-medium-font imso_mh__ft-mtchc]").isEmpty();
		if (!isTempoPartida) {
			statusPartida = StatusPartida.PARTIDA_ENCERRADA;

		}

		return statusPartida;
	}

	public String obtemTempoPartida(Document document) {

		String tempoPartida = null;
		// jogo rolando, penalidades ou intervalo
		boolean isTempoPartida = document.select("div[class=imso_mh__lv-m-stts-cont]").isEmpty();

		if (!isTempoPartida) {
			tempoPartida = document.select("div[class=imso_mh__lv-m-stts-cont]").first().text();
		}

		isTempoPartida = document.select("span[class=imso_mh__ft-mtch imso-medium-font imso_mh__ft-mtchc]").isEmpty();

		if (!isTempoPartida) {
			tempoPartida = document.select("span[class=imso_mh__ft-mtch imso-medium-font imso_mh__ft-mtchc]").first()
					.text();
		}

		return corrigeTempoPartida(tempoPartida);
	}

	public String corrigeTempoPartida(String tempo) {
		String tempoPartida = null;

		if (tempo.contains("'")) {
			tempoPartida = tempo.replace("'", " min");
		} else if ((tempo.contains("+"))) {
			tempoPartida = tempo.replace(" ", "").concat(" min");
		} else {
			return tempo;
		}

		return tempoPartida;
	}

	public String obtemNomeEquipeCasa(Document document) {

		Element elemento = document.selectFirst("div[class=imso_mh__first-tn-ed imso_mh__tnal-cont imso-tnol]");

		String nomeEquipeCasa = elemento.select("span").text();

		return nomeEquipeCasa;
	}

	private String obtemNomeEquipeVisitante(Document document) {

		Element elemento = document.selectFirst("div[class=imso_mh__second-tn-ed imso_mh__tnal-cont imso-tnol]");

		String nomeEquipeVisitante = elemento.select("span").text();

		return nomeEquipeVisitante;
	}

	private String obtemImagemEquipeCasa(Document document) {

		Element element = document.selectFirst("div[class=imso_mh__first-tn-ed imso_mh__tnal-cont imso-tnol]");

		element = element.selectFirst("div[class=imso_mh__t-l-cont kno-fb-ctx]");

		String logoEquipeCasa = "https:" + element.select("img[class=imso_btl__mh-logo]").attr("src");

		return logoEquipeCasa;
	}

	private String obtemImagemEquipeVisitante(Document document) {

		Element element = document.selectFirst("div[class=imso_mh__second-tn-ed imso_mh__tnal-cont imso-tnol]");

		element = element.selectFirst("div[class=imso_mh__t-l-cont kno-fb-ctx]");

		String logoEquipeVisitante = element.select("img[class=imso_btl__mh-logo]").attr("src");

		return logoEquipeVisitante;
	}

	public Integer recuperaPlacarEquipeCasa(Document document) {

		String placarEquipe = document.selectFirst("div[class=imso_mh__l-tm-sc imso_mh__scr-it imso-light-font]")
				.text();

		return Integer.valueOf(placarEquipe);
	}

	public Integer recuperaPlacarEquipeVisitante(Document document) {

		String placarEquipe = document.selectFirst("div[class=imso_mh__r-tm-sc imso_mh__scr-it imso-light-font]")
				.text();

		return Integer.valueOf(placarEquipe);
	}

}
