import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImageCloner {

    Logger LOG = LoggerFactory.getLogger(ImageCloner.class);

    // /Image with overlay output

    //Creates a folder to save edited copy of the analysed images
    private void createImageCopyFolder(File targetDirectory) {

        LOG.info("Creating Image copy folder of measured image at " + targetDirectory.getPath());

        boolean dirCreated = false;

        try {
            Files.createDirectory(targetDirectory.toPath());
            dirCreated = true;
        }
        catch (IOException ioe) {
            LOG.error("It seems that the plugin does not have permission to create folder");
            GenericDialog sediag = new GenericDialog("It seems that the plugin does not have permission to create folder");
            sediag.setAlwaysOnTop(true);
            sediag.showDialog();
            LOG.debug("Closing application");
            System.exit(0);
        }
        if (dirCreated) {
            LOG.info("Image copy folder created");
        }
    }

    //Get the filename of a File without its extension
    private String getNameonly(File fileToGetName) {
        return FilenameUtils.removeExtension(fileToGetName.getName());
    }

    /**
     * Saves a copy of the {@link File} with its overlay in a folder linked to where the {@link ProjectFile} is saved
     * @param currentImage ImagePlus file extracted from imageJ
     * @param currentImageFile File of the image to be saved
     * @param projectFile Current projectFile
     */
    public void saveImageCopy (ImagePlus currentImage, File currentImageFile, ProjectFile projectFile) {

        File copyFile = new File(projectFile.getOutputFile().getParentFile().getPath() + "\\MeasuredImageof_" + getNameonly(projectFile.getOutputFile()));

        if (!copyFile.exists()) {
            createImageCopyFolder(copyFile);
        }

        boolean fileSaved = false;
        Path pathToCloneImage = Paths.get(copyFile.getPath() + "\\" + getNameonly(currentImageFile) + "_Overlay");

        try {
            IJ.saveAs(currentImage, "tif", pathToCloneImage.toString());
            fileSaved = true;
        } catch (SecurityException se) {
            LOG.error("It seems that the plugin does not have permission to save image file");
            GenericDialog seDialog = new GenericDialog("It seems that the plugin does not have permission to save image file");
            seDialog.setAlwaysOnTop(true);
            seDialog.showDialog();
            LOG.debug("Closing application");
            System.exit(0);
        }
        if (fileSaved) {
            LOG.info("Image with overlay created at " + pathToCloneImage.toString());
        }
    }

}
