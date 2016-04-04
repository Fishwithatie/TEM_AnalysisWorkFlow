import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.gui.YesNoCancelDialog;
import ij.measure.Measurements;
import ij.plugin.frame.RoiManager;
import ij.process.EllipseFitter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.util.Random;

public class MeasurementWorkflow {

    Logger LOG = LoggerFactory.getLogger(MeasurementWorkflow.class);

    private String labeledType = "labeled";
    private String unlabeledType = "unlabeled";
    private String labeledColor = "Yellow";
    private String unlabeledColor = "Green";
    private String synapseColor = "Red";

    // Process all images and go through the workflow to measure images

    /**
     * Process an image and go through the workflow to measure it
     * @param currentImage ImagePlus file extrated from ImageJ
     * @param imageFile
     * @param projectFile
     */
    public void processImage(ImagePlus currentImage, File imageFile, ProjectFile projectFile) {

        //Create RoiManager
        RoiManager selection = new RoiManager();

        //Measure labeled varicosity
        setScale(selection, currentImage);
        measureVaricosity(selection, currentImage, labeledType, imageFile, labeledColor, projectFile);
        measureSynapse(askSynapse(), selection, currentImage, labeledColor, projectFile);

        placeStereologycross(currentImage);

        //Measure unlabeled varicosity
        measureVaricosity(selection, currentImage, unlabeledType, imageFile, unlabeledColor, projectFile);
        measureSynapse(askSynapse(), selection, currentImage, unlabeledColor, projectFile);

       //Close ROI manager and put Rois in overlay for later visualisation
        selection.runCommand("Save");
        selection.moveRoisToOverlay(currentImage);
        selection.close();

    }

    //Set scale bar in order to determine pixel value in length
    private void setScale(RoiManager selection, ImagePlus currentImage) {

        Frame superFrame = new Frame();
        YesNoCancelDialog scaleDialog = new YesNoCancelDialog(superFrame, "Scale bar value", "Do you need to reset the scale bar value?");

        if (scaleDialog.yesPressed()) {
            IJ.setTool("line");
            WaitForUserDialog scaleWaitDialog = new WaitForUserDialog("Scale bar", "Please draw a line over the scalebar");
            scaleWaitDialog.show();

            selection.runCommand(currentImage, "add");
            selection.select(selection.getCount()- 1);
            selection.runCommand("Rename", "Scale bar");

            GenericDialog scalebarLength = new GenericDialog("What is the value of the scale bar?", superFrame);
            scalebarLength.addNumericField("Value of the scale bar(in micron)", 1, 2);
            scalebarLength.setAlwaysOnTop(true);
            scalebarLength.showDialog();
            IJ.run(currentImage, "Set Scale...", "known=" + scalebarLength.getNextNumber() + " unit=micron global");
            LOG.info("Scalebar successfully set");
        }
    }

    //Measure of varicosities

    private void measureVaricosity(RoiManager selection, ImagePlus currentImage, String type, File image, String overlayColor, ProjectFile projectFile)
    {

        ImageProcessor ip = currentImage.getProcessor();

        //ask for measurements of varicosities
        IJ.setTool("freehand");
        WaitForUserDialog waitDialogCountour = new WaitForUserDialog("Action required", "Plz draw contour of " + type + " object");
        waitDialogCountour.show();
        selection.runCommand(currentImage, "Add");
        selection.select(selection.getCount() - 1);
        selection.runCommand("Rename", type);
        Roi varicosity = selection.getRoi(selection.getSelectedIndex());
        selection.moveRoisToOverlay(currentImage);
        selection.runCommand("Measure");
        selection.runCommand("Set Color", overlayColor);
        selection.runCommand("Save");
        ip.setRoi(varicosity);

        //Takes input and extract measurements, long axis and short axis are calculated wih a ellipse fitter

        projectFile.setCurrentRow(projectFile.getCurrentSheet().createRow(projectFile.getCurrentSheet().getLastRowNum() + 1));

        XSSFRow workingRow = projectFile.getCurrentRow();

        //Create and fill cell with filename
        createStringCell(workingRow, 0, image.getName());

        //Set type
        createStringCell(workingRow, 1, type);

        //Area
        ImageStatistics imageStats = ImageStatistics.getStatistics(ip, Measurements.MEAN, currentImage.getCalibration());
        double labeledArea = imageStats.area;
        createNumericCell(workingRow, 2, labeledArea);

        //Ellipse fitter
        EllipseFitter labeledEllipse = new EllipseFitter();
        labeledEllipse.fit(ip, imageStats);
        labeledEllipse.drawEllipse(ip);
        ip.createImage();

        //Long axis
        double labeledEllipseMajor = labeledEllipse.major * currentImage.getCalibration().pixelWidth;
        createNumericCell(workingRow, 3, labeledEllipseMajor);

        //Short axis
        double labeledEllipseMinor = labeledEllipse.minor * currentImage.getCalibration().pixelWidth;
        createNumericCell(workingRow, 4, labeledEllipseMinor);

        //Aspect Ratio
        createNumericCell(workingRow, 5, labeledEllipseMinor / labeledEllipseMajor);

        //Diameter
        createNumericCell(workingRow, 6, (labeledEllipseMajor + labeledEllipseMinor) / 2);

        //Mitochondria
        createNumericCell(workingRow, 7, askMitochondria());

    }

    //Ask number of mitochondria present on the current measured varicosity
    private double askMitochondria() {

        GenericDialog mitochondriaDialog = new GenericDialog("Please enter the number of mitochondria");
        mitochondriaDialog.addNumericField("Please enter the number of mitochondria", 0, 2);
        mitochondriaDialog.showDialog();

        return mitochondriaDialog.getNextNumber();
    }

    //Ask if there is any synapse and how many synapse on the image
    private double askSynapse() {

        Frame superFrame = new Frame();
        YesNoCancelDialog synapseDialog = new YesNoCancelDialog(superFrame, "Any synapse?", "Any synapse?");
        synapseDialog.setAlwaysOnTop(true);
        double synapseNumber;

        if (synapseDialog.yesPressed()) {
            GenericDialog gd1 = new GenericDialog("How many?");
            gd1.addNumericField("Number of synapse...", 1, 0);
            gd1.showDialog();
            synapseNumber = gd1.getNextNumber();

        } else {
            synapseNumber = 0;
        }

        return synapseNumber;
    }

    //Measure synapse and its target on the image
    private void measureSynapse(double synapseNumber, RoiManager selection, ImagePlus currentImage, String overlayColor, ProjectFile projectFile) {

        //Loop for number of synapse = number of measured synapse

        ImageProcessor ip = currentImage.getProcessor();

        for (int i = (int) synapseNumber; i > 0; i--) {
            IJ.setTool("line");
            WaitForUserDialog waitDialogSynapse = new WaitForUserDialog("Please measure synapse number " + i + " with line tool");
            waitDialogSynapse.show();
            selection.runCommand(currentImage, "add");
            selection.select(selection.getCount() - 1);
            selection.runCommand("Rename", "Synapse " + i);

            //Get length value of synapse

            if (projectFile.getCurrentRow().getCell(8) != null) {

                projectFile.setCurrentRow(projectFile.getCurrentSheet().createRow(projectFile.getCurrentSheet().getLastRowNum() + 1));

                Cell previousFilenameCell = projectFile.getCurrentSheet().getRow(projectFile.getCurrentSheet().getLastRowNum() - 1).getCell(0);
                createStringCell(projectFile.getCurrentRow(), 0, previousFilenameCell.getStringCellValue());

                Cell previousVaricosityTypeCell = projectFile.getCurrentSheet().getRow(projectFile.getCurrentSheet().getLastRowNum() - 1).getCell(1);
                createStringCell(projectFile.getCurrentRow(), 1, previousVaricosityTypeCell.getStringCellValue());
            }

            Roi synapse = selection.getRoi(selection.getCount() - 1);
            double synapseLength = synapse.getLength() * currentImage.getCalibration().pixelWidth;

            selection.runCommand("Measure");
            selection.runCommand("Set Color", synapseColor);
            selection.runCommand("Save");
            selection.moveRoisToOverlay(currentImage);

            createNumericCell(projectFile.getCurrentRow(), 8, synapseLength);

            //Ask synapse type

            String[] synapseTypeList = new String[3];
            synapseTypeList[0] = "Asymmetric";
            synapseTypeList[1] = "Symmetric";
            synapseTypeList[2] = "Unknown";

            GenericDialog synapseTypeDialog = new GenericDialog("Synapse type?");
            synapseTypeDialog.addChoice("Synapse type?", synapseTypeList, "Asymmetric");
            synapseTypeDialog.showDialog();
            String synapseType = synapseTypeDialog.getNextChoice();

            createStringCell(projectFile.getCurrentRow(), 9, synapseType);

            //Ask target type

            GenericDialog synapseTargetType = new GenericDialog("Type of target?");

            String[] targetTypeList = new String[5];
            targetTypeList[0] = "Spine";
            targetTypeList[1] = "Dendrite";
            targetTypeList[2] = "Cell body";
            targetTypeList[3] = "Axon";
            targetTypeList[4] = "Unknown";


            synapseTargetType.addChoice("Type of target?", targetTypeList, "Spine");
            synapseTargetType.showDialog();
            String targetype = synapseTargetType.getNextChoice();

            createStringCell(projectFile.getCurrentRow(), 10, targetype);

            //Get target measures

            IJ.setTool("freehand");
            WaitForUserDialog waittargetmeasure = new WaitForUserDialog("Please measure target of synapse " + i + " with freehand tool");
            waittargetmeasure.show();
            selection.runCommand(currentImage, "add");
            selection.select(selection.getCount() - 1);
            selection.runCommand("Set Color", overlayColor);
            selection.runCommand("Save");

            ImageStatistics targetstats = ImageStatistics.getStatistics(ip, Measurements.MEAN, currentImage.getCalibration());

            double targetArea = targetstats.area;
            createNumericCell(projectFile.getCurrentRow(), 11, targetArea);

            EllipseFitter targetEllipse = new EllipseFitter();
            targetEllipse.fit(ip, targetstats);
            targetEllipse.drawEllipse(ip);

            //Long axis
            double targetEllipseMajor = targetEllipse.major * currentImage.getCalibration().pixelWidth;
            createNumericCell(projectFile.getCurrentRow(), 12, targetEllipseMajor);

            //Short axis
            double targetEllipseMinor = targetEllipse.minor * currentImage.getCalibration().pixelWidth;
            createNumericCell(projectFile.getCurrentRow(), 13, targetEllipseMinor);

            //Diameter
            Cell diameterTargetCell = projectFile.getCurrentRow().createCell(14);
            createNumericCell(projectFile.getCurrentRow(), 14, (targetEllipseMajor + targetEllipseMinor) / 2);

            ip.createImage();
            LOG.info("Traced synapse and target successfully measured");
        }



    }

    //Places a random marker on the image to identify unlabeled varicosity
    private void placeStereologycross (ImagePlus currentImage) {

        int imageWidth = currentImage.getWidth();
        int imageHeigth = currentImage.getHeight();

        Random coordinateGenerator = new Random();

        Roi stereoRectangle = new Roi(coordinateGenerator.nextInt(imageWidth), coordinateGenerator.nextInt(imageHeigth), 50, 50);
        stereoRectangle.setFillColor(Color.RED);
        stereoRectangle.setStrokeColor(Color.RED);

        currentImage.setRoi(stereoRectangle);



    }

    /**
     * Create a numeric cell at a specified row and column index with a determined value
     * @param workingRow Current row where data need to be entered
     * @param columnIndex Current column where cell need to be created
     * @param value User-determined value for the cell
     */
    private void createNumericCell(XSSFRow workingRow, int columnIndex, double value) {

        Cell currentCell = workingRow.createCell(columnIndex);
        currentCell.setCellType(Cell.CELL_TYPE_NUMERIC);
        currentCell.setCellValue(value);
    }

    /**
     * Create a string cell at a specified row and column index with a determined value
     * @param workingRow Current row where data need to be entered
     * @param columnIndex Current column where cell need to be created
     * @param value User-determined value for the cell
     */
    private void createStringCell(XSSFRow workingRow, int columnIndex, String value) {

        Cell currentCell = workingRow.createCell(columnIndex);
        currentCell.setCellType(Cell.CELL_TYPE_STRING);
        currentCell.setCellValue(value);

    }

}
