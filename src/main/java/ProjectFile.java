import ij.ImageJ;
import ij.gui.GenericDialog;
import ij.gui.WaitForUserDialog;
import ij.gui.YesNoCancelDialog;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class ProjectFile  {

    final static Logger LOG = LoggerFactory.getLogger(ProjectFile.class);

    private XSSFWorkbook currentWorkbook;
    private XSSFSheet currentSheet;
    private FileChooser.ExtensionFilter excelFilter = new FileChooser.ExtensionFilter("Excel file only (*.xlsx )", "*.xlsx");
    private File outputFile;
    private XSSFRow currentRow;
    private FileOutputStream saveFile;
    public ImageJ imageJ = new ImageJ();



    //Create or load project

    /**
     * Create or load project
     * <p>
     *     Start a workflow to start or load a new project.
     * </p>
     * @param mainStage Stage coming from a JavaFX app
     */

    public void startProject(Stage mainStage) {

        if (askNewProject()) {
            selectSavepath(mainStage);
            createNewProject();
            saveFile(mainStage);


        } else {
            loadProject(mainStage);
            if (verifyProjectfile(currentWorkbook)) {
                GenericDialog invalidfilediag = new GenericDialog("Chosen file is invalid please select another excel file or create a new project");
                invalidfilediag.showDialog();
                imageJ.quit();
                MainApplication.launch();
            }

        }

    }

    //Ask if this is a new project
    private boolean askNewProject() {

        Frame superframe = new Frame();
        YesNoCancelDialog newProjectDialog = new YesNoCancelDialog(superframe, "New subject?", "New subject?");
        if (newProjectDialog.cancelPressed()) {
            System.exit(0);
        }

        return newProjectDialog.yesPressed();

    }

    //Create a new project including the excel workbook with header and saves it
    private void createNewProject(){

        java.util.List<String> headerList = Arrays.asList(
                "Image file name",
                "Type of measured",
                "Area",
                "Long axis",
                "Short axis",
                "Aspect ratio",
                "Diameter",
                "Mitochondria count",
                "Synapse length",
                "Asymmetric / Symmetric",
                "Target type",
                "Target Area",
                "Target Long axis",
                "Target Short axis",
                "Target Diameter"
                );

        currentWorkbook = new XSSFWorkbook();

        //Create header of excel file
        currentWorkbook.createSheet("Raw data");
        currentSheet = currentWorkbook.getSheetAt(0);
        XSSFRow activerow = currentSheet.createRow(0);

        for (int i=0; i < headerList.size(); i++) {
            Cell activeCell = activerow.createCell(i);
            activeCell.setCellType(Cell.CELL_TYPE_STRING);
            activeCell.setCellValue(headerList.get(i));
        }


        LOG.info("New project successfully created");
    }


    //Load an existing project
    private void loadProject(Stage mainStage) {

        try {
            FileChooser loader = new FileChooser();
            loader.getExtensionFilters().add(excelFilter);
            loader.setTitle("Please load existing project");
            outputFile = loader.showOpenDialog(mainStage);
            currentWorkbook = new XSSFWorkbook(outputFile);
            currentSheet = currentWorkbook.getSheetAt(0);
            currentRow = currentSheet.getRow(currentSheet.getLastRowNum());

        } catch (IOException e) {
            LOG.warn("File in use by another application please close and try again");
            WaitForUserDialog fileinusedia = new WaitForUserDialog("File in use", "File in use by another application please close and try again");
            fileinusedia.setAlwaysOnTop(true);
            fileinusedia.show();
            LOG.debug("Restarting application");
            MainApplication.launch();
        } catch (InvalidFormatException er) {
            LOG.warn("This file seems to be not valid or corrupt please select a XLSX or XLS file");
            WaitForUserDialog wrongfiletypediag = new WaitForUserDialog("Wrong or corrupt file", "This file seems to be not valid or corrupt please select a XLSX or XLS file");
            wrongfiletypediag.setAlwaysOnTop(true);
            wrongfiletypediag.show();
            LOG.debug("Restarting application");
            MainApplication.launch();
        }

        LOG.info(outputFile.getName() + " successfully loaded");
    }

    //Verify if the workbook is a valid project file, return true if it's valid
    private boolean verifyProjectfile(Workbook workbookToCheck) {

        return (workbookToCheck.getSheetAt(0).getRow(0) != null & workbookToCheck.getSheetAt(0).getRow(0).getCell(0).getStringCellValue() == "Image file name");

    }

    //Excel data output

    //Select an output path for the excel file, it edits outputfile which is also used in the saveImagecopy method
    private void selectSavepath(Stage mainStage) {

        FileChooser saver = new FileChooser();

        saver.getExtensionFilters().add(excelFilter);

        outputFile = saver.showSaveDialog(mainStage);
    }

    //Creates an outputstream to quickly call a save for the current excel workbook
    public void saveFile(Stage mainStage) {
        try {
            saveFile = new FileOutputStream(outputFile);
            currentWorkbook.write(saveFile);
        } catch (IOException e) {
            LOG.warn("It seems that this file is in use or the plugin does not have permission to edit this file, please choose another save path");
            WaitForUserDialog saveerrordiag = new WaitForUserDialog("It seems that this file is in use or the plugin does not have permission to edit this file, please choose another save path");
            saveerrordiag.setAlwaysOnTop(true);
            saveerrordiag.show();
            LOG.debug("Trying to select another file to edit");
            selectSavepath(mainStage);
        }

        LOG.info("Workbook " + outputFile.getName() + " saved");
    }

    //Getters for variables

    public XSSFWorkbook getCurrentWorkbook() {
        return currentWorkbook;
    }

    public XSSFSheet getCurrentSheet() {
        return currentSheet;
    }

    public XSSFRow getCurrentRow() {
        return currentRow;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public FileOutputStream getSaveFile() {
        return saveFile;
    }


    //Setters for variables

    public void setCurrentWorkbook(XSSFWorkbook workbook) {
        currentWorkbook = workbook;
    }

    public void setCurrentSheet(XSSFSheet sheet) {
        currentSheet = sheet;
    }

    public void setCurrentRow(XSSFRow row) {
        currentRow = row;
    }

    public void setOutputFile(File file) {
        outputFile = file;
    }

    public void setSaveFile(FileOutputStream saveStream) {
        saveFile = saveStream;
    }
}
