import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class DBClient extends JFrame {
    private JButton ejecutarBoton;
    private JTextField queryInput;
    private JTable tabla;
    private JPanel MainPanel;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public DBClient() {
        setupUI();
        this.setLocationRelativeTo(null);
        connectToServer();
        setContentPane(MainPanel);
        setTitle("Interfaz de cambios");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setVisible(true);

        ejecutarBoton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String query = queryInput.getText().trim();

                if (query.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Por favor, ingrese una solicitud SQL.");
                    return;
                }

                // Crear un nuevo hilo para ejecutar la consulta en segundo plano
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Enviar la solicitud al servidor
                            out.println(query);

                            // Recibir la respuesta del servidor
                            StringBuilder result = new StringBuilder();
                            String line;
                            while ((line = in.readLine()) != null && !line.isEmpty()) {
                                result.append(line).append("\n");                                
                                if (result.toString().contains("Solicitud ejecutada exitosamente. Filas afectadas: ")) {                                    
                                    JOptionPane.showMessageDialog(null, result.toString());
                                }else {}
                             
                            }

                            // Actualizar la interfaz gráfica de manera segura en el hilo principal
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    // Mostrar el resultado en la interfaz gráfica
                                    if (query.toUpperCase().startsWith("SELECT")) {
                                        displayResult(result.toString());
                                    } else {
                                        // Mostrar confirmación del UPDATE, INSERT o DELETE
                                        JOptionPane.showMessageDialog(null, result.toString());
                                    }
                                }
                            });
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            // Manejo de errores
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    JOptionPane.showMessageDialog(null, "Error al comunicarse con el servidor: " + ex.getMessage());
                                }
                            });
                        }
                    }
                }).start();
            }
        });
   
    }
        

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 8080); // Conexion al servidor a modo socket a traves del puerto 12345
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Conectado al servidor.");
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al conectarse al servidor: " + ex.getMessage());
        }
    }

    private void displayResult(String result) {
        String[] lines = result.split("\n");
        if (lines.length < 2) return;

        // Extraer columnas
        String[] columnNames = lines[0].split("\t");

        // Extraer filas
        DefaultTableModel tableModel = (DefaultTableModel) tabla.getModel();
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        for (String columnName : columnNames) {
            tableModel.addColumn(columnName);
        }

        for (int i = 1; i < lines.length; i++) {
            String[] row = lines[i].split("\t");
            tableModel.addRow(row);
        }
    }

    public static void main(String[] args) {
        new DBClient();
    }

    /**
     NO EDITAR
     */
    private void setupUI() {
        MainPanel = new JPanel();
        MainPanel.setLayout(new BorderLayout());

        // Panel superior para las queries
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        queryInput = new JTextField();
        queryInput.setFont(new Font("Arial", Font.PLAIN, 16));
        queryInput.setPreferredSize(new Dimension(0, 40));
        topPanel.add(queryInput, BorderLayout.CENTER);

        MainPanel.add(topPanel, BorderLayout.NORTH); // Agrega el panel superior a la parte superior del panel Main


        // Panel central para boton "Ejecutar"

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        ejecutarBoton = new JButton("Ejecutar");
        ejecutarBoton.setFont(new Font("Arial", Font.BOLD, 14));
        centerPanel.add(ejecutarBoton);

        MainPanel.add(centerPanel, BorderLayout.CENTER);

        // Panel inferior para los resultados (tablas)
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultTableModel tableModel = new DefaultTableModel();
        tabla = new JTable(tableModel);
        tabla.setFont(new Font("Arial", Font.PLAIN, 14));
        tabla.setRowHeight(20);
        JScrollPane tableScrollPane = new JScrollPane(tabla);

        bottomPanel.add(tableScrollPane, BorderLayout.CENTER);

        MainPanel.add(bottomPanel, BorderLayout.SOUTH);
    }
}
