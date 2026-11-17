package com.iotelito.taxiweb.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Todo error de la API sale con el esquema Error del contrato: codigo + mensaje. */
@RestControllerAdvice(assignableTypes = TaxiApiController.class)
public class ApiErrores {

    private static final Logger LOG = LoggerFactory.getLogger(ApiErrores.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiDtos.Error> api(ApiException error) {
        return ResponseEntity.status(error.getStatus()).body(new ApiDtos.Error(error.getCodigo(), error.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ApiDtos.Error> datos(Exception error) {
        return ResponseEntity.badRequest().body(new ApiDtos.Error("VALIDACION", "Revisa los datos enviados."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiDtos.Error> metodo(HttpRequestMethodNotSupportedException error) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ApiDtos.Error("VALIDACION", "Método no permitido en esta ruta."));
    }

    /** Dos cambios al mismo tiempo: la base rechazó uno. La app vuelve a consultar. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiDtos.Error> carrera(DataIntegrityViolationException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiDtos.Error("ESTADO_INVALIDO", "Otro cambio se adelantó. Vuelve a intentarlo."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiDtos.Error> inesperado(Exception error) {
        LOG.error("Error no controlado en la API", error);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiDtos.Error("NO_DISPONIBLE", "El servicio de taxis no está disponible. Intenta de nuevo en unos minutos."));
    }
}
