package com.sakura.reporte.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sakura.reporte.client.SesionGuard;
import com.sakura.reporte.model.Gasto;
import com.sakura.reporte.repository.GastoRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/gastos")
public class GastoController {

	@Autowired
	private GastoRepository gastoRepository;

	@Autowired
	private SesionGuard sesionGuard;

	@GetMapping
	public List<Gasto> listar(@RequestHeader(value = "Authorization", required = false) String authorization) {
		sesionGuard.exigirAdmin(authorization);
		return gastoRepository.findAll();
	}

	@PostMapping
	public ResponseEntity<Gasto> crear(@RequestHeader(value = "Authorization", required = false) String authorization,
			@Valid @RequestBody Gasto gasto) {
		sesionGuard.exigirAdmin(authorization);
		gasto.setId(null);
		return new ResponseEntity<>(gastoRepository.save(gasto), HttpStatus.CREATED);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long id) {
		sesionGuard.exigirAdmin(authorization);
		gastoRepository.deleteById(id);
		return ResponseEntity.noContent().build();
	}

}
