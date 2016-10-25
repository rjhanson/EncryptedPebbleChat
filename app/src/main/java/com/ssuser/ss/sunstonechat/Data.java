/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ssuser.ss.sunstonechat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

/**
 *
 * @author fpatton
 */
public class Data {
    public static final Integer[] baseList = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
    
    public List<Integer> shuffleBaseList() 
    {
        List<Integer> tmpflat = new ArrayList<Integer>();
        tmpflat.addAll(Arrays.asList(baseList));
        long seed = System.nanoTime();
        Collections.shuffle(tmpflat, new Random(seed));
        return tmpflat;
    }
    
    // originShift: returns next origin for the puzzle...
    public int originShift(List<Integer> puzzle, int userListElem, int shiftCount) {

        List<Integer> tmppuzzle = new ArrayList<Integer>();
        tmppuzzle.addAll(puzzle);
        int shiftTmp = 0;
        int locationTmp = 0;

        shiftTmp = maxStepper(tmppuzzle, userListElem % 16);
        shiftTmp = minStepper(tmppuzzle, shiftTmp % 16);
        shiftTmp = maxStepper(tmppuzzle, shiftTmp % 16); 
        shiftTmp = minStepper(tmppuzzle, shiftTmp % 16);

        //Razz-ul-Dazzle
        shiftTmp = shiftTmp + tmppuzzle.get( 6 );

        // change "puzzle": find element and move to the end             
        locationTmp = puzzle.indexOf(shiftTmp % 16);
        puzzle.remove(locationTmp);        
        puzzle.add(puzzle.size(), shiftTmp % 16);

        // take turns being the "0" !
        for (ListIterator<Integer> i = puzzle.listIterator(); i.hasNext();) {
                i.set((3 + (int) i.next()) % 16);
        } 
//        System.out.println("\t\tnew puzzle: " + puzzle);
//        System.out.println("\t\treturn\t"+shiftTmp);
                   
        // the final return           
        return shiftTmp;
    }
    
    // max stepper: returns the index of the puzzle element maxStep lands on...
    public int maxStepper(List<Integer> puzzle, int find) {
        List<Integer> tmppuzzle = new ArrayList<Integer>();
        int blockNum;
        int maxIndex;
        int nextElemIndex = 0;

        tmppuzzle.addAll(puzzle);

        //find an element of userList in puzzleList
        //find the index of the block of four associated with it
        blockNum = (int) tmppuzzle.indexOf(find) / 4;
        // four blocks of four in the puzzleList: 0, 1, 2, 3
        // global index of the --shifted--fourBlock's Max element
        maxIndex = tmppuzzle.indexOf(Collections.max(fourChunk(tmppuzzle, blockNum)));
        // Razz-Ma-Tazz !!
        maxIndex = maxIndex + tmppuzzle.get( (maxIndex + 0) % 16 );
        // the next step element
        nextElemIndex = (maxIndex + find ) % 16;

        // clear the temp puzzle
        tmppuzzle.clear();

        return nextElemIndex;
    }

    // min stepper: returns the index of the puzzle element minStep lands on...
    public int minStepper(List<Integer> puzzle, int find) {

        List<Integer> tmppuzzle = new ArrayList<Integer>();
        int blockNum;
        int minIndex;
        int nextElemIndex = 0;

        tmppuzzle.addAll(puzzle);

        //find an element of userList in puzzleList
        //find the index of the block of four associated with it
        blockNum = (int) tmppuzzle.indexOf(find) / 4;
        // four blocks of four in the puzzleList: 0, 1, 2, 3
        // global index of the --shifted-- fourBlock's Min element
        minIndex = tmppuzzle.indexOf(Collections.min(fourChunk(tmppuzzle, blockNum)));
        // Razz-Ma-Tazz !!
        minIndex = minIndex + tmppuzzle.get( (minIndex + 7) % 16 );
        // the next step element
        nextElemIndex = (minIndex + find) % 16;

        tmppuzzle.clear();
        //System.out.println(find + "  " + minIndex);
            
        return nextElemIndex;
    }
    
    public List<Integer> fourChunk(List<Integer> flat, int chunk) {
        List<Integer> tmpchunk = new ArrayList<Integer>();
        tmpchunk = flat.subList(4 * chunk, 4 * chunk + 4);
        return tmpchunk;
    }
    
    public static int[] getByteValsFromInt(int iVal, int numBytes)
    {
        String hxStr = Integer.toHexString(iVal);
        int[] bytes = new int[numBytes];
        
        int byteIndex = numBytes - 1;
        for(int i =  hxStr.length()-1; i >= 0 && byteIndex >= 0; --i)
        {
            bytes[byteIndex] = Character.getNumericValue(hxStr.charAt(i));
            --byteIndex;
        }
        for(int i = byteIndex; i >= 0; --i)
        {
            bytes[i] = (byte)0;
        }
        
        return bytes;
    }
    
    public static int[] splitByteToIntHalfVals(byte bVal)
    {
        int[] vals = new int[2];
        
        vals[0] = (bVal & 0xF0) >>> 4;
        vals[1] = (bVal & 0x0F);
        return vals;
    }
    
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
