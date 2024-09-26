package es.iessoterhernandez.daw.dwes.GestionNomina.Laboral;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class CalculaNominas {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int opcion;

        try {
            // Dar de alta un empleado individual
            Empleado empleadoIndividual = new Empleado("Guadalupe Martín", "28635397F", 'F');
            altaEmpleado(empleadoIndividual);

            // Dar de alta empleados a partir de un fichero
            altaEmpleado("empleadosNuevos.txt");

            // Menú en el que el usuario escoge una opción
            do {
                System.out.println("\n--- Menú Gestión de Nóminas ---");
                System.out.println("1. Mostrar todos los empleados");
                System.out.println("2. Mostrar salario por DNI");
                System.out.println("3. Modificar datos de un empleado");
                System.out.println("4. Recalcular y actualizar sueldo de un empleado");
                System.out.println("5. Recalcular y actualizar sueldos de todos los empleados");
                System.out.println("6. Realizar backup de la base de datos");
                System.out.println("0. Salir");
                System.out.print("Seleccione una opción: ");
                opcion = scanner.nextInt();
    
                switch (opcion) {
                    case 1:
                        mostrarEmpleados();
                        break;
                    case 2:
                        System.out.print("Introduce el DNI del empleado: ");
                        String dniSalario = scanner.next();
                        mostrarSalarioPorDni(dniSalario);
                        break;
                    case 3:
                        System.out.print("Introduce el DNI del empleado a modificar: ");
                        String dniModificar = scanner.next();
                        modificarDatosEmpleado(dniModificar);
                        break;
                    case 4:
                        System.out.print("Introduce el DNI del empleado: ");
                        String dniRecalcular = scanner.next();
                        recalcularYActualizarSueldo(dniRecalcular);
                        break;
                    case 5:
                        recalcularYActualizarTodosLosSueldos();
                        break;
                    case 6:
                        realizarBackup();
                        break;
                    case 0:
                        System.out.println("Saliendo del programa...");
                        break;
                    default:
                        System.out.println("Opción no válida.");
                }
            } while (opcion != 0);
    
            scanner.close();
            
        } catch (DatosNoCorrectosException e) {
            System.out.println("Datos no correctos");
        }
    }

    // Método para dar de alta un empleado individual y guardar el sueldo en la BD
    public static void altaEmpleado(Empleado empleado) {
        Nomina nomina = new Nomina();
        double sueldoFinal = nomina.sueldo(empleado);
        
        // SQL para insertar o actualizar en la tabla empleados
        String sqlEmpleado = "INSERT INTO empleados (dni, nombre, sexo, categoria, anyos) VALUES (?, ?, ?, ?, ?) "
                           + "ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), sexo = VALUES(sexo), "
                           + "categoria = VALUES(categoria), anyos = VALUES(anyos)";

        // SQL para insertar o actualizar en la tabla nominas
        String sqlNomina = "INSERT INTO nominas (dni, categoria, sueldofinal) VALUES (?, ?, ?) "
                         + "ON DUPLICATE KEY UPDATE categoria = VALUES(categoria), sueldofinal = VALUES(sueldofinal)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement psEmpleado = conn.prepareStatement(sqlEmpleado);
             PreparedStatement psNomina = conn.prepareStatement(sqlNomina)) {

            // Inserción o actualización en la tabla empleados
            psEmpleado.setString(1, empleado.getDni());
            psEmpleado.setString(2, empleado.getNombre());
            psEmpleado.setString(3, String.valueOf(empleado.getSexo()));
            psEmpleado.setInt(4, empleado.getCategoria());
            psEmpleado.setInt(5, empleado.getAnyos());
            psEmpleado.executeUpdate();
            
            // Inserción o actualización en la tabla nominas
            psNomina.setString(1, empleado.getDni());
            psNomina.setInt(2, empleado.getCategoria());
            psNomina.setDouble(3, sueldoFinal);
            psNomina.executeUpdate();

            System.out.println("Empleado " + empleado.getNombre() + " dado de alta o actualizado con éxito.");

        } catch (SQLException e) {
            System.out.println("Error al insertar o actualizar empleado en la base de datos: " + e.getMessage());
        }
    }

    // Sobrecarga para dar de alta empleados desde un fichero
    public static void altaEmpleado(String archivoEntrada) {
        try (BufferedReader reader = new BufferedReader(new FileReader(archivoEntrada))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.startsWith("nombre:")) {
                    String nombre = obtenerValor(linea, "nombre");
                    String dni = obtenerValor(reader.readLine(), "dni");
                    char sexo = obtenerValor(reader.readLine(), "sexo").charAt(0);

                    String categoriaLine = reader.readLine();
                    String anyosLine = reader.readLine();

                    int categoria = (categoriaLine != null && categoriaLine.contains(":"))
                            ? Integer.parseInt(obtenerValor(categoriaLine, "categoria"))
                            : 1;
                    int anyos = (anyosLine != null && anyosLine.contains(":"))
                            ? Integer.parseInt(obtenerValor(anyosLine, "anyos"))
                            : 0;

                    Empleado empleado;
                    if (categoria != 1 && anyos != 0) {
                        empleado = new Empleado(nombre, dni, sexo, categoria, anyos);
                    } else {
                        empleado = new Empleado(nombre, dni, sexo);
                    }

                    altaEmpleado(empleado);
                }
            }
        } catch (IOException | DatosNoCorrectosException e) {
            System.out.println(e);
        }
    }

    // Método que ayuda a obtener los valores de empleadosNuevos.txt
    private static String obtenerValor(String linea, String campo) {
        String[] partes = linea.split(":");
        if (partes.length > 1) {
            return partes[1].trim();
        } else {
            throw new IllegalArgumentException("Formato incorrecto para el campo: " + campo);
        }
    }

    // Muestra los valores de todos los empleados (menú)
    private static void mostrarEmpleados() {
        String query = "SELECT * FROM empleados";
        try (Connection conn = DBUtils.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                System.out.println("DNI: " + rs.getString("dni")
                        + ", Nombre: " + rs.getString("nombre")
                        + ", Sexo: " + rs.getString("sexo")
                        + ", Categoría: " + rs.getInt("categoria")
                        + ", Años: " + rs.getInt("anyos"));
            }

        } catch (SQLException e) {
            System.out.println("Error al obtener los empleados: " + e.getMessage());
        }
    }

    // A partir de un DNI, muestra el salario del empleado
    private static void mostrarSalarioPorDni(String dni) {
        String query = "SELECT sueldofinal FROM nominas WHERE dni = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Salario: " + rs.getDouble("sueldofinal"));
                } else {
                    System.out.println("No se encontró el salario para el DNI proporcionado.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener el salario: " + e.getMessage());
        }
    }

    // Pide DNI y se puede modificar (opcionalmente) los datos de un empleado
    private static void modificarDatosEmpleado(String dni) {
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM empleados WHERE dni = ?")) {

            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Scanner scanner = new Scanner(System.in);

                    System.out.println("Empleado actual:");
                    System.out.println("Nombre: " + rs.getString("nombre"));
                    System.out.println("Sexo: " + rs.getString("sexo"));
                    System.out.println("Categoría: " + rs.getInt("categoria"));
                    System.out.println("Años: " + rs.getInt("anyos"));

                    System.out.print("Nuevo nombre (enter para mantener): ");
                    String nuevoNombre = scanner.nextLine();
                    nuevoNombre = nuevoNombre.isEmpty() ? rs.getString("nombre") : nuevoNombre;

                    System.out.print("Nuevo sexo (M/F, enter para mantener): ");
                    String nuevoSexo = scanner.nextLine();
                    nuevoSexo = nuevoSexo.isEmpty() ? rs.getString("sexo") : nuevoSexo;

                    System.out.print("Nueva categoría (enter para mantener): ");
                    String nuevaCategoriaStr = scanner.nextLine();
                    int nuevaCategoria = nuevaCategoriaStr.isEmpty() ? rs.getInt("categoria") : Integer.parseInt(nuevaCategoriaStr);

                    System.out.print("Nuevos años (enter para mantener): ");
                    String nuevosAnyosStr = scanner.nextLine();
                    int nuevosAnyos = nuevosAnyosStr.isEmpty() ? rs.getInt("anyos") : Integer.parseInt(nuevosAnyosStr);

                    // Actualizar los datos del empleado
                    PreparedStatement updatePs = conn.prepareStatement(
                        "UPDATE empleados SET nombre = ?, sexo = ?, categoria = ?, anyos = ? WHERE dni = ?"
                    );
                    updatePs.setString(1, nuevoNombre);
                    updatePs.setString(2, nuevoSexo);
                    updatePs.setInt(3, nuevaCategoria);
                    updatePs.setInt(4, nuevosAnyos);
                    updatePs.setString(5, dni);
                    updatePs.executeUpdate();

                    // Recalcular el sueldo del empleado después de la actualización
                    recalcularYActualizarSueldo(dni);

                } else {
                    System.out.println("No se encontró el empleado con el DNI proporcionado.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Error al modificar los datos del empleado: " + e.getMessage());
        }
    }

    // Actualiza el salario de un empleado si hay cambios en la categoría o años trabajados
    private static void recalcularYActualizarSueldo(String dni) {
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM empleados WHERE dni = ?")) {

            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Empleado empleado = new Empleado(
                        rs.getString("nombre"),
                        rs.getString("dni"),
                        rs.getString("sexo").charAt(0),
                        rs.getInt("categoria"),
                        rs.getInt("anyos")
                    );

                    Nomina nomina = new Nomina();
                    double sueldoFinal = nomina.sueldo(empleado);

                    PreparedStatement updateNomina = conn.prepareStatement(
                        "INSERT INTO nominas (dni, categoria, sueldofinal) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE categoria = VALUES(categoria), sueldofinal = VALUES(sueldofinal)"
                    );
                    updateNomina.setString(1, empleado.getDni());
                    updateNomina.setInt(2, empleado.getCategoria());
                    updateNomina.setDouble(3, sueldoFinal);
                    updateNomina.executeUpdate();

                    System.out.println("Sueldo actualizado con éxito.");
                } else {
                    System.out.println("No se encontró el empleado con el DNI proporcionado.");
                }
            }
        } catch (SQLException | DatosNoCorrectosException e) {
            System.out.println("Error al recalcular el sueldo: " + e.getMessage());
        }
    }

    // Actualiza el salario de todos los empleados si hay cambios
    private static void recalcularYActualizarTodosLosSueldos() {
        String query = "SELECT * FROM empleados";
        try (Connection conn = DBUtils.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                Empleado empleado = new Empleado(
                    rs.getString("nombre"),
                    rs.getString("dni"),
                    rs.getString("sexo").charAt(0),
                    rs.getInt("categoria"),
                    rs.getInt("anyos")
                );
                recalcularYActualizarSueldo(empleado.getDni());
            }

            System.out.println("Sueldos de todos los empleados actualizados con éxito.");

        } catch (SQLException | DatosNoCorrectosException e) {
            System.out.println("Error al actualizar los sueldos: " + e.getMessage());
        }
    }

    // Crea o actualiza un archivo backup.txt en el que figuran todos los empleados y salarios
    private static void realizarBackup() {
        String empleadosQuery = "SELECT * FROM empleados";
        String nominasQuery = "SELECT * FROM nominas";
        try (Connection conn = DBUtils.getConnection();
             Statement st = conn.createStatement();
             ResultSet empleadosRs = st.executeQuery(empleadosQuery);
             ResultSet nominasRs = st.executeQuery(nominasQuery);
             BufferedWriter writer = new BufferedWriter(new FileWriter("backup.txt"))) {

            writer.write("Backup de empleados:\n");
            while (empleadosRs.next()) {
                writer.write("DNI: " + empleadosRs.getString("dni")
                        + ", Nombre: " + empleadosRs.getString("nombre")
                        + ", Sexo: " + empleadosRs.getString("sexo")
                        + ", Categoría: " + empleadosRs.getInt("categoria")
                        + ", Años: " + empleadosRs.getInt("anyos") + "\n");
            }

            writer.write("\nBackup de nominas:\n");
            while (nominasRs.next()) {
                writer.write("DNI: " + nominasRs.getString("dni")
                        + ", Categoría: " + nominasRs.getInt("categoria")
                        + ", Sueldo final: " + nominasRs.getDouble("sueldofinal") + "\n");
            }

            System.out.println("Backup realizado con éxito en backup.txt");

        } catch (SQLException | IOException e) {
            System.out.println("Error al realizar el backup: " + e.getMessage());
        }
    }
}
