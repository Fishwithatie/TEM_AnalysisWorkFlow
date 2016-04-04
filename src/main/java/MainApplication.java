import ij.ImageJ;
import ij.gui.GenericDialog;
import ij.gui.YesNoCancelDialog;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;


public class MainApplication extends Application{


    public ProjectFile projectFile;
    static final Logger LOG = LoggerFactory.getLogger(MainApplication.class);


    public static void main (String[] args){
        MainApplication.launch(args);
    }

    @Override
    public void start(Stage mainStage){

        LOG.info("Starting application");

        projectFile = new ProjectFile();
        ImageSelector selectedImage = new ImageSelector();

        //Project creation
        projectFile.startProject(mainStage);

        //Image selection and measurements processing
        selectedImage.openImages(projectFile, mainStage);

        //Save file and end of analysis
        projectFile.saveFile(mainStage);
        endAnalysis(mainStage);

    }

    //End of analysis dialog and end of measurements
    private void endAnalysis(Stage mainStage) {
        //End dialog

        YesNoCancelDialog endDialog = new YesNoCancelDialog(new Frame(),"You have measured all selected images", "Do you want to continue with another folder?");
        endDialog.setAlwaysOnTop(true);
        if (endDialog.yesPressed()) {
            projectFile.imageJ.quit();
            start(mainStage);
            LOG.info("Starting a new measurement set");
        } else {
            LOG.info("Closing Application");
            System.exit(0);
        }

    }


}
