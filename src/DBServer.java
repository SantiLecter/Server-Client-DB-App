import java.io.*;
import java.net.*;
import java.sql.*;

public class DBServer {
    private static Connection con;

    public static void main(String[] args) {
        try {
            // Establece la conexion con la base de datos
            createConnection();

            // Crea el socket del servidor para que se conecte el cliente a traves del puerto 12345
            ServerSocket serverSocket = new ServerSocket(8080); // Port 12345
            System.out.println("El servidor esta listo para recibir nuevas conexiones...");

            while (true) {
                // Accept client connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado!");

                // Handle client in a separate thread
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private static void createConnection() {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver"); //Carga el driver
            con = DriverManager.getConnection("jdbc:oracle:thin:@//localhost:1521/XEPDB1", "\"Poli01\"", "123456789");
            System.out.println("Conexión establecida con la base de datos Oracle.");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: No se encontró el driver JDBC.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Error: No se pudo establecer la conexión con la base de datos.");
            e.printStackTrace();
        }
    }
    
    private static void handleClient(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@//localhost:1521/XEPDB1", "\"Poli01\"", "123456789");
        ) {
            conn.setAutoCommit(false); // Desactivar auto commit para manejar transacciones manuales

            String query;
            while ((query = in.readLine()) != null) {
                // Verificar si el cliente desea terminar la conexión
                if ("EXIT".equalsIgnoreCase(query.trim())) {
                    System.out.println("Cliente solicitó desconexión.");
                    out.println("Conexión cerrada. ¡Adiós!");
                    break; // Salir del bucle
                }

                System.out.println("Solicitud recibida: " + query);

                // Procesar la solicitud a la DB
                String result = executeQuery(query);  // Pasar la conexión al método que ejecuta la consulta

                // Enviar la respuesta de vuelta al cliente
                out.println(result);
            }

            conn.commit(); // Confirmar la transacción después de procesar todas las consultas
        } catch (SQLException ex) {
            ex.printStackTrace();
            try {
            	Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@//localhost:1521/XEPDB1", "\"Poli01\"", "123456789");
                if (conn != null) conn.rollback(); // Hacer rollback en caso de error
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static String executeQuery(String query) {
        try {
            // Desactivar AutoCommit para manejar la transacción manualmente
            con.setAutoCommit(false);
            // Ajustar el nivel de aislamiento para permitir un bloqueo más ligero
            con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            
            try (Statement stmt = con.createStatement()) {
                // Configurar un timeout para evitar bloqueos prolongados
                stmt.setQueryTimeout(10);  // Timeout de 30 segundos
                
                if (query.toUpperCase().startsWith("SELECT")) {
                    // Manejo de consultas SELECT
                    ResultSet rs = stmt.executeQuery(query);
                    StringBuilder result = new StringBuilder();

                    // Obtener nombres de columnas
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        result.append(metaData.getColumnName(i)).append("\t");
                    }
                    result.append("\n");

                    // Obtener filas
                    while (rs.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            result.append(rs.getObject(i)).append("\t");
                        }
                        result.append("\n");
                    }
                    return result.toString();
                } else {
                    // Manejo de consultas INSERT, UPDATE, DELETE
                    int rowsAffected = stmt.executeUpdate(query);                    
                    con.commit();  // Confirmamos los cambios en la base de datos
                    String response = "Solicitud ejecutada exitosamente. Filas afectadas: " + rowsAffected;
                    return response;
                }
            } catch (SQLException ex) {
                // Si ocurre un error, revertimos la transacción
                con.rollback();
                ex.printStackTrace();
                return "Error ejecutando su solicitud: " + ex.getMessage();
            } finally {
                // Restauramos el AutoCommit a su estado original
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error de conexión: " + e.getMessage();
        }
    }


}
