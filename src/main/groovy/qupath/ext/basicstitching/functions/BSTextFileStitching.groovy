package qupath.ext.basicstitching.functions

import javafx.scene.control.*
import javafx.scene.layout.GridPane
import qupath.lib.gui.dialogs.Dialogs
import javafx.stage.Modality
import javafx.stage.DirectoryChooser
import qupath.lib.gui.scripting.QPEx
import qupath.lib.images.writers.ome.OMEPyramidWriter

//import java.nio.file.Paths
//import qupath.lib.images.writers.ome.OMETiffWriter

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BSTextFileStitching {

    private static final Logger logger = LoggerFactory.getLogger(BSTextFileStitching.class)
    static TextField folderField = new TextField()
    static ComboBox<String> compressionBox = new ComboBox<>()
    static TextField pixelSizeField = new TextField("0.4988466")
    static TextField downsampleField = new TextField("1")
    static TextField matchStringField = new TextField("20x")

    static void createGUI() {
        // Create the dialog
        def dlg = new Dialog<ButtonType>()
        dlg.initModality(Modality.APPLICATION_MODAL)
        dlg.setTitle("File Name Based Stitching")
        dlg.setHeaderText("Enter your settings below:")

        // Set the content
        dlg.getDialogPane().setContent(createContent())

        // Add Okay and Cancel buttons
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)

        // Show the dialog and capture the response
        def result = dlg.showAndWait()

        // Handling the response
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String folderPath = folderField.getText() // Assuming folderField is accessible
            String compressionType = compressionBox.getValue() // Assuming compressionBox is accessible

            // Check if pixelSizeField and downsampleField are not empty
            double pixelSize = pixelSizeField.getText() ? Double.parseDouble(pixelSizeField.getText()) : 0 // Default to 0 if empty
            double downsample = downsampleField.getText() ? Double.parseDouble(downsampleField.getText()) : 1 // Default to 1 if empty

            String matchingString = matchStringField.getText() // Assuming matchStringField is accessible

            // Call the function with collected data
            stitchByFileName(folderPath, compressionType, pixelSize, downsample, matchingString)
        }

    }

    private static GridPane createContent() {
        // Create the layout
        GridPane pane = new GridPane()
        pane.setHgap(10)
        pane.setVgap(10)

        // Folder selection button and text field
        Label folderLabel = new Label("Folder location:")
        Button folderButton = new Button("Select Folder")
        // Initialize folder path with default value
        try {
            String defaultFolderPath = QPEx.buildPathInProject("Tiles")
            logger.info("Default folder path: {}", defaultFolderPath)
            folderField.setText(defaultFolderPath)
        } catch (Exception e) {
            logger.error("Error setting default folder path", e)
        }
        // Set the action for the button
        folderButton.setOnAction { e ->
            try {
                DirectoryChooser dirChooser = new DirectoryChooser()
                dirChooser.setTitle("Select Folder")

                String initialDirPath = folderField.getText()
                logger.info("Initial directory path from field: {}", initialDirPath)

                File initialDir = new File(initialDirPath)
                if (initialDir.exists() && initialDir.isDirectory()) {
                    dirChooser.setInitialDirectory(initialDir)
                    logger.info("Directory chooser starting at: {}", initialDir.getAbsolutePath())
                } else {
                    logger.warn("Initial directory does not exist or is not a directory: {}", initialDir.getAbsolutePath())
                }

                File selectedDir = dirChooser.showDialog(null) // Replace null with your stage if available
                if (selectedDir != null) {
                    folderField.setText(selectedDir.getAbsolutePath())
                    logger.info("Selected folder path: {}", selectedDir.getAbsolutePath())
                }
            } catch (Exception ex) {
                logger.error("Error selecting folder", ex)
            }
        }

        pane.add(folderLabel, 0, 0)
        pane.add(folderField, 1, 0)
        pane.add(folderButton, 2, 0)

        // Combo box for compression options
        Label compressionLabel = new Label("Compression type:")
        compressionBox.getItems().addAll("Lossy compression", "Lossless compression")
        compressionBox.setValue("Lossless compression")  // Set default value
        pane.add(compressionLabel, 0, 1)
        pane.add(compressionBox, 1, 1)

        // Numerical field for Pixel size
        Label pixelSizeLabel = new Label("Pixel size, microns:")
        pane.add(pixelSizeLabel, 0, 2)
        pane.add(pixelSizeField, 1, 2)

        // Numerical field for downsample
        Label downsampleLabel = new Label("Downsample:")
        pane.add(downsampleLabel, 0, 3)
        pane.add(downsampleField, 1, 3)

        // Text field for Matching string
        Label matchStringLabel = new Label("Matching string:")
        pane.add(matchStringLabel, 0, 4)
        pane.add(matchStringField, 1, 4)

        return pane
    }
    private static OMEPyramidWriter.CompressionType getCompressionType(String selectedOption) {
        switch (selectedOption) {
            case "Lossy compression":
                return OMEPyramidWriter.CompressionType.J2K_LOSSY
            case "Lossless compression":
                return OMEPyramidWriter.CompressionType.J2K
            default:
                return null // or a default value
        }
    }


    private static void stitchByFileName(String folderPath, String compressionType, double pixelSize, double downsample, String matchingString) {
        // Constructing the message to display
        String message = "Folder Path: " + folderPath + "\n" +
                "Compression Type: " + compressionType + "\n" +
                "Pixel Size: " + pixelSize + "\n" +
                "Downsample: " + downsample + "\n" +
                "Matching String: " + matchingString

        // Display the dialog with the constructed message
        Dialogs.showConfirmDialog("Values Passed", message)
        OMEPyramidWriter.CompressionType compresionType = getCompressionType(compressionType)
    }

}
