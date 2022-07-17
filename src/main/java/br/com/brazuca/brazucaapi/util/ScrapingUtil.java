package br.com.brazuca.brazucaapi.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.http.HttpRequest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import br.com.brazuca.brazucaapi.dto.PartidaGoogleDTO;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

public class ScrapingUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScrapingUtil.class);
	private static final String BASE_URL_GOOGLE = "https://www.google.com/search?q=";
	private static final String COMPLEMENTO_URL_GOOGLE = "&hl=pt-BR";
	private static final String CASA = "casa";
	private static final String VISITANTE = "visitante";

	// constantes 
	private static final String RESULTADO_PENALTI = "div[class=imso_mh_s__psn-sc]";
	private static final String STATUS_PARTIDA = "div[class=imso_mh__lv-m-stts-cont]";
	private static final String OBTEM_TEMPO_PARTIDA = "span[class=imso_mh__ft-mtch imso-medium-font imso_mh__ft-mtchc]";
	private static final String NOME_EQUIPE = "div[class=imso_mh__second-tn-ed imso_mh__tnal-cont imso-tnol]";
	private static final String OBTEM_NOME_EQUIPE_VISITANTE = "div[class=imso_mh__second-tn-ed imso_mh__tnal-cont imso-tnol]";
	private static final String OBTEM_NOME_EQUIPE_CASA = "div[class=imso_mh__first-tn-ed imso_mh__tnal-cont imso-tnol]";
	private static final String SELECT_ELEMENTO_IMAGEM = "div[class=imso_mh__t-l-cont kno-fb-ctx]";
	private static final String OBTEM_LOGO = "img[class=imso_btl__mh-logo]";
	private static final String OBTEM_PLACAR = "div[class=imso_mh__ma-sc-cont]";
	private static final String PLACAR_CASA = "div[class=imso_mh__l-tm-sc imso_mh__scr-it imso-light-font]";
	private static final String PLACAR_VISITANTE = "div[class=imso_mh__r-tm-sc imso_mh__scr-it imso-light-font]";
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//String url = BASE_URL_GOOGLE + "corinthians x guarani" + COMPLEMENTO_URL_GOOGLE;
		// argentina+x+venezuela
		// fc+emmen+x+sc+telstar
		// EGITO+X+SENEGAL
		// corinthians x guarani
		ScrapingUtil scraping = new ScrapingUtil();

		String url = scraping.montarUrlGoogle("corinthians", "guarani");
		scraping.obtemInformacoesPartida(url);

	}

	
	public PartidaGoogleDTO obtemInformacoesPartida(String URL) {

		PartidaGoogleDTO partidaGoogleDto = new PartidaGoogleDTO();

		try {
			requestUol();
			Document document = Jsoup.connect(URL).get();

			String title = document.title();

			StatusPartida statusPartida = obtemStatusPartida(document);
			partidaGoogleDto.setStatusPartida(statusPartida.toString());
			
			
			String tempoPartida = obtemTempoPartida(document);
			partidaGoogleDto.setTempoPartida(tempoPartida);
			
			LOGGER.info("Titilo da pagina: {}", title);
			LOGGER.info(statusPartida.toString());

			if (statusPartida != StatusPartida.PARTIDA_NAO_INICIADA) {

				LOGGER.info("Tempo da Partida: {}", tempoPartida);

				String placarCasa = obtemPlacar(document, PLACAR_CASA);
				partidaGoogleDto.setPlacarEquipeCasa(Integer.parseInt(placarCasa));
				LOGGER.info("Placar casa: {}", placarCasa);

				String placarVisitante = obtemPlacar(document, PLACAR_VISITANTE);
				partidaGoogleDto.setPlacarEquipeVisitante(Integer.parseInt(placarVisitante));
				LOGGER.info("Placar visitante: {}", placarVisitante);

				String golsJogadorCasa = obtemGolsJogadosCasa(document);
				partidaGoogleDto.setGolsEquipeCasa(golsJogadorCasa);
				LOGGER.info("Gols Jogador Casa: {}", golsJogadorCasa);
				
				String golsJogadorVisitante = obtemGolsJogadosVisitante(document);
				partidaGoogleDto.setGolsEquipeVisitante(placarVisitante);
				LOGGER.info("Gols Jogador Visitante: {}", golsJogadorVisitante);
				

				int resultadoPenaltiCasa = obtemResutatoPenalti(document, CASA);
				partidaGoogleDto.setPlacarEstendidoEquipeCasa(placarCasa);
				LOGGER.info("Penalti casa: {}", resultadoPenaltiCasa);

				int resultadoPenaltiVisitante = obtemResutatoPenalti(document, VISITANTE);
				partidaGoogleDto.setPlacarEquipeVisitante(resultadoPenaltiVisitante);
				LOGGER.info("Penalti visitante: {}", resultadoPenaltiVisitante);
			}

			String nomeEquipeCasa = obtemNomeEquipe(document, OBTEM_NOME_EQUIPE_CASA);
			partidaGoogleDto.setNomeEquipeCasa(nomeEquipeCasa);
			LOGGER.info("Equipe casa: {}", nomeEquipeCasa);

			String nomeEquipevisitante = obtemNomeEquipe(document, OBTEM_NOME_EQUIPE_VISITANTE);
			partidaGoogleDto.setNomeEquipeVisitante(nomeEquipevisitante);
			LOGGER.info("Equipe visitante: {}", nomeEquipevisitante);

			String logoEquipeCasa = obtemImagemEquipe(document, OBTEM_NOME_EQUIPE_CASA);
			partidaGoogleDto.setUrlLogoEquipeCasa(logoEquipeCasa);
			LOGGER.info("Logo Equipe casa: {}", logoEquipeCasa);

			String logoEquipeVisitante = obtemImagemEquipe(document, OBTEM_NOME_EQUIPE_VISITANTE);
			partidaGoogleDto.setUrlLogoEquipeVisitante(logoEquipeVisitante);
			LOGGER.info("Logo Equipe visitante: {}", logoEquipeVisitante);

			
			return partidaGoogleDto;
			
		} catch (IOException e) {

			LOGGER.error("Erro ao conectar no GOOGLE com Jsoup -> {}", e.getMessage());

		}

		return partidaGoogleDto;
	}

	public Integer obtemResutatoPenalti(Document document, String tipoEquipe) {

		Boolean isPenalti = document.select(RESULTADO_PENALTI).isEmpty();
		String[] penalidadeCompleta = null;
		if (!isPenalti) {

			String resultado = document.select(RESULTADO_PENALTI).text();

			String divisao = resultado.substring(0, 5).replace(" ", "");

			penalidadeCompleta = divisao.split("-");
			return (tipoEquipe.equals("casa")) ? validarPenalti(penalidadeCompleta[0])
					: validarPenalti(penalidadeCompleta[1]);
		}

		return null;
	}

	public Integer validarPenalti(String resultadoGols) {
		Integer valor;
		try {
			valor = Integer.parseInt(resultadoGols);

		} catch (Exception e) {

			valor = 0;
		}

		return valor;
	}

	public StatusPartida obtemStatusPartida(Document document) {
		// 1 - partida não iniciada
		// 2 - partida iniciada/ jogo rolando / intervalo
		// 3 - partida encerrada
		// 4 - partida penaltis
		StatusPartida statusPartida = StatusPartida.PARTIDA_NAO_INICIADA;

		boolean isTempoPartida = document.select(STATUS_PARTIDA).isEmpty();

		if (!isTempoPartida) {
			String tempoPartida = document.select(STATUS_PARTIDA).first().text();

			statusPartida = StatusPartida.PARTIDA_EM_ENDAMENTO;

			if (tempoPartida.contains("Pênaltis")) {
				statusPartida = StatusPartida.PARTIDA_PENALTIS;
			}

		}

		isTempoPartida = document.select(OBTEM_TEMPO_PARTIDA).isEmpty();

		if (!isTempoPartida) {
			statusPartida = StatusPartida.PARTIDA_ENCERRADA;

		}

		return statusPartida;
	}

	public String obtemTempoPartida(Document document) {

		String tempoPartida = null;

		// Jogo rolando, penalidades ou intervalo
		Boolean isTempoPartida = document.select(STATUS_PARTIDA).isEmpty();

		if (!isTempoPartida) {
			try {
				tempoPartida = document.select(STATUS_PARTIDA).first().text();
			} catch (Exception e) {
				// TODO: handle exception
			}
		}

		isTempoPartida = document.select(OBTEM_TEMPO_PARTIDA).isEmpty();

		if (!isTempoPartida) {

			tempoPartida = document.select(OBTEM_TEMPO_PARTIDA).first()
					.text();
		}

		return validaTempoPartida(tempoPartida);
	}

	private String validaTempoPartida(String tempoPartida) {

		if (tempoPartida.contains("'")) {

			tempoPartida = tempoPartida.replace("' ", "min");
		} else if (tempoPartida.contains("+")) {
			tempoPartida = tempoPartida.replace(" ", "").concat("min");
		}

		return tempoPartida;
	}

	
	private String obtemNomeEquipe(Document document, String tipoEquipe) {

		org.jsoup.nodes.Element elemento = document
				.selectFirst(tipoEquipe);

		String nomeEquipeVisitante = elemento.select("span").text();

		return nomeEquipeVisitante;
	}

	private String obtemImagemEquipe(Document document, String tagEquipe) {

		org.jsoup.nodes.Element element = document
				.selectFirst(tagEquipe);

		element = element.selectFirst(SELECT_ELEMENTO_IMAGEM);

		String logoEquipeCasa = element.select(OBTEM_LOGO).attr("src");
		return logoEquipeCasa;
	}

	
	private String obtemPlacar(Document document, String tagTipoEquipe) {

		org.jsoup.nodes.Element element = document.selectFirst(OBTEM_PLACAR);

		String placarCasa = element.select(tagTipoEquipe).first()
				.text();

		return placarCasa;
	}

	
	private String obtemGolsJogadosCasa(Document document) {

		ArrayList<String> golsCasa = new ArrayList<>();

		Elements elements = document.selectFirst("div[class=imso_gs__tgs imso_gs__left-team]")
				.select("div[class=imso_gs__gs-r]");

		for (org.jsoup.nodes.Element e : elements) {

			String golsAux = e.select("div[class=imso_gs__gs-r]").text();
			golsCasa.add(golsAux);
		}

		return String.join(", ", golsCasa).replace("'", " min");
	}

	private String obtemGolsJogadosVisitante(Document document) {

		ArrayList<String> golsVisitante = new ArrayList<>();

		Elements elements = document.selectFirst("div[class=imso_gs__tgs imso_gs__right-team]")
				.select("div[class=imso_gs__gs-r]");

		for (org.jsoup.nodes.Element e : elements) {

			String golsAux = e.select("div[class=imso_gs__gs-r]").text();
			golsVisitante.add(golsAux);
		}

		return String.join(", ", golsVisitante).replace("'", " min");
	}

	private void requestUol() throws IOException {

		URL url = null;
		try {
			url = new URL("https://www.uol.com.br/esporte/service/?loadComponent=match-service&contentType=json");
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		httpConn.setRequestMethod("GET");

		httpConn.setRequestProperty("authority", "www.uol.com.br");
		httpConn.setRequestProperty("sec-ch-ua",
				"\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"99\", \"Google Chrome\";v=\"99\"");
		httpConn.setRequestProperty("accept", "application/json, text/javascript, */*; q=0.01");
		httpConn.setRequestProperty("x-requested-with", "XMLHttpRequest");
		httpConn.setRequestProperty("sec-ch-ua-mobile", "?0");
		httpConn.setRequestProperty("user-agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.82 Safari/537.36");
		httpConn.setRequestProperty("sec-ch-ua-platform", "\"Windows\"");
		httpConn.setRequestProperty("sec-fetch-site", "same-origin");
		httpConn.setRequestProperty("sec-fetch-mode", "cors");
		httpConn.setRequestProperty("sec-fetch-dest", "empty");
		httpConn.setRequestProperty("referer", "https://www.uol.com.br/esporte/futebol/central-de-jogos/");
		httpConn.setRequestProperty("accept-language", "pt-BR,pt;q=0.9,en;q=0.8,de;q=0.7");
		httpConn.setRequestProperty("cookie",
				"BTCTL=a3; _hjTLDTest=1; TS011ad88a=0160ed0a6cf848fc4717dd78d6b39390384ddec07c1c9d74fcc86bafb922d194ee23c6355ed426c37a00c6cc9bca7475aa7736151ab0c82efeb5781113bc63f4cdb4daa1bf26e1798266945edddb2f34432525b1f8eb75c31f319a87fcd791548ef5e89ee971d9e46a58558cd3d49bf3913e6a1cce99da14764d3531f823204e493d7a0eb3; nvg18889=b1d369269d668d1bc4e73f9e609|2_91; nvg72696=b1d3692694eaefa02ba13df6b09|0_110; _matheriSegs=MATHER_U9_INSTANTMET2_20200701; _matherSegments=MATHER_U9_INSTANTMET2_20200701; DNA=a37a74b18b49466da3885016bece82c0|179e7896f96|true; _cb_ls=1; _cb=YE_wCycbhXz9AKD; cX_S=kq11p4gwbblt3ggd; cX_G=cx%3A9juzrxhdkcck1a5q2kexafx10%3Aqzz7o6kla9tw; tt.u=0100007F51CE7660AD068F4D02038F27; _sp_id.7839=3f569953ac6ff9f8.1621262289.4.1624617782.1622633003; _cc_id=2d20ebd05894649d44b14df6746bc0bd; _cb=DdRc05Bn5iiaBjpojp; _fbp=fb.2.1643630789850.1486112385; __tbc=%7Bkpex%7DFEjHp3gg5mhcRc5YvkWDjCK81kSZEljFNkTkwIl-Kq5ii14KxuceDQ6g1cTJxQqX; cX_P=kq11p3c9hc25qvaf; _ga_V46CYWTD2Z=GS1.1.1643640408.2.0.1643640408.60; UOLAF.CAD=0000017f-c36a-2aa4-0000-01805de8f2a4; UOL_FS=clicklogger.rm.uol.com.br; _ugfc=1; _pbjs_userid_consent_data=3524755945110770; __gads=ID=5273218535db7353:T=1648251002:S=ALNI_MbDNG9UwtHyxhMzG6yGwIFZh7uXnA; __pat=10800000; xbc=%7Bkpex%7DxPnhvOwi6uC6PGS1SQCob2q-edjJy4z9dPYR7Fdh_l7Mc9W_nyN8lCPcu8yk9OQeUHsvkNZj8lEOV80oBQBsBkMNWKcTr8mpoGptyncc03sMiEbH9-aTlD6_S25FVw13r-Nhzl-3mLwHzjudqh3inS83cs2ZP3PtfseLNZZFoTBzlhgEW6xNKjDV8qKm-9Y_ZUQrPmzIo2KbBw8VqRYgvvQc9oWRDj3nQ1qCnBOuZ9UNi9HhCGcxm2b-oBH-Ul_JWZtmGqDqeoDY70if0owi1Q; _gid=GA1.3.735297500.1648381561; tt.nprf=; nav23947=b1d369269fd075aa4d3d30a2b09|2_87; _ga_ZR3VDENZ5K=GS1.1.1648406289.3.0.1648406289.60; _ga=GA1.3.148775184.1611706525; _chartbeat2=.1623942935787.1648423672173.0000000000000101.f5XjUCffG5CC6LCPuBvhkrotZq8o.1; _cb_svref=https%3A%2F%2Fwww.uol.com.br%2Fesporte%2Ffutebol%2Fcampeonatos%2F; __pvi=%7B%22id%22%3A%22v-l19wwbule93mdgvc%22%2C%22domain%22%3A%22.uol.com.br%22%2C%22time%22%3A1648423679997%7D; FCNEC=[[\"AKsRol-oSQeQYd1yIAiWZ1fVAHqeWaZPbuUlb24ChhChgY6qNzaX8y1olEhYv-vnq1hTtZCD-f8NxRQFSNdqotjwDQ5laTHoH8zKbGQYatLQ50untBoYOOmcnsK0evHrsTEKp2L-cyr-xAXSTZg-xQKWGrhBukgCyQ==\"],null,[]]; tt_c_vmt=1648423686; tt_c_c=direct; tt_c_s=direct; tt_c_m=direct; _ttuu.s=1648423687362; _hjIncludedInSessionSample=0");
		httpConn.setRequestProperty("if-none-match", "\"62434eb14f11cbd97b965fd93d3742f3\"");
		httpConn.setRequestProperty("if-modified-since", "Sun, 27 Mar 2022 22:59:53 GMT");

		InputStream responseStream = null;
		try {
			responseStream = httpConn.getResponseCode() / 100 == 2 ? httpConn.getInputStream()
					: httpConn.getErrorStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Scanner s = new Scanner(responseStream).useDelimiter("\\A");
		String response = s.hasNext() ? s.next() : "";
		System.out.println(response);
	}
	
	
	
	public String montarUrlGoogle(String nomeEquipeCasa, String nomeEquipeVisitante) {
		
		
		try {
			
			String equipeCasa = nomeEquipeCasa.replace(" ", "+").replace("-", "+");
			String equipeVisitante = nomeEquipeVisitante.replace(" ", "+").replace("-", "+");
						
			return BASE_URL_GOOGLE+equipeCasa+"+"+equipeVisitante+COMPLEMENTO_URL_GOOGLE;
		}catch(Exception e){
			
			
		}
		
		return null;
	}
	
	
	
	
	
	
	
	
	
	
	
	
}
