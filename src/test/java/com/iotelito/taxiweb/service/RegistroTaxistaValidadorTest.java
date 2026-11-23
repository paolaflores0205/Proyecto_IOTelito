package com.iotelito.taxiweb.service;

import com.iotelito.taxiweb.dto.TaxistaRegistroForm;
import com.iotelito.taxiweb.model.TipoDocumento;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroTaxistaValidadorTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 23);

    static TaxistaRegistroForm valido() {
        TaxistaRegistroForm f = new TaxistaRegistroForm();
        f.setNombres("José Luis");
        f.setApellidos("Pérez Ñahui");
        f.setTipoDocumento(TipoDocumento.DNI);
        f.setNumeroDocumento("71234567");
        f.setFechaNacimiento(LocalDate.of(1990, 5, 10));
        f.setTelefono("912345678");
        f.setCorreo("jose@gmail.com");
        f.setDomicilio("Av. Universitaria 1801");
        f.setNumeroLicencia("q12345678");
        f.setCategoriaLicencia("A-IIb");
        f.setFechaVencimientoLicencia(HOY);
        f.setMarcaVehiculo("Toyota");
        f.setModeloVehiculo("Yaris");
        f.setAnioVehiculo(1998);
        f.setColorVehiculo("Plata");
        f.setPlacaVehiculo("abc-123");
        f.setAceptaTerminos(true);
        return f;
    }

    private static Map<String, String> errores(TaxistaRegistroForm f) {
        return RegistroTaxistaValidador.validar(f, HOY);
    }

    @Test
    void formularioCorrectoNoTieneErrores() {
        assertThat(errores(valido())).isEmpty();
    }

    @Test
    void dniExigeOchoDigitosYPasaporteAceptaLetras() {
        TaxistaRegistroForm f = valido();
        f.setNumeroDocumento("7123456");
        assertThat(errores(f)).containsKey("numeroDocumento");
        f.setNumeroDocumento("A1234567");
        assertThat(errores(f)).containsKey("numeroDocumento");
        f.setTipoDocumento(TipoDocumento.PASAPORTE);
        assertThat(errores(f)).doesNotContainKey("numeroDocumento");
        f.setTipoDocumento(TipoDocumento.CARNET_EXTRANJERIA);
        assertThat(errores(f)).containsKey("numeroDocumento");
    }

    @Test
    void nacimientoNoFuturoYMayorDeEdad() {
        TaxistaRegistroForm f = valido();
        f.setFechaNacimiento(HOY.plusDays(1));
        assertThat(errores(f)).containsEntry("fechaNacimiento", "La fecha de nacimiento no puede ser futura.");
        f.setFechaNacimiento(HOY.minusYears(18).plusDays(1));
        assertThat(errores(f)).containsEntry("fechaNacimiento", "Debes ser mayor de 18 años.");
        f.setFechaNacimiento(HOY.minusYears(18));
        assertThat(errores(f)).doesNotContainKey("fechaNacimiento");
    }

    @Test
    void licenciaVencidaSeRechaza() {
        TaxistaRegistroForm f = valido();
        f.setFechaVencimientoLicencia(HOY.minusDays(1));
        assertThat(errores(f)).containsKey("fechaVencimientoLicencia");
        f.setFechaVencimientoLicencia(null);
        assertThat(errores(f)).containsKey("fechaVencimientoLicencia");
    }

    @Test
    void telefonoPlacaLicenciaYCategoria() {
        TaxistaRegistroForm f = valido();
        f.setTelefono("12345678");
        f.setPlacaVehiculo("ABC123");
        f.setNumeroLicencia("12345678");
        f.setCategoriaLicencia("B");
        assertThat(errores(f)).containsKeys("telefono", "placaVehiculo", "numeroLicencia", "categoriaLicencia");
        f.setTelefono("012345678");
        assertThat(errores(f)).doesNotContainKey("telefono");
    }

    @Test
    void anioDelVehiculoSoloRechazaValoresImposibles() {
        TaxistaRegistroForm f = valido();
        f.setAnioVehiculo(1950);
        assertThat(errores(f)).doesNotContainKey("anioVehiculo");
        f.setAnioVehiculo(2027);
        assertThat(errores(f)).doesNotContainKey("anioVehiculo");
        for (Integer malo : new Integer[]{0, 1949, 2028, null}) {
            f.setAnioVehiculo(malo);
            assertThat(errores(f)).containsKey("anioVehiculo");
        }
    }

    @Test
    void nombresSoloLetrasYTerminosObligatorios() {
        TaxistaRegistroForm f = valido();
        f.setNombres("Juan123");
        f.setApellidos("X");
        f.setAceptaTerminos(false);
        assertThat(errores(f)).containsKeys("nombres", "apellidos", "aceptaTerminos");
    }
}
