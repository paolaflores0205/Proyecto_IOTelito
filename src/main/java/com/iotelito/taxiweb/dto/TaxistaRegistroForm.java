package com.iotelito.taxiweb.dto;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public class TaxistaRegistroForm {

    // Usuario
    private String nombres;
    private String apellidos;
    private String correo;
    private String telefono;

    // Taxista
    private String dni;
    private LocalDate fechaNacimiento;
    private String domicilio;

    private String numeroLicencia;
    private String categoriaLicencia;
    private LocalDate fechaVencimientoLicencia;

    private String marcaVehiculo;
    private String modeloVehiculo;
    private Integer anioVehiculo;
    private String colorVehiculo;
    private String placaVehiculo;

    private Boolean aceptaTerminos;

    // Documentos
    private MultipartFile fotoConductorFile;
    private MultipartFile dniFile;
    private MultipartFile licenciaFile;
    private MultipartFile soatFile;
    private MultipartFile tarjetaPropiedadFile;

    public TaxistaRegistroForm() {
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getDomicilio() {
        return domicilio;
    }

    public void setDomicilio(String domicilio) {
        this.domicilio = domicilio;
    }

    public String getNumeroLicencia() {
        return numeroLicencia;
    }

    public void setNumeroLicencia(String numeroLicencia) {
        this.numeroLicencia = numeroLicencia;
    }

    public String getCategoriaLicencia() {
        return categoriaLicencia;
    }

    public void setCategoriaLicencia(String categoriaLicencia) {
        this.categoriaLicencia = categoriaLicencia;
    }

    public LocalDate getFechaVencimientoLicencia() {
        return fechaVencimientoLicencia;
    }

    public void setFechaVencimientoLicencia(LocalDate fechaVencimientoLicencia) {
        this.fechaVencimientoLicencia = fechaVencimientoLicencia;
    }

    public String getMarcaVehiculo() {
        return marcaVehiculo;
    }

    public void setMarcaVehiculo(String marcaVehiculo) {
        this.marcaVehiculo = marcaVehiculo;
    }

    public String getModeloVehiculo() {
        return modeloVehiculo;
    }

    public void setModeloVehiculo(String modeloVehiculo) {
        this.modeloVehiculo = modeloVehiculo;
    }

    public Integer getAnioVehiculo() {
        return anioVehiculo;
    }

    public void setAnioVehiculo(Integer anioVehiculo) {
        this.anioVehiculo = anioVehiculo;
    }

    public String getColorVehiculo() {
        return colorVehiculo;
    }

    public void setColorVehiculo(String colorVehiculo) {
        this.colorVehiculo = colorVehiculo;
    }

    public String getPlacaVehiculo() {
        return placaVehiculo;
    }

    public void setPlacaVehiculo(String placaVehiculo) {
        this.placaVehiculo = placaVehiculo;
    }

    public Boolean getAceptaTerminos() {
        return aceptaTerminos;
    }

    public void setAceptaTerminos(Boolean aceptaTerminos) {
        this.aceptaTerminos = aceptaTerminos;
    }

    public MultipartFile getFotoConductorFile() {
        return fotoConductorFile;
    }

    public void setFotoConductorFile(MultipartFile fotoConductorFile) {
        this.fotoConductorFile = fotoConductorFile;
    }

    public MultipartFile getDniFile() {
        return dniFile;
    }

    public void setDniFile(MultipartFile dniFile) {
        this.dniFile = dniFile;
    }

    public MultipartFile getLicenciaFile() {
        return licenciaFile;
    }

    public void setLicenciaFile(MultipartFile licenciaFile) {
        this.licenciaFile = licenciaFile;
    }

    public MultipartFile getSoatFile() {
        return soatFile;
    }

    public void setSoatFile(MultipartFile soatFile) {
        this.soatFile = soatFile;
    }

    public MultipartFile getTarjetaPropiedadFile() {
        return tarjetaPropiedadFile;
    }

    public void setTarjetaPropiedadFile(MultipartFile tarjetaPropiedadFile) {
        this.tarjetaPropiedadFile = tarjetaPropiedadFile;
    }
}