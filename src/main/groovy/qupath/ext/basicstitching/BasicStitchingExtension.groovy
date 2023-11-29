package qupath.ext.basicstitching;

import javafx.scene.control.MenuItem;
import qupath.ext.basicstitching.functions.BSTextFileStitching
import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.ActionTools;
import qupath.lib.gui.ActionTools.ActionDescription;
import qupath.lib.gui.ActionTools.ActionMenu;
import org.controlsfx.control.action.Action;

import qupath.lib.gui.extensions.QuPathExtension;


/**
 * This is a demo to provide a template for creating a new QuPath extension in Groovy.
 * <p>
 * <b>Important!</b> For your extension to work in QuPath, you need to make sure the name & package
 * of this class is consistent with the file
 * <pre>
 *     /resources/META-INF/services/qupath.lib.gui.extensions.QuPathExtension
 * </pre>
 */
class BasicStitchingExtension implements QuPathExtension {

	// Setting the variables here is enough for them to be available in the extension
	String name = "Basic stitching"
	String description = "Basic stitching extension that puts tiles together into pyramidal ome.tif files, no overlap resolution or flat field correction."
	Version QuPathVersion = Version.parse("v0.4.0")

//	@Override
//	void installExtension(QuPathGUI qupath) {
//		qupath.installActions(ActionTools.getAnnotatedActions(new BSCommands(qupath)))
//		addMenuItem(qupath)
//	}
	@Override
	void installExtension(QuPathGUI qupath) {
		addMenuItem(qupath)
	}
	/**
	 * Get the description of the extension.
	 *
	 * @return The description of the extension.
	 */
	@Override
	public String getDescription() {
		return "Stitch tiles into a pyramidal ome.tif";
	}

	/**
	 * Get the name of the extension.
	 *
	 * @return The name of the extension.
	 */
	@Override
	public String getName() {
		return "BasicStitching";
	}

	private void addMenuItem(QuPathGUI qupath) {
		def menu = qupath.getMenu("Extensions>${name}", true)
		def fileNameStitching = new MenuItem("Stitching using coordinates in file names")
		// TODO tooltip "Coordinates are expected to be in the format ImageName[xCoordinateInMicrons, yCoordinateInMicrons].tif"
		fileNameStitching.setOnAction(e -> {
			BSTextFileStitching.createGUI()

		})
		menu.getItems() << fileNameStitching
	}
	
}
//@ActionMenu("Extensions")
//public class BSCommands {
//
//	@ActionMenu("BasicStitching")
//	@ActionDescription("Launch BasicStitching dialog.")
//	public final Action BSCommand;
//
//	/**
//	 * Constructor.
//	 *
//	 * @param qupath
//	 *            The QuPath GUI.
//	 */
//	private BSCommands(QuPathGUI qupath) {
//		BSMainCommand bsCommand = new BSMainCommand(qupath);
//		actionBSCommand = new Action(event -> bsCommand.run());
//	}
//
//}