package qupath.ext.basicstitching.functions

import javafx.scene.Node
import qupath.ext.basicstitching.stitching.stitchingImplementations

import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.stage.Modality
import javafx.stage.DirectoryChooser
import qupath.lib.gui.scripting.QPEx

//import java.nio.file.Paths

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class stitchingGUI {

    private static final Logger logger = LoggerFactory.getLogger(stitchingGUI.class);
    static TextField folderField = new TextField();
    static ComboBox<String> compressionBox = new ComboBox<>();
    static TextField pixelSizeField = new TextField("0.4988466");
    static TextField downsampleField = new TextField("1");
    static TextField matchStringField = new TextField("20x");
    static ComboBox<String> stitchingGridBox = new ComboBox<>(); // New combo box for stitching grid options
    static Button folderButton = new Button("Select Folder");
    // Declare labels as static fields
    static Label stitchingGridLabel = new Label("Stitching Method:");
    static Label folderLabel = new Label("Folder location:");
    static Label compressionLabel = new Label("Compression type:");
    static Label pixelSizeLabel = new Label("Pixel size, microns:");
    static Label downsampleLabel = new Label("Downsample:");
    static Label matchStringLabel = new Label("Matching string:");

    // Map to hold the positions of each GUI element
    private static Map<Node, Integer> guiElementPositions = new HashMap<>();


    static void createGUI() {
        // Create the dialog
        def dlg = new Dialog<ButtonType>()
        dlg.initModality(Modality.APPLICATION_MODAL)
        dlg.setTitle("Input Stitching Method and Options")
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
            String stitchingType = stitchingGridBox.getValue()
            // Call the function with collected data
            stitchingImplementations.stitchCore(stitchingType, folderPath, compressionType, pixelSize, downsample, matchingString)
            //stitchByFileName(folderPath, compressionType, pixelSize, downsample, matchingString)
        }

    }

    private static GridPane createContent() {
        GridPane pane = new GridPane();
        pane.setHgap(10);
        pane.setVgap(10);

        // Add different components to the grid pane
        initializePositions();

        // Add components to the grid pane
        addStitchingGridComponents(pane);
        addFolderSelectionComponents(pane);
        addCompressionComponents(pane);
        addPixelSizeComponents(pane);
        addDownsampleComponents(pane);
        addMatchStringComponents(pane);

        // Call once to set initial visibility of components
        updateComponentsBasedOnSelection(pane);

        return pane;
    }

    // Helper method to add label and control to the grid
    private static void addToGrid(GridPane pane, Node label, Node control) {
        Integer rowIndex = guiElementPositions.get(label);
        if (rowIndex != null) {
            pane.add(label, 0, rowIndex);
            pane.add(control, 1, rowIndex);
        } else {
            logger.error("Row index not found for component: " + label);
        }
    }

    private static void initializePositions() {
        // Reset position counter
        def currentPosition = 0;

        // Dynamically assign positions
        guiElementPositions.put(stitchingGridLabel, currentPosition++);
        guiElementPositions.put(folderLabel, currentPosition++);
        guiElementPositions.put(compressionLabel, currentPosition++);
        guiElementPositions.put(pixelSizeLabel, currentPosition++);
        guiElementPositions.put(downsampleLabel, currentPosition++);
        guiElementPositions.put(matchStringLabel, currentPosition++);
        // Add more components as needed
    }
    // Method to add stitching grid components
    private static void addStitchingGridComponents(GridPane pane) {
        stitchingGridBox.getItems().clear() // Clear existing items before adding new ones
        stitchingGridBox.getItems().addAll(
                "Vectra tiles with metadata",
                "Filename[x,y] with coordinates in microns",
                "Coordinates in TileCoordinates.txt file"
        );
        stitchingGridBox.setValue("Coordinates in TileCoordinates.txt file"); // Set default value
        stitchingGridBox.setOnAction(e -> updateComponentsBasedOnSelection(pane));
        addToGrid(pane, stitchingGridLabel as Node, stitchingGridBox as Node);
    }

    // Method to add folder selection components
    private static void addFolderSelectionComponents(GridPane pane) {

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
        addToGrid(pane, folderLabel as Node, folderField as Node);
        Integer rowIndex = guiElementPositions.get(folderLabel); // Get the row index for the folder label (same as button)
        if (rowIndex != null) {
            pane.add(folderButton, 2, rowIndex); // Place the button at the correct row
        } else {
            logger.error("Row index not found for folderButton");
        }
    }

    // Method to add compression components
    private static void addCompressionComponents(GridPane pane) {
        compressionBox.getItems().clear()
        compressionBox.getItems().addAll("Lossy compression", "Lossless compression");
        compressionBox.setValue("Lossless compression"); // Set default value
        // Create a Tooltip
        Tooltip compressionTooltip = new Tooltip("Select the type of image compression.");

        // Set the Tooltip for both the label and the control
        compressionLabel.setTooltip(compressionTooltip);
        compressionBox.setTooltip(compressionTooltip);

        addToGrid(pane, compressionLabel as Node, compressionBox as Node);
    }

    // Method to add pixel size components
    private static void addPixelSizeComponents(GridPane pane) {
        addToGrid(pane, pixelSizeLabel as Node, pixelSizeField as Node);
    }

    // Method to add downsample components
    private static void addDownsampleComponents(GridPane pane) {
        addToGrid(pane, downsampleLabel as Node, downsampleField as Node);
    }

    // Method to add matching string components
    private static void addMatchStringComponents(GridPane pane) {
        addToGrid(pane, matchStringLabel as Node, matchStringField as Node);
    }
    private static void updateComponentsBasedOnSelection(GridPane pane) {
        // Hide pixel size field and label for specific selections
        boolean hidePixelSize = stitchingGridBox.getValue().equals("Vectra multiplex tif") ||
                stitchingGridBox.getValue().equals("Coordinates in TileCoordinates.txt file");
        pixelSizeLabel.setVisible(!hidePixelSize);
        pixelSizeField.setVisible(!hidePixelSize);

        adjustLayout(pane);
    }
    private static void adjustLayout(GridPane pane) {
        // Update the GridPane layout based on positions from guiElementPositions
        for (Map.Entry<Node, Integer> entry : guiElementPositions.entrySet()) {
            Node node = entry.getKey();
            Integer newRow = entry.getValue();

            // Update the node's position only if it is part of the GridPane
            if (pane.getChildren().contains(node)) {
                GridPane.setRowIndex(node, newRow);
            }
        }
    }

}
