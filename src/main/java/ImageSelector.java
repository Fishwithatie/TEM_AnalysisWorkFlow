import ij.IJ;
import ij.ImagePlus;
import ij.gui.YesNoCancelDialog;
import ij.plugin.frame.RoiManager;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.poi.ss.usermodel.Row;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Administrateur on 2016-03-24.
 */
public class ImageSelector {

    FileChooser.ExtensionFilter imagefilter;
    // TODO: 2016-03-24 Add support for multiple varicosities on 1 image
    //Select images in a folder and return a list of selected file
    public java.util.List<File> setImages(Stage mainstage) {

        FileChooser imageselector = new FileChooser();
        imagefilter = new FileChooser.ExtensionFilter("Image file only (*.tif)", "*.tif");

        imageselector.getExtensionFilters().add(imagefilter);
        return imageselector.showOpenMultipleDialog(mainstage);
    }

    //Goes through image list in order to measure images one at a time
    public void openImages(ProjectFile projectfile, Stage mainstage) {

        ArrayList<String> filenamelist = new ArrayList<String>();

        Iterator<Row> rowit = projectfile.currentsheet.rowIterator();

        while(rowit.hasNext()){
            Row activerow = rowit.next();
            if (activerow.getCell(0).getStringCellValue() != null) {
                filenamelist.add(activerow.getCell(0).getStringCellValue());
            }
        }

        java.util.List<File> imagelist = setImages(mainstage);

        for (File imageit : imagelist) {

            //Create RoiManager
            RoiManager selection = new RoiManager();

            //Open image
            String imagepath = imageit.getAbsolutePath();
            ImagePlus currentimage = IJ.openImage(imagepath);
            currentimage.show();

            //ImageCloner is created to keep a copy of images with overlays
            ImageCloner imagecloner = new ImageCloner();

            //Ask if there is any labeling on the current image and proceed to measurements if true
            if (askLabeled() & checkDuplicata(filenamelist, imageit)) {

                //Measurements
                MeasurementWorkflow workflow = new MeasurementWorkflow();
                workflow.processImage(selection, currentimage, imageit, projectfile);

                //Close image and roi manager
                selection.runCommand("Save");
                selection.moveRoisToOverlay(currentimage);

                selection.close();
                imagecloner.saveImagecopy(currentimage, imageit, projectfile);
                currentimage.hide();
                currentimage.flush();
                System.out.println(imageit.getName() + " successfully measured!");
                filenamelist.add(imageit.getName());
                projectfile.saveFile(mainstage);
            } else {
                currentimage.hide();
                currentimage.flush();
                selection.close();
            }


        }
    }

    //Return false if current image is already in the filename list
    public boolean checkDuplicata(ArrayList<String> filenamelist, File imageit) {

        if (filenamelist.contains(imageit.getName())) {
            YesNoCancelDialog duplicatadiag = new YesNoCancelDialog(new Frame(), "Image already in measured", "are you sure you want to measure it again?");
            duplicatadiag.setAlwaysOnTop(true);
            if (duplicatadiag.yesPressed()) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }

    }

    //Ask if there is a labeled varicosity
    public boolean askLabeled() {
        YesNoCancelDialog labeleddiag = new YesNoCancelDialog(new Frame(), "Labeled?", "Is there any labeled varicosities on the image (If no image appear please click NO)");
        labeleddiag.setAlwaysOnTop(true);
        if (labeleddiag.yesPressed()) {
            return true;
        } else {
            return false;
        }
    }

}
