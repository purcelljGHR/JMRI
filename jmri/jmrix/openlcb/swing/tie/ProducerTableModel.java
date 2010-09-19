// ProducerTableModel.java

package jmri.jmrix.openlcb.swing.tie;

import jmri.util.davidflanagan.HardcopyWriter;
import jmri.jmrix.maple.SerialTrafficController;
import jmri.jmrix.maple.SerialNode;
import jmri.jmrix.maple.SerialAddress;

import jmri.jmrix.AbstractNode;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.io.IOException;

import java.util.ResourceBundle;

import javax.swing.border.Border;
import javax.swing.*;
import javax.swing.table.*;

import java.lang.Integer;

/**
 * Table Model for access to producer info
 * @author	 Bob Jacobsen 2008
 * @version	 $Revision: 1.2 $
 * @since 2.3.7
 */

public class ProducerTableModel extends AbstractTableModel {
    static    ResourceBundle rb = ResourceBundle.getBundle("jmri.jmrix.openlcb.swing.tie.TieBundle");

    
    public static final int USERNAME_COLUMN = 0;
    public static final int NODE_COLUMN = 1;
    public static final int NUMBER_COLUMN = 2;
    String[] columnName  = new String[]{"User Name", "Node", "Event"};

    public String getColumnName(int c) {
        return columnName[c];
    }
    public Class<?> getColumnClass(int c) {
        return String.class;
    }
    public boolean isCellEditable(int r,int c) { return false; }
    public int getColumnCount () {
        return columnName.length;
    }
    public int getRowCount () {
        return dummy.length;
    }
    public Object getValueAt (int r,int c) {
        return dummy[r][c];  // for testing
    }
    public void setValueAt(Object type,int r,int c) {
        // nothing is stored here
    }
    
    String[][] dummy = {{"East Lower Yard Button 1", "12", "1"}, // row then column
                        {"East Lower Yard Button 2", "12", "2"}, 
                        {"East Lower Yard Button 3", "12", "3"},
                        {"East Lower Yard Button 4", "12", "4"},
                        {"East Lower Yard Button 5", "12", "5"},
                        {"West Lower Yard Button 1", "14", "5"},
                        {"West Lower Yard Button 2", "14", "4"},
                        {"West Lower Yard Button 3", "14", "3"},
                        {"West Lower Yard Button 4", "14", "2"},
                        {"West Lower Yard Button 5", "14", "1"},
                    }; 
    
    /**
    * Method to print or print preview the assignment table.
    * Printed in proportionately sized columns across the page with headings and
    * vertical lines between each column. Data is word wrapped within a column.
    * Can only handle 4 columns of data as strings.
    * Adapted from routines in BeanTableDataModel.java by Bob Jacobsen and Dennis Miller
    */
    public void printTable(HardcopyWriter w,int colWidth[]) {
        // determine the column sizes - proportionately sized, with space between for lines
        int[] columnSize = new int[4];
        int charPerLine = w.getCharactersPerLine();
        int tableLineWidth = 0;  // table line width in characters
        int totalColWidth = 0;
        for (int j = 0; j < 4; j++) {
            totalColWidth += colWidth[j];
        }
        float ratio = ((float)charPerLine)/((float)totalColWidth);
        for (int j = 0; j < 4; j++) {
            columnSize[j] = (int)Math.round((((float)colWidth[j])*ratio) - 1.);
            tableLineWidth += (columnSize[j] + 1);
        }
    
        // Draw horizontal dividing line
        w.write(w.getCurrentLineNumber(), 0, w.getCurrentLineNumber(),
            tableLineWidth);
    
        // print the column header labels
        String[] columnStrings = new String[4];
        // Put each column header in the array
        for (int i = 0; i < 4; i++){
            columnStrings[i] = this.getColumnName(i);
        }
        w.setFontStyle(Font.BOLD);
        printColumns(w, columnStrings, columnSize);
        w.setFontStyle(0);
        // draw horizontal line
        w.write(w.getCurrentLineNumber(), 0, w.getCurrentLineNumber(),
                    tableLineWidth);

        // now print each row of data
        String[] spaces = new String[4];
        // create base strings the width of each of the columns
        for (int k = 0; k < 4; k++) {
            spaces[k] = "";
            for (int i = 0; i < columnSize[k]; i++) {
                spaces[k] = spaces[k] + " ";
            }
        }
        for (int i = 0; i < this.getRowCount(); i++) {
            for (int j = 0; j < 4; j++) {
                //check for special, null contents
                if (this.getValueAt(i, j) == null) {
                    columnStrings[j] = spaces[j];
                } 
                else {
                    columnStrings[j] = (String) this.getValueAt(i, j);
                }
            }
            printColumns(w, columnStrings, columnSize);
            // draw horizontal line
            w.write(w.getCurrentLineNumber(), 0, w.getCurrentLineNumber(),
                    tableLineWidth);
        }            
        w.close();
    }

    protected void printColumns(HardcopyWriter w, String columnStrings[], int columnSize[]) 
    {
        String columnString = "";
        String lineString = "";
        String[] spaces = new String[4];
        // create base strings the width of each of the columns
        for (int k = 0; k < 4; k++) {
            spaces[k] = "";
            for (int i = 0; i < columnSize[k]; i++) {
                spaces[k] = spaces[k] + " ";
            }
        }
        // loop through each column
        boolean complete = false;
        while (!complete){
            complete = true;
            for (int i = 0; i < 4; i++) {
                // if the column string is too wide cut it at word boundary (valid delimiters are space, - and _)
                // use the initial part of the text,pad it with spaces and place the remainder back in the array
                // for further processing on next line
                // if column string isn't too wide, pad it to column width with spaces if needed
                if (columnStrings[i].length() > columnSize[i]) {
                    // this column string will not fit on one line
                    boolean noWord = true;
                    for (int k = columnSize[i]; k >= 1 ; k--) {
                        if (columnStrings[i].substring(k-1,k).equals(" ") 
                                || columnStrings[i].substring(k-1,k).equals("-")
                                || columnStrings[i].substring(k-1,k).equals("_")) {
                            columnString = columnStrings[i].substring(0,k) 
                                    + spaces[i].substring(columnStrings[i].substring(0,k).length());
                            columnStrings[i] = columnStrings[i].substring(k);
                            noWord = false;
                            complete = false;
                            break;
                        }
                    }
                    if (noWord) {
                        columnString = columnStrings[i].substring(0,columnSize[i]);
                        columnStrings[i] = columnStrings[i].substring(columnSize[i]);
                        complete = false;
                    }                    
                }	
                else {
                    // this column string will fit on one line
                    columnString = columnStrings[i] + spaces[i].substring(columnStrings[i].length());
                    columnStrings[i] = "";
                }
                lineString = lineString + columnString + " ";
            }
            try {
                w.write(lineString);
                //write vertical dividing lines
                int iLine = w.getCurrentLineNumber();
                for (int i = 0, k = 0; i < w.getCharactersPerLine(); k++) {
                    w.write( iLine, i, iLine + 1, i);
                    if (k<4) {
                        i = i+columnSize[k]+1;
                    }
                    else {
                        i = w.getCharactersPerLine();
                    }
                }
                lineString = "\n";
                w.write(lineString);
                lineString = "";
            } 
            catch (IOException e) { 
                log.warn("error during printing: "+e);
            }
        }
    }

    static org.apache.log4j.Category log = org.apache.log4j.Logger.getLogger(ProducerTableModel.class.getName());
	
}

/* @(#)ProducerTableModel.java */
