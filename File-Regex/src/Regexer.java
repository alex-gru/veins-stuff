import java.io.File;
import java.io.FilenameFilter;

/**
 * User: alexgru
 * This class provides some snippets, which were needed frequently, e.g. naming conventions etc..
 */
public class Regexer {
    public static void main(String[] args) {

        String dirName = "E:\\Bak\\Database\\ROI-Datenbank\\links\\";
        File dir = new File(dirName);

        File newFile = null;
        String newFileName = null;

        FilenameFilter fileNameFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jpg");
            }
        };



//        int[] indexImageOfID = new int[150];
//        for (int i = 0; i < indexImageOfID.length; i++) {
//            indexImageOfID[i] = 1;
//        }

        for (final File currentFile : dir.listFiles(fileNameFilter)) {
            System.out.println(currentFile.getName());
//            newFileName = currentFile.getName().replace("durchlicht", "auflicht");
//            newFile = new File(newFileName);
//            boolean success = currentFile.renameTo(newFile);
//            if (success) {
//                System.out.println("OK");
//            } else {
//                System.err.println("ERROR");
//            }


//            String[] split = currentFile.getName().split("_");
//            int id = 9999999;
//            boolean auflicht = false;
//            int indexOfImagesOfID;
//
//            for (int i = 0; i < split.length; i++) {
//                String currSplit = split[i];
//                System.out.println(currSplit);
//                if (currSplit.startsWith("ID")) {
//                    id = Integer.parseInt(currSplit.substring("ID".length()));
//                    System.out.println("ID: " + id);
//                } else if (currSplit.equals("AUFLICHT")) {  //auflicht, durchlicht
//                    auflicht = true;
//                } else if (currSplit.equals("DURCHLICHT")) {  //auflicht, durchlicht
//                    auflicht = false;
//                }
//            }
//
//            indexOfImagesOfID = indexImageOfID[id];
//            indexImageOfID[id]++;
//            newFileName = dirName + id + "_" + indexOfImagesOfID;
//
//            if (auflicht) {
//                newFileName += "_auflicht.JPG";
//                System.out.println("New name: " + newFileName);
//                newFile = new File(newFileName);
//            } else {
//                newFileName += "_durchlicht.JPG";
//                System.out.println("New name: " + newFileName);
//                newFile = new File(newFileName);
//            }
//
//            boolean success = currentFile.renameTo(newFile);
//            if (success) {
//                System.out.println("OK");
//            } else {
//                System.err.println("ERROR");
//            }
//            newFile = new File(currentFile.getName().replace("_0_","_1_") + ".JPG");

//            newFile = new File(currentFile.getName().replace(".JPG.JPG", ".JPG"));
//            currentFile.renameTo(newFile);
        }
    }
}
