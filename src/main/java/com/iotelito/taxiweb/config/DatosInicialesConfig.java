package com.iotelito.taxiweb.config;

import com.iotelito.taxiweb.model.EstadoSolicitudTaxista;
import com.iotelito.taxiweb.model.Rol;
import com.iotelito.taxiweb.model.Taxista;
import com.iotelito.taxiweb.model.Usuario;
import com.iotelito.taxiweb.repository.TaxistaRepository;
import com.iotelito.taxiweb.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class DatosInicialesConfig {

    @Bean
    CommandLineRunner crearDatosIniciales(
            UsuarioRepository usuarioRepository,
            TaxistaRepository taxistaRepository
    ) {
        return args -> {
            if (!usuarioRepository.existsByCorreo("admin@iotelito.com")) {
                Usuario admin = new Usuario();
                admin.setNombres("Super");
                admin.setApellidos("Administrador");
                admin.setCorreo("admin@iotelito.com");
                admin.setPassword("admin123");
                admin.setTelefono("999999999");
                admin.setRol(Rol.SUPERADMIN);
                admin.setActivo(true);
                admin.marcarCreacion();

                usuarioRepository.save(admin);
            }

            guardarTaxistaDemoSiNoExiste(taxistaRepository,
                    "Carlos", "Mendoza Quispe", "carlos.demo@iotelito.com", "987654321", "71234567", "ABC-123", "Toyota", "Corolla", 2022, EstadoSolicitudTaxista.PENDIENTE, false, "0.00", 0);
            guardarTaxistaDemoSiNoExiste(taxistaRepository,
                    "Maria", "Silva Torres", "maria.demo@iotelito.com", "987111222", "73569421", "XYZ-987", "Nissan", "Sentra", 2021, EstadoSolicitudTaxista.APROBADO, true, "4.80", 98);
            guardarTaxistaDemoSiNoExiste(taxistaRepository,
                    "Luis", "Ruiz Vargas", "luis.demo@iotelito.com", "987333444", "70324598", "MNO-456", "Kia", "Cerato", 2020, EstadoSolicitudTaxista.RECHAZADO, false, "3.90", 25);
            guardarTaxistaDemoSiNoExiste(taxistaRepository,
                    "Patricia", "Luna Vargas", "patricia.demo@iotelito.com", "987555666", "75842136", "KLP-789", "Hyundai", "Elantra", 2023, EstadoSolicitudTaxista.PENDIENTE, false, "0.00", 0);
            guardarTaxistaDemoSiNoExiste(taxistaRepository,
                    "Diego", "Ramos Paredes", "diego.demo@iotelito.com", "987777888", "74681293", "TAX-452", "Toyota", "Yaris", 2022, EstadoSolicitudTaxista.PENDIENTE, false, "0.00", 0);
            guardarTaxistaDemoSiNoExiste(taxistaRepository,
                    "Andrea", "Cordova Rios", "andrea.demo@iotelito.com", "987222333", "72148965", "HIL-310", "Honda", "City", 2021, EstadoSolicitudTaxista.APROBADO, true, "4.95", 132);
            guardarTaxistaDemoSiNoExiste(taxistaRepository,
                    "Fernando", "Salazar Vega", "fernando.demo@iotelito.com", "987444555", "73456789", "LIM-204", "Chevrolet", "Onix", 2020, EstadoSolicitudTaxista.APROBADO, true, "4.60", 74);
            guardarTaxistaDemoSiNoExiste(taxistaRepository,
                    "Rosa", "Huaman Castillo", "rosa.demo@iotelito.com", "987888999", "76985214", "HOT-118", "Kia", "Rio", 2019, EstadoSolicitudTaxista.APROBADO, false, "4.30", 41);
            guardarTaxistaDemoSiNoExiste(taxistaRepository,
                    "Jorge", "Navarro Soto", "jorge.demo@iotelito.com", "987000111", "71896325", "CUS-620", "Volkswagen", "Virtus", 2022, EstadoSolicitudTaxista.RECHAZADO, false, "2.80", 12);
            guardarTaxistaDemoSiNoExiste(taxistaRepository,
                    "Valeria", "Mejia Flores", "valeria.demo@iotelito.com", "987121212", "74321698", "MIR-907", "Mazda", "2 Sedan", 2023, EstadoSolicitudTaxista.PENDIENTE, false, "0.00", 0);
        };
    }

    private void guardarTaxistaDemoSiNoExiste(
            TaxistaRepository taxistaRepository,
            String nombres,
            String apellidos,
            String correo,
            String telefono,
            String dni,
            String placa,
            String marca,
            String modelo,
            Integer anio,
            EstadoSolicitudTaxista estado,
            boolean disponible,
            String calificacion,
            int servicios
    ) {
        if (!taxistaRepository.existsByDni(dni)) {
            taxistaRepository.save(crearTaxistaDemo(
                    nombres,
                    apellidos,
                    correo,
                    telefono,
                    dni,
                    placa,
                    marca,
                    modelo,
                    anio,
                    estado,
                    disponible,
                    calificacion,
                    servicios
            ));
        }
    }

    private Taxista crearTaxistaDemo(
            String nombres,
            String apellidos,
            String correo,
            String telefono,
            String dni,
            String placa,
            String marca,
            String modelo,
            Integer anio,
            EstadoSolicitudTaxista estado,
            boolean disponible,
            String calificacion,
            int servicios
    ) {
        Usuario usuario = new Usuario();
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setCorreo(correo);
        usuario.setPassword("demo123");
        usuario.setTelefono(telefono);
        usuario.setRol(Rol.TAXISTA);
        usuario.setActivo(estado == EstadoSolicitudTaxista.APROBADO);
        usuario.marcarCreacion();

        Taxista taxista = new Taxista();
        taxista.setUsuario(usuario);
        taxista.setDni(dni);
        taxista.setFechaNacimiento(LocalDate.of(1992, 4, 15));
        taxista.setDomicilio("Av. Larco 123, Miraflores, Lima");
        taxista.setNumeroLicencia("Q" + dni);
        taxista.setCategoriaLicencia("A-IIb");
        taxista.setFechaVencimientoLicencia(LocalDate.of(2028, 8, 18));
        taxista.setMarcaVehiculo(marca);
        taxista.setModeloVehiculo(modelo);
        taxista.setAnioVehiculo(anio);
        taxista.setColorVehiculo("Gris");
        taxista.setPlacaVehiculo(placa);
        taxista.setEstadoSolicitud(estado);
        taxista.setDisponible(disponible);
        taxista.setCalificacion(new BigDecimal(calificacion));
        taxista.setCantidadServicios(servicios);
        taxista.setAceptaTerminos(true);
        taxista.marcarSolicitud();

        return taxista;
    }
}
