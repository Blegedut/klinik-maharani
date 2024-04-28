import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ClinicManagementSystem extends JFrame {
    // Deklarasi variabel-variabel kelas
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton addButton, prevButton, nextButton, exitButton, refreshButton, editButton, deleteButton;
    private int currentIndex = 0; // Variable to track the current index of displayed data
    private int displayedId = 1; // Variable to track the displayed ID in the table

    public ClinicManagementSystem() {
        // Konfigurasi frame utama
        setTitle("Clinic Management System");
        setSize(600, 400);
        setLayout(new BorderLayout());

        // Inisialisasi tombol-tombol
        addButton = new JButton("Add Data");
        prevButton = new JButton("Prev");
        nextButton = new JButton("Next");
        exitButton = new JButton("Exit");
        refreshButton = new JButton("Refresh");
        editButton = new JButton("Edit");
        deleteButton = new JButton("Delete");
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);

        // Panel atas dengan tombol-tombol
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(addButton);
        topPanel.add(refreshButton);
        add(topPanel, BorderLayout.NORTH);

        // Tabel dan model tabel
        table = new JTable();
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Panel bawah dengan tombol-tombol
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(prevButton);
        bottomPanel.add(nextButton);
        bottomPanel.add(editButton);
        bottomPanel.add(deleteButton);
        bottomPanel.add(exitButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // Koneksi ke database dan inisialisasi tabel
        initializeDatabase();

        // Listener tombol
        addButton.addActionListener(e -> showAddPatientDialog());
        prevButton.addActionListener(e -> previousPatient());
        nextButton.addActionListener(e -> nextPatient());
        editButton.addActionListener(e -> editPatient());
        deleteButton.addActionListener(e -> deletePatient());
        exitButton.addActionListener(e -> exit());
        refreshButton.addActionListener(e -> refreshData());

        // Listener untuk memperbarui status tombol "Edit" dan "Delete"
        table.getSelectionModel().addListSelectionListener(e -> {
            int checkedCount = 0; // Variable to count the number of checked checkboxes
            for (int i = 0; i < table.getRowCount(); i++) {
                boolean isChecked = (boolean) table.getValueAt(i, table.getColumnCount() - 1); // Check the value of the checkbox column
                if (isChecked) {
                    checkedCount++;
                    if (checkedCount > 1) { // If more than one checkbox is checked, disable the Edit button and exit loop
                        editButton.setEnabled(false);
                        return;
                    }
                }
            }
            // Enable the Edit button if only one checkbox is checked, otherwise disable it
            editButton.setEnabled(checkedCount == 1);
            // Enable the Delete button if at least one checkbox is checked
            deleteButton.setEnabled(checkedCount > 0);
            // Disable both buttons if no checkbox is checked
            if (checkedCount == 0) {
                editButton.setEnabled(false);
                deleteButton.setEnabled(false);
            }
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    // Metode untuk menginisialisasi koneksi dan memuat data dari database ke tabel
    private void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/klinik_maharani", "root", "");
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

            // Retrieve the latest 5 records from the database
            resultSet = statement.executeQuery("SELECT * FROM patients ORDER BY id DESC LIMIT 5");

            // Inisialisasi model tabel dengan nama kolom
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            String[] columnNames = new String[columnCount + 1]; // Add one more column for checkbox
            for (int i = 0; i < columnCount; i++) {
                columnNames[i] = metaData.getColumnName(i + 1);
            }
            columnNames[columnCount] = "Select"; // Set the header for the new column
            tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    if (columnIndex == columnCount) {
                        return Boolean.class; // Set the class type for checkbox column
                    }
                    return super.getColumnClass(columnIndex);
                }
            };
            table.setModel(tableModel);

            // Populate table with data
            while (resultSet.next()) {
                Object[] rowData = new Object[columnCount + 1]; // Add one more element for checkbox
                rowData[0] = displayedId++; // Set the displayed ID
                for (int i = 1; i < columnCount; i++) {
                    rowData[i] = resultSet.getObject(i + 1);
                }
                rowData[columnCount] = false; // Set default value for checkbox
                tableModel.addRow(rowData);
            }

            // Set initial index value
            currentIndex = 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: Failed to connect to the database.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Metode untuk menampilkan dialog penambahan data pasien
    private void showAddPatientDialog() {
        // Buat panel untuk komponen dialog
        JPanel panel = new JPanel(new GridLayout(4, 2));

        // Tambahkan label dan field teks untuk input
        panel.add(new JLabel("Name:"));
        JTextField nameField = new JTextField();
        panel.add(nameField);

        panel.add(new JLabel("NIK:"));
        JTextField NIKField = new JTextField();
        panel.add(NIKField);

        panel.add(new JLabel("Birth Date (YYYY-MMM-DD):"));
        JTextField birthDateField = new JTextField();
        panel.add(birthDateField);

        panel.add(new JLabel("Address:"));
        JTextField addressField = new JTextField();
        panel.add(addressField);

        // Tampilkan dialog dan ambil input pengguna
        int result = JOptionPane.showConfirmDialog(this, panel, "Add Patient", JOptionPane.OK_CANCEL_OPTION);

        // Jika pengguna menekan OK, tambahkan data pasien baru ke database
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText();
            String NIK = NIKField.getText();
            String birthDateStr = birthDateField.getText();

            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MMM-dd");
                java.util.Date birthDate = inputFormat.parse(birthDateStr);
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
                String formattedBirthDate = outputFormat.format(birthDate);

                // Masukkan data ke dalam database
                String sql = "INSERT INTO patients (name, NIK, birth_date, address) VALUES (?, ?, ?, ?)";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, name);
                preparedStatement.setString(2, NIK);
                preparedStatement.setString(3, formattedBirthDate);
                preparedStatement.setString(4, addressField.getText());
                preparedStatement.executeUpdate();
                
                // Tampilkan pesan sukses setelah berhasil ditambahkan ke database
                JOptionPane.showMessageDialog(this, "Patient added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error: Failed to add patient.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Method for handling Edit button action
    private void editPatient() {
        // Get the selected row index
        int selectedRow = table.getSelectedRow();
        // If a row is selected
        if (selectedRow != -1) {
            // Get the NIK from the selected row
            Long NIK = (Long) table.getValueAt(selectedRow, 3);
            
            // Retrieve patient data from the database based on the NIK
            try {
                // Query to retrieve patient data based on NIK
                String sql = "SELECT * FROM patients WHERE NIK = ?";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setLong(1, NIK);
                ResultSet rs = preparedStatement.executeQuery();
    
                // If data is retrieved successfully
                if (rs.next()) {
                    // Create a panel for dialog components
                    JPanel panel = new JPanel(new GridLayout(5, 2));
    
                    // Add labels and text fields for input
                    panel.add(new JLabel("Name:"));
                    JTextField nameField = new JTextField(rs.getString("name"));
                    panel.add(nameField);
    
                    panel.add(new JLabel("NIK:"));
                    JTextField NIKField = new JTextField(rs.getString("NIK"));
                    panel.add(NIKField);
    
                    panel.add(new JLabel("Birth Date (YYYY-MM-DD):"));
                    JTextField birthDateField = new JTextField(rs.getString("birth_date"));
                    panel.add(birthDateField);
    
                    panel.add(new JLabel("Address:"));
                    JTextField addressField = new JTextField(rs.getString("address"));
                    panel.add(addressField);
    
                    // Show dialog and capture user input
                    int result = JOptionPane.showConfirmDialog(this, panel, "Edit Patient", JOptionPane.OK_CANCEL_OPTION);
    
                    // If the user clicks OK, update patient data in the database
                    if (result == JOptionPane.OK_OPTION) {
                        String name = nameField.getText();
                        String NIKUpdated = NIKField.getText();
                        String birthDate = birthDateField.getText();
                        String address = addressField.getText();
    
                        // Update patient data in the database
                        String updateSql = "UPDATE patients SET name = ?, NIK = ?, birth_date = ?, address = ? WHERE NIK = ?";
                        PreparedStatement updateStatement = connection.prepareStatement(updateSql);
                        updateStatement.setString(1, name);
                        updateStatement.setString(2, NIKUpdated);
                        updateStatement.setString(3, birthDate);
                        updateStatement.setString(4, address);
                        updateStatement.setLong(5, NIK);
                        updateStatement.executeUpdate();
    
                        // Show success message after update
                        JOptionPane.showMessageDialog(this, "Patient data updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Patient data not found.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error: Failed to retrieve patient data.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Method for handling Delete button action
    private void deletePatient() {
        // Iterate through the table rows to find patients to delete
        for (int i = 0; i < table.getRowCount(); i++) {
            boolean isChecked = (boolean) table.getValueAt(i, table.getColumnCount() - 1); // Check the value of the checkbox column
            // If the checkbox is checked, get the NIK from the selected row and print it to console
            if (isChecked) {
                Object NIKObj = table.getValueAt(i, 3); // Get NIK from the selected row
                // Check if NIKObj is not null and is an instance of Long
                if (NIKObj != null && NIKObj instanceof Long) {
                    Long NIK = (Long) NIKObj;
                    // Print the NIK to console
                    System.out.println("Selected NIK: " + NIK);
                    
                    // Delete data from the database
                    try {
                        // Prepare SQL statement to delete data with matching NIKs
                        String sql = "DELETE FROM patients WHERE NIK = ?";
                        PreparedStatement preparedStatement = connection.prepareStatement(sql);
                        
                        // Set the NIK parameter value in the DELETE statement
                        preparedStatement.setLong(1, NIK);
                        // Execute the DELETE statement
                        preparedStatement.executeUpdate();
    
                        // Show success message after deletion is successful
                        JOptionPane.showMessageDialog(this, "Data deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(this, "Error: Failed to delete data.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    // Handle case where NIKObj is not a Long or is null
                    System.out.println("NIKObj is not a Long or is null.");
                }
            }
        }
        
        // Reload data to update the view
        refreshData();
    }

    private void previousPatient() {
        // Logic for displaying previous patient's data
    
        // Clear data in the table
        tableModel.setRowCount(0);
    
        // Update current index
        currentIndex -= 5;
    
        if (currentIndex < 0) {
            currentIndex = 0;
        }
    
        // Retrieve previous 5 records from the database based on the updated index
        try {
            resultSet = statement.executeQuery("SELECT * FROM patients ORDER BY id DESC LIMIT " + currentIndex + ", 5");
    
            // Populate table with data
            int displayedId = currentIndex + 1; // Set the displayed ID
            while (resultSet.next()) {
                Object[] rowData = new Object[tableModel.getColumnCount()];
                rowData[0] = displayedId++; // Increment displayed ID
                for (int i = 1; i < tableModel.getColumnCount() - 1; i++) {
                    rowData[i] = resultSet.getObject(i + 1);
                }
                rowData[tableModel.getColumnCount() - 1] = false; // Set default value for checkbox
                tableModel.addRow(rowData);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: Failed to retrieve previous data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void nextPatient() {
        // Logic for displaying next patient's data
    
        // Clear data in the table
        tableModel.setRowCount(0);
    
        // Update current index
        currentIndex += 5;
    
        // Retrieve next 5 records from the database based on the updated index
        try {
            resultSet = statement.executeQuery("SELECT * FROM patients ORDER BY id DESC LIMIT " + currentIndex + ", 5");
    
            // Populate table with data
            int displayedId = currentIndex + 1; // Set the displayed ID
            while (resultSet.next()) {
                Object[] rowData = new Object[tableModel.getColumnCount()];
                rowData[0] = displayedId++; // Increment displayed ID
                for (int i = 1; i < tableModel.getColumnCount() - 1; i++) {
                    rowData[i] = resultSet.getObject(i + 1);
                }
                rowData[tableModel.getColumnCount() - 1] = false; // Set default value for checkbox
                tableModel.addRow(rowData);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: Failed to retrieve next data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }  

    private void exit() {
        // Logic for exiting the application
        dispose();
    }

    // Metode untuk memuat ulang data dari database ke tabel
    private void refreshData() {
        // Clear data in the table
        tableModel.setRowCount(0);
    
        // Reset the displayed ID
        displayedId = 1;
    
        // Reload data from the database
        try {
            resultSet = statement.executeQuery("SELECT * FROM patients ORDER BY id DESC LIMIT 5");
    
            // Populate table with data
            while (resultSet.next()) {
                Object[] rowData = new Object[tableModel.getColumnCount()];
                rowData[0] = displayedId++; // Set the displayed ID
                for (int i = 1; i < tableModel.getColumnCount() - 1; i++) {
                    rowData[i] = resultSet.getObject(i + 1);
                }
                rowData[tableModel.getColumnCount() - 1] = false; // Set default value for checkbox
                tableModel.addRow(rowData);
            }
    
            // Reset the currentIndex
            currentIndex = 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: Failed to refresh data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        new ClinicManagementSystem();
    }
}



