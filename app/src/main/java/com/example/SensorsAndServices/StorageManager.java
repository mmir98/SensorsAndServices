package com.example.SensorsAndServices;

import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class StorageManager {

    public static boolean writeOnMemory(String fileDirectory, String fileName, JSONObject jsonObject) {
        try {
            String fileBody = jsonObject.toString();
            File root = new File(fileDirectory);
            if (!root.exists()) {
                root.mkdirs();
            }
            File jsonFile = new File(root, fileName);
            FileWriter writer = new FileWriter(jsonFile);
            writer.append(fileBody);
            writer.flush();
            writer.close();
            Log.i("SaveFileCompletedTag", fileName + " file saved in" + fileDirectory);
//            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
//            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
            Log.i("SaveFileErrorTag", e.toString());
            return false;
        }
    }


    public static String readFromMemory (String fileDirectory, String fileName) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        BufferedReader in = null;

        File jsonFile = new File(fileDirectory, fileName);
        FileReader fileReader = new FileReader(jsonFile);

        in = new BufferedReader(fileReader);
        while ((line = in.readLine()) != null) stringBuilder.append(line);



        Log.i("ReadFileCompletedTag", "reading file from " + fileDirectory + " completed successfully");

        return stringBuilder.toString();
    }

    public boolean deleteFromMemory (String fileDirectory, String fileName){

        File file = new File(fileDirectory, fileName);
        return file.delete();
    }

}





