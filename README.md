# TEM-Analysis Studio (TAS) <h1>

# Why? <h2>
Our lab was using imageJ for ultrastructural analysis of TEM image. The old protocol was tedious, slow (manual copy and
paste in an excel workbook) required a printing of each images in order to keep trace of analysed images and measured 
objects. To see an exemple of data generated with this kind of measurements you can read this [article](http://www.ncbi.nlm.nih.gov/pubmed/?term=26462663) This simple Java project is using the ImageJ API and Apache POI to automatize all the data entry in an excel 
excel workbook. Furthermore, it guides you through all the steps of the measurements with comprehensive dialogs and
possibility to load an existing project. This project in still undergoing development with a lot of ambitious features to
add for a noob in programming like me. 

# How to use it <h2>
1. You will be asked if this is a new project:

    * If it's a new project, you will be prompted to select a save path
        * Only XLSX extension will be accepted
    * If you are loading an existing project
        * Only XLSX extension will be accepted
        * Only file with the correct header will be accepted (excel file created with this script)
    * When saving:
        * The output file will be a XLSX file so name it accordingly

2. You will need to select the images to analyse:
    * Multiple selection is enabled
        * There is no limit in the number of images you can select
        * Only TIF and JPEG files will be accepted
        
3. You will start the measurements workflow:
    * It is very important to set the scalebar on the first image to be measured or you will get the default image calibration
    from imageJ. Measurements will be invalid.
    * When you are prompted to measure something:
        * It's a "wait for user" dialog, so click "ok" only when you are done with the measurements
        * You can take your time and re-do measurements as many times as deemed necessary.
    * All measured images with their overlay (measured contours) will be saved in a folder linked to the saved project file

4. When all the selected images are measured:
    * A dialog will ask you if you want to continue with another set of images or quit

# To do list <h2>

- [x] Working measurements workflow of TEM images
- [x] Load and save measurements in excel format
- [x] Save a copy of measured image with overlays in another folder
- [ ] Automatic calculation of mean value for the project (probably placed in another sheet)
- [ ] Automatic calculation of synaptic incidence with formula from [Beaudet and Sotelo (1981)](http://www.ncbi.nlm.nih.gov/pubmed/7214137) 
- [ ] Create a working GUI in javaFX (some issues with integration of imageJ inside a JavaFX application)
    - [ ] Exit button 
    - [ ] Return to previous image button
    - [ ] Live count display of measured images in the 
    - [ ] Look at current excel workbook button
    - [ ] Help button (opens read me or other docs)

# GUI issues personal notes <h2>

* On hold, can't seem to be able to correctly launch imagej inside a JavaFX application.
* Each time a dialog is launched by imagej it seems to crash
* Workflow heavily relies on those IJ.dialog to work properly, migration of the whole code to javaFX dialog would probably fix the issue
* This issue will be resolved in the next version... (hopefully)