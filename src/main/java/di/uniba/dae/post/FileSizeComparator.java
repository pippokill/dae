/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.dae.post;

import java.io.File;
import java.util.Comparator;

/**
 *
 * @author pierpaolo
 */
public class FileSizeComparator implements Comparator<File> {

    @Override
    public int compare(File o1, File o2) {
        return Long.compare(o1.length(), o2.length());
    }
    
}
