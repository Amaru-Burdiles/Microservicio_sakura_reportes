package com.sakura.reporte.client;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.sakura.reporte.exception.ReglaNegocioException;

@Component
public class AgendaCliente {

	@Autowired
	private RestTemplate restTemplate;

	@Value("${sakura.agenda.url}")
	private String baseUrl;

	public List<ReservaDTO> rango(LocalDate desde, LocalDate hasta, String authorization) {
		try {
			String url = baseUrl + "/api/v1/reservas/rango?desde=" + desde + "&hasta=" + hasta;
			HttpHeaders headers = new HttpHeaders();
			if (authorization != null) {
				headers.set(HttpHeaders.AUTHORIZATION, authorization);
			}
			List<ReservaDTO> cuerpo = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
					new ParameterizedTypeReference<List<ReservaDTO>>() {
					}).getBody();
			return cuerpo == null ? List.of() : cuerpo;
		} catch (RestClientException ex) {
			throw new ReglaNegocioException("No se pudo consultar la agenda");
		}
	}

}
