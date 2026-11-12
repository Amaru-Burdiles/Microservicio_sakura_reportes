package com.sakura.reporte.client;

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
public class PersonalCliente {

	@Autowired
	private RestTemplate restTemplate;

	@Value("${sakura.personal.url}")
	private String baseUrl;

	public List<ProfesionalDTO> todos(String authorization) {
		return lista(baseUrl + "/api/v1/profesionales/gestion", authorization);
	}

	public List<ProfesionalDTO> activos() {
		return lista(baseUrl + "/api/v1/profesionales", null);
	}

	private List<ProfesionalDTO> lista(String url, String authorization) {
		try {
			HttpHeaders headers = new HttpHeaders();
			if (authorization != null) {
				headers.set(HttpHeaders.AUTHORIZATION, authorization);
			}
			List<ProfesionalDTO> cuerpo = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
					new ParameterizedTypeReference<List<ProfesionalDTO>>() {
					}).getBody();
			return cuerpo == null ? List.of() : cuerpo;
		} catch (RestClientException ex) {
			throw new ReglaNegocioException("No se pudo consultar el personal");
		}
	}

}
