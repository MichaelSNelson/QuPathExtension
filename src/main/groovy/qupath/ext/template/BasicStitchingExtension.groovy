package qupath.ext.template

import javafx.scene.control.MenuItem
import qupath.lib.common.Version
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.gui.extensions.QuPathExtension



/**
 * This is a demo to provide a template for creating a new QuPath extension in Groovy.
 * <p>
 * <b>Important!</b> For your extension to work in QuPath, you need to make sure the name & package
 * of this class is consistent with the file
 * <pre>
 *     /resources/META-INF/services/qupath.lib.gui.extensions.BasicStitchingExtension
 * </pre>
 */
class BasicStitchingExtension implements QuPathExtension {

	// Setting the variables here is enough for them to be available in the extension
	String name = "Basic Stitching"
	String description = "Perform basic tile-based stitching without any fancy resolution of overlaps."
	Version QuPathVersion = Version.parse("v0.4.4")

	@Override
	void installExtension(QuPathGUI qupath) {
		addMenuItem(qupath)
	}

	private void addMenuItem(QuPathGUI qupath) {
		def menu = qupath.getMenu("Extensions>${name}", true)

		def stitchByText = new MenuItem("Stitch by text file")
		stitchByText.setOnAction(e -> {





		})
		menu.getItems() << stitchByText

		def stitchByFileName = new MenuItem("Stitch by file name")
		stitchByFileName.setOnAction(e -> {
			Dialogs.showMessageDialog(name,
					"The other script here I guess")
		})
		menu.getItems() << stitchByFileName
	}
	
}
