import ij.ImageJ;
import ij.gui.GenericDialog;
import ij.gui.WaitForUserDialog;
import ij.gui.YesNoCancelDialog;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Administrateur on 2016-03-24.
 */
public class ProjectFile  {

    public XSSFWorkbook currentworkbook;
    public XSSFSheet currentsheet;
    public FileChooser.ExtensionFilter excelfilter = new FileChooser.ExtensionFilter("Excel file only (*.xlsx )", "*.xlsx");
    public File outputfile;
    public XSSFRow currentrow;
    public FileOutputStream savefile;
    public ImageJ imagej = new ImageJ();

    //Create or load project

    public void startProject(Stage mainstage) {

        if (askNewProject()) {
            selectSavepath(mainstage);
            createNewProject();
            saveFile(mainstage);

        } else {
            loadProject(mainstage);
            if (verifyProjectfile(currentworkbook)) {
                GenericDialog invalidfilediag = new GenericDialog("Chosen file is invalid please select another excel file or create a new project");
                invalidfilediag.showDialog();
                imagej.quit();
                MainApplication.launch();
            }

        }

    }

    //Ask if this is a new project
    public boolean askNewProject() {

        Frame superframe = new Frame();
        YesNoCancelDialog newdiag = new YesNoCancelDialog(superframe, "New subject?", "New subject?");
        if (newdiag.cancelPressed()) {
            System.exit(0);
        }

        return newdiag.yesPressed();

    }

    //Create a new project including the excel workbook with header and saves it
    public void createNewProject(){

        currentworkbook = new XSSFWorkbook();

        //Create header of excel file
        currentworkbook.createSheet("Raw data");
        currentsheet = currentworkbook.getSheetAt(0);
        XSSFRow activerow = currentsheet.createRow(0);

        Cell activecell = activerow.createCell(0);
        activecell.setCellType(Cell.CELL_TYPE_STRING);
        activecell.setCellValue("Image file name");

        activecell = activerow.createCell(1);
        activecell.setCellType(Cell.CELL_TYPE_STRING);
        activecell.setCellValue("Type of measured");

        activecell = activerow.createCell(2);
        activecell.setCellType(Cell.CELL_TYPE_STRING);
        activecell.setCellValue("Area");

        activecell = activerow.createCell(3);
        activecell.setCellType(Cell.CELL_TYPE_STRING);
        activecell.setCellValue("Long axis");

        activecell = activerow.createCell(4);
        activecell.setCellType(Cell.CELL_TYPE_STRING);
        activecell.setCellValue("Short axis");

        activecell = activerow.createCell(5);
        activecell.setCellType(Cell.CELL_TYPE_STRING);
        activecell.setCellValue("Aspect ratio");

        activecell = activerow.createCell(6);
        activecell.setCellType((Cell.CELL_TYPE_STRING));
        activecell.setCellValue("Diameter");

        activecell = activerow.createCell(7);
        activecell.setCellType(Cell.CELL_TYPE_STRING);
        activecell.setCellValue("Mitochondria count");

        activecell = activerow.createCell(8);
        activecell.setCellType(Cell.CELL_TYPE_STRING);
        activecell.setCellValue("Synapse length");

        activecell = activerow.createCell(9);
        activecell.setCellType(Cell.CELL_TYPE_STRING);
        activecell.setCellValue("Asymmetric / Symmetric");

        activecell = activerow.createCell(10);
        activecell.setCellType((Cell.CELL_TYPE_STRING));
        activecell.setCellValue("Target type");

        activecell = activerow.createCell(11);
        activecell.setCellType(Cell.CELL_TYPE_STRING);
        activecell.setCellValue("Target Area");

        activecell = activerow.createCell(12);
        activecell.setCellType(Cell.CELL_TYPE_STRING);
        activecell.setCellValue("Target Long axis");

        activecell = activerow.createCell(13);
        activecell.setCellType(Cell.CELL_TYPE_STRING);
        activecell.setCellValue("Target Short axis");

        activecell = activerow.createCell(14);
        activecell.setCellType(Cell.CELL_TYPE_STRING);
        activecell.setCellValue("Target Diameter");

        System.out.println("New project successfully created");
    }

    //Load an existing project
    public void loadProject(Stage mainstage) {

        try {
            FileChooser loader = new FileChooser();
            loader.getExtensionFilters().add(excelfilter);
            loader.setTitle("Please load existing project");
            outputfile = loader.showOpenDialog(mainstage);
            currentworkbook = new XSSFWorkbook(outputfile);
            currentsheet = currentworkbook.getSheetAt(0);
            currentrow = currentsheet.getRow(currentsheet.getLastRowNum());

        } catch (IOException e) {
            WaitForUserDialog fileinusedia = new WaitForUserDialog("File in use", "File in use by another application please close and try again");
            fileinusedia.setAlwaysOnTop(true);
            fileinusedia.show();
            MainApplication.launch();
        } catch (InvalidFormatException er) {
            WaitForUserDialog wrongfiletypediag = new WaitForUserDialog("Wrong or corrupt file", "This file seems to be not valid or corrupt please select a XLSX or XLS file");
            wrongfiletypediag.setAlwaysOnTop(true);
            wrongfiletypediag.show();
            MainApplication.launch();
        }

        System.out.println(outputfile.getName() + " successfully loaded");
    }

    //Verify if the workbook is a valid project file, return true if it's valid
    public boolean verifyProjectfile(Workbook workbooktocheck) {

        return (workbooktocheck.getSheetAt(0).getRow(0) != null & workbooktocheck.getSheetAt(0).getRow(0).getCell(0).getStringCellValue() == "Image file name");

    }

    //Excel data output

    //Select an output path for the excel file, it edits outputfile which is also used in the saveImagecopy method
    public void selectSavepath(Stage mainstage) {

        FileChooser saver = new FileChooser();

        saver.getExtensionFilters().add(excelfilter);

        outputfile = saver.showSaveDialog(mainstage);
    }

    //Creates an outputstream to quickly call a save for the current excel workbook
    public void saveFile(Stage mainstage) {
        try {
            savefile = new FileOutputStream(outputfile);
            currentworkbook.write(savefile);
        } catch (IOException e) {
            WaitForUserDialog saveerrordiag = new WaitForUserDialog("It seems that this file is in use or the plugin does not have permission to edit this file, please choose another save path");
            saveerrordiag.setAlwaysOnTop(true);
            saveerrordiag.show();
            selectSavepath(mainstage);
        }

        System.out.println("Workbook " + outputfile.getName() + " saved");
    }

}
