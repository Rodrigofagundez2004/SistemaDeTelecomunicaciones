# Proyecto Integrador 2 - Sistema de Telecomunicaciones

Base del proyecto. Contiene:

- `src/main/java/ucu/edu/aed/tda/grafo/`: interfaces provistas por la cátedra (no modificar sin consultar).
- `.../tda/grafo/Impl/`: vacía. Ahí va la implementación de los TDA (dirigidos y no dirigidos), que hace el grupo.
- `.../telecom/`: modelo, servicios y aplicación del Desafío 2 (esqueletos).
- `src/main/resources/data/`: archivos CSV de ejemplo (solo encabezados por ahora).
- `docs/informe/`: plantillas del informe.

## Compilar

    mvn compile

## Ejecutar

    java -cp target/classes ucu.edu.aed.telecom.App

## Estado

Pendiente: implementar los TDA de grafos dirigidos y no dirigidos (Desafío 1) y los servicios del Desafío 2.
