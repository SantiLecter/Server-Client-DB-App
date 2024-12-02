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
            ServerSocket serverSocket = new ServerSocket(12345); // Port 12345
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
    // METODO PARA CONEXION DE BASE DE DATOS
    private static void createConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            // EDITAR la variable 'con' segun sea necesario, cada base de datos puede usar un puerto diferente asi como
            // credenciales diferentes
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/mydb", "root", "#Olakease1");
            System.out.println("Conexion establecida con la base de datos.");
        } catch (ClassNotFoundException | SQLException ex) {
            ex.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        ) {
            String query;
            while ((query = in.readLine()) != null) {
                System.out.println("Solicitud recibida: " + query);

                // Procesar la solicitud a la DB
                String result = executeQuery(query);

                // Enviar la solicitud de vuelta al cliente
                out.println(result);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static String executeQuery(String query) {
        try (Statement stmt = con.createStatement()) {
            if (query.toUpperCase().startsWith("SELECT")) {
                // Handle SELECT queries
                ResultSet rs = stmt.executeQuery(query);
                StringBuilder result = new StringBuilder();

                // Get column names
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    result.append(metaData.getColumnName(i)).append("\t");
                }
                result.append("\n");

                // Get rows
                while (rs.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        result.append(rs.getObject(i)).append("\t");
                    }
                    result.append("\n");
                }
                return result.toString();
            } else {
                // Handle INSERT, UPDATE, DELETE queries
                int rowsAffected = stmt.executeUpdate(query);
                return "Solicitud ejecutada exitosamente. Filas afectadas: " + rowsAffected;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return "Error ejecutando su solicitud: " + ex.getMessage();
        }
    }
}
