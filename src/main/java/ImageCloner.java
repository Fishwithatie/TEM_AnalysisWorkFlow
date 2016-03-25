import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Created by Administrateur on 2016-03-24.
 */
public class ImageCloner {

    // /Image with overlay output

    //Creates a folder to save edited copy of the analysed images
    public void createImagecopyfolder(File targetdirectory) {

        System.out.println("Creating Image copy folder of measured image at " + targetdirectory.getPath());

        boolean dircreated = false;

        try {
            Files.createDirectory(targetdirectory.toPath());
            dircreated = true;
        }
        catch (IOException ioe) {
            GenericDialog sediag = new GenericDialog("It seems that the plugin does not have permission to create folder");
            sediag.setAlwaysOnTop(true);
            sediag.showDialog();
        }
        if (dircreated) {
            System.out.println("Image copy folder created");
        }
    }

    //Get the filename of a File without its extension
    public String getNameonly(File filetogetname) {
        String name = filetogetname.getName();
        int pos = name.lastIndexOf(".");
        if (pos > 0) {
            name = name.substring(0, pos);
        }
        return name;
    }

    //Saves a copy of the image with its overlay
    public void saveImagecopy (ImagePlus currentimage, File currentimagefile, ProjectFile projectfile) {


        File copyfile = new File(projectfile.outputfile.getParentFile().getPath() + "\\MeasuredImageof_" + getNameonly(projectfile.outputfile));

        if (!copyfile.exists()) {
            createImagecopyfolder(copyfile);
        }

        boolean filesaved = false;
        String savepathcopyimage = copyfile.getPath() + "\\" + getNameonly(currentimagefile) + "_Overlay";

        try {
            IJ.saveAs(currentimage, "tif", savepathcopyimage);
            filesaved = true;
        } catch (SecurityException SE) {
            GenericDialog sediag = new GenericDialog("It seems that the plugin does not have permission to save image file");
            sediag.setAlwaysOnTop(true);
            sediag.showDialog();
        }
        if (filesaved) {
            System.out.println("Image with overlay created at " + savepathcopyimage);
        }
    }

}
