import ij.ImageJ;
import ij.gui.GenericDialog;
import ij.gui.YesNoCancelDialog;
import javafx.application.Application;
import javafx.stage.Stage;

import java.awt.*;

/**
 * Created by Administrateur on 2016-03-24.
 */
public class MainApplication extends Application{


    public ProjectFile projectfile;

    //Main method
    public static void main (String[] args){
        MainApplication.launch(args);
    }

    @Override
    public void start(Stage mainstage){

        projectfile = new ProjectFile();
        ImageSelector selectedimage = new ImageSelector();

        //Project creation
        projectfile.startProject(mainstage);

        //Image selection and measurements processing
        selectedimage.openImages(projectfile, mainstage);

        //Save file and end of analysis
        projectfile.saveFile(mainstage);
        endAnalysis(mainstage);

    }

    //End of analysis dialog and end of measurements
    public void endAnalysis(Stage mainstage) {
        //End dialog

        YesNoCancelDialog enddiag = new YesNoCancelDialog(new Frame(),"You have measured all selected images", "Do you want to continue with another folder?");
        enddiag.setAlwaysOnTop(true);
        if (enddiag.yesPressed()) {
            projectfile.imagej.quit();
            start(mainstage);
        } else {
            projectfile.imagej.quit();
            System.exit(0);
        }

    }


}
