package com.breelig.scfsmon;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class BgLogger {
    private File logFile =  null;
    private BufferedWriter logWriter = null;
    public static BgLogger Logger = new BgLogger();
    private static boolean LogToFile = true;

    public BgLogger() {

        if(LogToFile) {
            if (logWriter != null)
                return;

            File sdCard = Environment.getExternalStorageDirectory();
            logFile = new File(sdCard.getAbsolutePath() + "/ScFsMon_Log.txt");
            if (!logFile.exists()) {
                try {
                    logFile.createNewFile();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            try {
                logWriter = new BufferedWriter(new FileWriter(logFile, true));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void Info(String text) {


        if(LogToFile && logWriter != null) {
            try {
                logWriter.append(text);
                logWriter.newLine();
                logWriter.flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            Log.i("BgLogger", text);
        }
    }

    public void Close()
    {
        if(LogToFile) {
            try {
                Info("Closing log file.");
                logWriter.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
