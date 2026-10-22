package com.santana.chamados;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// classe principal do microserviço
// o spring sobe tudo a partir daqui e encontra os componentes sozinho
@SpringBootApplication
public class ChamadosApplication {

	// liga o servidor (porta 8081 por padrão) e conecta no mongo
	public static void main(String[] args) {
		SpringApplication.run(ChamadosApplication.class, args);
	}

}
