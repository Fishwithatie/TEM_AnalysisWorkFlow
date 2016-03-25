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

import java.awt.*;
import java.io.File;
import java.util.Random;

/**
 * Created by Administrateur on 2016-03-24.
 */
public class MeasurementWorkflow {

    public String labeledtype = "labeled";
    public String unlabeledtype = "unlabeled";
    public String labeledcolor = "Yellow";
    public String unlabeledcolor = "Green";
    public String synapsecolor = "Red";

    // Process all images and go through the workflow to measure images

    public void processImage(RoiManager selection, ImagePlus currentimage, File imageit, ProjectFile projectfile) {

        //Measure labeled varicosity
        setScale(selection, currentimage);
        measureVaricosity(selection, currentimage, labeledtype, imageit, labeledcolor, projectfile);
        measureSynapse(askSynapse(), selection, currentimage, labeledcolor, projectfile);

        placeStereologycross(currentimage);

        //Measure unlabeled varicosity
        measureVaricosity(selection, currentimage, unlabeledtype, imageit, unlabeledcolor, projectfile);
        measureSynapse(askSynapse(), selection, currentimage, unlabeledcolor, projectfile);

    }

    //Set scale bar in order to determine pixel value in length
    public void setScale(RoiManager selection, ImagePlus currentimage) {

        Frame superframe = new Frame();
        YesNoCancelDialog scalediag = new YesNoCancelDialog(superframe, "Scale bar value", "Do you need to reset the scale bar value?");

        if (scalediag.yesPressed()) {
            IJ.setTool("line");
            WaitForUserDialog scalewaitdiag = new WaitForUserDialog("Scale bar", "Please draw a line over the scalebar");
            scalewaitdiag.show();

            selection.runCommand(currentimage, "add");
            selection.select(selection.getCount()- 1);
            selection.runCommand("Rename", "Scale bar");

            GenericDialog scalebarlength = new GenericDialog("What is the value of the scale bar?", superframe);
            scalebarlength.addNumericField("Value of the scale bar(in micron)", 1, 2);
            scalebarlength.setAlwaysOnTop(true);
            scalebarlength.showDialog();
            IJ.run(currentimage, "Set Scale...", "known=" + scalebarlength.getNextNumber() + " unit=micron global");
            System.out.println("Scalebar successfully set");
        }
    }

    //Measure of varicosities

    public void measureVaricosity(RoiManager selection, ImagePlus currentimage, String type, File image, String overlaycolor, ProjectFile projectfile)
    {

        ImageProcessor ip = currentimage.getProcessor();

        //ask for measurements of varicosities
        IJ.setTool("freehand");
        WaitForUserDialog waitdiag1 = new WaitForUserDialog("Action required", "Plz draw contour of " + type + " object");
        waitdiag1.show();
        selection.runCommand(currentimage, "Add");
        selection.select(selection.getCount() - 1);
        selection.runCommand("Rename", type);
        Roi varicosity = selection.getRoi(selection.getSelectedIndex());
        selection.moveRoisToOverlay(currentimage);
        selection.runCommand("Measure");
        selection.runCommand("Set Color", overlaycolor);
        selection.runCommand("Save");
        ip.setRoi(varicosity);

        //Takes input and extract measurements, long axis and short axis are calculated wih a ellipse fitter

        projectfile.currentrow = projectfile.currentsheet.createRow(projectfile.currentsheet.getLastRowNum() + 1);

        //Create and fill cell with filename and add it to the filename list
        Cell currentcell = projectfile.currentrow.createCell(0);
        currentcell.setCellType(Cell.CELL_TYPE_STRING);
        currentcell.setCellValue(image.getName());

        //Set type
        Cell typecell = projectfile.currentrow.createCell(1);
        typecell.setCellType(Cell.CELL_TYPE_STRING);
        typecell.setCellValue(type);

        //Area
        ImageStatistics imagestats = ImageStatistics.getStatistics(ip, Measurements.MEAN, currentimage.getCalibration());
        double labeledarea = imagestats.area;
        Cell areacell = projectfile.currentrow.createCell(2);
        areacell.setCellType(Cell.CELL_TYPE_NUMERIC);
        areacell.setCellValue(labeledarea);

        //Ellipse fitter
        EllipseFitter labeledellipse = new EllipseFitter();
        labeledellipse.fit(ip, imagestats);
        labeledellipse.drawEllipse(ip);
        ip.createImage();

        //Long axis
        double labeledellipsemajor = labeledellipse.major * currentimage.getCalibration().pixelWidth;
        Cell majorcell = projectfile.currentrow.createCell(3);
        majorcell.setCellType(Cell.CELL_TYPE_NUMERIC);
        majorcell.setCellValue(labeledellipsemajor);

        //Short axis
        double labeledellipseminor = labeledellipse.minor * currentimage.getCalibration().pixelWidth;
        Cell minorcell = projectfile.currentrow.createCell(4);
        minorcell.setCellType(Cell.CELL_TYPE_NUMERIC);
        minorcell.setCellValue(labeledellipseminor);

        //Aspect Ratio
        Cell aspectratiocell = projectfile.currentrow.createCell(5);
        aspectratiocell.setCellType(Cell.CELL_TYPE_NUMERIC);
        aspectratiocell.setCellValue(labeledellipseminor / labeledellipsemajor);

        //Diameter
        Cell diametercell = projectfile.currentrow.createCell(6);
        diametercell.setCellType(Cell.CELL_TYPE_NUMERIC);
        diametercell.setCellValue((labeledellipsemajor + labeledellipseminor) / 2 );

        //Mitochondria
        Cell mitocell = projectfile.currentrow.createCell(7);
        mitocell.setCellType(Cell.CELL_TYPE_NUMERIC);
        mitocell.setCellValue(askMitochondria());

    }

    //Ask number of mitochondria present on the current measured varicosity
    public double askMitochondria() {

        GenericDialog mitodiag = new GenericDialog("Please enter the number of mitochondria");
        mitodiag.addNumericField("Please enter the number of mitochondria", 0, 2);
        mitodiag.showDialog();

        return mitodiag.getNextNumber();
    }

    //Ask if there is any synapse and how many synapse on the image
    public double askSynapse() {

        Frame superframe = new Frame();
        YesNoCancelDialog gd = new YesNoCancelDialog(superframe, "Any synapse?", "Any synapse?");
        gd.setAlwaysOnTop(true);
        double synapsenum;

        if (gd.yesPressed()) {
            GenericDialog gd1 = new GenericDialog("How many?");
            gd1.addNumericField("Number of synapse...", 1, 0);
            gd1.showDialog();
            synapsenum = gd1.getNextNumber();

        } else {
            synapsenum = 0;
        }

        return synapsenum;
    }

    //Measure synapse and its target on the image
    public void measureSynapse(double synapsenum, RoiManager selection, ImagePlus currentimage, String overlaycolor, ProjectFile projectfile) {

        //Loop for number of synapse = number of measured synapse

        ImageProcessor ip = currentimage.getProcessor();

        for (int i = (int) synapsenum; i > 0; i--) {
            IJ.setTool("line");
            WaitForUserDialog waitdiagsynapse = new WaitForUserDialog("Please measure synapse number " + i + " with line tool");
            waitdiagsynapse.show();
            selection.runCommand(currentimage, "add");
            selection.select(selection.getCount() - 1);
            selection.runCommand("Rename", "Synapse " + i);

            //Get length value of synapse

            if (projectfile.currentrow.getCell(8) != null) {
                projectfile.currentrow = projectfile.currentsheet.createRow(projectfile.currentsheet.getLastRowNum() + 1);

                Cell filenamecell = projectfile.currentrow.createCell(0);
                filenamecell.setCellType(Cell.CELL_TYPE_STRING);
                Cell previousfilenamecell = projectfile.currentsheet.getRow(projectfile.currentsheet.getLastRowNum() - 1).getCell(0);
                filenamecell.setCellValue(previousfilenamecell.getStringCellValue());

                Cell vartypecell = projectfile.currentrow.createCell(1);
                vartypecell.setCellType(Cell.CELL_TYPE_STRING);
                Cell previousvartypecell = projectfile.currentsheet.getRow(projectfile.currentsheet.getLastRowNum() - 1).getCell(1);
                vartypecell.setCellValue(previousvartypecell.getStringCellValue());
            }

            Roi synapse = selection.getRoi(selection.getCount() - 1);
            double synapselength = synapse.getLength() * currentimage.getCalibration().pixelWidth;

            selection.runCommand("Measure");
            selection.runCommand("Set Color", synapsecolor);
            selection.runCommand("Save");
            selection.moveRoisToOverlay(currentimage);

            Cell synapselengthcell = projectfile.currentrow.createCell(8);
            synapselengthcell.setCellType(Cell.CELL_TYPE_NUMERIC);
            synapselengthcell.setCellValue(synapselength);

            //Ask synapse type

            String[] synapsetypelist = new String[3];
            synapsetypelist[0] = "Asymmetric";
            synapsetypelist[1] = "Symmetric";
            synapsetypelist[2] = "Unknown";

            GenericDialog synapsetypediag = new GenericDialog("Synapse type?");
            synapsetypediag.addChoice("Synapse type?", synapsetypelist, "Asymmetric");
            synapsetypediag.showDialog();
            String synapsetype = synapsetypediag.getNextChoice();

            Cell synapsetypecell = projectfile.currentrow.createCell(9);
            synapsetypecell.setCellType(Cell.CELL_TYPE_STRING);
            synapsetypecell.setCellValue(synapsetype);

            //Ask target type

            GenericDialog syntargettype = new GenericDialog("Type of target?");

            String[] targettypelist = new String[5];
            targettypelist[0] = "Spine";
            targettypelist[1] = "Dendrite";
            targettypelist[2] = "Cell body";
            targettypelist[3] = "Axon";
            targettypelist[4] = "Unknown";


            syntargettype.addChoice("Type of target?", targettypelist, "Spine");
            syntargettype.showDialog();
            String targetype = syntargettype.getNextChoice();

            Cell targettypecell = projectfile.currentrow.createCell(10);
            targettypecell.setCellType(Cell.CELL_TYPE_STRING);
            targettypecell.setCellValue(targetype);

            //Get target measures

            IJ.setTool("freehand");
            WaitForUserDialog waittargetmeasure = new WaitForUserDialog("Please measure target of synapse " + i + " with freehand tool");
            waittargetmeasure.show();
            selection.runCommand(currentimage, "add");
            selection.select(selection.getCount() - 1);
            selection.runCommand("Set Color", overlaycolor);
            selection.runCommand("Save");

            ImageStatistics targetstats = ImageStatistics.getStatistics(ip, Measurements.MEAN, currentimage.getCalibration());

            double targetdarea = targetstats.area;

            Cell targetareacell = projectfile.currentrow.createCell(11);
            targetareacell.setCellType(Cell.CELL_TYPE_NUMERIC);
            targetareacell.setCellValue(targetdarea);

            EllipseFitter targetellipse = new EllipseFitter();
            targetellipse.fit(ip, targetstats);
            targetellipse.drawEllipse(ip);

            //Long axis
            double targetellipsemajor = targetellipse.major * currentimage.getCalibration().pixelWidth;
            Cell majorcell = projectfile.currentrow.createCell(12);
            majorcell.setCellType(Cell.CELL_TYPE_NUMERIC);
            majorcell.setCellValue(targetellipsemajor);

            //Short axis
            double targetellipseminor = targetellipse.minor * currentimage.getCalibration().pixelWidth;
            Cell minorcell = projectfile.currentrow.createCell(13);
            minorcell.setCellType(Cell.CELL_TYPE_NUMERIC);
            minorcell.setCellValue(targetellipseminor);

            //Diameter
            Cell diametertargetcell = projectfile.currentrow.createCell(14);
            diametertargetcell.setCellType(Cell.CELL_TYPE_NUMERIC);
            diametertargetcell.setCellValue((targetellipsemajor + targetellipseminor) / 2);

            ip.createImage();
            System.out.println("Traced synapse and target successfully measured");
        }


    }

    //Places a random marker on the image to identify unlabeled varicosity
    public void placeStereologycross (ImagePlus currentimage) {

        int imagewidth = currentimage.getWidth();
        int imageheigth = currentimage.getHeight();

        Random coordinategenerator = new Random();

        Roi stereorectangle = new Roi(coordinategenerator.nextInt(imagewidth), coordinategenerator.nextInt(imageheigth), 50, 50);
        stereorectangle.setFillColor(Color.RED);
        stereorectangle.setStrokeColor(Color.RED);

        currentimage.setRoi(stereorectangle);



    }

}
